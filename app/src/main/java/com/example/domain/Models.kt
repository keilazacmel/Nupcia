package com.example.domain

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

// --- Firestore Domain Entities (1:1 parity with firestore.rules) ---

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val workspaceMode: String = "COUPLE", // COUPLE | VENDOR
    val onboardingDone: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Wedding(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val brideName: String = "",
    val partnerName: String = "",
    val weddingDate: String = "2027-06-19",
    val city: String = "Provo, UT",
    val lat: Double = 40.2338,
    val lng: Double = -111.6585,
    val totalBudget: Double = 35000.0,
    val estimatedGuests: Long = 140L,
    val slug: String = "our-wedding",
    val selectedSuiteId: String = "botanical_ivory",
    val styleTags: List<String> = listOf("Garden", "Classic"),
    val colorPalette: List<String> = listOf("#7B2D3B", "#FAF6F1", "#536B5E"),
    val ceremonyType: String = "Outdoor Garden",
    val visionExpectations: String = "",
    val priorityOrder: List<String> = DefaultWeddingCatalog.CATEGORIES,
    val websiteHeadline: String = "We're Getting Married",
    val websiteStory: String = "We are so excited to celebrate the beginning of our next chapter with you in Utah County.",
    val websiteVenueName: String = "To be announced in Provo, UT",
    val websiteSchedule: String = "4:30 PM Ceremony • 6:00 PM Cocktail Hour • 7:30 PM Dinner & Dancing",
    val websiteProtected: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BudgetAllocation(
    @DocumentId val id: String = "",
    val category: String = "",
    val allocatedAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val priorityRank: Long = 1L,
    val locked: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Guest(
    @DocumentId val id: String = "",
    val householdName: String = "",
    val fullName: String = "",
    val partySize: Long = 1L,
    val email: String = "",
    val phone: String = "",
    val postalAddress: String = "",
    val rsvpStatus: String = "PENDING", // PENDING | CONFIRMED | DECLINED
    val mealChoice: String = "Main Course",
    val dietaryNotes: String = "",
    val tableNumber: Long = 0L, // 0 = unassigned
    val inviteToken: String = "NUP-1000",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RegistryItem(
    @DocumentId val id: String = "",
    val itemType: String = "CASH_FUND", // PRODUCT | CASH_FUND
    val title: String = "",
    val description: String = "",
    val targetAmount: Double = 1000.0,
    val collectedAmount: Double = 0.0,
    val contributorCount: Long = 0L,
    val lastContributorName: String = "",
    val thankYouSent: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StationeryDesign(
    @DocumentId val id: String = "",
    val suiteId: String = "botanical_ivory",
    val productType: String = "INVITATION", // INVITATION | SAVE_THE_DATE | MENU | ENVELOPE
    val headline: String = "",
    val subheadline: String = "",
    val bodyText: String = "",
    val bgColorHex: String = "#FAF6F1",
    val accentColorHex: String = "#7B2D3B",
    val fontStyle: String = "EDITORIAL_SERIF",
    val bleedValid: Boolean = true,
    val proofApproved: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VendorProfile(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val businessName: String = "",
    val category: String = "Photography",
    val description: String = "",
    val city: String = "Provo, UT",
    val lat: Double = 40.2338,
    val lng: Double = -111.6585,
    val coverageRadiusKm: Double = 60.0,
    val minPrice: Double = 2200.0,
    val maxPrice: Double = 5500.0,
    val styleTags: List<String> = listOf("Classic"),
    val ratingAvg: Double = 4.9,
    val completedEventsCount: Long = 0L,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VendorCatalogItem(
    @DocumentId val id: String = "",
    val vendorOwnerId: String = "",
    val title: String = "",
    val description: String = "",
    val basePrice: Double = 0.0,
    val pricingUnit: String = "Per event",
    val active: Boolean = true,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ServiceRequest(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val vendorId: String = "",
    val vendorOwnerId: String = "",
    val coupleName: String = "",
    val vendorName: String = "",
    val category: String = "",
    val catalogItemTitle: String = "",
    val eventDate: String = "",
    val proposedBudget: Double = 0.0,
    val counterOfferBudget: Double = 0.0,
    val notes: String = "",
    val status: String = "PENDING", // PENDING | ACCEPTED | REJECTED | COUNTERED | COMPLETED | CANCELLED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VendorTask(
    @DocumentId val id: String = "",
    val serviceRequestId: String = "",
    val coupleName: String = "",
    val category: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: String = "",
    val eventDate: String = "",
    val status: String = "PENDING", // PENDING | IN_PROGRESS | COMPLETED | CANCELLED
    val milestone: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VendorCalendarBlock(
    @DocumentId val id: String = "",
    val serviceRequestId: String = "",
    val title: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val blockType: String = "BOOKED", // BOOKED | MANUAL_BLOCK
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// --- Curated Static Reference Data (Utah County Geocoded Cities, Styles, Suites) ---

data class CityOption(
    val name: String,
    val lat: Double,
    val lng: Double
)

data class StationerySuite(
    val id: String,
    val name: String,
    val styleTag: String,
    val bgHex: String,
    val accentHex: String,
    val subtitle: String
)

object DefaultWeddingCatalog {
    val CATEGORIES = listOf(
        "Venue",
        "Catering",
        "Photography",
        "Florals",
        "Music",
        "Attire",
        "Stationery",
        "Coordination"
    )

    val STYLE_TAGS = listOf(
        "Classic",
        "Bohemian",
        "Modern",
        "Garden",
        "Minimalist",
        "Mountain",
        "Romantic",
        "Editorial"
    )

    val CEREMONY_TYPES = listOf(
        "Outdoor Garden",
        "Mountain Lodge / Canyon",
        "Historic Estate",
        "Modern Ballroom",
        "Intimate Ceremony"
    )

    val COLOR_PALETTES: Map<String, List<String>> = mapOf(
        "Bordeaux & Ivory" to listOf("#7B2D3B", "#FAF6F1", "#536B5E"),
        "Eucalyptus & Champagne" to listOf("#4B6858", "#F7F3EB", "#B68D40"),
        "Terracotta & Sand" to listOf("#A4573E", "#F8EFE6", "#6D594D"),
        "Midnight & Gold" to listOf("#24293E", "#F4F1EA", "#C59B43")
    )

    // Geocoded Utah County, Utah wedding destinations
    val CITIES = listOf(
        CityOption("Provo, UT", 40.2338, -111.6585),
        CityOption("Orem, UT", 40.2969, -111.6946),
        CityOption("Lehi, UT", 40.3916, -111.8508),
        CityOption("American Fork, UT", 40.3769, -111.7958),
        CityOption("Sundance / Provo Canyon", 40.3934, -111.5888),
        CityOption("Alpine, UT", 40.4533, -111.7780),
        CityOption("Pleasant Grove, UT", 40.3641, -111.7385),
        CityOption("Highland, UT", 40.4272, -111.7966),
        CityOption("Springville, UT", 40.1652, -111.6108),
        CityOption("Spanish Fork, UT", 40.1150, -111.6549)
    )

    val SUITES = listOf(
        StationerySuite("botanical_ivory", "Botanical Ivory", "Garden", "#FAF6F1", "#4B6858", "Fine foliage & classic serif"),
        StationerySuite("bordeaux_couture", "Bordeaux Couture", "Classic", "#FDF7F4", "#7B2D3B", "Timeless editorial elegance"),
        StationerySuite("terracotta_boho", "Terracotta Sun", "Bohemian", "#F9F0E7", "#A4573E", "Warm organic desert tones"),
        StationerySuite("champagne_minimal", "Champagne Line", "Minimalist", "#FFFFFF", "#9C762F", "Clean gold architectural lines"),
        StationerySuite("alpine_linen", "Alpine Linen", "Mountain", "#F4F8F9", "#3B6275", "Crisp Wasatch mountain air")
    )

    fun suiteForStyles(styles: List<String>): StationerySuite {
        return SUITES.firstOrNull { suite -> styles.any { it.equals(suite.styleTag, ignoreCase = true) } }
            ?: SUITES.first()
    }
}
