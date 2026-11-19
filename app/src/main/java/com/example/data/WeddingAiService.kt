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
                description = "Respuesta cálida, editorial y práctica en español dirigida a la novia."
            ),
            "impactSummary" to Schema.string(
                description = "Resumen breve (1-2 frases) explicando el impacto financiero en otras categorías y cómo respeta sus prioridades."
            ),
            "shouldOfferRebalance" to Schema.boolean(
                description = "true si conviene mostrar el botón de aplicar ajuste de presupuesto."
            ),
            "recommendedVendorIds" to Schema.array(
                items = Schema.string(),
                description = "Lista de hasta 3 IDs de proveedores reales proporcionados en el contexto que mejor encajan."
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
            // 1. Detect focus categories mentioned by the bride
            val detectedCategories = detectCategoriesFromPrompt(userPrompt, wedding?.priorityOrder.orEmpty())

            // 2. Run deterministic budget math (zero LLM arithmetic!)
            val proposal = BudgetRebalancer.rebalanceForCategories(
                totalBudget = wedding?.totalBudget ?: 300000.0,
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
                appendLine("Eres el Asistente Editorial de Planificación de Bodas de Nupcia. Responde siempre en español cálido y claro.")
                appendLine("REGLA CRÍTICA: Nunca inventes números ni proveedores. Usa EXCLUSIVAMENTE los cálculos deterministas y proveedores reales listados abajo.")
                appendLine()
                appendLine("PERFIL DE LA NOVIA:")
                appendLine("- Pareja: ${wedding?.brideName ?: "Novia"} & ${wedding?.partnerName ?: "Pareja"}")
                appendLine("- Fecha: ${wedding?.weddingDate ?: "Por definir"} | Ciudad: ${wedding?.city ?: "CDMX"} | Invitados: ${wedding?.estimatedGuests ?: 120}")
                appendLine("- Estilos: ${wedding?.styleTags?.joinToString(", ") ?: "Clásico"}")
                appendLine("- Expectativas: ${wedding?.visionExpectations?.ifBlank { "Elegancia y armonía" } ?: "Elegancia"}")
                appendLine("- Ranking de Prioridades (1 = más alta): ${wedding?.priorityOrder?.joinToString(" > ") ?: ""}")
                appendLine("- Presupuesto Total: ${BudgetRebalancer.formatCurrency(proposal.totalBudget)} | Comprometido: ${BudgetRebalancer.formatCurrency(proposal.totalSpent)}")
                appendLine()
                appendLine("CÁLCULO DETERMINISTA DE REBALANCEO (ya verificado, suma <= total):")
                proposal.ranges.forEach { r ->
                    val sign = if (r.delta >= 0) "+" else ""
                    appendLine("- #${r.priorityRank} ${r.category}: actual ${BudgetRebalancer.formatCurrency(r.currentAllocated)} -> ideal ${BudgetRebalancer.formatCurrency(r.suggestedIdeal)} (rango ${BudgetRebalancer.formatCurrency(r.suggestedMin)} - ${BudgetRebalancer.formatCurrency(r.suggestedMax)}, ajuste $sign${BudgetRebalancer.formatCurrency(r.delta)})")
                }
                appendLine()
                appendLine("PROVEEDORES REALES COMPATIBLES EN FIRESTORE:")
                if (scoredVendors.isEmpty()) {
                    appendLine("- Ningún proveedor publicado aún para esta categoría.")
                } else {
                    scoredVendors.forEach { sv ->
                        val v = sv.vendor
                        appendLine("- ID=${v.id} | ${v.businessName} (${v.category}) | Precio: ${BudgetRebalancer.formatCurrency(v.minPrice)}-${BudgetRebalancer.formatCurrency(v.maxPrice)} | Afinidad: ${sv.affinityScore}% | Distancia: ${"%.1f".format(sv.distanceKm)} km | Cubre zona: ${sv.coversWeddingLocation}")
                    }
                }
                appendLine()
                appendLine("MENSAJE DE LA NOVIA:")
                appendLine(userPrompt)
            }

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("El asistente no devolvió contenido.")

            // Direct JSON parsing (responseSchema guarantees clean JSON without markdown fences)
            val json = JSONObject(rawJson)
            val assistantMsg = json.optString("assistantMessage", "").ifBlank {
                "He recalculado tu presupuesto según tus prioridades sin exceder el total."
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
            Result.failure(Exception("El asistente tardó más de 60 segundos. Intenta de nuevo."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Firebase AI generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "No se pudo consultar al asistente IA."))
        }
    }

    private fun detectCategoriesFromPrompt(prompt: String, priorities: List<String>): List<String> {
        val lower = prompt.lowercase()
        val keywords = mapOf(
            "Lugar" to listOf("lugar", "venue", "hacienda", "jardín", "jardin", "salón", "salon", "espacio"),
            "Banquete" to listOf("banquete", "comida", "catering", "cena", "menú", "menu", "gastronomía"),
            "Fotografía" to listOf("foto", "fotógraf", "fotograf", "video", "cámara", "editorial"),
            "Flores" to listOf("flor", "floral", "ramo", "decoración", "decoracion", "centros de mesa"),
            "Música" to listOf("música", "musica", "dj", "banda", "fiesta", "cuarteto", "sonido"),
            "Vestido" to listOf("vestido", "traje", "novia", "moda", "atelier", "maquillaje"),
            "Papelería" to listOf("papelería", "papeleria", "invitaci", "sobre", "menús"),
            "Coordinación" to listOf("coordinaci", "planner", "organizador", "logística")
        )
        val matched = keywords.filter { (_, keys) -> keys.any { lower.contains(it) } }.keys.toList()
        return matched.ifEmpty { priorities.take(2) }
    }
}
