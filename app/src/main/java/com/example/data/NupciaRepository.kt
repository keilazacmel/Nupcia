package com.example.data

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.domain.BudgetAllocation
import com.example.domain.BudgetRebalancer
import com.example.domain.DefaultWeddingCatalog
import com.example.domain.Guest
import com.example.domain.RegistryItem
import com.example.domain.ServiceRequest
import com.example.domain.StationeryDesign
import com.example.domain.UserProfile
import com.example.domain.VendorCalendarBlock
import com.example.domain.VendorCatalogItem
import com.example.domain.VendorProfile
import com.example.domain.VendorTask
import com.example.domain.Wedding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NupciaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión con Google para continuar.")
    }

    private fun <T> Flow<T>.handleListenerErrors(uid: String, path: String, op: OperationType): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- Real-time Observations (All use ServerTimestampBehavior.ESTIMATE) ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .handleListenerErrors(userId, path, OperationType.GET)
        )
    }

    fun observeWedding(userId: String): Flow<Wedding?> = flow {
        val path = "weddings/$userId"
        emitAll(
            db.collection("weddings").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(Wedding::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .handleListenerErrors(userId, path, OperationType.GET)
        )
    }

    fun observeBudgetAllocations(userId: String): Flow<List<BudgetAllocation>> = flow {
        val path = "users/$userId/budgetAllocations"
        emitAll(
            db.collection("users").document(userId).collection("budgetAllocations")
                .snapshots()
                .map { snap ->
                    snap.toObjects(BudgetAllocation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.priorityRank }
                }
                .handleListenerErrors(userId, path, OperationType.LIST)
        )
    }

    fun observeGuests(userId: String): Flow<List<Guest>> = flow {
        val path = "users/$userId/guests"
        emitAll(
            db.collection("users").document(userId).collection("guests")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Guest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.householdName }
                }
                .handleListenerErrors(userId, path, OperationType.LIST)
        )
    }

    fun observeRegistryItems(userId: String): Flow<List<RegistryItem>> = flow {
        val path = "users/$userId/registryItems"
        emitAll(
            db.collection("users").document(userId).collection("registryItems")
                .snapshots()
                .map { snap ->
                    snap.toObjects(RegistryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleListenerErrors(userId, path, OperationType.LIST)
        )
    }

    fun observeStationeryDesigns(userId: String): Flow<List<StationeryDesign>> = flow {
        val path = "users/$userId/stationeryDesigns"
        emitAll(
            db.collection("users").document(userId).collection("stationeryDesigns")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StationeryDesign::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleListenerErrors(userId, path, OperationType.LIST)
        )
    }

    fun observePublishedVendors(userId: String): Flow<List<VendorProfile>> = flow {
        val path = "vendors"
        emitAll(
            db.collection("vendors")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(VendorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.ratingAvg }
                }
                .handleListenerErrors(userId, path, OperationType.LIST)
        )
    }

    fun observeMyVendorProfile(userId: String): Flow<VendorProfile?> = flow {
        val path = "vendors/$userId"
        emitAll(
            db.collection("vendors").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(VendorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .handleListenerErrors(userId, path, OperationType.GET)
        )
    }

    fun observeVendorCatalog(vendorId: String, userId: String, includePrivate: Boolean): Flow<List<VendorCatalogItem>> = flow {
        val path = "vendors/$vendorId/catalogItems"
        val query = if (includePrivate) {
            db.collection("vendors").document(vendorId).collection("catalogItems")
                .whereEqualTo("vendorOwnerId", userId)
        } else {
            db.collection("vendors").document(vendorId).collection("catalogItems")
                .whereEqualTo("published", true)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(VendorCatalogItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleListenerErrors(userId, path, OperationType.LIST)
        )
    }

    fun observeCoupleRequests(userId: String): Flow<List<ServiceRequest>> = flow {
        val path = "serviceRequests"
        emitAll(
            db.collection("serviceRequests")
                .whereEqualTo("coupleId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ServiceRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleListenerErrors(userId, path, OperationType.LIST)
        )
    }

    fun observeVendorRequests(userId: String): Flow<List<ServiceRequest>> = flow {
        val path = "serviceRequests"
        emitAll(
            db.collection("serviceRequests")
                .whereEqualTo("vendorOwnerId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ServiceRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .handleListenerErrors(userId, path, OperationType.LIST)
        )
    }

    fun observeVendorTasks(userId: String): Flow<List<VendorTask>> = flow {
        val path = "users/$userId/vendorTasks"
        emitAll(
            db.collection("users").document(userId).collection("vendorTasks")
                .snapshots()
                .map { snap ->
                    snap.toObjects(VendorTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.dueDate }
                }
                .handleListenerErrors(userId, path, OperationType.LIST)
        )
    }

    fun observeVendorCalendar(userId: String): Flow<List<VendorCalendarBlock>> = flow {
        val path = "users/$userId/calendarBlocks"
        emitAll(
            db.collection("users").document(userId).collection("calendarBlocks")
                .snapshots()
                .map { snap ->
                    snap.toObjects(VendorCalendarBlock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.startDate }
                }
                .handleListenerErrors(userId, path, OperationType.LIST)
        )
    }

    // --- Mutations (All validate inputs, use serverTimestamp(), and return Result<T>) ---

    suspend fun ensureUserProfileExists(displayName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No autenticado"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "Usuario Nupcia" }.take(80)
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "workspaceMode" to "COUPLE",
                        "onboardingDone" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun switchWorkspaceMode(mode: String): Result<Unit> {
        val safeMode = if (mode == "VENDOR") "VENDOR" else "COUPLE"
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "workspaceMode" to safeMode,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to (auth.currentUser?.displayName?.ifBlank { "Usuario Nupcia" } ?: "Usuario Nupcia").take(80),
                        "workspaceMode" to safeMode,
                        "onboardingDone" to (safeMode == "VENDOR"),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * RF-02 & RF-03: Atomically saves bridal onboarding preferences, matched suite,
     * initial priority-weighted budget allocations, and starter invitation design.
     */
    suspend fun completeCoupleOnboarding(
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
    ): Result<Unit> {
        var path = "weddings"
        return try {
            val uid = requireUserId()
            path = "weddings/$uid"
            val safeBride = brideName.trim().ifBlank { "Novia" }.take(80)
            val safePartner = partnerName.trim().ifBlank { "Pareja" }.take(80)
            val safeCity = city.trim().ifBlank { "Ciudad de México" }.take(100)
            val safeBudget = totalBudget.coerceIn(10000.0, 50000000.0)
            val safeGuests = estimatedGuests.coerceIn(2L, 5000L)
            val safeStyles = styleTags.take(8).ifEmpty { listOf("Clásico") }
            val safePalette = colorPalette.take(6).ifEmpty { listOf("#7B2D3B", "#FAF6F1", "#536B5E") }
            val safePriorities = priorityOrder.take(10).ifEmpty { DefaultWeddingCatalog.CATEGORIES }
            val matchedSuite = DefaultWeddingCatalog.suiteForStyles(safeStyles)
            val slug = "${safeBride.lowercase()}-y-${safePartner.lowercase()}"
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifBlank { "nuestra-boda" }
                .take(80)

            val batch = db.batch()

            // 1. User profile -> onboardingDone = true
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            if (userSnap.exists()) {
                batch.update(
                    userRef,
                    mapOf(
                        "displayName" to safeBride,
                        "workspaceMode" to "COUPLE",
                        "onboardingDone" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    userRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeBride,
                        "workspaceMode" to "COUPLE",
                        "onboardingDone" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // 2. Wedding document
            val weddingRef = db.collection("weddings").document(uid)
            val weddingSnap = weddingRef.get().await()
            val weddingData = mapOf(
                "ownerId" to uid,
                "brideName" to safeBride,
                "partnerName" to safePartner,
                "weddingDate" to weddingDate.trim().ifBlank { "2027-06-19" }.take(30),
                "city" to safeCity,
                "lat" to lat.coerceIn(-90.0, 90.0),
                "lng" to lng.coerceIn(-180.0, 180.0),
                "totalBudget" to safeBudget,
                "estimatedGuests" to safeGuests,
                "slug" to slug,
                "selectedSuiteId" to matchedSuite.id,
                "styleTags" to safeStyles,
                "colorPalette" to safePalette,
                "ceremonyType" to ceremonyType.trim().ifBlank { "Jardín al aire libre" }.take(60),
                "visionExpectations" to visionExpectations.trim().take(1000),
                "priorityOrder" to safePriorities,
                "websiteHeadline" to "$safeBride & $safePartner",
                "websiteStory" to "Nos encantaría celebrar contigo el inicio de nuestra nueva etapa en $safeCity.",
                "websiteVenueName" to "Por confirmar en $safeCity",
                "websiteSchedule" to "16:30 Ceremonia • 18:00 Cóctel • 19:30 Cena y Baile",
                "websiteProtected" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (weddingSnap.exists()) {
                batch.update(
                    weddingRef,
                    weddingData.filterKeys { it != "ownerId" && it != "websiteHeadline" && it != "websiteStory" && it != "websiteVenueName" && it != "websiteSchedule" && it != "websiteProtected" }
                )
            } else {
                batch.set(weddingRef, weddingData + ("createdAt" to FieldValue.serverTimestamp()))
            }

            // 3. Initial Priority-Weighted Budget Allocations
            val allocations = BudgetRebalancer.computeInitialAllocations(safeBudget, safePriorities)
            allocations.forEachIndexed { idx, alloc ->
                val docId = "cat_${idx + 1}"
                val allocRef = db.collection("users").document(uid)
                    .collection("budgetAllocations").document(docId)
                batch.set(
                    allocRef,
                    mapOf(
                        "category" to alloc.category,
                        "allocatedAmount" to alloc.allocatedAmount,
                        "spentAmount" to 0.0,
                        "priorityRank" to alloc.priorityRank,
                        "locked" to false,
                        "notes" to alloc.notes,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // 4. Coordinated Starter Invitation Design matching suite
            val invRef = db.collection("users").document(uid)
                .collection("stationeryDesigns").document("inv_main")
            batch.set(
                invRef,
                mapOf(
                    "suiteId" to matchedSuite.id,
                    "productType" to "INVITATION",
                    "headline" to "$safeBride & $safePartner",
                    "subheadline" to "${weddingDate.trim().ifBlank { "2027-06-19" }} • $safeCity",
                    "bodyText" to "Con inmensa alegría te invitamos a celebrar nuestra boda. Confirma tu asistencia en nuestro sitio web.",
                    "bgColorHex" to matchedSuite.bgHex,
                    "accentColorHex" to matchedSuite.accentHex,
                    "fontStyle" to "EDITORIAL_SERIF",
                    "bleedValid" to true,
                    "proofApproved" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun applyBudgetProposal(
        allocations: List<BudgetAllocation>,
        proposedMap: Map<String, Double>
    ): Result<Unit> {
        var path = "users/budgetAllocations"
        return try {
            val uid = requireUserId()
            path = "users/$uid/budgetAllocations"
            val batch = db.batch()
            allocations.forEach { alloc ->
                val newVal = proposedMap[alloc.category] ?: return@forEach
                if (alloc.id.isNotBlank()) {
                    val ref = db.collection("users").document(uid)
                        .collection("budgetAllocations").document(alloc.id)
                    batch.update(
                        ref,
                        mapOf(
                            "allocatedAmount" to newVal.coerceIn(0.0, 50000000.0),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBudgetAllocation(
        allocationId: String,
        allocatedAmount: Double,
        spentAmount: Double,
        locked: Boolean
    ): Result<Unit> {
        var path = "users/budgetAllocations/$allocationId"
        return try {
            val uid = requireUserId()
            require(allocationId.isNotBlank())
            path = "users/$uid/budgetAllocations/$allocationId"
            db.collection("users").document(uid)
                .collection("budgetAllocations").document(allocationId)
                .update(
                    mapOf(
                        "allocatedAmount" to allocatedAmount.coerceIn(0.0, 50000000.0),
                        "spentAmount" to spentAmount.coerceIn(0.0, 50000000.0),
                        "locked" to locked,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Guests & Seating (RF-15) ---

    suspend fun saveGuest(
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
    ): Result<Unit> {
        var path = "users/guests"
        return try {
            val uid = requireUserId()
            val col = db.collection("users").document(uid).collection("guests")
            val safeHousehold = householdName.trim().ifBlank { "Familia Invitada" }.take(80)
            val safeName = fullName.trim().ifBlank { safeHousehold }.take(80)
            val safeStatus = if (rsvpStatus in listOf("PENDING", "CONFIRMED", "DECLINED")) rsvpStatus else "PENDING"
            if (guestId.isNullOrBlank()) {
                val docRef = col.document()
                path = docRef.path
                val token = "NUP-${UUID.randomUUID().toString().take(6).uppercase()}"
                docRef.set(
                    mapOf(
                        "householdName" to safeHousehold,
                        "fullName" to safeName,
                        "partySize" to partySize.coerceIn(1L, 20L),
                        "email" to email.trim().take(120),
                        "phone" to phone.trim().take(40),
                        "postalAddress" to postalAddress.trim().take(240),
                        "rsvpStatus" to safeStatus,
                        "mealChoice" to mealChoice.trim().take(80),
                        "dietaryNotes" to dietaryNotes.trim().take(200),
                        "tableNumber" to tableNumber.coerceIn(0L, 100L),
                        "inviteToken" to token,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = col.document(guestId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "householdName" to safeHousehold,
                        "fullName" to safeName,
                        "partySize" to partySize.coerceIn(1L, 20L),
                        "email" to email.trim().take(120),
                        "phone" to phone.trim().take(40),
                        "postalAddress" to postalAddress.trim().take(240),
                        "rsvpStatus" to safeStatus,
                        "mealChoice" to mealChoice.trim().take(80),
                        "dietaryNotes" to dietaryNotes.trim().take(200),
                        "tableNumber" to tableNumber.coerceIn(0L, 100L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteGuest(guestId: String): Result<Unit> {
        var path = "users/guests/$guestId"
        return try {
            val uid = requireUserId()
            path = "users/$uid/guests/$guestId"
            db.collection("users").document(uid).collection("guests").document(guestId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Registry & Cash Funds (RF-16) ---

    suspend fun addRegistryItem(
        itemType: String,
        title: String,
        description: String,
        targetAmount: Double
    ): Result<Unit> {
        var path = "users/registryItems"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("registryItems").document()
            path = docRef.path
            val safeType = if (itemType == "PRODUCT") "PRODUCT" else "CASH_FUND"
            docRef.set(
                mapOf(
                    "itemType" to safeType,
                    "title" to title.trim().ifBlank { "Fondo de Boda" }.take(100),
                    "description" to description.trim().take(400),
                    "targetAmount" to targetAmount.coerceIn(1.0, 10000000.0),
                    "collectedAmount" to 0.0,
                    "contributorCount" to 0L,
                    "lastContributorName" to "",
                    "thankYouSent" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordContribution(
        item: RegistryItem,
        contributorName: String,
        amount: Double
    ): Result<Unit> {
        var path = "users/registryItems/${item.id}"
        return try {
            val uid = requireUserId()
            path = "users/$uid/registryItems/${item.id}"
            val newCollected = (item.collectedAmount + amount.coerceAtLeast(1.0)).coerceAtMost(10000000.0)
            db.collection("users").document(uid).collection("registryItems").document(item.id)
                .update(
                    mapOf(
                        "collectedAmount" to newCollected,
                        "contributorCount" to (item.contributorCount + 1L),
                        "lastContributorName" to contributorName.trim().ifBlank { "Invitado" }.take(80),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleThankYouSent(itemId: String, sent: Boolean): Result<Unit> {
        var path = "users/registryItems/$itemId"
        return try {
            val uid = requireUserId()
            path = "users/$uid/registryItems/$itemId"
            db.collection("users").document(uid).collection("registryItems").document(itemId)
                .update(
                    mapOf(
                        "thankYouSent" to sent,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Stationery Visual Editor (RF-17) ---

    suspend fun saveStationeryDesign(
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
    ): Result<Unit> {
        var path = "users/stationeryDesigns/$designId"
        return try {
            val uid = requireUserId()
            val safeId = designId.ifBlank { "inv_${productType.lowercase()}" }
            val ref = db.collection("users").document(uid).collection("stationeryDesigns").document(safeId)
            path = ref.path
            val snap = ref.get().await()
            val fields = mapOf(
                "suiteId" to suiteId.take(60),
                "productType" to if (productType in listOf("INVITATION", "SAVE_THE_DATE", "MENU", "ENVELOPE")) productType else "INVITATION",
                "headline" to headline.trim().ifBlank { "Nuestra Boda" }.take(120),
                "subheadline" to subheadline.trim().take(120),
                "bodyText" to bodyText.trim().take(600),
                "bgColorHex" to bgColorHex.take(9),
                "accentColorHex" to accentColorHex.take(9),
                "fontStyle" to fontStyle.take(40),
                "bleedValid" to bleedValid,
                "proofApproved" to proofApproved,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (snap.exists()) {
                ref.update(fields).await()
            } else {
                ref.set(fields + ("createdAt" to FieldValue.serverTimestamp())).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Wedding Website Customization (RF-14) ---

    suspend fun updateWeddingWebsite(
        selectedSuiteId: String,
        headline: String,
        story: String,
        venueName: String,
        schedule: String,
        websiteProtected: Boolean,
        slug: String
    ): Result<Unit> {
        var path = "weddings"
        return try {
            val uid = requireUserId()
            path = "weddings/$uid"
            db.collection("weddings").document(uid).update(
                mapOf(
                    "selectedSuiteId" to selectedSuiteId.take(60),
                    "websiteHeadline" to headline.trim().take(140),
                    "websiteStory" to story.trim().take(2000),
                    "websiteVenueName" to venueName.trim().take(120),
                    "websiteSchedule" to schedule.trim().take(1500),
                    "websiteProtected" to websiteProtected,
                    "slug" to slug.trim().ifBlank { "nuestra-boda" }.take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Vendor Portal: Profile, Catalog, Requests -> Automatic Tasks + Calendar (RF-09 to RF-13) ---

    suspend fun saveVendorProfile(
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
    ): Result<Unit> {
        var path = "vendors"
        return try {
            val uid = requireUserId()
            path = "vendors/$uid"
            val ref = db.collection("vendors").document(uid)
            val snap = ref.get().await()
            val safeMin = minPrice.coerceIn(0.0, 50000000.0)
            val safeMax = maxPrice.coerceIn(safeMin, 50000000.0)
            val fields = mapOf(
                "businessName" to businessName.trim().ifBlank { "Mi Estudio de Bodas" }.take(100),
                "category" to category.trim().ifBlank { "Fotografía" }.take(60),
                "description" to description.trim().take(800),
                "city" to city.trim().ifBlank { "Ciudad de México" }.take(100),
                "lat" to lat.coerceIn(-90.0, 90.0),
                "lng" to lng.coerceIn(-180.0, 180.0),
                "coverageRadiusKm" to coverageRadiusKm.coerceIn(1.0, 2000.0),
                "minPrice" to safeMin,
                "maxPrice" to safeMax,
                "styleTags" to styleTags.take(8).ifEmpty { listOf("Clásico") },
                "published" to published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (snap.exists()) {
                ref.update(fields).await()
            } else {
                ref.set(
                    fields + mapOf(
                        "ownerId" to uid,
                        "ratingAvg" to 4.9,
                        "completedEventsCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCatalogItem(
        itemId: String?,
        title: String,
        description: String,
        basePrice: Double,
        pricingUnit: String,
        active: Boolean
    ): Result<Unit> {
        var path = "vendors/catalogItems"
        return try {
            val uid = requireUserId()
            val col = db.collection("vendors").document(uid).collection("catalogItems")
            if (itemId.isNullOrBlank()) {
                val docRef = col.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "vendorOwnerId" to uid,
                        "title" to title.trim().ifBlank { "Paquete de Servicio" }.take(100),
                        "description" to description.trim().take(500),
                        "basePrice" to basePrice.coerceIn(0.0, 50000000.0),
                        "pricingUnit" to pricingUnit.trim().ifBlank { "Por evento" }.take(50),
                        "active" to active,
                        "published" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = col.document(itemId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "title" to title.trim().ifBlank { "Paquete de Servicio" }.take(100),
                        "description" to description.trim().take(500),
                        "basePrice" to basePrice.coerceIn(0.0, 50000000.0),
                        "pricingUnit" to pricingUnit.trim().ifBlank { "Por evento" }.take(50),
                        "active" to active,
                        "published" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun sendServiceRequest(
        vendor: VendorProfile,
        coupleName: String,
        catalogItemTitle: String,
        eventDate: String,
        proposedBudget: Double,
        notes: String
    ): Result<Unit> {
        var path = "serviceRequests"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("serviceRequests").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "coupleId" to uid,
                    "vendorId" to vendor.id,
                    "vendorOwnerId" to vendor.ownerId,
                    "coupleName" to coupleName.trim().ifBlank { "Pareja Nupcia" }.take(100),
                    "vendorName" to vendor.businessName.take(100),
                    "category" to vendor.category.take(60),
                    "catalogItemTitle" to catalogItemTitle.trim().ifBlank { "Paquete Personalizado" }.take(100),
                    "eventDate" to eventDate.trim().ifBlank { "2027-06-19" }.take(30),
                    "proposedBudget" to proposedBudget.coerceIn(0.0, 50000000.0),
                    "counterOfferBudget" to 0.0,
                    "notes" to notes.trim().take(600),
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * RF-10 & Business Rule #2: Atomic WriteBatch when vendor accepts a ServiceRequest:
     * 1. Marks serviceRequest status = ACCEPTED
     * 2. Creates BOOKED block in vendor's calendarBlocks
     * 3. Auto-generates 4 milestone operational tasks in vendor's vendorTasks
     */
    suspend fun acceptServiceRequestAtomically(request: ServiceRequest): Result<Unit> {
        var path = "serviceRequests/${request.id}"
        return try {
            val uid = requireUserId()
            check(uid == request.vendorOwnerId) { "Solo el proveedor propietario puede aceptar esta solicitud." }
            val batch = db.batch()

            // 1. Update service request status
            val reqRef = db.collection("serviceRequests").document(request.id)
            batch.update(
                reqRef,
                mapOf(
                    "status" to "ACCEPTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // 2. Block event date in vendor calendar
            val blockRef = db.collection("users").document(uid)
                .collection("calendarBlocks").document("blk_${request.id}")
            batch.set(
                blockRef,
                mapOf(
                    "serviceRequestId" to request.id,
                    "title" to "Boda ${request.coupleName} (${request.category})",
                    "startDate" to request.eventDate,
                    "endDate" to request.eventDate,
                    "blockType" to "BOOKED",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // 3. Auto-generate operational milestone tasks
            val milestones = listOf(
                Triple(
                    "Confirmación de contrato y anticipo",
                    "Enviar propuesta formal de ${request.catalogItemTitle} y confirmar pago de reserva.",
                    "Inmediato (+3 días)"
                ),
                Triple(
                    "Reunión de alineación creativa",
                    "Validar estilo, paleta y expectativas con ${request.coupleName}.",
                    "45 días antes del evento"
                ),
                Triple(
                    "Confirmación de logística y horarios",
                    "Cerrar acceso al venue, tiempos de montaje y requerimientos técnicos.",
                    "10 días antes del evento"
                ),
                Triple(
                    "Ejecución del servicio y cierre post-evento",
                    "Entregar el servicio el ${request.eventDate} y solicitar reseña de la pareja.",
                    request.eventDate
                )
            )

            milestones.forEachIndexed { idx, (title, desc, due) ->
                val taskRef = db.collection("users").document(uid)
                    .collection("vendorTasks").document("task_${request.id}_${idx + 1}")
                batch.set(
                    taskRef,
                    mapOf(
                        "serviceRequestId" to request.id,
                        "coupleName" to request.coupleName,
                        "category" to request.category,
                        "title" to title,
                        "description" to desc,
                        "dueDate" to due,
                        "eventDate" to request.eventDate,
                        "status" to "PENDING",
                        "milestone" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun counterOfferServiceRequest(
        requestId: String,
        counterAmount: Double,
        notes: String
    ): Result<Unit> {
        val path = "serviceRequests/$requestId"
        return try {
            requireUserId()
            db.collection("serviceRequests").document(requestId).update(
                mapOf(
                    "status" to "COUNTERED",
                    "counterOfferBudget" to counterAmount.coerceIn(0.0, 50000000.0),
                    "notes" to notes.trim().take(600),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateServiceRequestStatus(
        request: ServiceRequest,
        newStatus: String,
        relatedVendorTasks: List<VendorTask> = emptyList()
    ): Result<Unit> {
        val path = "serviceRequests/${request.id}"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            val reqRef = db.collection("serviceRequests").document(request.id)
            batch.update(
                reqRef,
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Business Rule #2: If cancelled by vendor, cancel pending tasks and free calendar block
            if (newStatus == "CANCELLED" && uid == request.vendorOwnerId) {
                relatedVendorTasks
                    .filter { it.serviceRequestId == request.id && it.status != "COMPLETED" }
                    .forEach { task ->
                        val tRef = db.collection("users").document(uid)
                            .collection("vendorTasks").document(task.id)
                        batch.update(tRef, mapOf("status" to "CANCELLED", "updatedAt" to FieldValue.serverTimestamp()))
                    }
                val blkRef = db.collection("users").document(uid)
                    .collection("calendarBlocks").document("blk_${request.id}")
                batch.delete(blkRef)
            }

            // Business Rule #3: When vendor marks ACCEPTED event as COMPLETED, increment completedEventsCount
            if (newStatus == "COMPLETED" && uid == request.vendorOwnerId) {
                val vendorRef = db.collection("vendors").document(request.vendorId)
                val vSnap = vendorRef.get().await()
                if (vSnap.exists() && vSnap.getString("ownerId") == uid) {
                    val curCount = vSnap.getLong("completedEventsCount") ?: 0L
                    batch.update(
                        vendorRef,
                        mapOf(
                            "completedEventsCount" to (curCount + 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateVendorTaskStatus(taskId: String, newStatus: String): Result<Unit> {
        var path = "users/vendorTasks/$taskId"
        return try {
            val uid = requireUserId()
            path = "users/$uid/vendorTasks/$taskId"
            val safe = if (newStatus in listOf("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED")) newStatus else "PENDING"
            db.collection("users").document(uid).collection("vendorTasks").document(taskId)
                .update(
                    mapOf(
                        "status" to safe,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addManualCalendarBlock(title: String, date: String): Result<Unit> {
        var path = "users/calendarBlocks"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("calendarBlocks").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "serviceRequestId" to "",
                    "title" to title.trim().ifBlank { "Fecha bloqueada" }.take(120),
                    "startDate" to date.trim().ifBlank { "2027-07-04" }.take(30),
                    "endDate" to date.trim().ifBlank { "2027-07-04" }.take(30),
                    "blockType" to "MANUAL_BLOCK",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCalendarBlock(blockId: String): Result<Unit> {
        var path = "users/calendarBlocks/$blockId"
        return try {
            val uid = requireUserId()
            path = "users/$uid/calendarBlocks/$blockId"
            db.collection("users").document(uid).collection("calendarBlocks").document(blockId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    /**
     * Explicit opt-in marketplace directory loader for MVP testing near the couple's city.
     * Owned by current user so they can also test the Vendor Portal request -> task workflow end-to-end.
     */
    suspend fun seedDemoMarketplaceVendors(city: String, baseLat: Double, baseLng: Double): Result<Unit> {
        val path = "vendors"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            val demoVendors = listOf(
                VendorProfile(
                    id = "demo_venue_${uid.take(8)}",
                    businessName = "Hacienda Los Laureles",
                    category = "Lugar",
                    description = "Jardines centenarios, capilla abierta y terraza panorámica para bodas memorables.",
                    city = city,
                    lat = baseLat + 0.045,
                    lng = baseLng - 0.032,
                    coverageRadiusKm = 120.0,
                    minPrice = 65000.0,
                    maxPrice = 145000.0,
                    styleTags = listOf("Jardín", "Clásico", "Romántico"),
                    ratingAvg = 4.9,
                    completedEventsCount = 28
                ),
                VendorProfile(
                    id = "demo_photo_${uid.take(8)}",
                    businessName = "Luz & Grano Editorial",
                    category = "Fotografía",
                    description = "Fotografía documental y editorial con luz natural, álbum artesanal impreso.",
                    city = city,
                    lat = baseLat - 0.025,
                    lng = baseLng + 0.041,
                    coverageRadiusKm = 250.0,
                    minPrice = 28000.0,
                    maxPrice = 58000.0,
                    styleTags = listOf("Editorial", "Minimalista", "Moderno"),
                    ratingAvg = 5.0,
                    completedEventsCount = 41
                ),
                VendorProfile(
                    id = "demo_floral_${uid.take(8)}",
                    businessName = "Atelier Botánico Silvestre",
                    category = "Flores",
                    description = "Instalaciones aéreas, arcos orgánicos y arreglos de mesa de temporada.",
                    city = city,
                    lat = baseLat + 0.018,
                    lng = baseLng + 0.028,
                    coverageRadiusKm = 90.0,
                    minPrice = 22000.0,
                    maxPrice = 65000.0,
                    styleTags = listOf("Jardín", "Bohemio", "Romántico"),
                    ratingAvg = 4.8,
                    completedEventsCount = 19
                ),
                VendorProfile(
                    id = "demo_catering_${uid.take(8)}",
                    businessName = "Mesa de Autor Banquetes",
                    category = "Banquete",
                    description = "Cocina de temporada de 4 tiempos, barra de coctelería botánica y opciones veganas.",
                    city = city,
                    lat = baseLat - 0.040,
                    lng = baseLng - 0.022,
                    coverageRadiusKm = 150.0,
                    minPrice = 70000.0,
                    maxPrice = 160000.0,
                    styleTags = listOf("Clásico", "Moderno", "Editorial"),
                    ratingAvg = 4.9,
                    completedEventsCount = 34
                ),
                VendorProfile(
                    id = "demo_music_${uid.take(8)}",
                    businessName = "Velada Quartet & DJ Collective",
                    category = "Música",
                    description = "Cuarteto de cuerdas para ceremonia + DJ híbrido con saxofón en vivo para pista.",
                    city = city,
                    lat = baseLat + 0.062,
                    lng = baseLng + 0.012,
                    coverageRadiusKm = 180.0,
                    minPrice = 18000.0,
                    maxPrice = 42000.0,
                    styleTags = listOf("Moderno", "Destino", "Clásico"),
                    ratingAvg = 4.7,
                    completedEventsCount = 23
                )
            )
            demoVendors.forEach { v ->
                val ref = db.collection("vendors").document(v.id)
                batch.set(
                    ref,
                    mapOf(
                        "ownerId" to uid,
                        "businessName" to v.businessName,
                        "category" to v.category,
                        "description" to v.description,
                        "city" to v.city,
                        "lat" to v.lat,
                        "lng" to v.lng,
                        "coverageRadiusKm" to v.coverageRadiusKm,
                        "minPrice" to v.minPrice,
                        "maxPrice" to v.maxPrice,
                        "styleTags" to v.styleTags,
                        "ratingAvg" to v.ratingAvg,
                        "completedEventsCount" to v.completedEventsCount,
                        "published" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
