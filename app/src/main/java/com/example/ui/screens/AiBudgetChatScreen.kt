package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.BudgetRebalancer
import com.example.domain.RebalanceProposal
import com.example.domain.ScoredVendor
import com.example.domain.VendorProfile
import com.example.ui.viewmodel.ChatTurn

@Composable
fun AiBudgetChatScreen(
    turns: List<ChatTurn>,
    isThinking: Boolean,
    onSendPrompt: (String) -> Unit,
    onApplyProposal: (turnId: String, proposal: RebalanceProposal) -> Unit,
    onViewVendorOnMap: (String) -> Unit,
    onSendInquiry: (vendor: VendorProfile, packageTitle: String, budget: Double, notes: String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickPrompts = listOf(
        "Find photography & florals in Utah County within budget",
        "Prioritize venue and catering without going over total",
        "How can we save on music and stationery?"
    )

    LaunchedEffect(turns.size, isThinking) {
        if (turns.isNotEmpty()) {
            listState.animateScrollToItem(turns.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AI Budget Concierge",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                Text(
                    text = "Deterministic priority rebalancing + real Utah County vendors",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Chat Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(turns, key = { it.id }) { turn ->
                ChatTurnBubble(
                    turn = turn,
                    onApplyProposal = { prop -> onApplyProposal(turn.id, prop) },
                    onViewVendorOnMap = onViewVendorOnMap,
                    onSendInquiry = onSendInquiry
                )
            }

            if (isThinking) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(
                                "Calculating exact budget rebalance & matching vendors…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick Prompt Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { suggestion ->
                AssistChip(
                    onClick = { onSendPrompt(suggestion) },
                    label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                    enabled = !isThinking
                )
            }
        }

        // Message Composer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("e.g. Find editorial photography & florals…") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input")
                )
                FilledIconButton(
                    onClick = {
                        val msg = inputText
                        inputText = ""
                        onSendPrompt(msg)
                    },
                    enabled = inputText.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ai_chat_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
                }
            }
        }
    }
}

@Composable
private fun ChatTurnBubble(
    turn: ChatTurn,
    onApplyProposal: (RebalanceProposal) -> Unit,
    onViewVendorOnMap: (String) -> Unit,
    onSendInquiry: (VendorProfile, String, Double, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (turn.fromUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = when {
                turn.errorBanner -> MaterialTheme.colorScheme.errorContainer
                turn.fromUser -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surface
            },
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (turn.fromUser) 18.dp else 4.dp,
                bottomEnd = if (turn.fromUser) 4.dp else 18.dp
            ),
            tonalElevation = if (turn.fromUser) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth(if (turn.fromUser) 0.84f else 0.96f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = turn.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        turn.errorBanner -> MaterialTheme.colorScheme.onErrorContainer
                        turn.fromUser -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                if (turn.impactSummary.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Financial impact: ${turn.impactSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Deterministic Budget Rebalance Action Card (RF-05, RF-06)
        turn.proposal?.let { proposal ->
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(0.96f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Exact Budget Rebalance Proposal",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Verified total: ${BudgetRebalancer.formatCurrency(proposal.totalProposed)} / ${BudgetRebalancer.formatCurrency(proposal.totalBudget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    HorizontalDivider()

                    proposal.ranges.take(5).forEach { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "#${r.priorityRank} ${r.category}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    "Range ${BudgetRebalancer.formatCurrency(r.suggestedMin)} – ${BudgetRebalancer.formatCurrency(r.suggestedMax)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    BudgetRebalancer.formatCurrency(r.suggestedIdeal),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (r.delta != 0.0) {
                                    val sign = if (r.delta > 0) "+" else ""
                                    Text(
                                        "$sign${BudgetRebalancer.formatCurrency(r.delta)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (r.delta > 0) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { onApplyProposal(proposal) },
                        enabled = !turn.proposalApplied,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_budget_proposal_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (turn.proposalApplied) "Budget adjustment applied" else "Apply this budget adjustment")
                    }
                }
            }
        }

        // Interactive Matched Vendor Cards (RF-06)
        if (turn.recommendedVendors.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(0.96f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                turn.recommendedVendors.forEach { scored ->
                    ChatVendorRecommendationCard(
                        scored = scored,
                        onViewMap = { onViewVendorOnMap(scored.vendor.id) },
                        onSendRequest = {
                            onSendInquiry(
                                scored.vendor,
                                "${scored.vendor.category} Inquiry (AI Match)",
                                scored.categoryBudgetAllocated.coerceAtLeast(scored.vendor.minPrice),
                                "Recommended by Nupcia AI Concierge with ${scored.affinityScore}% wedding affinity."
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatVendorRecommendationCard(
    scored: ScoredVendor,
    onViewMap: () -> Unit,
    onSendRequest: () -> Unit
) {
    val v = scored.vendor
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                Text(v.businessName, style = MaterialTheme.typography.titleSmall)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "${scored.affinityScore}% match",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${v.category} • ${BudgetRebalancer.formatCurrency(v.minPrice)}–${BudgetRebalancer.formatCurrency(v.maxPrice)}", style = MaterialTheme.typography.bodySmall)
                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                Text("${v.ratingAvg}", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                "${"%.1f".format(scored.distanceMiles)} mi from venue • ${if (scored.coversWeddingLocation) "Covers your area" else "Outside usual radius"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onViewMap,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("View on map", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = onSendRequest,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Send inquiry", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
