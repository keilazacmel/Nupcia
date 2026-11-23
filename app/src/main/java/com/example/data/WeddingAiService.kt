package com.example.data

import android.util.Log
import com.example.domain.BudgetAllocation
import com.example.domain.BudgetRebalancer
import com.example.domain.DefaultWeddingCatalog
import com.example.domain.GeoMatchEngine
import com.example.domain.RebalanceProposal
import com.example.domain.ScoredVendor
import com.example.domain.VendorProfile
import com.example.domain.Wedding
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val AI_TAG = "WeddingAiService"

data class AiAssistantReply(
    val message: String,
    val impactSummary: String,
    val proposal: RebalanceProposal?,
    val recommendedVendors: List<ScoredVendor>
)

/**
 * Firebase AI Logic (gemini-3.8-flash) Bridal Budget & Vendor Concierge (RF-04, RF-05, RF-06).
 * Deterministic math is ALWAYS calculated by BudgetRebalancer BEFORE calling Gemini;
 * Gemini receives the exact verified numbers + matching Firestore vendors and returns
 * structured JSON advice and selected vendor IDs.
 */
class WeddingAiService {

    private val responseSchema = Schema.obj(
        properties = mapOf(
            "assistantMessage" to Schema.string(
                description = "Warm, editorial, practical response in English addressed to the couple."
            ),
            "impactSummary" to Schema.string(
                description = "Brief 1-2 sentence summary explaining the financial trade-off across categories while honoring their top priorities."
            ),
            "shouldOfferRebalance" to Schema.boolean(
                description = "true if showing the one-tap budget rebalance action card is helpful."
            ),
            "recommendedVendorIds" to Schema.array(
                items = Schema.string(),
                description = "List of up to 3 real vendor IDs provided in the context that best match."
            )
        )
    )

    // Lazy initialization prevents startup evaluation before App Check is installed
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@WeddingAiService.responseSchema
                temperature = 0.4f
            }
        )
    }

    suspend fun askConcierge(
        userPrompt: String,
        wedding: Wedding?,
        allocations: List<BudgetAllocation>,
        allVendors: List<VendorProfile>
    ): Result<AiAssistantReply> {
        return try {
            // 1. Detect focus categories mentioned by the couple
            val detectedCategories = detectCategoriesFromPrompt(userPrompt, wedding?.priorityOrder.orEmpty())

            // 2. Run deterministic budget math (zero LLM arithmetic!)
            val proposal = BudgetRebalancer.rebalanceForCategories(
                totalBudget = wedding?.totalBudget ?: 35000.0,
                priorityOrder = wedding?.priorityOrder ?: DefaultWeddingCatalog.CATEGORIES,
                currentAllocations = allocations,
                focusCategories = detectedCategories
            )

            // 3. Score real vendors from Firestore against the newly calculated ideal ranges
            val proposedAllocations = allocations.map { alloc ->
                val ideal = proposal.ranges.firstOrNull { it.category == alloc.category }?.suggestedIdeal
                    ?: alloc.allocatedAmount
                alloc.copy(allocatedAmount = ideal)
            }
            val scoredVendors = allVendors
                .map { GeoMatchEngine.scoreVendor(it, wedding, proposedAllocations) }
                .filter { sv ->
                    detectedCategories.isEmpty() ||
                        detectedCategories.any { it.equals(sv.vendor.category, ignoreCase = true) }
                }
                .sortedByDescending { it.affinityScore }
                .take(5)

            // 4. Build concise rich context for Gemini 3.8 Flash
            val prompt = buildString {
                appendLine("You are Nupcia's Editorial Wedding Planning & Budget Concierge serving Utah County, Utah. Always reply in warm, clear, concise English.")
                appendLine("CRITICAL RULE: Never invent numbers or vendors. Use EXCLUSIVELY the verified deterministic calculations and real Firestore vendors listed below.")
                appendLine()
                appendLine("COUPLE PROFILE:")
                appendLine("- Couple: ${wedding?.brideName ?: "Bride"} & ${wedding?.partnerName ?: "Partner"}")
                appendLine("- Date: ${wedding?.weddingDate ?: "TBD"} | Location: ${wedding?.city ?: "Provo, UT"} | Guests: ${wedding?.estimatedGuests ?: 140}")
                appendLine("- Styles: ${wedding?.styleTags?.joinToString(", ") ?: "Classic"}")
                appendLine("- Vision: ${wedding?.visionExpectations?.ifBlank { "Timeless elegance and heartfelt moments" } ?: "Timeless elegance"}")
                appendLine("- Priority Ranking (1 = highest): ${wedding?.priorityOrder?.joinToString(" > ") ?: ""}")
                appendLine("- Total Budget: ${BudgetRebalancer.formatCurrency(proposal.totalBudget)} | Committed: ${BudgetRebalancer.formatCurrency(proposal.totalSpent)}")
                appendLine()
                appendLine("DETERMINISTIC REBALANCE CALCULATION (verified, sum <= total):")
                proposal.ranges.forEach { r ->
                    val sign = if (r.delta >= 0) "+" else ""
                    appendLine("- #${r.priorityRank} ${r.category}: current ${BudgetRebalancer.formatCurrency(r.currentAllocated)} -> ideal ${BudgetRebalancer.formatCurrency(r.suggestedIdeal)} (range ${BudgetRebalancer.formatCurrency(r.suggestedMin)} - ${BudgetRebalancer.formatCurrency(r.suggestedMax)}, delta $sign${BudgetRebalancer.formatCurrency(r.delta)})")
                }
                appendLine()
                appendLine("REAL MATCHING VENDORS IN FIRESTORE:")
                if (scoredVendors.isEmpty()) {
                    appendLine("- No published vendors found yet for this category.")
                } else {
                    scoredVendors.forEach { sv ->
                        val v = sv.vendor
                        appendLine("- ID=${v.id} | ${v.businessName} (${v.category}, ${v.city}) | Price: ${BudgetRebalancer.formatCurrency(v.minPrice)}-${BudgetRebalancer.formatCurrency(v.maxPrice)} | Match: ${sv.affinityScore}% | Distance: ${"%.1f".format(sv.distanceMiles)} mi | Covers venue: ${sv.coversWeddingLocation}")
                    }
                }
                appendLine()
                appendLine("COUPLE MESSAGE:")
                appendLine(userPrompt)
            }

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("The AI assistant returned an empty response.")

            // Direct JSON parsing (responseSchema guarantees clean JSON without markdown fences)
            val json = JSONObject(rawJson)
            val assistantMsg = json.optString("assistantMessage", "").ifBlank {
                "I recalculated your wedding budget based on your priorities without exceeding your total."
            }
            val impactSummary = json.optString("impactSummary", "")
            val offerRebalance = json.optBoolean("shouldOfferRebalance", true)
            val idsArray = json.optJSONArray("recommendedVendorIds")
            val pickedIds = mutableListOf<String>()
            if (idsArray != null) {
                for (i in 0 until idsArray.length()) {
                    pickedIds.add(idsArray.optString(i))
                }
            }

            val matchedVendors = if (pickedIds.isNotEmpty()) {
                val byId = scoredVendors.associateBy { it.vendor.id }
                pickedIds.mapNotNull { byId[it] }.ifEmpty { scoredVendors.take(3) }
            } else {
                scoredVendors.take(3)
            }

            Result.success(
                AiAssistantReply(
                    message = assistantMsg,
                    impactSummary = impactSummary,
                    proposal = if (offerRebalance) proposal else null,
                    recommendedVendors = matchedVendors
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Gemini generation timed out", e)
            Result.failure(Exception("The AI concierge took more than 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Firebase AI generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Could not reach the AI concierge."))
        }
    }

    private fun detectCategoriesFromPrompt(prompt: String, priorities: List<String>): List<String> {
        val lower = prompt.lowercase()
        val keywords = mapOf(
            "Venue" to listOf("venue", "estate", "garden", "canyon", "lodge", "hall", "barn", "location"),
            "Catering" to listOf("catering", "food", "dinner", "menu", "cake", "banquet", "bar"),
            "Photography" to listOf("photo", "photographer", "photography", "video", "film", "camera", "editorial"),
            "Florals" to listOf("floral", "flower", "bouquet", "decor", "centerpiece", "arch"),
            "Music" to listOf("music", "dj", "band", "quartet", "strings", "sound"),
            "Attire" to listOf("dress", "attire", "gown", "suit", "tux", "hair", "makeup"),
            "Stationery" to listOf("stationery", "invite", "invitation", "envelope", "paper", "card"),
            "Coordination" to listOf("coordinator", "coordination", "planner", "logistics", "timeline")
        )
        val matched = keywords.filter { (_, keys) -> keys.any { lower.contains(it) } }.keys.toList()
        return matched.ifEmpty { priorities.take(2) }
    }
}
