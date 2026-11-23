package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.BudgetRebalancer
import com.example.domain.DefaultWeddingCatalog
import com.example.domain.ServiceRequest
import com.example.domain.VendorCalendarBlock
import com.example.domain.VendorCatalogItem
import com.example.domain.VendorProfile
import com.example.domain.VendorTask
import kotlin.math.roundToInt

@Composable
fun VendorPortalScreen(
    vendorProfile: VendorProfile?,
    catalogItems: List<VendorCatalogItem>,
    requests: List<ServiceRequest>,
    tasks: List<VendorTask>,
    calendarBlocks: List<VendorCalendarBlock>,
    onAcceptRequest: (ServiceRequest) -> Unit,
    onCounterOffer: (String, Double, String) -> Unit,
    onTransitionRequest: (ServiceRequest, String) -> Unit,
    onCycleTask: (VendorTask) -> Unit,
    onSaveProfile: (String, String, String, String, Double, Double, Double, Double, Double, List<String>, Boolean) -> Unit,
    onSaveCatalogItem: (String?, String, String, Double, String, Boolean) -> Unit,
    onAddManualBlock: (String, String) -> Unit,
    onDeleteCalendarBlock: (String) -> Unit,
    onSwitchToCouple: () -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    BackHandler(enabled = selectedTab != 0) { selectedTab = 0 }
    val tabs = listOf(
        "Inquiries & Tasks" to Icons.Default.TaskAlt,
        "Catalog" to Icons.Default.Inventory2,
        "Calendar" to Icons.Default.CalendarMonth,
        "Analytics" to Icons.Default.Analytics
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Vendor Header Bar
        Surface(color = MaterialTheme.colorScheme.primary, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vendorProfile?.businessName?.ifBlank { "Vendor Portal" } ?: "Vendor Portal",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "${requests.count { it.status == "PENDING" }} pending • ${tasks.count { it.status != "COMPLETED" && it.status != "CANCELLED" }} active tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
                IconButton(
                    onClick = onSwitchToCouple,
                    modifier = Modifier.testTag("switch_to_couple_button")
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Switch to Couple Portal", tint = MaterialTheme.colorScheme.onPrimary)
                }
                IconButton(onClick = onSignOut) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { idx, (label, icon) ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label) },
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        when (selectedTab) {
            0 -> VendorRequestsAndTasksTab(
                requests = requests,
                tasks = tasks,
                onAcceptRequest = onAcceptRequest,
                onCounterOffer = onCounterOffer,
                onTransitionRequest = onTransitionRequest,
                onCycleTask = onCycleTask
            )
            1 -> VendorCatalogTab(
                profile = vendorProfile,
                catalogItems = catalogItems,
                onSaveProfile = onSaveProfile,
                onSaveCatalogItem = onSaveCatalogItem
            )
            2 -> VendorCalendarTab(
                blocks = calendarBlocks,
                tasks = tasks,
                onAddBlock = onAddManualBlock,
                onDeleteBlock = onDeleteCalendarBlock
            )
            else -> VendorClientsAndAnalyticsTab(
                profile = vendorProfile,
                requests = requests,
                tasks = tasks
            )
        }
    }
}

// --- Tab 1: Service Requests -> Atomic Task Generation + Kanban/Checklist (RF-10) ---

@Composable
private fun VendorRequestsAndTasksTab(
    requests: List<ServiceRequest>,
    tasks: List<VendorTask>,
    onAcceptRequest: (ServiceRequest) -> Unit,
    onCounterOffer: (String, Double, String) -> Unit,
    onTransitionRequest: (ServiceRequest, String) -> Unit,
    onCycleTask: (VendorTask) -> Unit
) {
    var counterTarget by remember { mutableStateOf<ServiceRequest?>(null) }
    val activeTasks = tasks.filter { it.status != "CANCELLED" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Incoming Inquiries (${requests.size})", style = MaterialTheme.typography.titleLarge)
            Text(
                "Accepting an inquiry automatically blocks the event date on your calendar and creates 4 operational tasks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (requests.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "No inquiries yet. Publish your profile in the Catalog tab or send yourself a test inquiry from the Couple Portal.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(requests, key = { it.id }) { req ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.coupleName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${req.catalogItemTitle} • Date: ${req.eventDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = when (req.status) {
                                "ACCEPTED", "COMPLETED" -> MaterialTheme.colorScheme.secondaryContainer
                                "COUNTERED" -> MaterialTheme.colorScheme.tertiaryContainer
                                "REJECTED", "CANCELLED" -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                req.status,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        "Proposed: ${BudgetRebalancer.formatCurrency(req.proposedBudget)}" +
                            if (req.counterOfferBudget > 0) " • Counteroffer: ${BudgetRebalancer.formatCurrency(req.counterOfferBudget)}" else "",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (req.notes.isNotBlank()) {
                        Text(req.notes, style = MaterialTheme.typography.bodySmall)
                    }

                    when (req.status) {
                        "PENDING", "COUNTERED" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onAcceptRequest(req) },
                                    modifier = Modifier.testTag("vendor_accept_request_button")
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Accept")
                                }
                                if (req.status == "PENDING") {
                                    FilledTonalButton(onClick = { counterTarget = req }) {
                                        Text("Counteroffer")
                                    }
                                }
                                TextButton(onClick = { onTransitionRequest(req, "REJECTED") }) {
                                    Text("Decline")
                                }
                            }
                        }
                        "ACCEPTED" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onTransitionRequest(req, "COMPLETED") },
                                    modifier = Modifier.testTag("vendor_complete_event_button")
                                ) {
                                    Text("Mark Event Completed")
                                }
                                OutlinedButton(onClick = { onTransitionRequest(req, "CANCELLED") }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Text("Operational Task Board (${activeTasks.size})", style = MaterialTheme.typography.titleLarge)
            Text(
                "Tap any task card to advance its status: Pending → In Progress → Done.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(activeTasks, key = { it.id }) { task ->
            Card(
                onClick = { onCycleTask(task) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${task.coupleName} • Due: ${task.dueDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (task.description.isNotBlank()) {
                            Text(task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = when (task.status) {
                            "COMPLETED" -> MaterialTheme.colorScheme.secondaryContainer
                            "IN_PROGRESS" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            when (task.status) {
                                "COMPLETED" -> "Done ✓"
                                "IN_PROGRESS" -> "In Progress"
                                else -> "Pending"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    counterTarget?.let { target ->
        var amountText by remember { mutableStateOf(target.proposedBudget.toLong().toString()) }
        var notes by remember { mutableStateOf("We can include extended event coverage for this adjustment.") }
        AlertDialog(
            onDismissRequest = { counterTarget = null },
            title = { Text("Counteroffer for ${target.coupleName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter(Char::isDigit) },
                        label = { Text("Suggested amount (USD)") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Message") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCounterOffer(target.id, amountText.toDoubleOrNull() ?: target.proposedBudget, notes)
                    counterTarget = null
                }) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { counterTarget = null }) { Text("Cancel") } }
        )
    }
}

// --- Tab 2: Vendor Business Profile & Service Catalog CRUD (RF-09) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VendorCatalogTab(
    profile: VendorProfile?,
    catalogItems: List<VendorCatalogItem>,
    onSaveProfile: (String, String, String, String, Double, Double, Double, Double, Double, List<String>, Boolean) -> Unit,
    onSaveCatalogItem: (String?, String, String, Double, String, Boolean) -> Unit
) {
    var businessName by remember(profile) { mutableStateOf(profile?.businessName ?: "Wasatch Bridal Studio") }
    var category by remember(profile) { mutableStateOf(profile?.category ?: "Photography") }
    var description by remember(profile) { mutableStateOf(profile?.description ?: "Editorial Utah County wedding coverage with full digital gallery and heirloom album.") }
    var selectedCity by remember(profile) {
        mutableStateOf(
            DefaultWeddingCatalog.CITIES.firstOrNull { it.name == profile?.city }
                ?: DefaultWeddingCatalog.CITIES.first()
        )
    }
    var radiusMilesText by remember(profile) {
        val defaultMiles = ((profile?.coverageRadiusKm ?: 48.0) * 0.621371).roundToInt()
        mutableStateOf(defaultMiles.toString())
    }
    var minPriceText by remember(profile) { mutableStateOf((profile?.minPrice ?: 2200.0).toLong().toString()) }
    var maxPriceText by remember(profile) { mutableStateOf((profile?.maxPrice ?: 5500.0).toLong().toString()) }
    var published by remember(profile) { mutableStateOf(profile?.published ?: true) }
    val styles = remember(profile) {
        mutableStateListOf<String>().apply {
            addAll(profile?.styleTags?.ifEmpty { listOf("Editorial", "Classic") } ?: listOf("Editorial", "Classic"))
        }
    }

    var showPackageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Public Marketplace Profile", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text("Business name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Service description") }, minLines = 2, modifier = Modifier.fillMaxWidth())

        Text("Category", style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DefaultWeddingCatalog.CATEGORIES.forEach { cat ->
                FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
            }
        }

        Text("Utah County Base Location (Map Pin)", style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DefaultWeddingCatalog.CITIES.forEach { city ->
                FilterChip(selected = selectedCity.name == city.name, onClick = { selectedCity = city }, label = { Text(city.name) })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = minPriceText,
                onValueChange = { minPriceText = it.filter(Char::isDigit) },
                label = { Text("Min price") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = maxPriceText,
                onValueChange = { maxPriceText = it.filter(Char::isDigit) },
                label = { Text("Max price") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = radiusMilesText,
                onValueChange = { radiusMilesText = it.filter(Char::isDigit) },
                label = { Text("Radius mi") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(0.85f),
                singleLine = true
            )
        }

        Text("Matching Wedding Styles", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DefaultWeddingCatalog.STYLE_TAGS.forEach { tag ->
                val sel = styles.contains(tag)
                FilterChip(
                    selected = sel,
                    onClick = { if (sel && styles.size > 1) styles.remove(tag) else if (!sel && styles.size < 4) styles.add(tag) },
                    label = { Text(tag) }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Visible on Utah County map & AI Assistant", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = published, onCheckedChange = { published = it })
        }

        Button(
            onClick = {
                val radiusKm = ((radiusMilesText.toDoubleOrNull() ?: 30.0) * 1.60934).coerceIn(5.0, 2000.0)
                onSaveProfile(
                    businessName, category, description, selectedCity.name,
                    selectedCity.lat, selectedCity.lng,
                    radiusKm,
                    minPriceText.toDoubleOrNull() ?: 2200.0,
                    maxPriceText.toDoubleOrNull() ?: 5500.0,
                    styles.toList(), published
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_vendor_profile_button")
        ) { Text("Save & publish profile") }

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Service Packages (${catalogItems.size})", style = MaterialTheme.typography.titleLarge)
            FilledTonalButton(
                onClick = { showPackageDialog = true },
                enabled = profile != null,
                modifier = Modifier.testTag("add_catalog_item_button")
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("New package")
            }
        }

        catalogItems.forEach { item ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        Text("${BudgetRebalancer.formatCurrency(item.basePrice)} • ${item.pricingUnit}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = item.active,
                        onCheckedChange = { onSaveCatalogItem(item.id, item.title, item.description, item.basePrice, item.pricingUnit, it) }
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    if (showPackageDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("3200") }
        var unit by remember { mutableStateOf("Per event") }
        AlertDialog(
            onDismissRequest = { showPackageDialog = false },
            title = { Text("New Catalog Package") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Package title") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Includes…") }, minLines = 2)
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it.filter(Char::isDigit) },
                        label = { Text("Base price (USD)") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit (e.g. Per event / Per guest)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveCatalogItem(null, title, desc, price.toDoubleOrNull() ?: 1800.0, unit, true)
                    showPackageDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPackageDialog = false }) { Text("Cancel") } }
        )
    }
}

// --- Tab 3: Unified Calendar (RF-11) ---

@Composable
private fun VendorCalendarTab(
    blocks: List<VendorCalendarBlock>,
    tasks: List<VendorTask>,
    onAddBlock: (String, String) -> Unit,
    onDeleteBlock: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Unified Calendar", style = MaterialTheme.typography.titleLarge)
                    Text("Booked weddings & manual date blocks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Block date")
                }
            }
        }

        if (blocks.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "Accepted wedding dates are blocked automatically here. You can also add manual hold dates.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        items(blocks, key = { it.id }) { block ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(block.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${block.startDate} • ${if (block.blockType == "BOOKED") "Booked wedding" else "Manual block"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (block.blockType == "BOOKED") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (block.blockType == "MANUAL_BLOCK") {
                        IconButton(onClick = { onDeleteBlock(block.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Release date block")
                        }
                    }
                }
            }
        }

        val upcomingTasks = tasks.filter { it.status != "COMPLETED" && it.status != "CANCELLED" }
        if (upcomingTasks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(6.dp))
                Text("Upcoming Task Deadlines", style = MaterialTheme.typography.titleMedium)
            }
            items(upcomingTasks, key = { "cal_${it.id}" }) { t ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.labelLarge)
                            Text(t.coupleName, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(t.dueDate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("Blocked date (private event)") }
        var date by remember { mutableStateOf("2027-07-10") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Manual Calendar Block") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Reason") }, singleLine = true)
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = { onAddBlock(title, date); showAddDialog = false }) { Text("Block") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

// --- Tab 4: Clients & Completed Events Analytics (RF-12 & RF-13, Business Rule #3) ---

@Composable
private fun VendorClientsAndAnalyticsTab(
    profile: VendorProfile?,
    requests: List<ServiceRequest>,
    tasks: List<VendorTask>
) {
    // Business Rule #3: strictly count COMPLETED events for Completed Events metrics
    val completedRequests = requests.filter { it.status == "COMPLETED" }
    val acceptedOrCompleted = requests.filter { it.status == "ACCEPTED" || it.status == "COMPLETED" }
    val conversionRate = if (requests.isNotEmpty()) {
        ((acceptedOrCompleted.size.toDouble() / requests.size.toDouble()) * 100).toInt()
    } else 0

    val fulfilledRevenue = completedRequests.sumOf {
        if (it.counterOfferBudget > 0) it.counterOfferBudget else it.proposedBudget
    }
    val pipelineRevenue = requests.filter { it.status == "ACCEPTED" }.sumOf {
        if (it.counterOfferBudget > 0) it.counterOfferBudget else it.proposedBudget
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Completed Event Metrics", style = MaterialTheme.typography.titleLarge)
            Text(
                "Counts strictly events marked COMPLETED (Business Rule #3).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Completed", "${completedRequests.size}", Modifier.weight(1f))
                MetricTile("Conversion", "$conversionRate%", Modifier.weight(1f))
                MetricTile("Rating", "${profile?.ratingAvg ?: 4.9} ★", Modifier.weight(1f))
            }
        }

        item {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Revenue Summary", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Completed revenue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(BudgetRebalancer.formatCurrency(fulfilledRevenue), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Booked pipeline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(BudgetRebalancer.formatCurrency(pipelineRevenue), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Native Compose Canvas Bar Chart: Pipeline Breakdown
                    val barPrimary = MaterialTheme.colorScheme.primary
                    val barSecondary = MaterialTheme.colorScheme.secondary
                    val barTertiary = MaterialTheme.colorScheme.tertiary
                    val counts = listOf(
                        "Pend." to requests.count { it.status == "PENDING" }.toFloat(),
                        "Accep." to requests.count { it.status == "ACCEPTED" }.toFloat(),
                        "Done" to completedRequests.size.toFloat(),
                        "Tasks" to tasks.count { it.status == "COMPLETED" }.toFloat()
                    )
                    val maxVal = (counts.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .padding(top = 8.dp)
                    ) {
                        val barWidth = size.width / (counts.size * 2f)
                        counts.forEachIndexed { idx, (_, value) ->
                            val barHeight = ((value / maxVal) * (size.height - 12.dp.toPx())).coerceAtLeast(8.dp.toPx())
                            val left = (idx * 2 + 0.5f) * barWidth
                            val color = when (idx) {
                                0 -> barTertiary
                                1 -> barPrimary
                                2 -> barSecondary
                                else -> barPrimary.copy(alpha = 0.65f)
                            }
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(left, size.height - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        counts.forEach { (label, v) ->
                            Text("$label (${v.toInt()})", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        item {
            Text("Client Directory (${acceptedOrCompleted.size})", style = MaterialTheme.typography.titleLarge)
        }

        if (acceptedOrCompleted.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "Couples with accepted or completed bookings will appear here along with their task progress.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        items(acceptedOrCompleted, key = { "cli_${it.id}" }) { client ->
            val clientTasks = tasks.filter { it.serviceRequestId == client.id }
            val doneTasks = clientTasks.count { it.status == "COMPLETED" }
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(client.coupleName, style = MaterialTheme.typography.titleSmall)
                        Text(client.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text("${client.catalogItemTitle} • Date: ${client.eventDate}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Tasks completed: $doneTasks / ${clientTasks.size} • Deal: ${BudgetRebalancer.formatCurrency(if (client.counterOfferBudget > 0) client.counterOfferBudget else client.proposedBudget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
