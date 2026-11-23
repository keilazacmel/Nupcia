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
            ?: throw IllegalStateException("Please sign in with Google to continue.")
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
                    if (snap.exists()) {
                        snap.toObject(Wedding::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.let(EnglishLocaleNormalizer::normalizeWedding)
                    } else null
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
                        .map(EnglishLocaleNormalizer::normalizeAllocation)
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
                        .map(EnglishLocaleNormalizer::normalizeGuest)
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
                        .map(EnglishLocaleNormalizer::normalizeStationery)
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
                        .map(EnglishLocaleNormalizer::normalizeVendor)
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
                    if (snap.exists()) {
                        snap.toObject(VendorProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.let(EnglishLocaleNormalizer::normalizeVendor)
                    } else null
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
                        .map(EnglishLocaleNormalizer::normalizeServiceRequest)
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
                        .map(EnglishLocaleNormalizer::normalizeServiceRequest)
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
                        .map(EnglishLocaleNormalizer::normalizeVendorTask)
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
                        .map(EnglishLocaleNormalizer::normalizeCalendarBlock)
                        .sortedBy { it.startDate }
                }
                .handleListenerErrors(userId, path, OperationType.LIST)
        )
    }

    // --- Mutations (All validate inputs, use serverTimestamp(), and return Result<T>) ---

    suspend fun ensureUserProfileExists(displayName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "Nupcia User" }.take(80)
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
                        "displayName" to (auth.currentUser?.displayName?.ifBlank { "Nupcia User" } ?: "Nupcia User").take(80),
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
            val safeBride = brideName.trim().ifBlank { "Bride" }.take(80)
            val safePartner = partnerName.trim().ifBlank { "Partner" }.take(80)
            val safeCity = city.trim().ifBlank { "Provo, UT" }.take(100)
            val safeBudget = totalBudget.coerceIn(1000.0, 50000000.0)
            val safeGuests = estimatedGuests.coerceIn(2L, 5000L)
            val safeStyles = styleTags.take(8).ifEmpty { listOf("Classic") }
            val safePalette = colorPalette.take(6).ifEmpty { listOf("#7B2D3B", "#FAF6F1", "#536B5E") }
            val safePriorities = priorityOrder.take(10).ifEmpty { DefaultWeddingCatalog.CATEGORIES }
            val matchedSuite = DefaultWeddingCatalog.suiteForStyles(safeStyles)
            val slug = "${safeBride.lowercase()}-and-${safePartner.lowercase()}"
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifBlank { "our-wedding" }
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
                "ceremonyType" to ceremonyType.trim().ifBlank { "Outdoor Garden" }.take(60),
                "visionExpectations" to visionExpectations.trim().take(1000),
                "priorityOrder" to safePriorities,
                "websiteHeadline" to "$safeBride & $safePartner",
                "websiteStory" to "We would love to celebrate the beginning of our next chapter with you in $safeCity.",
                "websiteVenueName" to "To be announced in $safeCity",
                "websiteSchedule" to "4:30 PM Ceremony • 6:00 PM Cocktail Hour • 7:30 PM Dinner & Dancing",
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

            // 3. Initial Priority-Weighted Budget Allocations (idempotent to preserve createdAt)
            val existingAllocIds = db.collection("users").document(uid)
                .collection("budgetAllocations").get().await().documents.map { it.id }.toSet()
            val allocations = BudgetRebalancer.computeInitialAllocations(safeBudget, safePriorities)
            allocations.forEachIndexed { idx, alloc ->
                val docId = "cat_${idx + 1}"
                val allocRef = db.collection("users").document(uid)
                    .collection("budgetAllocations").document(docId)
                val baseFields = mapOf(
                    "category" to alloc.category,
                    "allocatedAmount" to alloc.allocatedAmount,
                    "spentAmount" to 0.0,
                    "priorityRank" to alloc.priorityRank,
                    "locked" to false,
                    "notes" to alloc.notes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (docId in existingAllocIds) {
                    batch.update(allocRef, baseFields)
                } else {
                    batch.set(allocRef, baseFields + ("createdAt" to FieldValue.serverTimestamp()))
                }
            }

            // 4. Coordinated Starter Invitation Design matching suite (idempotent to preserve createdAt)
            val invRef = db.collection("users").document(uid)
                .collection("stationeryDesigns").document("inv_main")
            val invSnap = invRef.get().await()
            val invFields = mapOf(
                "suiteId" to matchedSuite.id,
                "productType" to "INVITATION",
                "headline" to "$safeBride & $safePartner",
                "subheadline" to "${weddingDate.trim().ifBlank { "2027-06-19" }} • $safeCity",
                "bodyText" to "Together with our families, we joyfully invite you to celebrate our wedding. Please RSVP on our wedding website.",
                "bgColorHex" to matchedSuite.bgHex,
                "accentColorHex" to matchedSuite.accentHex,
                "fontStyle" to "EDITORIAL_SERIF",
                "bleedValid" to true,
                "proofApproved" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (invSnap.exists()) {
                batch.update(invRef, invFields)
            } else {
                batch.set(invRef, invFields + ("createdAt" to FieldValue.serverTimestamp()))
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
            val safeHousehold = householdName.trim().ifBlank { "Guest Household" }.take(80)
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
                    "title" to title.trim().ifBlank { "Wedding Fund" }.take(100),
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
                        "lastContributorName" to contributorName.trim().ifBlank { "Guest" }.take(80),
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
                "headline" to headline.trim().ifBlank { "Our Wedding" }.take(120),
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
                    "slug" to slug.trim().ifBlank { "our-wedding" }.take(80),
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
                "businessName" to businessName.trim().ifBlank { "My Wedding Studio" }.take(100),
                "category" to category.trim().ifBlank { "Photography" }.take(60),
                "description" to description.trim().take(800),
                "city" to city.trim().ifBlank { "Provo, UT" }.take(100),
                "lat" to lat.coerceIn(-90.0, 90.0),
                "lng" to lng.coerceIn(-180.0, 180.0),
                "coverageRadiusKm" to coverageRadiusKm.coerceIn(1.0, 2000.0),
                "minPrice" to safeMin,
                "maxPrice" to safeMax,
                "styleTags" to styleTags.take(8).ifEmpty { listOf("Classic") },
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
                        "title" to title.trim().ifBlank { "Service Package" }.take(100),
                        "description" to description.trim().take(500),
                        "basePrice" to basePrice.coerceIn(0.0, 50000000.0),
                        "pricingUnit" to pricingUnit.trim().ifBlank { "Per event" }.take(50),
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
                        "title" to title.trim().ifBlank { "Service Package" }.take(100),
                        "description" to description.trim().take(500),
                        "basePrice" to basePrice.coerceIn(0.0, 50000000.0),
                        "pricingUnit" to pricingUnit.trim().ifBlank { "Per event" }.take(50),
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
                    "coupleName" to coupleName.trim().ifBlank { "Nupcia Couple" }.take(100),
                    "vendorName" to vendor.businessName.take(100),
                    "category" to vendor.category.take(60),
                    "catalogItemTitle" to catalogItemTitle.trim().ifBlank { "Custom Package" }.take(100),
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
            check(uid == request.vendorOwnerId) { "Only the vendor owner can accept this inquiry." }
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
                    "title" to "${request.coupleName} Wedding (${request.category})",
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
                    "Contract & Deposit Confirmation",
                    "Send formal proposal for ${request.catalogItemTitle} and confirm booking deposit.",
                    "Immediate (+3 days)"
                ),
                Triple(
                    "Creative Alignment Briefing",
                    "Review aesthetic style, color palette, and vision with ${request.coupleName}.",
                    "45 days before event"
                ),
                Triple(
                    "Final Logistics & Venue Access",
                    "Confirm venue load-in times, setup schedule, and technical needs.",
                    "10 days before event"
                ),
                Triple(
                    "Event Execution & Post-Event Review",
                    "Deliver services on ${request.eventDate} and invite couple review.",
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
                    "title" to title.trim().ifBlank { "Blocked Date" }.take(120),
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
                    businessName = "Timpanogos Garden Estate",
                    category = "Venue",
                    description = "Manicured foothills gardens, open-air pavilion, and mountain views in Provo Canyon.",
                    city = "Sundance / Provo Canyon",
                    lat = 40.3785,
                    lng = -111.5960,
                    coverageRadiusKm = 65.0,
                    minPrice = 4500.0,
                    maxPrice = 11000.0,
                    styleTags = listOf("Garden", "Classic", "Mountain"),
                    ratingAvg = 4.9,
                    completedEventsCount = 28
                ),
                VendorProfile(
                    id = "demo_photo_${uid.take(8)}",
                    businessName = "Wasatch Light & Film Co.",
                    category = "Photography",
                    description = "Editorial documentary & film wedding photography with heirloom linen albums.",
                    city = "Provo, UT",
                    lat = 40.2444,
                    lng = -111.6485,
                    coverageRadiusKm = 120.0,
                    minPrice = 2200.0,
                    maxPrice = 4800.0,
                    styleTags = listOf("Editorial", "Minimalist", "Modern"),
                    ratingAvg = 5.0,
                    completedEventsCount = 41
                ),
                VendorProfile(
                    id = "demo_floral_${uid.take(8)}",
                    businessName = "Wild Sage Floral Studio",
                    category = "Florals",
                    description = "Organic floral arches, seasonal Utah blooms, and candlelit reception tablescapes.",
                    city = "Lehi, UT",
                    lat = 40.3880,
                    lng = -111.8320,
                    coverageRadiusKm = 80.0,
                    minPrice = 1800.0,
                    maxPrice = 4500.0,
                    styleTags = listOf("Garden", "Bohemian", "Romantic"),
                    ratingAvg = 4.8,
                    completedEventsCount = 19
                ),
                VendorProfile(
                    id = "demo_catering_${uid.take(8)}",
                    businessName = "Canyon Table Catering",
                    category = "Catering",
                    description = "Locally sourced seasonal plated dinners, mocktail craft bars, and artisan grazing tables.",
                    city = "American Fork, UT",
                    lat = 40.3690,
                    lng = -111.7850,
                    coverageRadiusKm = 90.0,
                    minPrice = 5000.0,
                    maxPrice = 12500.0,
                    styleTags = listOf("Classic", "Modern", "Editorial"),
                    ratingAvg = 4.9,
                    completedEventsCount = 34
                ),
                VendorProfile(
                    id = "demo_music_${uid.take(8)}",
                    businessName = "Valley Strings & DJ Collective",
                    category = "Music",
                    description = "Live string quartet for ceremonies plus modern reception DJ and sound production.",
                    city = "Springville, UT",
                    lat = 40.1720,
                    lng = -111.6150,
                    coverageRadiusKm = 100.0,
                    minPrice = 1400.0,
                    maxPrice = 3200.0,
                    styleTags = listOf("Modern", "Classic", "Romantic"),
                    ratingAvg = 4.8,
                    completedEventsCount = 23
                )
            )
            val existingOwnedVendorIds = db.collection("vendors")
                .whereEqualTo("ownerId", uid)
                .get()
                .await()
                .documents
                .map { it.id }
                .toSet()

            demoVendors.forEach { v ->
                val ref = db.collection("vendors").document(v.id)
                val baseFields = mapOf(
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
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (v.id in existingOwnedVendorIds) {
                    batch.update(ref, baseFields)
                } else {
                    batch.set(
                        ref,
                        baseFields + mapOf(
                            "ownerId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
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

    /**
     * Silently migrates any legacy Spanish/Mexico Firestore documents owned by the signed-in user
     * into English and Utah County, Utah defaults while strictly honoring firestore.rules update sets.
     */
    suspend fun migrateLegacySpanishDataToUtahEnglish() {
        val uid = auth.currentUser?.uid ?: return

        // 1. Wedding document (split into core update + website update per firestore.rules)
        try {
            val wRef = db.collection("weddings").document(uid)
            val wSnap = wRef.get().await()
            if (wSnap.exists()) {
                val raw = wSnap.toObject(Wedding::class.java)
                if (raw != null) {
                    val norm = EnglishLocaleNormalizer.normalizeWedding(raw)
                    val coreChanged = raw.city != norm.city || raw.lat != norm.lat || raw.lng != norm.lng ||
                        raw.styleTags != norm.styleTags || raw.ceremonyType != norm.ceremonyType ||
                        raw.priorityOrder != norm.priorityOrder || raw.visionExpectations != norm.visionExpectations
                    if (coreChanged) {
                        wRef.update(
                            mapOf(
                                "city" to norm.city,
                                "lat" to norm.lat,
                                "lng" to norm.lng,
                                "styleTags" to norm.styleTags,
                                "ceremonyType" to norm.ceremonyType,
                                "visionExpectations" to norm.visionExpectations,
                                "priorityOrder" to norm.priorityOrder,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                    val webChanged = raw.websiteHeadline != norm.websiteHeadline ||
                        raw.websiteStory != norm.websiteStory ||
                        raw.websiteVenueName != norm.websiteVenueName ||
                        raw.websiteSchedule != norm.websiteSchedule ||
                        raw.slug != norm.slug
                    if (webChanged) {
                        wRef.update(
                            mapOf(
                                "websiteHeadline" to norm.websiteHeadline,
                                "websiteStory" to norm.websiteStory,
                                "websiteVenueName" to norm.websiteVenueName,
                                "websiteSchedule" to norm.websiteSchedule,
                                "slug" to norm.slug,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort migration; read-time normalizer still guarantees English UI
        }

        // 2. Budget Allocations
        try {
            val allocDocs = db.collection("users").document(uid).collection("budgetAllocations").get().await()
            val batch = db.batch()
            var ops = 0
            allocDocs.documents.forEach { doc ->
                val raw = doc.toObject(BudgetAllocation::class.java) ?: return@forEach
                val norm = EnglishLocaleNormalizer.normalizeAllocation(raw)
                if (raw.category != norm.category || raw.notes != norm.notes) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "category" to norm.category,
                            "notes" to norm.notes,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    ops++
                }
            }
            if (ops > 0) batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        // 3. Stationery Designs
        try {
            val statDocs = db.collection("users").document(uid).collection("stationeryDesigns").get().await()
            val batch = db.batch()
            var ops = 0
            statDocs.documents.forEach { doc ->
                val raw = doc.toObject(StationeryDesign::class.java) ?: return@forEach
                val norm = EnglishLocaleNormalizer.normalizeStationery(raw)
                if (raw.headline != norm.headline || raw.subheadline != norm.subheadline || raw.bodyText != norm.bodyText) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "headline" to norm.headline,
                            "subheadline" to norm.subheadline,
                            "bodyText" to norm.bodyText,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    ops++
                }
            }
            if (ops > 0) batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        // 4. Guests
        try {
            val guestDocs = db.collection("users").document(uid).collection("guests").get().await()
            val batch = db.batch()
            var ops = 0
            guestDocs.documents.forEach { doc ->
                val raw = doc.toObject(Guest::class.java) ?: return@forEach
                val norm = EnglishLocaleNormalizer.normalizeGuest(raw)
                if (raw.householdName != norm.householdName || raw.mealChoice != norm.mealChoice) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "householdName" to norm.householdName,
                            "mealChoice" to norm.mealChoice,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    ops++
                }
            }
            if (ops > 0) batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        // 5. Vendor Tasks (only update rule-allowed keys: title, description, dueDate, updatedAt)
        try {
            val taskDocs = db.collection("users").document(uid).collection("vendorTasks").get().await()
            val batch = db.batch()
            var ops = 0
            taskDocs.documents.forEach { doc ->
                val raw = doc.toObject(VendorTask::class.java) ?: return@forEach
                val norm = EnglishLocaleNormalizer.normalizeVendorTask(raw)
                if (raw.title != norm.title || raw.description != norm.description || raw.dueDate != norm.dueDate) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "title" to norm.title,
                            "description" to norm.description,
                            "dueDate" to norm.dueDate,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    ops++
                }
            }
            if (ops > 0) batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        // 6. Calendar Blocks
        try {
            val blkDocs = db.collection("users").document(uid).collection("calendarBlocks").get().await()
            val batch = db.batch()
            var ops = 0
            blkDocs.documents.forEach { doc ->
                val raw = doc.toObject(VendorCalendarBlock::class.java) ?: return@forEach
                val norm = EnglishLocaleNormalizer.normalizeCalendarBlock(raw)
                if (raw.title != norm.title) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "title" to norm.title,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    ops++
                }
            }
            if (ops > 0) batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        // 7. Owned Vendors (demo vendors + user's own vendor profile)
        try {
            val ownedVendors = db.collection("vendors").whereEqualTo("ownerId", uid).get().await()
            var needsDemoReseed = false
            val batch = db.batch()
            var ops = 0
            ownedVendors.documents.forEach { doc ->
                val raw = doc.toObject(VendorProfile::class.java) ?: return@forEach
                if (doc.id.startsWith("demo_") &&
                    (EnglishLocaleNormalizer.isLegacyLocation(raw.city, raw.lat) ||
                        raw.category != EnglishLocaleNormalizer.normalizeCategory(raw.category))
                ) {
                    needsDemoReseed = true
                } else {
                    val norm = EnglishLocaleNormalizer.normalizeVendor(raw)
                    if (raw.businessName != norm.businessName || raw.category != norm.category ||
                        raw.description != norm.description || raw.city != norm.city ||
                        raw.lat != norm.lat || raw.lng != norm.lng || raw.styleTags != norm.styleTags
                    ) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "businessName" to norm.businessName,
                                "category" to norm.category,
                                "description" to norm.description,
                                "city" to norm.city,
                                "lat" to norm.lat,
                                "lng" to norm.lng,
                                "styleTags" to norm.styleTags,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        ops++
                    }
                }
            }
            if (ops > 0) batch.commit().await()
            if (needsDemoReseed) {
                seedDemoMarketplaceVendors("Provo, UT", 40.2338, -111.6585)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}
    }
}
