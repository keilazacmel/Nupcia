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
    val city: String = "Ciudad de México",
    val lat: Double = 19.4326,
    val lng: Double = -99.1332,
    val totalBudget: Double = 300000.0,
    val estimatedGuests: Long = 120L,
    val slug: String = "nuestra-boda",
    val selectedSuiteId: String = "botanical_ivory",
    val styleTags: List<String> = listOf("Jardín", "Clásico"),
    val colorPalette: List<String> = listOf("#7B2D3B", "#FAF6F1", "#536B5E"),
    val ceremonyType: String = "Jardín al aire libre",
    val visionExpectations: String = "",
    val priorityOrder: List<String> = DefaultWeddingCatalog.CATEGORIES,
    val websiteHeadline: String = "Nos casamos",
    val websiteStory: String = "Estamos muy felices de compartir este capítulo contigo.",
    val websiteVenueName: String = "Por confirmar",
    val websiteSchedule: String = "16:30 Ceremonia • 18:00 Cóctel • 19:30 Recepción",
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
    val mealChoice: String = "Menú principal",
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
    val category: String = "Fotografía",
    val description: String = "",
    val city: String = "Ciudad de México",
    val lat: Double = 19.4326,
    val lng: Double = -99.1332,
    val coverageRadiusKm: Double = 100.0,
    val minPrice: Double = 15000.0,
    val maxPrice: Double = 45000.0,
    val styleTags: List<String> = listOf("Clásico"),
    val ratingAvg: Double = 4.8,
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
    val pricingUnit: String = "Por evento",
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

// --- Curated Static Reference Data (Geocoded Cities, Styles, Suites) ---

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
        "Lugar",
        "Banquete",
        "Fotografía",
        "Flores",
        "Música",
        "Vestido",
        "Papelería",
        "Coordinación"
    )

    val STYLE_TAGS = listOf(
        "Clásico",
        "Bohemio",
        "Moderno",
        "Jardín",
        "Minimalista",
        "Destino",
        "Romántico",
        "Editorial"
    )

    val CEREMONY_TYPES = listOf(
        "Jardín al aire libre",
        "Hacienda histórica",
        "Salón contemporáneo",
        "Playa / Destino",
        "Ceremonia íntima"
    )

    val COLOR_PALETTES: Map<String, List<String>> = mapOf(
        "Burdeos & Marfil" to listOf("#7B2D3B", "#FAF6F1", "#536B5E"),
        "Eucalipto & Champagne" to listOf("#4B6858", "#F7F3EB", "#B68D40"),
        "Terracota & Arena" to listOf("#A4573E", "#F8EFE6", "#6D594D"),
        "Noche & Oro" to listOf("#24293E", "#F4F1EA", "#C59B43")
    )

    val CITIES = listOf(
        CityOption("Ciudad de México", 19.4326, -99.1332),
        CityOption("Guadalajara", 20.6597, -103.3496),
        CityOption("Monterrey", 25.6866, -100.3161),
        CityOption("San Miguel de Allende", 20.9144, -100.7452),
        CityOption("Valle de Bravo", 19.1951, -100.1314),
        CityOption("Cancún / Riviera Maya", 21.1619, -86.8515),
        CityOption("Oaxaca", 17.0732, -96.7266),
        CityOption("Madrid", 40.4168, -3.7038),
        CityOption("Bogotá", 4.7110, -74.0721)
    )

    val SUITES = listOf(
        StationerySuite("botanical_ivory", "Marfil Botánico", "Jardín", "#FAF6F1", "#4B6858", "Follaje fino y serif clásica"),
        StationerySuite("bordeaux_couture", "Atelier Burdeos", "Clásico", "#FDF7F4", "#7B2D3B", "Elegancia editorial atemporal"),
        StationerySuite("terracotta_boho", "Sol Terracota", "Bohemio", "#F9F0E7", "#A4573E", "Tonos cálidos y orgánicos"),
        StationerySuite("champagne_minimal", "Línea Champagne", "Minimalista", "#FFFFFF", "#9C762F", "Geometría limpia en oro"),
        StationerySuite("coastal_linen", "Lino Costero", "Destino", "#F4F8F9", "#3B6275", "Aire mediterráneo y fresco")
    )

    fun suiteForStyles(styles: List<String>): StationerySuite {
        return SUITES.firstOrNull { suite -> styles.any { it.equals(suite.styleTag, ignoreCase = true) } }
            ?: SUITES.first()
    }
}
