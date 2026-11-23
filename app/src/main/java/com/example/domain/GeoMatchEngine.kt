package com.example.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class ScoredVendor(
    val vendor: VendorProfile,
    val distanceKm: Double,
    val coversWeddingLocation: Boolean,
    val withinCategoryBudget: Boolean,
    val affinityScore: Int, // 0..100
    val categoryBudgetAllocated: Double
) {
    val distanceMiles: Double get() = distanceKm * 0.621371
    val coverageRadiusMiles: Int get() = (vendor.coverageRadiusKm * 0.621371).roundToInt().coerceAtLeast(1)
}

data class ReadinessReport(
    val score: Int, // 0..100
    val budgetHealth: Int,
    val bookedHighPriorityCount: Int,
    val confirmedGuestsCount: Int,
    val seatedGuestsCount: Int,
    val nextBestActionTitle: String,
    val nextBestActionSubtitle: String,
    val nextBestActionTab: Int // 0=Home, 1=AI, 2=Map, 3=Event
)

object GeoMatchEngine {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Exact Haversine great-circle distance in kilometers (RF-08).
     */
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Computes affinity score (0..100%) combining:
     * - Style overlap with bridal preferences (35 pts)
     * - Budget compatibility with allocated category budget (30 pts)
     * - Geospatial coverage radius fit (20 pts)
     * - Vendor rating quality (15 pts)
     */
    fun scoreVendor(
        vendor: VendorProfile,
        wedding: Wedding?,
        allocations: List<BudgetAllocation>
    ): ScoredVendor {
        // Default fallback center: Provo, Utah County, UT
        val wLat = wedding?.lat ?: 40.2338
        val wLng = wedding?.lng ?: -111.6585
        val dist = distanceKm(wLat, wLng, vendor.lat, vendor.lng)
        val covers = dist <= vendor.coverageRadiusKm

        val catAlloc = allocations.firstOrNull {
            it.category.equals(vendor.category, ignoreCase = true)
        }?.allocatedAmount ?: ((wedding?.totalBudget ?: 35000.0) * 0.12)

        val withinBudget = vendor.minPrice <= catAlloc * 1.10

        val weddingStyles = wedding?.styleTags.orEmpty().map { it.lowercase() }.toSet()
        val vendorStyles = vendor.styleTags.map { it.lowercase() }.toSet()
        val styleScore = if (weddingStyles.isEmpty()) {
            24.0
        } else {
            val overlap = weddingStyles.intersect(vendorStyles).size
            ((overlap.toDouble() / weddingStyles.size.coerceAtLeast(1)) * 35.0).coerceAtMost(35.0)
        }

        val budgetScore = when {
            vendor.minPrice <= catAlloc && vendor.maxPrice >= catAlloc * 0.5 -> 30.0
            vendor.minPrice <= catAlloc * 1.15 -> 20.0
            else -> 8.0
        }

        val geoScore = when {
            covers && dist <= vendor.coverageRadiusKm * 0.5 -> 20.0
            covers -> 15.0
            else -> 3.0
        }

        val ratingScore = (vendor.ratingAvg.coerceIn(0.0, 5.0) / 5.0) * 15.0
        val total = (styleScore + budgetScore + geoScore + ratingScore).roundToInt().coerceIn(25, 99)

        return ScoredVendor(
            vendor = vendor,
            distanceKm = dist,
            coversWeddingLocation = covers,
            withinCategoryBudget = withinBudget,
            affinityScore = total,
            categoryBudgetAllocated = catAlloc
        )
    }

    /**
     * Smart Readiness Index & Next Best Action optimizer for the Couple Dashboard.
     */
    fun computeReadiness(
        wedding: Wedding?,
        allocations: List<BudgetAllocation>,
        requests: List<ServiceRequest>,
        guests: List<Guest>,
        stationery: List<StationeryDesign>
    ): ReadinessReport {
        val budgetHealth = BudgetRebalancer.computeBudgetHealthScore(
            wedding?.totalBudget ?: 0.0,
            allocations
        )
        val topPriorities = wedding?.priorityOrder?.take(3).orEmpty()
        val bookedCats = requests
            .filter { it.status == "ACCEPTED" || it.status == "COMPLETED" }
            .map { it.category }
            .toSet()
        val bookedTop = topPriorities.count { top -> bookedCats.any { it.equals(top, ignoreCase = true) } }

        val confirmedGuests = guests.filter { it.rsvpStatus == "CONFIRMED" }.sumOf { it.partySize.toInt() }
        val seatedGuests = guests.filter { it.rsvpStatus == "CONFIRMED" && it.tableNumber > 0 }.sumOf { it.partySize.toInt() }
        val hasApprovedProof = stationery.any { it.proofApproved }

        val budgetPts = (budgetHealth * 0.25).roundToInt()
        val vendorPts = ((bookedTop.toDouble() / topPriorities.size.coerceAtLeast(1)) * 30.0).roundToInt()
        val guestPts = if (guests.isNotEmpty()) 20 else 5
        val seatingPts = if (confirmedGuests > 0) ((seatedGuests.toDouble() / confirmedGuests) * 15.0).roundToInt() else 5
        val stationeryPts = if (hasApprovedProof) 10 else if (stationery.isNotEmpty()) 5 else 0

        val totalScore = (budgetPts + vendorPts + guestPts + seatingPts + stationeryPts).coerceIn(10, 100)

        val (title, subtitle, tab) = when {
            bookedTop < topPriorities.size -> {
                val missing = topPriorities.firstOrNull { top -> bookedCats.none { it.equals(top, ignoreCase = true) } } ?: "your #1 priority"
                Triple(
                    "Book your $missing vendor",
                    "This is one of your top priorities. Explore Utah County matches on the map or ask the AI Concierge.",
                    2
                )
            }
            guests.isEmpty() -> Triple(
                "Add your first households & guests",
                "Add guests to collect mailing addresses, track RSVPs, and unlock your Seating Chart.",
                3
            )
            confirmedGuests > seatedGuests -> Triple(
                "Assign tables for ${confirmedGuests - seatedGuests} confirmed guests",
                "Organize your reception seating chart and review dietary notes.",
                3
            )
            !hasApprovedProof -> Triple(
                "Approve your wedding stationery proof",
                "Customize your coordinated suite and verify print bleed safe zones.",
                3
            )
            else -> Triple(
                "Fine-tune your final balance with AI Concierge",
                "Your wedding plan is in great shape. Explore ways to optimize your remaining budget.",
                1
            )
        }

        return ReadinessReport(
            score = totalScore,
            budgetHealth = budgetHealth,
            bookedHighPriorityCount = bookedTop,
            confirmedGuestsCount = confirmedGuests,
            seatedGuestsCount = seatedGuests,
            nextBestActionTitle = title,
            nextBestActionSubtitle = subtitle,
            nextBestActionTab = tab
        )
    }
}
