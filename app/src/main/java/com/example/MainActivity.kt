package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.signOutUser
import com.example.data.NupciaRepository
import com.example.data.WeddingAiService
import com.example.ui.screens.AiBudgetChatScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CoupleHomeScreen
import com.example.ui.screens.EventHubScreen
import com.example.ui.screens.GeospatialMapScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.VendorPortalScreen
import com.example.ui.theme.NupciaTheme
import com.example.ui.viewmodel.NupciaViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            NupciaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* authStateFlow updates automatically */ })
    } else {
        // Key by UID so switching accounts tears down and recreates ViewModel + listeners cleanly
        key(user.uid) {
            AuthenticatedNupciaShell(
                currentUserId = user.uid,
                initialDisplayName = user.displayName ?: "Nupcia Bride"
            )
        }
    }
}

@Composable
private fun AuthenticatedNupciaShell(
    currentUserId: String,
    initialDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: NupciaViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                NupciaViewModel(
                    repository = NupciaRepository(db),
                    aiService = WeddingAiService(),
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName
                )
            }
        }
    )

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val wedding by viewModel.wedding.collectAsStateWithLifecycle()
    val budgetAllocations by viewModel.budgetAllocations.collectAsStateWithLifecycle()
    val guests by viewModel.guests.collectAsStateWithLifecycle()
    val registryItems by viewModel.registryItems.collectAsStateWithLifecycle()
    val stationeryDesigns by viewModel.stationeryDesigns.collectAsStateWithLifecycle()
    val scoredVendors by viewModel.scoredVendors.collectAsStateWithLifecycle()
    val coupleRequests by viewModel.coupleRequests.collectAsStateWithLifecycle()
    val readiness by viewModel.readinessReport.collectAsStateWithLifecycle()

    val myVendorProfile by viewModel.myVendorProfile.collectAsStateWithLifecycle()
    val myCatalogItems by viewModel.myCatalogItems.collectAsStateWithLifecycle()
    val vendorRequests by viewModel.vendorRequests.collectAsStateWithLifecycle()
    val vendorTasks by viewModel.vendorTasks.collectAsStateWithLifecycle()
    val vendorCalendarBlocks by viewModel.vendorCalendarBlocks.collectAsStateWithLifecycle()

    val selectedCoupleTab by viewModel.selectedCoupleTab.collectAsStateWithLifecycle()
    val highlightedVendorId by viewModel.highlightedVendorId.collectAsStateWithLifecycle()
    val chatTurns by viewModel.chatTurns.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val handleSignOut: () -> Unit = {
        signOutUser(credentialManager, scope)
    }

    when (val state = userProfileState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        is UiState.Success -> {
            val profile = state.data
            val workspaceMode = profile?.workspaceMode ?: "COUPLE"
            val onboardingDone = profile?.onboardingDone ?: false

            if (workspaceMode == "VENDOR") {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        VendorPortalScreen(
                            vendorProfile = myVendorProfile,
                            catalogItems = myCatalogItems,
                            requests = vendorRequests,
                            tasks = vendorTasks,
                            calendarBlocks = vendorCalendarBlocks,
                            onAcceptRequest = viewModel::acceptRequestAsVendor,
                            onCounterOffer = viewModel::counterOfferAsVendor,
                            onTransitionRequest = viewModel::transitionRequestStatus,
                            onCycleTask = viewModel::cycleVendorTaskStatus,
                            onSaveProfile = viewModel::saveVendorProfile,
                            onSaveCatalogItem = viewModel::saveCatalogPackage,
                            onAddManualBlock = viewModel::addManualBlock,
                            onDeleteCalendarBlock = viewModel::deleteCalendarBlock,
                            onSwitchToCouple = { viewModel.switchWorkspace("COUPLE") },
                            onSignOut = handleSignOut
                        )
                    }
                }
            } else if (!onboardingDone) {
                OnboardingScreen(
                    initialName = initialDisplayName,
                    isBusy = isBusy,
                    onSwitchToVendor = { viewModel.switchWorkspace("VENDOR") },
                    onCompleteCoupleOnboarding = viewModel::completeOnboarding
                )
            } else {
                val navItems = listOf(
                    Triple("Home", Icons.Default.Home, "nav_home"),
                    Triple("AI", Icons.Default.AutoAwesome, "nav_ai"),
                    Triple("Map", Icons.Default.Map, "nav_map"),
                    Triple("Event", Icons.Default.Celebration, "nav_event")
                )

                BackHandler(enabled = selectedCoupleTab != 0) {
                    viewModel.selectCoupleTab(0)
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 600.dp

                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                    navItems.forEachIndexed { idx, (label, icon, tag) ->
                                        NavigationBarItem(
                                            selected = selectedCoupleTab == idx,
                                            onClick = { viewModel.selectCoupleTab(idx) },
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label) },
                                            modifier = Modifier.testTag(tag)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                        ) {
                            if (useRail) {
                                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                                    navItems.forEachIndexed { idx, (label, icon, tag) ->
                                        NavigationRailItem(
                                            selected = selectedCoupleTab == idx,
                                            onClick = { viewModel.selectCoupleTab(idx) },
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label) },
                                            modifier = Modifier.testTag(tag)
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                when (selectedCoupleTab) {
                                    0 -> CoupleHomeScreen(
                                        wedding = wedding,
                                        allocations = budgetAllocations,
                                        requests = coupleRequests,
                                        readiness = readiness,
                                        onNavigateTab = viewModel::selectCoupleTab,
                                        onUpdateAllocation = viewModel::updateAllocation,
                                        onRequestAction = viewModel::transitionRequestStatus,
                                        onSwitchToVendor = { viewModel.switchWorkspace("VENDOR") },
                                        onSignOut = handleSignOut
                                    )
                                    1 -> AiBudgetChatScreen(
                                        turns = chatTurns,
                                        isThinking = isAiThinking,
                                        onSendPrompt = viewModel::sendAiPrompt,
                                        onApplyProposal = viewModel::applyProposalFromChat,
                                        onViewVendorOnMap = viewModel::openVendorOnMap,
                                        onSendInquiry = viewModel::sendInquiryToVendor
                                    )
                                    2 -> GeospatialMapScreen(
                                        wedding = wedding,
                                        scoredVendors = scoredVendors,
                                        highlightedVendorId = highlightedVendorId,
                                        isBusy = isBusy,
                                        onClearHighlight = viewModel::clearHighlightedVendor,
                                        onSendInquiry = viewModel::sendInquiryToVendor,
                                        onLoadDemoVendors = viewModel::loadOptInDemoVendors,
                                        onSwitchToVendorPortal = { viewModel.switchWorkspace("VENDOR") }
                                    )
                                    else -> EventHubScreen(
                                        wedding = wedding,
                                        guests = guests,
                                        registryItems = registryItems,
                                        stationeryDesigns = stationeryDesigns,
                                        onSaveGuest = viewModel::saveGuest,
                                        onDeleteGuest = viewModel::deleteGuest,
                                        onAddRegistryItem = viewModel::addRegistryItem,
                                        onAddContribution = viewModel::addContribution,
                                        onToggleThankYou = viewModel::toggleThankYou,
                                        onSaveStationery = viewModel::saveStationery,
                                        onSaveWebsite = viewModel::saveWeddingWebsite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
