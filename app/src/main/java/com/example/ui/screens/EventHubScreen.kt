package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.BudgetRebalancer
import com.example.domain.DefaultWeddingCatalog
import com.example.domain.Guest
import com.example.domain.RegistryItem
import com.example.domain.StationeryDesign
import com.example.domain.Wedding

@Composable
fun EventHubScreen(
    wedding: Wedding?,
    guests: List<Guest>,
    registryItems: List<RegistryItem>,
    stationeryDesigns: List<StationeryDesign>,
    onSaveGuest: (String?, String, String, Long, String, String, String, String, String, String, Long) -> Unit,
    onDeleteGuest: (String) -> Unit,
    onAddRegistryItem: (String, String, String, Double) -> Unit,
    onAddContribution: (RegistryItem, String, Double) -> Unit,
    onToggleThankYou: (String, Boolean) -> Unit,
    onSaveStationery: (String, String, String, String, String, String, String, String, String, Boolean, Boolean) -> Unit,
    onSaveWebsite: (String, String, String, String, String, Boolean, String) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    BackHandler(enabled = selectedSubTab != 0) { selectedSubTab = 0 }
    val subTabs = listOf(
        "Guests" to Icons.Default.People,
        "Registry" to Icons.Default.CardGiftcard,
        "Stationery" to Icons.Default.Mail,
        "Website" to Icons.Default.Language
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 12.dp
        ) {
            subTabs.forEachIndexed { idx, (label, icon) ->
                Tab(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    text = { Text(label) },
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        when (selectedSubTab) {
            0 -> GuestsAndSeatingTab(
                wedding = wedding,
                guests = guests,
                onSaveGuest = onSaveGuest,
                onDeleteGuest = onDeleteGuest
            )
            1 -> RegistryAndCashFundsTab(
                items = registryItems,
                onAddItem = onAddRegistryItem,
                onAddContribution = onAddContribution,
                onToggleThankYou = onToggleThankYou
            )
            2 -> StationeryVisualEditorTab(
                wedding = wedding,
                guests = guests,
                designs = stationeryDesigns,
                onSaveDesign = onSaveStationery
            )
            else -> WeddingWebsiteTab(
                wedding = wedding,
                onSaveWebsite = onSaveWebsite
            )
        }
    }
}

// --- Sub-tab 1: Guests, Households, RSVP & Interactive Seating Chart (RF-15) ---

@Composable
private fun GuestsAndSeatingTab(
    wedding: Wedding?,
    guests: List<Guest>,
    onSaveGuest: (String?, String, String, Long, String, String, String, String, String, String, Long) -> Unit,
    onDeleteGuest: (String) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGuest by remember { mutableStateOf<Guest?>(null) }
    var showSeatingView by remember { mutableStateOf(false) }

    val confirmedTotal = guests.filter { it.rsvpStatus == "CONFIRMED" }.sumOf { it.partySize }
    val pendingTotal = guests.filter { it.rsvpStatus == "PENDING" }.sumOf { it.partySize }
    val withAddress = guests.count { it.postalAddress.isNotBlank() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventStatCard("Confirmed", "$confirmedTotal", MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
                EventStatCard("Pending", "$pendingTotal", MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
                EventStatCard("Addresses", "$withAddress/${guests.size}", MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_guest_button")
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add household")
                }
                FilledTonalButton(
                    onClick = { showSeatingView = !showSeatingView },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (showSeatingView) "View RSVP List" else "Seating Chart")
                }
            }
        }

        if (showSeatingView) {
            val byTable = guests.filter { it.rsvpStatus != "DECLINED" }.groupBy { it.tableNumber }
            val tableNumbers = (listOf(0L) + (1L..8L)).filter { num -> num == 0L || byTable.containsKey(num) }
            items(tableNumbers) { tableNum ->
                val list = byTable[tableNum].orEmpty()
                val seats = list.sumOf { it.partySize }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (tableNum == 0L) "Unassigned Table" else "Table #$tableNum",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (tableNum == 0L) "$seats guests" else "$seats / 8 seats",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (seats > 8 && tableNum > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (list.isEmpty()) {
                            Text("No guests assigned here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            list.forEach { g ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editingGuest = g }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${g.fullName} (${g.partySize}) • ${g.mealChoice}", style = MaterialTheme.typography.bodySmall)
                                    if (g.dietaryNotes.isNotBlank()) {
                                        Text(g.dietaryNotes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (guests.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Text(
                            "Add households and guests to collect mailing addresses, track RSVPs, meal selections, and table seating.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            }
            items(guests, key = { it.id }) { guest ->
                Card(
                    onClick = { editingGuest = guest },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${guest.householdName} — ${guest.fullName}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${guest.partySize} seats • Table ${if (guest.tableNumber > 0) "#${guest.tableNumber}" else "Unassigned"} • ${guest.mealChoice}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                val slug = wedding?.slug ?: "our-wedding"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Hi ${guest.fullName}! Share your mailing address and RSVP for our wedding at https://nupcia.app/$slug/rsvp?token=${guest.inviteToken}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share RSVP link"))
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share address and RSVP link")
                            }
                            IconButton(onClick = { onDeleteGuest(guest.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete guest")
                            }
                        }
                        if (guest.postalAddress.isNotBlank()) {
                            Text("Address: ${guest.postalAddress}", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("CONFIRMED" to "Confirmed", "PENDING" to "Pending", "DECLINED" to "Declined").forEach { (code, lbl) ->
                                FilterChip(
                                    selected = guest.rsvpStatus == code,
                                    onClick = {
                                        onSaveGuest(
                                            guest.id, guest.householdName, guest.fullName, guest.partySize,
                                            guest.email, guest.phone, guest.postalAddress, code,
                                            guest.mealChoice, guest.dietaryNotes, guest.tableNumber
                                        )
                                    },
                                    label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingGuest != null) {
        GuestEditorDialog(
            existing = editingGuest,
            onDismiss = { showAddDialog = false; editingGuest = null },
            onSave = { household, fullName, size, email, phone, address, status, meal, diet, table ->
                onSaveGuest(editingGuest?.id, household, fullName, size, email, phone, address, status, meal, diet, table)
                showAddDialog = false
                editingGuest = null
            }
        )
    }
}

@Composable
private fun EventStatCard(label: String, value: String, bg: Color, modifier: Modifier = Modifier) {
    Surface(color = bg, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun GuestEditorDialog(
    existing: Guest?,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, String, String, String, String, String, String, Long) -> Unit
) {
    var household by remember { mutableStateOf(existing?.householdName ?: "") }
    var fullName by remember { mutableStateOf(existing?.fullName ?: "") }
    var partySize by remember { mutableStateOf((existing?.partySize ?: 2L).toString()) }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var address by remember { mutableStateOf(existing?.postalAddress ?: "") }
    var meal by remember { mutableStateOf(existing?.mealChoice ?: "Main Course") }
    var diet by remember { mutableStateOf(existing?.dietaryNotes ?: "") }
    var table by remember { mutableStateOf((existing?.tableNumber ?: 1L).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Household / Guest" else "Edit Guest") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = household, onValueChange = { household = it }, label = { Text("Family / Household") }, singleLine = true)
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Primary Guest Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = partySize,
                        onValueChange = { partySize = it.filter(Char::isDigit) },
                        label = { Text("Seats") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = table,
                        onValueChange = { table = it.filter(Char::isDigit) },
                        label = { Text("Table #") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Mailing Address (for envelopes)") }, singleLine = true)
                OutlinedTextField(value = meal, onValueChange = { meal = it }, label = { Text("Meal Choice") }, singleLine = true)
                OutlinedTextField(value = diet, onValueChange = { diet = it }, label = { Text("Allergies / Dietary Notes") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    household, fullName,
                    partySize.toLongOrNull()?.coerceIn(1L, 20L) ?: 1L,
                    email, phone, address,
                    existing?.rsvpStatus ?: "PENDING",
                    meal, diet,
                    table.toLongOrNull()?.coerceIn(0L, 100L) ?: 0L
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// --- Sub-tab 2: Registry & Cash Funds (RF-16) ---

@Composable
private fun RegistryAndCashFundsTab(
    items: List<RegistryItem>,
    onAddItem: (String, String, String, Double) -> Unit,
    onAddContribution: (RegistryItem, String, Double) -> Unit,
    onToggleThankYou: (String, Boolean) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var contributeTarget by remember { mutableStateOf<RegistryItem?>(null) }

    val totalCollected = items.sumOf { it.collectedAmount }
    val totalTarget = items.sumOf { it.targetAmount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Registry & Cash Funds Raised", style = MaterialTheme.typography.labelMedium)
                        Text(
                            BudgetRebalancer.formatCurrency(totalCollected),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Total goal: ${BudgetRebalancer.formatCurrency(totalTarget)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { showCreate = true },
                        modifier = Modifier.testTag("add_registry_item_button")
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New fund")
                    }
                }
            }
        }

        if (items.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "Create cash funds (Honeymoon, Home Down Payment) or physical gifts and track guest contributions and thank-you notes.",
                        modifier = Modifier.padding(18.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        items(items, key = { it.id }) { item ->
            val progress = if (item.targetAmount > 0) (item.collectedAmount / item.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (item.itemType == "CASH_FUND") "Cash fund • ${item.contributorCount} gifts"
                                else "Physical gift • ${item.contributorCount} gifts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "${BudgetRebalancer.formatCurrency(item.collectedAmount)} / ${BudgetRebalancer.formatCurrency(item.targetAmount)}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp))
                    )
                    if (item.lastContributorName.isNotBlank()) {
                        Text(
                            "Latest gift: ${item.lastContributorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(onClick = { contributeTarget = item }) {
                            Text("Log gift")
                        }
                        FilterChip(
                            selected = item.thankYouSent,
                            onClick = { onToggleThankYou(item.id, !item.thankYouSent) },
                            label = { Text(if (item.thankYouSent) "Thanked ✓" else "Send thank-you") }
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        var isCashFund by remember { mutableStateOf(true) }
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("1500") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Add to Gift Registry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = isCashFund, onClick = { isCashFund = true }, label = { Text("Cash fund") })
                        FilterChip(selected = !isCashFund, onClick = { isCashFund = false }, label = { Text("Physical gift") })
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title (e.g. Honeymoon Fund)") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, singleLine = true)
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it.filter(Char::isDigit) },
                        label = { Text("Goal (USD)") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddItem(if (isCashFund) "CASH_FUND" else "PRODUCT", title, desc, target.toDoubleOrNull() ?: 500.0)
                    showCreate = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }

    contributeTarget?.let { targetItem ->
        var name by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("200") }
        AlertDialog(
            onDismissRequest = { contributeTarget = null },
            title = { Text("Log Gift to ${targetItem.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Guest name") }, singleLine = true)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter(Char::isDigit) },
                        label = { Text("Amount (USD)") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddContribution(targetItem, name, amount.toDoubleOrNull() ?: 100.0)
                    contributeTarget = null
                }) { Text("Save gift") }
            },
            dismissButton = { TextButton(onClick = { contributeTarget = null }) { Text("Cancel") } }
        )
    }
}

// --- Sub-tab 3: Visual Canvas Stationery Editor with Live Guest Address Merge & Bleed Guides (RF-17) ---

@Composable
private fun StationeryVisualEditorTab(
    wedding: Wedding?,
    guests: List<Guest>,
    designs: List<StationeryDesign>,
    onSaveDesign: (String, String, String, String, String, String, String, String, String, Boolean, Boolean) -> Unit
) {
    val activeDesign = designs.firstOrNull()
    var productType by remember(activeDesign) { mutableStateOf(activeDesign?.productType ?: "INVITATION") }
    var suite by remember(activeDesign) {
        mutableStateOf(
            DefaultWeddingCatalog.SUITES.firstOrNull { it.id == (activeDesign?.suiteId ?: wedding?.selectedSuiteId) }
                ?: DefaultWeddingCatalog.SUITES.first()
        )
    }
    var headline by remember(activeDesign, wedding) {
        mutableStateOf(
            activeDesign?.headline?.ifBlank { "${wedding?.brideName ?: "Emma"} & ${wedding?.partnerName ?: "Liam"}" }
                ?: "${wedding?.brideName ?: "Emma"} & ${wedding?.partnerName ?: "Liam"}"
        )
    }
    var subheadline by remember(activeDesign, wedding) {
        mutableStateOf(
            activeDesign?.subheadline?.ifBlank { "${wedding?.weddingDate ?: "2027-06-19"} • ${wedding?.city ?: "Provo, UT"}" }
                ?: "${wedding?.weddingDate ?: "2027-06-19"} • ${wedding?.city ?: "Provo, UT"}"
        )
    }
    var bodyText by remember(activeDesign) {
        mutableStateOf(
            activeDesign?.bodyText?.ifBlank { "Together with our families, we joyfully invite you to celebrate our wedding in Utah County." }
                ?: "Together with our families, we joyfully invite you to celebrate our wedding in Utah County."
        )
    }
    var showBleedGuides by remember { mutableStateOf(true) }
    var previewGuestIdx by remember { mutableIntStateOf(0) }

    val previewGuest = guests.getOrNull(previewGuestIdx % guests.size.coerceAtLeast(1))
    val bgColor = remember(suite.bgHex) { Color(android.graphics.Color.parseColor(suite.bgHex)) }
    val accentColor = remember(suite.accentHex) { Color(android.graphics.Color.parseColor(suite.accentHex)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Product Type Selector
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "INVITATION" to "Invitation",
                "SAVE_THE_DATE" to "Save the Date",
                "MENU" to "Menu",
                "ENVELOPE" to "Envelope"
            ).forEach { (code, label) ->
                FilterChip(
                    selected = productType == code,
                    onClick = { productType = code },
                    label = { Text(label) }
                )
            }
        }

        // Visual Stationery Canvas Preview with Bleed Margin Guide
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            // Ornamental frame & Bleed Safe-Zone Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val inset = 18.dp.toPx()
                drawRect(
                    color = accentColor.copy(alpha = 0.55f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                if (showBleedGuides) {
                    val bleed = 8.dp.toPx()
                    drawRect(
                        color = Color(0xFFD32F2F).copy(alpha = 0.55f),
                        topLeft = Offset(bleed, bleed),
                        size = Size(size.width - bleed * 2, size.height - bleed * 2),
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when (productType) {
                        "SAVE_THE_DATE" -> "SAVE THE DATE"
                        "MENU" -> "RECEPTION MENU"
                        "ENVELOPE" -> "MAILING RECIPIENT"
                        else -> "WEDDING INVITATION"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (productType == "ENVELOPE" && previewGuest != null) {
                        previewGuest.householdName
                    } else headline,
                    style = MaterialTheme.typography.headlineMedium,
                    color = accentColor,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (productType == "ENVELOPE" && previewGuest != null) {
                        previewGuest.fullName
                    } else subheadline,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF3E3436),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (productType == "ENVELOPE" && previewGuest != null) {
                        previewGuest.postalAddress.ifBlank { "Mailing address pending collection" }
                    } else bodyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4F4345),
                    textAlign = TextAlign.Center
                )
                if (previewGuest != null && productType != "ENVELOPE") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Personalized for: ${previewGuest.householdName} (${previewGuest.partySize} seats)",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                }
            }
        }

        // Bleed Guide Toggle & Household Address Injection Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = showBleedGuides, onCheckedChange = { showBleedGuides = it })
                Spacer(Modifier.width(8.dp))
                Text("Print bleed guide", style = MaterialTheme.typography.bodySmall)
            }
            if (guests.isNotEmpty()) {
                TextButton(onClick = { previewGuestIdx += 1 }) {
                    Text("Next household (${(previewGuestIdx % guests.size) + 1}/${guests.size})")
                }
            }
        }

        // Coordinated Suite Picker
        Text("Coordinated Design Suite", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DefaultWeddingCatalog.SUITES.forEach { s ->
                FilterChip(
                    selected = suite.id == s.id,
                    onClick = { suite = s },
                    label = { Text(s.name) }
                )
            }
        }

        OutlinedTextField(value = headline, onValueChange = { headline = it }, label = { Text("Main headline") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = subheadline, onValueChange = { subheadline = it }, label = { Text("Date & location") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = bodyText, onValueChange = { bodyText = it }, label = { Text("Invitation message") }, minLines = 2, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    onSaveDesign(
                        activeDesign?.id ?: "inv_main",
                        suite.id, productType, headline, subheadline, bodyText,
                        suite.bgHex, suite.accentHex, "EDITORIAL_SERIF", true, false
                    )
                },
                modifier = Modifier.weight(1f)
            ) { Text("Save draft") }
            Button(
                onClick = {
                    onSaveDesign(
                        activeDesign?.id ?: "inv_main",
                        suite.id, productType, headline, subheadline, bodyText,
                        suite.bgHex, suite.accentHex, "EDITORIAL_SERIF", true, true
                    )
                },
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("approve_stationery_proof_button")
            ) {
                Icon(Icons.Default.Verified, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Approve proof")
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// --- Sub-tab 4: Wedding Website Builder (RF-14) ---

@Composable
private fun WeddingWebsiteTab(
    wedding: Wedding?,
    onSaveWebsite: (String, String, String, String, String, Boolean, String) -> Unit
) {
    val context = LocalContext.current
    var headline by remember(wedding) { mutableStateOf(wedding?.websiteHeadline ?: "We're Getting Married!") }
    var story by remember(wedding) { mutableStateOf(wedding?.websiteStory ?: "") }
    var venueName by remember(wedding) { mutableStateOf(wedding?.websiteVenueName ?: "") }
    var schedule by remember(wedding) { mutableStateOf(wedding?.websiteSchedule ?: "") }
    var slug by remember(wedding) { mutableStateOf(wedding?.slug ?: "our-wedding") }
    var isProtected by remember(wedding) { mutableStateOf(wedding?.websiteProtected ?: false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Website Preview Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("nupcia.app/$slug", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(headline, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text("${wedding?.weddingDate ?: ""} • $venueName", style = MaterialTheme.typography.bodySmall)
                if (schedule.isNotBlank()) {
                    Text(schedule, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(4.dp))
                FilledTonalButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "$headline — Visit our wedding website for schedule, RSVP & registry: https://nupcia.app/$slug"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share wedding website"))
                }) {
                    Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share wedding link")
                }
            }
        }

        OutlinedTextField(value = slug, onValueChange = { slug = it.lowercase().replace(" ", "-") }, label = { Text("Custom URL (slug)") }, prefix = { Text("nupcia.app/") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = headline, onValueChange = { headline = it }, label = { Text("Cover headline") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = venueName, onValueChange = { venueName = it }, label = { Text("Venue name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = schedule, onValueChange = { schedule = it }, label = { Text("Event schedule") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = story, onValueChange = { story = it }, label = { Text("Our story") }, minLines = 3, modifier = Modifier.fillMaxWidth())

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Protect website with guest passcode", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = isProtected, onCheckedChange = { isProtected = it })
        }

        Button(
            onClick = {
                onSaveWebsite(
                    wedding?.selectedSuiteId ?: "botanical_ivory",
                    headline, story, venueName, schedule, isProtected, slug
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_website_button")
        ) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Publish website changes")
        }
    }
}
