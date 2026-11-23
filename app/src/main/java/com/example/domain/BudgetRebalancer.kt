package com.example.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Deterministic Priority-Weighted Wedding Budget Optimization Engine (RF-03, RF-05).
 * Never delegates arithmetic to an LLM; guarantees sum(allocations) <= totalBudget
 * and strictly preserves spent amounts and locked categories.
 */
data class CategoryBudgetRange(
    val category: String,
    val priorityRank: Int,
    val currentAllocated: Double,
    val suggestedMin: Double,
    val suggestedIdeal: Double,
    val suggestedMax: Double,
    val delta: Double,
    val locked: Boolean
)

data class RebalanceProposal(
    val totalBudget: Double,
    val totalSpent: Double,
    val totalProposed: Double,
    val remainingUnallocated: Double,
    val focusCategories: List<String>,
    val ranges: List<CategoryBudgetRange>,
    val healthScore: Int,
    val warnings: List<String>
)

object BudgetRebalancer {

    // Baseline industry share weights before priority amplification
    private val baseShareWeights: Map<String, Double> = mapOf(
        "Venue" to 0.24,
        "Catering" to 0.26,
        "Photography" to 0.13,
        "Florals" to 0.11,
        "Music" to 0.09,
        "Attire" to 0.08,
        "Stationery" to 0.04,
        "Coordination" to 0.05
    )

    /**
     * Generates initial priority-weighted allocations during bridal onboarding (RF-03).
     * Top-ranked categories get up to +42% boost; lower-ranked categories gently compress.
     */
    fun computeInitialAllocations(
        totalBudget: Double,
        priorityOrder: List<String>
    ): List<BudgetAllocation> {
        val categories = priorityOrder.ifEmpty { DefaultWeddingCatalog.CATEGORIES }
        val n = categories.size.coerceAtLeast(1)
        val rawWeights = categories.mapIndexed { index, cat ->
            val base = baseShareWeights[cat] ?: (1.0 / n)
            val ratio = if (n > 1) index.toDouble() / (n - 1).toDouble() else 0.0
            val multiplier = 1.42 - (0.74 * ratio)
            cat to (base * multiplier)
        }
        val weightSum = rawWeights.sumOf { it.second }.coerceAtLeast(0.0001)

        var runningTotal = 0.0
        return rawWeights.mapIndexed { index, (cat, w) ->
            val amount = if (index == rawWeights.lastIndex) {
                max(0.0, roundToFifty(totalBudget - runningTotal))
            } else {
                val computed = roundToFifty(totalBudget * (w / weightSum))
                runningTotal += computed
                computed
            }
            val rankLabel = when {
                index <= 1 -> "High Priority"
                index <= 4 -> "Medium Priority"
                else -> "Flexible Priority"
            }
            BudgetAllocation(
                category = cat,
                allocatedAmount = amount,
                spentAmount = 0.0,
                priorityRank = (index + 1).toLong(),
                locked = false,
                notes = rankLabel
            )
        }
    }

    /**
     * Deterministic budget reallocation triggered by the AI Assistant or user optimization (RF-05).
     * Boosts requested focus categories according to priority ranking while pulling proportionally
     * from lower-priority unlocked categories, strictly enforcing totalProposed <= totalBudget
     * and proposed >= spentAmount.
     */
    fun rebalanceForCategories(
        totalBudget: Double,
        priorityOrder: List<String>,
        currentAllocations: List<BudgetAllocation>,
        focusCategories: List<String>,
        boostPercent: Double = 0.16
    ): RebalanceProposal {
        val safeTotal = totalBudget.coerceAtLeast(0.0)
        val ordered = if (currentAllocations.isNotEmpty()) {
            currentAllocations.sortedBy { it.priorityRank }
        } else {
            computeInitialAllocations(safeTotal, priorityOrder)
        }

        val normalizedFocus = focusCategories.map { it.trim() }.filter { it.isNotBlank() }
        val lockedSum = ordered.filter { it.locked }.sumOf { max(it.allocatedAmount, it.spentAmount) }
        val unlocked = ordered.filter { !it.locked }
        val availableForUnlocked = max(0.0, safeTotal - lockedSum)

        val warnings = mutableListOf<String>()
        val totalSpent = ordered.sumOf { it.spentAmount }
        if (totalSpent > safeTotal) {
            warnings.add("Committed spending exceeds your total budget by ${formatCurrency(totalSpent - safeTotal)}.")
        }

        val unlockedMinFloors = unlocked.associate { it.category to it.spentAmount }
        val minFloorsSum = unlockedMinFloors.values.sum()
        val redistributableHeadroom = max(0.0, availableForUnlocked - minFloorsSum)

        val n = ordered.size.coerceAtLeast(1)
        val desiredWeights = unlocked.associate { alloc ->
            val rankIdx = (alloc.priorityRank.toInt() - 1).coerceIn(0, n - 1)
            val base = baseShareWeights[alloc.category] ?: (1.0 / n)
            val priorityFactor = 1.45 - (0.75 * (rankIdx.toDouble() / max(1, n - 1).toDouble()))
            val isFocus = normalizedFocus.any { it.equals(alloc.category, ignoreCase = true) }
            val focusFactor = if (isFocus) (1.0 + boostPercent) else 1.0
            alloc.category to (base * priorityFactor * focusFactor)
        }
        val weightTotal = desiredWeights.values.sum().coerceAtLeast(0.0001)

        var allocatedSum = lockedSum
        val proposalsMap = mutableMapOf<String, Double>()

        ordered.filter { it.locked }.forEach {
            proposalsMap[it.category] = max(it.allocatedAmount, it.spentAmount)
        }

        unlocked.forEachIndexed { idx, alloc ->
            val floor = unlockedMinFloors[alloc.category] ?: 0.0
            val share = if (idx == unlocked.lastIndex) {
                max(0.0, safeTotal - allocatedSum - floor)
            } else {
                roundToFifty(redistributableHeadroom * ((desiredWeights[alloc.category] ?: 0.0) / weightTotal))
            }
            val proposed = min(
                max(floor, floor + share),
                max(floor, safeTotal - allocatedSum)
            )
            val finalAmount = roundToFifty(proposed).coerceAtLeast(floor)
            proposalsMap[alloc.category] = finalAmount
            allocatedSum += finalAmount
        }

        // Strict integrity clamp: sum never exceeds totalBudget
        val sumNow = proposalsMap.values.sum()
        if (sumNow > safeTotal && unlocked.isNotEmpty()) {
            val excess = sumNow - safeTotal
            val donor = unlocked.maxByOrNull { (proposalsMap[it.category] ?: 0.0) - it.spentAmount }
            if (donor != null) {
                val cur = proposalsMap[donor.category] ?: 0.0
                proposalsMap[donor.category] = max(donor.spentAmount, cur - excess)
            }
        }

        val ranges = ordered.map { alloc ->
            val ideal = proposalsMap[alloc.category] ?: alloc.allocatedAmount
            val minVal = max(alloc.spentAmount, roundToFifty(ideal * 0.85))
            val maxVal = min(safeTotal, roundToFifty(ideal * 1.15))
            CategoryBudgetRange(
                category = alloc.category,
                priorityRank = alloc.priorityRank.toInt(),
                currentAllocated = alloc.allocatedAmount,
                suggestedMin = minVal,
                suggestedIdeal = ideal,
                suggestedMax = maxVal,
                delta = ideal - alloc.allocatedAmount,
                locked = alloc.locked
            )
        }

        val finalTotal = ranges.sumOf { it.suggestedIdeal }
        val health = computeBudgetHealthScore(safeTotal, ordered)
        if (finalTotal > safeTotal) {
            warnings.add("Locked categories prevent reducing the total budget further.")
        }

        return RebalanceProposal(
            totalBudget = safeTotal,
            totalSpent = totalSpent,
            totalProposed = finalTotal,
            remainingUnallocated = max(0.0, safeTotal - finalTotal),
            focusCategories = normalizedFocus,
            ranges = ranges,
            healthScore = health,
            warnings = warnings
        )
    }

    fun computeBudgetHealthScore(totalBudget: Double, allocations: List<BudgetAllocation>): Int {
        if (totalBudget <= 0.0 || allocations.isEmpty()) return 65
        val allocated = allocations.sumOf { it.allocatedAmount }
        val spent = allocations.sumOf { it.spentAmount }
        var score = 100
        if (allocated > totalBudget * 1.02) score -= 25
        if (spent > totalBudget) score -= 35
        val overCategories = allocations.count { it.spentAmount > it.allocatedAmount && it.allocatedAmount > 0 }
        score -= overCategories * 10
        if (allocated < totalBudget * 0.80) score -= 8
        return score.coerceIn(10, 100)
    }

    fun roundToFifty(value: Double): Double = round(value / 50.0) * 50.0

    fun formatCurrency(amount: Double): String {
        val rounded = round(amount).toLong()
        return "$" + "%,d".format(rounded)
    }
}
