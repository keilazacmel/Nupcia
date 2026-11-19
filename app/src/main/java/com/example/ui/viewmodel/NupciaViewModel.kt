package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiAssistantReply
import com.example.data.NupciaRepository
import com.example.data.WeddingAiService
import com.example.domain.BudgetAllocation
import com.example.domain.GeoMatchEngine
import com.example.domain.Guest
import com.example.domain.ReadinessReport
import com.example.domain.RebalanceProposal
import com.example.domain.RegistryItem
import com.example.domain.ScoredVendor
import com.example.domain.ServiceRequest
import com.example.domain.StationeryDesign
import com.example.domain.UserProfile
import com.example.domain.VendorCalendarBlock
import com.example.domain.VendorCatalogItem
import com.example.domain.VendorProfile
import com.example.domain.VendorTask
import com.example.domain.Wedding
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val VM_TAG = "NupciaViewModel"
private const val STOP_TIMEOUT_MS = 5000L

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ChatTurn(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val impactSummary: String = "",
    val proposal: RebalanceProposal? = null,
    val proposalApplied: Boolean = false,
    val recommendedVendors: List<ScoredVendor> = emptyList(),
    val errorBanner: Boolean = false
)

class NupciaViewModel(
    private val repository: NupciaRepository,
    private val aiService: WeddingAiService,
    val currentUserId: String,
    initialDisplayName: String
) : ViewModel() {

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(VM_TAG, "Error observing $label", root)
                emit(UiState.Error(root.message ?: "Error al sincronizar $label"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private fun <T> Flow<T>.toSafeState(initial: T, label: String): StateFlow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(VM_TAG, "Error observing $label", root)
            _errorMessage.value = root.message ?: "Error al cargar $label"
            emit(initial)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initial)

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiState("perfil")

    val wedding: StateFlow<Wedding?> =
        repository.observeWedding(currentUserId).toSafeState(null, "boda")

    val budgetAllocations: StateFlow<List<BudgetAllocation>> =
        repository.observeBudgetAllocations(currentUserId).toSafeState(emptyList(), "presupuesto")

    val guests: StateFlow<List<Guest>> =
        repository.observeGuests(currentUserId).toSafeState(emptyList(), "invitados")

    val registryItems: StateFlow<List<RegistryItem>> =
        repository.observeRegistryItems(currentUserId).toSafeState(emptyList(), "mesa de regalos")

    val stationeryDesigns: StateFlow<List<StationeryDesign>> =
        repository.observeStationeryDesigns(currentUserId).toSafeState(emptyList(), "papelería")

    val publishedVendors: StateFlow<List<VendorProfile>> =
        repository.observePublishedVendors(currentUserId).toSafeState(emptyList(), "proveedores")

    val myVendorProfile: StateFlow<VendorProfile?> =
        repository.observeMyVendorProfile(currentUserId).toSafeState(null, "mi perfil proveedor")

    val myCatalogItems: StateFlow<List<VendorCatalogItem>> =
        repository.observeVendorCatalog(currentUserId, currentUserId, includePrivate = true)
            .toSafeState(emptyList(), "mi catálogo")

    val coupleRequests: StateFlow<List<ServiceRequest>> =
        repository.observeCoupleRequests(currentUserId).toSafeState(emptyList(), "solicitudes pareja")

    val vendorRequests: StateFlow<List<ServiceRequest>> =
        repository.observeVendorRequests(currentUserId).toSafeState(emptyList(), "solicitudes proveedor")

    val vendorTasks: StateFlow<List<VendorTask>> =
        repository.observeVendorTasks(currentUserId).toSafeState(emptyList(), "tareas proveedor")

    val vendorCalendarBlocks: StateFlow<List<VendorCalendarBlock>> =
        repository.observeVendorCalendar(currentUserId).toSafeState(emptyList(), "calendario proveedor")

    // Derived Geospatial & Affinity-Scored Vendors
    val scoredVendors: StateFlow<List<ScoredVendor>> =
        combine(publishedVendors, wedding, budgetAllocations) { vendors, w, allocs ->
            vendors.map { GeoMatchEngine.scoreVendor(it, w, allocs) }
                .sortedByDescending { it.affinityScore }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // Derived Wedding Readiness Report
    val readinessReport: StateFlow<ReadinessReport> =
        combine(wedding, budgetAllocations, coupleRequests, guests, stationeryDesigns) { w, allocs, reqs, g, stat ->
            GeoMatchEngine.computeReadiness(w, allocs, reqs, g, stat)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            GeoMatchEngine.computeReadiness(null, emptyList(), emptyList(), emptyList(), emptyList())
        )

    // UI Transient State
    private val _selectedCoupleTab = MutableStateFlow(0)
    val selectedCoupleTab: StateFlow<Int> = _selectedCoupleTab.asStateFlow()

    private val _highlightedVendorId = MutableStateFlow<String?>(null)
    val highlightedVendorId: StateFlow<String?> = _highlightedVendorId.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // AI Chat State
    private val _chatTurns = MutableStateFlow<List<ChatTurn>>(
        listOf(
            ChatTurn(
                id = "welcome",
                fromUser = false,
                text = "Hola, soy tu Asistente Editorial de Presupuesto en Nupcia. Conozco tu visión, tu ranking de prioridades y los proveedores reales en tu zona. ¿Qué rubro quieres optimizar o cotizar hoy?"
            )
        )
    )
    val chatTurns: StateFlow<List<ChatTurn>> = _chatTurns.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfileExists(initialDisplayName)
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun selectCoupleTab(index: Int) {
        _selectedCoupleTab.value = index
    }

    fun openVendorOnMap(vendorId: String) {
        _highlightedVendorId.value = vendorId
        _selectedCoupleTab.value = 2 // Map tab
    }

    fun clearHighlightedVendor() {
        _highlightedVendorId.value = null
    }

    fun switchWorkspace(mode: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.switchWorkspaceMode(mode)
                .onSuccess {
                    _snackbarMessage.value = if (mode == "VENDOR") {
                        "Portal de Proveedor activado"
                    } else {
                        "Portal de Pareja activado"
                    }
                }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "No se pudo cambiar de portal." }
            _isBusy.value = false
        }
    }

    fun completeOnboarding(
        brideName: String,
        partnerName: String,
        weddingDate: String,
        city: String,
        lat: Double,
        lng: Double,
        totalBudget: Double,
        estimatedGuests: Long,
        styleTags: List<String>,
        colorPalette: List<String>,
        ceremonyType: String,
        visionExpectations: String,
        priorityOrder: List<String>
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.completeCoupleOnboarding(
                brideName, partnerName, weddingDate, city, lat, lng,
                totalBudget, estimatedGuests, styleTags, colorPalette,
                ceremonyType, visionExpectations, priorityOrder
            ).onSuccess {
                _snackbarMessage.value = "¡Tu boda y presupuesto inteligente están listos!"
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "No se pudo guardar el onboarding."
            }
            _isBusy.value = false
        }
    }

    // --- AI Concierge ---

    fun sendAiPrompt(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty() || _isAiThinking.value) return

        val userTurn = ChatTurn(id = "u_${System.currentTimeMillis()}", fromUser = true, text = clean)
        _chatTurns.update { it + userTurn }
        _isAiThinking.value = true

        viewModelScope.launch {
            val result = aiService.askConcierge(
                userPrompt = clean,
                wedding = wedding.value,
                allocations = budgetAllocations.value,
                allVendors = publishedVendors.value
            )
            result.onSuccess { reply: AiAssistantReply ->
                _chatTurns.update { turns ->
                    turns + ChatTurn(
                        id = "ai_${System.currentTimeMillis()}",
                        fromUser = false,
                        text = reply.message,
                        impactSummary = reply.impactSummary,
                        proposal = reply.proposal,
                        recommendedVendors = reply.recommendedVendors
                    )
                }
            }.onFailure { err ->
                _chatTurns.update { turns ->
                    turns + ChatTurn(
                        id = "err_${System.currentTimeMillis()}",
                        fromUser = false,
                        text = err.localizedMessage ?: "Error al contactar con Gemini AI Logic.",
                        errorBanner = true
                    )
                }
            }
            _isAiThinking.value = false
        }
    }

    fun applyProposalFromChat(turnId: String, proposal: RebalanceProposal) {
        viewModelScope.launch {
            val map = proposal.ranges.associate { it.category to it.suggestedIdeal }
            repository.applyBudgetProposal(budgetAllocations.value, map)
                .onSuccess {
                    _chatTurns.update { list ->
                        list.map { if (it.id == turnId) it.copy(proposalApplied = true) else it }
                    }
                    _snackbarMessage.value = "Presupuesto reasignado según tus prioridades"
                }
                .onFailure {
                    _errorMessage.value = it.localizedMessage ?: "No se pudo aplicar el ajuste."
                }
        }
    }

    // --- Budget Manual Edit ---

    fun updateAllocation(id: String, allocated: Double, spent: Double, locked: Boolean) {
        viewModelScope.launch {
            repository.updateBudgetAllocation(id, allocated, spent, locked)
                .onSuccess { _snackbarMessage.value = "Partida actualizada" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Error al actualizar partida." }
        }
    }

    // --- Service Requests (Couple & Vendor) ---

    fun sendInquiryToVendor(
        vendor: VendorProfile,
        catalogTitle: String,
        proposedBudget: Double,
        notes: String
    ) {
        val w = wedding.value
        val coupleName = if (w != null && w.brideName.isNotBlank()) {
            "${w.brideName} & ${w.partnerName}"
        } else {
            "Pareja Nupcia"
        }
        val date = w?.weddingDate ?: "2027-06-19"
        viewModelScope.launch {
            repository.sendServiceRequest(vendor, coupleName, catalogTitle, date, proposedBudget, notes)
                .onSuccess { _snackbarMessage.value = "Solicitud enviada a ${vendor.businessName}" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "No se pudo enviar la solicitud." }
        }
    }

    fun acceptRequestAsVendor(request: ServiceRequest) {
        viewModelScope.launch {
            repository.acceptServiceRequestAtomically(request)
                .onSuccess {
                    _snackbarMessage.value = "Solicitud aceptada: fecha bloqueada y 4 tareas operativas creadas"
                }
                .onFailure {
                    _errorMessage.value = it.localizedMessage ?: "No se pudo aceptar la solicitud."
                }
        }
    }

    fun counterOfferAsVendor(requestId: String, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.counterOfferServiceRequest(requestId, amount, notes)
                .onSuccess { _snackbarMessage.value = "Contraoferta enviada a la pareja" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "No se pudo enviar contraoferta." }
        }
    }

    fun transitionRequestStatus(request: ServiceRequest, newStatus: String) {
        viewModelScope.launch {
            repository.updateServiceRequestStatus(request, newStatus, vendorTasks.value)
                .onSuccess {
                    _snackbarMessage.value = when (newStatus) {
                        "COMPLETED" -> "¡Evento marcado como cumplido y sumado a tus métricas!"
                        "REJECTED" -> "Solicitud rechazada"
                        "CANCELLED" -> "Solicitud cancelada y agenda liberada"
                        "ACCEPTED" -> "Contraoferta aceptada"
                        else -> "Estado actualizado"
                    }
                }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Error al actualizar solicitud." }
        }
    }

    fun cycleVendorTaskStatus(task: VendorTask) {
        val next = when (task.status) {
            "PENDING" -> "IN_PROGRESS"
            "IN_PROGRESS" -> "COMPLETED"
            else -> "PENDING"
        }
        viewModelScope.launch {
            repository.updateVendorTaskStatus(task.id, next)
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Error al actualizar tarea." }
        }
    }

    // --- Vendor Profile, Catalog & Calendar ---

    fun saveVendorProfile(
        businessName: String,
        category: String,
        description: String,
        city: String,
        lat: Double,
        lng: Double,
        coverageRadiusKm: Double,
        minPrice: Double,
        maxPrice: Double,
        styleTags: List<String>,
        published: Boolean
    ) {
        viewModelScope.launch {
            repository.saveVendorProfile(
                businessName, category, description, city, lat, lng,
                coverageRadiusKm, minPrice, maxPrice, styleTags, published
            ).onSuccess { _snackbarMessage.value = "Perfil de proveedor publicado en el mapa" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "No se pudo guardar el perfil." }
        }
    }

    fun saveCatalogPackage(
        itemId: String?,
        title: String,
        description: String,
        basePrice: Double,
        pricingUnit: String,
        active: Boolean
    ) {
        viewModelScope.launch {
            repository.saveCatalogItem(itemId, title, description, basePrice, pricingUnit, active)
                .onSuccess { _snackbarMessage.value = "Paquete guardado en tu catálogo" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Primero guarda tu perfil de proveedor." }
        }
    }

    fun addManualBlock(title: String, date: String) {
        viewModelScope.launch {
            repository.addManualCalendarBlock(title, date)
                .onSuccess { _snackbarMessage.value = "Fecha bloqueada en tu calendario" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "No se pudo bloquear fecha." }
        }
    }

    fun deleteCalendarBlock(blockId: String) {
        viewModelScope.launch {
            repository.deleteCalendarBlock(blockId)
                .onSuccess { _snackbarMessage.value = "Bloqueo eliminado" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "No se pudo eliminar bloqueo." }
        }
    }

    fun loadOptInDemoVendors() {
        val w = wedding.value
        val city = w?.city ?: "Ciudad de México"
        val lat = w?.lat ?: 19.4326
        val lng = w?.lng ?: -99.1332
        viewModelScope.launch {
            _isBusy.value = true
            repository.seedDemoMarketplaceVendors(city, lat, lng)
                .onSuccess { _snackbarMessage.value = "5 proveedores de demostración cargados cerca de $city" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Error al cargar proveedores." }
            _isBusy.value = false
        }
    }

    // --- Event Hub: Guests, Registry, Stationery, Website ---

    fun saveGuest(
        guestId: String?,
        householdName: String,
        fullName: String,
        partySize: Long,
        email: String,
        phone: String,
        postalAddress: String,
        rsvpStatus: String,
        mealChoice: String,
        dietaryNotes: String,
        tableNumber: Long
    ) {
        viewModelScope.launch {
            repository.saveGuest(
                guestId, householdName, fullName, partySize, email, phone,
                postalAddress, rsvpStatus, mealChoice, dietaryNotes, tableNumber
            ).onSuccess { _snackbarMessage.value = "Invitado guardado" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Error al guardar invitado." }
        }
    }

    fun deleteGuest(guestId: String) {
        viewModelScope.launch {
            repository.deleteGuest(guestId)
                .onSuccess { _snackbarMessage.value = "Invitado eliminado" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Error al eliminar invitado." }
        }
    }

    fun addRegistryItem(type: String, title: String, desc: String, target: Double) {
        viewModelScope.launch {
            repository.addRegistryItem(type, title, desc, target)
                .onSuccess { _snackbarMessage.value = "Elemento agregado a tu mesa de regalos" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Error al crear elemento." }
        }
    }

    fun addContribution(item: RegistryItem, contributorName: String, amount: Double) {
        viewModelScope.launch {
            repository.recordContribution(item, contributorName, amount)
                .onSuccess { _snackbarMessage.value = "Aporte registrado de $contributorName" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Error al registrar aporte." }
        }
    }

    fun toggleThankYou(itemId: String, sent: Boolean) {
        viewModelScope.launch {
            repository.toggleThankYouSent(itemId, sent)
        }
    }

    fun saveStationery(
        designId: String,
        suiteId: String,
        productType: String,
        headline: String,
        subheadline: String,
        bodyText: String,
        bgColorHex: String,
        accentColorHex: String,
        fontStyle: String,
        bleedValid: Boolean,
        proofApproved: Boolean
    ) {
        viewModelScope.launch {
            repository.saveStationeryDesign(
                designId, suiteId, productType, headline, subheadline,
                bodyText, bgColorHex, accentColorHex, fontStyle, bleedValid, proofApproved
            ).onSuccess {
                _snackbarMessage.value = if (proofApproved) {
                    "Prueba aprobada y lista para impresión"
                } else {
                    "Diseño de papelería guardado"
                }
            }.onFailure {
                _errorMessage.value = it.localizedMessage ?: "Error al guardar papelería."
            }
        }
    }

    fun saveWeddingWebsite(
        suiteId: String,
        headline: String,
        story: String,
        venueName: String,
        schedule: String,
        protected: Boolean,
        slug: String
    ) {
        viewModelScope.launch {
            repository.updateWeddingWebsite(suiteId, headline, story, venueName, schedule, protected, slug)
                .onSuccess { _snackbarMessage.value = "Sitio web de boda actualizado" }
                .onFailure { _errorMessage.value = it.localizedMessage ?: "Error al actualizar sitio web." }
        }
    }
}
