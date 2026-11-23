package com.example.data

import com.example.domain.BudgetAllocation
import com.example.domain.Guest
import com.example.domain.ServiceRequest
import com.example.domain.StationeryDesign
import com.example.domain.VendorCalendarBlock
import com.example.domain.VendorProfile
import com.example.domain.VendorTask
import com.example.domain.Wedding

/**
 * Normalizes legacy persisted Firestore documents (from earlier non-English sessions)
 * into English and Utah County, Utah defaults both on read and during one-time migration.
 */
object EnglishLocaleNormalizer {

    private val categoryMap = mapOf(
        "lugar" to "Venue",
        "banquete" to "Catering",
        "fotografía" to "Photography",
        "fotografia" to "Photography",
        "flores" to "Florals",
        "música" to "Music",
        "musica" to "Music",
        "vestido" to "Attire",
        "papelería" to "Stationery",
        "papeleria" to "Stationery",
        "coordinación" to "Coordination",
        "coordinacion" to "Coordination"
    )

    private val styleMap = mapOf(
        "clásico" to "Classic",
        "clasico" to "Classic",
        "bohemio" to "Bohemian",
        "moderno" to "Modern",
        "jardín" to "Garden",
        "jardin" to "Garden",
        "minimalista" to "Minimalist",
        "playa" to "Mountain",
        "romántico" to "Romantic",
        "romantico" to "Romantic",
        "editorial" to "Editorial"
    )

    private val ceremonyMap = mapOf(
        "civil y recepción" to "Outdoor Garden",
        "civil y recepcion" to "Outdoor Garden",
        "religiosa y recepción" to "Historic Estate",
        "religiosa y recepcion" to "Historic Estate",
        "simbólica / jardín" to "Outdoor Garden",
        "simbolica / jardin" to "Outdoor Garden",
        "íntima / elopement" to "Intimate Ceremony",
        "intima / elopement" to "Intimate Ceremony",
        "destino" to "Mountain Lodge / Canyon"
    )

    private val legacyMexicanCities = listOf(
        "ciudad de méxico", "ciudad de mexico", "cdmx",
        "guadalajara", "monterrey", "san miguel",
        "querétaro", "queretaro", "cancún", "cancun", "cuernavaca"
    )

    fun isLegacyLocation(city: String, lat: Double): Boolean {
        val lower = city.lowercase()
        return lat < 35.0 || legacyMexicanCities.any { lower.contains(it) }
    }

    fun normalizeCategory(raw: String): String {
        val trimmed = raw.trim()
        return categoryMap[trimmed.lowercase()] ?: trimmed
    }

    fun normalizeStyle(raw: String): String {
        val trimmed = raw.trim()
        return styleMap[trimmed.lowercase()] ?: trimmed
    }

    fun normalizeCeremony(raw: String): String {
        val trimmed = raw.trim()
        return ceremonyMap[trimmed.lowercase()] ?: trimmed
    }

    fun normalizeNotes(raw: String): String = when (raw.trim().lowercase()) {
        "prioridad alta" -> "High Priority"
        "prioridad media" -> "Medium Priority"
        "prioridad flexible" -> "Flexible Priority"
        else -> raw
    }

    fun normalizeCity(raw: String, lat: Double): String {
        return if (isLegacyLocation(raw, lat)) "Provo, UT" else raw
    }

    fun normalizeWedding(w: Wedding): Wedding {
        val legacyLoc = isLegacyLocation(w.city, w.lat)
        val city = if (legacyLoc) "Provo, UT" else w.city
        val lat = if (legacyLoc) 40.2338 else w.lat
        val lng = if (legacyLoc) -111.6585 else w.lng
        val styles = w.styleTags.map(::normalizeStyle)
        val priorities = w.priorityOrder.map(::normalizeCategory)
        val ceremony = normalizeCeremony(w.ceremonyType)
        val vision = if (w.visionExpectations.contains("celebración", ignoreCase = true) ||
            w.visionExpectations.contains("fotografía", ignoreCase = true) ||
            w.visionExpectations.contains("gastronomía", ignoreCase = true)
        ) {
            "A luminous mountain-view garden celebration with seasonal dining and timeless editorial photography."
        } else {
            w.visionExpectations
        }
        val headline = if (w.websiteHeadline.equals("Nos casamos", ignoreCase = true)) {
            "We're Getting Married!"
        } else {
            w.websiteHeadline
        }
        val story = if (w.websiteStory.contains("alegría", ignoreCase = true) ||
            w.websiteStory.contains("celebrar", ignoreCase = true) ||
            w.websiteStory.contains("capítulo", ignoreCase = true)
        ) {
            "We would love to celebrate the beginning of our next chapter with you in $city."
        } else {
            w.websiteStory
        }
        val venueName = if (w.websiteVenueName.startsWith("Por confirmar", ignoreCase = true) ||
            legacyMexicanCities.any { w.websiteVenueName.lowercase().contains(it) }
        ) {
            "To be announced in $city"
        } else {
            w.websiteVenueName
        }
        val schedule = if (w.websiteSchedule.contains("Ceremonia", ignoreCase = true) ||
            w.websiteSchedule.contains("Cóctel", ignoreCase = true)
        ) {
            "4:30 PM Ceremony • 6:00 PM Cocktail Hour • 7:30 PM Dinner & Dancing"
        } else {
            w.websiteSchedule
        }
        val slug = if (w.slug == "nuestra-boda") "our-wedding" else w.slug

        return w.copy(
            city = city,
            lat = lat,
            lng = lng,
            styleTags = styles,
            priorityOrder = priorities,
            ceremonyType = ceremony,
            visionExpectations = vision,
            websiteHeadline = headline,
            websiteStory = story,
            websiteVenueName = venueName,
            websiteSchedule = schedule,
            slug = slug
        )
    }

    fun normalizeAllocation(a: BudgetAllocation): BudgetAllocation {
        return a.copy(
            category = normalizeCategory(a.category),
            notes = normalizeNotes(a.notes)
        )
    }

    fun normalizeGuest(g: Guest): Guest {
        val meal = when (g.mealChoice.trim().lowercase()) {
            "menú principal", "menu principal", "menú estándar", "menu estandar", "standard entrée" -> "Main Course"
            else -> g.mealChoice
        }
        val household = if (g.householdName.equals("Familia Invitada", ignoreCase = true)) {
            "Guest Household"
        } else {
            g.householdName
        }
        return g.copy(householdName = household, mealChoice = meal)
    }

    fun normalizeStationery(s: StationeryDesign): StationeryDesign {
        val headline = if (s.headline.contains("Novia", ignoreCase = true) && s.headline.contains("Pareja", ignoreCase = true)) {
            "Emma & Liam"
        } else {
            s.headline
        }
        val sub = if (legacyMexicanCities.any { s.subheadline.lowercase().contains(it) }) {
            val datePrefix = s.subheadline.substringBefore("•").trim().ifBlank { "2027-06-19" }
            "$datePrefix • Provo, UT"
        } else {
            s.subheadline
        }
        val body = if (s.bodyText.contains("inmensa alegría", ignoreCase = true) ||
            s.bodyText.contains("te invitamos", ignoreCase = true) ||
            s.bodyText.contains("nuestra boda", ignoreCase = true)
        ) {
            "Together with our families, we joyfully invite you to celebrate our wedding. Please RSVP on our wedding website."
        } else {
            s.bodyText
        }
        return s.copy(headline = headline, subheadline = sub, bodyText = body)
    }

    fun normalizeVendor(v: VendorProfile): VendorProfile {
        val legacyLoc = isLegacyLocation(v.city, v.lat)
        val businessName = when (v.businessName.trim()) {
            "Hacienda Jardín Real" -> "Timpanogos Garden Estate"
            "Luz & Memoria Fotografía" -> "Wasatch Light & Film Co."
            "Botánica Atelier Floral" -> "Wild Sage Floral Studio"
            "Mesa de Autor Catering" -> "Canyon Table Catering"
            "Sonido & Cuerdas Live" -> "Valley Strings & DJ Collective"
            "Atelier Nupcial Studio" -> "Wasatch Bridal Studio"
            else -> v.businessName
        }
        val category = normalizeCategory(v.category)
        val desc = when {
            v.description.contains("Jardines históricos", ignoreCase = true) ->
                "Manicured foothills gardens, open-air pavilion, and mountain views in Provo Canyon."
            v.description.contains("Fotografía documental", ignoreCase = true) ->
                "Editorial documentary & film wedding photography with heirloom linen albums."
            v.description.contains("Diseño floral orgánico", ignoreCase = true) ->
                "Organic floral arches, seasonal Utah blooms, and candlelit reception tablescapes."
            v.description.contains("Menú degustación", ignoreCase = true) ->
                "Locally sourced seasonal plated dinners, mocktail craft bars, and artisan grazing tables."
            v.description.contains("Cuarteto de cuerdas", ignoreCase = true) ->
                "Live string quartet for ceremonies plus modern reception DJ and sound production."
            v.description.contains("Cobertura editorial de bodas", ignoreCase = true) ->
                "Editorial Utah County wedding coverage with full digital gallery and heirloom album."
            else -> v.description
        }
        val city = if (legacyLoc) {
            when (businessName) {
                "Timpanogos Garden Estate" -> "Sundance / Provo Canyon"
                "Wild Sage Floral Studio" -> "Lehi, UT"
                "Canyon Table Catering" -> "American Fork, UT"
                "Valley Strings & DJ Collective" -> "Springville, UT"
                else -> "Provo, UT"
            }
        } else {
            v.city
        }
        val (lat, lng) = if (legacyLoc) {
            when (businessName) {
                "Timpanogos Garden Estate" -> 40.3785 to -111.5960
                "Wasatch Light & Film Co." -> 40.2444 to -111.6485
                "Wild Sage Floral Studio" -> 40.3880 to -111.8320
                "Canyon Table Catering" -> 40.3690 to -111.7850
                "Valley Strings & DJ Collective" -> 40.1720 to -111.6150
                else -> 40.2338 to -111.6585
            }
        } else {
            v.lat to v.lng
        }
        return v.copy(
            businessName = businessName,
            category = category,
            description = desc,
            city = city,
            lat = lat,
            lng = lng,
            styleTags = v.styleTags.map(::normalizeStyle)
        )
    }

    fun normalizeServiceRequest(r: ServiceRequest): ServiceRequest {
        val cat = normalizeCategory(r.category)
        val vendorName = when (r.vendorName.trim()) {
            "Hacienda Jardín Real" -> "Timpanogos Garden Estate"
            "Luz & Memoria Fotografía" -> "Wasatch Light & Film Co."
            "Botánica Atelier Floral" -> "Wild Sage Floral Studio"
            "Mesa de Autor Catering" -> "Canyon Table Catering"
            "Sonido & Cuerdas Live" -> "Valley Strings & DJ Collective"
            "Atelier Nupcial Studio" -> "Wasatch Bridal Studio"
            else -> r.vendorName
        }
        val pkgTitle = when {
            r.catalogItemTitle.startsWith("Paquete Integral de ", ignoreCase = true) ->
                "Full $cat Package"
            r.catalogItemTitle.startsWith("Consulta de ", ignoreCase = true) ->
                "$cat Inquiry (AI Match)"
            r.catalogItemTitle.equals("Paquete Personalizado", ignoreCase = true) ->
                "Custom Package"
            else -> r.catalogItemTitle
        }
        val notes = when {
            r.notes.contains("Nos encantó su estilo", ignoreCase = true) ->
                "We love your work! Are you available on our wedding date in Utah County?"
            r.notes.contains("Recomendado por Asistente IA", ignoreCase = true) ->
                "Recommended by Nupcia AI Concierge."
            r.notes.contains("Podemos incluir montaje extendido", ignoreCase = true) ->
                "We can include extended event coverage for this adjustment."
            else -> r.notes
        }
        return r.copy(
            vendorName = vendorName,
            category = cat,
            catalogItemTitle = pkgTitle,
            notes = notes
        )
    }

    fun normalizeVendorTask(t: VendorTask): VendorTask {
        val title = when (t.title.trim()) {
            "Confirmación de Contrato y Anticipo" -> "Contract & Deposit Confirmation"
            "Briefing de Estilo y Detalles" -> "Creative Alignment Briefing"
            "Logística Final y Acceso al Venue" -> "Final Logistics & Venue Access"
            "Ejecución del Evento y Reseña" -> "Event Execution & Post-Event Review"
            else -> t.title
        }
        val desc = when {
            t.description.contains("Enviar propuesta formal", ignoreCase = true) ->
                "Send formal proposal and confirm booking deposit."
            t.description.contains("Revisar paleta estética", ignoreCase = true) ->
                "Review aesthetic style, color palette, and vision with ${t.coupleName}."
            t.description.contains("Confirmar horarios de montaje", ignoreCase = true) ->
                "Confirm venue load-in times, setup schedule, and technical needs."
            t.description.contains("Entregar servicio el", ignoreCase = true) ->
                "Deliver services on ${t.eventDate} and invite couple review."
            else -> t.description
        }
        val due = when (t.dueDate.trim()) {
            "Inmediato (+3 días)" -> "Immediate (+3 days)"
            "45 días antes" -> "45 days before event"
            "10 días antes" -> "10 days before event"
            else -> t.dueDate
        }
        return t.copy(
            category = normalizeCategory(t.category),
            title = title,
            description = desc,
            dueDate = due
        )
    }

    fun normalizeCalendarBlock(b: VendorCalendarBlock): VendorCalendarBlock {
        val title = when {
            b.title.startsWith("Boda ") -> b.title.removePrefix("Boda ") + " Wedding"
            b.title.equals("Fecha bloqueada (evento privado)", ignoreCase = true) -> "Blocked date (private event)"
            b.title.equals("Fecha bloqueada", ignoreCase = true) -> "Blocked Date"
            else -> b.title
        }
        return b.copy(title = title)
    }
}
