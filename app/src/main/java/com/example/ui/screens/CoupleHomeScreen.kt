package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.BudgetAllocation
import com.example.domain.BudgetRebalancer
import com.example.domain.ReadinessReport
import com.example.domain.ServiceRequest
import com.example.domain.Wedding

@Composable
fun CoupleHomeScreen(
    wedding: Wedding?,
    allocations: List<BudgetAllocation>,
    requests: List<ServiceRequest>,
    readiness: ReadinessReport,
    onNavigateTab: (Int) -> Unit,
    onUpdateAllocation: (id: String, allocated: Double, spent: Double, locked: Boolean) -> Unit,
    onRequestAction: (ServiceRequest, String) -> Unit,
    onSwitchToVendor: () -> Unit,
    onSignOut: () -> Unit
) {
    var editingAlloc by remember { mutableStateOf<BudgetAllocation?>(null) }

    val totalBudget = wedding?.totalBudget ?: 35000.0
    val totalAllocated = allocations.sumOf { it.allocatedAmount }
    val totalSpent = allocations.sumOf { it.spentAmount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Couple Hero Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.84f),
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "${wedding?.weddingDate ?: "Date TBD"} • ${wedding?.city ?: "Provo, UT"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = onSwitchToVendor,
                                    modifier = Modifier.testTag("switch_to_vendor_button")
                                ) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = "Switch to Vendor Portal",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                IconButton(
                                    onClick = onSignOut,
                                    modifier = Modifier.testTag("sign_out_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Sign out",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${wedding?.brideName ?: "Bride"} & ${wedding?.partnerName ?: "Partner"}",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        if (!wedding?.styleTags.isNullOrEmpty()) {
                            Text(
                                text = "Style: ${wedding?.styleTags?.joinToString(" • ")} | ${wedding?.estimatedGuests ?: 140} guests",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroMetricPill(
                                label = "Budget",
                                value = BudgetRebalancer.formatCurrency(totalBudget),
                                modifier = Modifier.weight(1f)
                            )
                            HeroMetricPill(
                                label = "Committed",
                                value = BudgetRebalancer.formatCurrency(totalSpent),
                                modifier = Modifier.weight(1f)
                            )
                            HeroMetricPill(
                                label = "Readiness",
                                value = "${readiness.score}%",
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Optimization Feature: Smart Readiness Index + Next Best Action
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Next Best Action",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Budget health ${readiness.budgetHealth}/100",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = readiness.nextBestActionTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = readiness.nextBestActionSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { onNavigateTab(readiness.nextBestActionTab) },
                            modifier = Modifier.testTag("next_best_action_button")
                        ) {
                            Text("Go now")
                        }
                        OutlinedButton(
                            onClick = { onNavigateTab(1) },
                            modifier = Modifier.testTag("open_ai_from_home_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Optimize with AI")
                        }
                    }
                }
            }
        }

        // Active Service Requests Tracker (Couple side)
        if (requests.isNotEmpty()) {
            item {
                Text(
                    text = "Vendor Inquiries (${requests.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(requests.take(4), key = { it.id }) { req ->
                CoupleRequestCard(req = req, onAction = onRequestAction)
            }
        }

        // Priority-Weighted Budget Allocations Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Priority Budget Plan",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Allocated ${BudgetRebalancer.formatCurrency(totalAllocated)} of ${BudgetRebalancer.formatCurrency(totalBudget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = { onNavigateTab(1) },
                    label = { Text("AI Rebalance") },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp)) }
                )
            }
        }

        items(allocations, key = { it.id }) { alloc ->
            BudgetAllocationRowCard(
                alloc = alloc,
                onEditClick = { editingAlloc = alloc },
                onToggleLock = {
                    onUpdateAllocation(alloc.id, alloc.allocatedAmount, alloc.spentAmount, !alloc.locked)
                }
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    editingAlloc?.let { target ->
        EditAllocationDialog(
            allocation = target,
            onDismiss = { editingAlloc = null },
            onSave = { newAlloc, newSpent, locked ->
                onUpdateAllocation(target.id, newAlloc, newSpent, locked)
                editingAlloc = null
            }
        )
    }
}

@Composable
private fun HeroMetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BudgetAllocationRowCard(
    alloc: BudgetAllocation,
    onEditClick: () -> Unit,
    onToggleLock: () -> Unit
) {
    val progress = if (alloc.allocatedAmount > 0) {
        (alloc.spentAmount / alloc.allocatedAmount).toFloat().coerceIn(0f, 1f)
    } else 0f
    val isOver = alloc.spentAmount > alloc.allocatedAmount && alloc.allocatedAmount > 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (alloc.priorityRank <= 2) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "#${alloc.priorityRank}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (alloc.priorityRank <= 2) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(alloc.category, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${BudgetRebalancer.formatCurrency(alloc.spentAmount)} spent / ${BudgetRebalancer.formatCurrency(alloc.allocatedAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleLock) {
                    Icon(
                        if (alloc.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (alloc.locked) "Unlock category" else "Lock category",
                        tint = if (alloc.locked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit budget amount")
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun CoupleRequestCard(
    req: ServiceRequest,
    onAction: (ServiceRequest, String) -> Unit
) {
    val (statusLabel, badgeColor) = when (req.status) {
        "ACCEPTED" -> "Accepted" to MaterialTheme.colorScheme.secondaryContainer
        "COUNTERED" -> "Counteroffer" to MaterialTheme.colorScheme.tertiaryContainer
        "COMPLETED" -> "Completed" to MaterialTheme.colorScheme.secondaryContainer
        "REJECTED" -> "Declined" to MaterialTheme.colorScheme.errorContainer
        "CANCELLED" -> "Cancelled" to MaterialTheme.colorScheme.surfaceVariant
        else -> "Pending" to MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(req.vendorName, style = MaterialTheme.typography.titleSmall)
                Surface(color = badgeColor, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                "${req.category} • ${req.catalogItemTitle} • ${req.eventDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val priceLine = if (req.status == "COUNTERED" && req.counterOfferBudget > 0) {
                "Proposed: ${BudgetRebalancer.formatCurrency(req.proposedBudget)} → Counteroffer: ${BudgetRebalancer.formatCurrency(req.counterOfferBudget)}"
            } else {
                "Proposed budget: ${BudgetRebalancer.formatCurrency(req.proposedBudget)}"
            }
            Text(priceLine, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

            if (req.status == "COUNTERED" || req.status == "PENDING") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (req.status == "COUNTERED") {
                        Button(onClick = { onAction(req, "ACCEPTED") }) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Accept counteroffer")
                        }
                    }
                    TextButton(onClick = { onAction(req, "CANCELLED") }) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditAllocationDialog(
    allocation: BudgetAllocation,
    onDismiss: () -> Unit,
    onSave: (allocated: Double, spent: Double, locked: Boolean) -> Unit
) {
    var allocatedText by remember { mutableStateOf(allocation.allocatedAmount.toLong().toString()) }
    var spentText by remember { mutableStateOf(allocation.spentAmount.toLong().toString()) }
    var locked by remember { mutableStateOf(allocation.locked) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${allocation.category}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = allocatedText,
                    onValueChange = { allocatedText = it.filter(Char::isDigit) },
                    label = { Text("Allocated budget") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = spentText,
                    onValueChange = { spentText = it.filter(Char::isDigit) },
                    label = { Text("Committed / spent amount") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lock amount during AI rebalance", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = locked, onCheckedChange = { locked = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    allocatedText.toDoubleOrNull() ?: allocation.allocatedAmount,
                    spentText.toDoubleOrNull() ?: allocation.spentAmount,
                    locked
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
