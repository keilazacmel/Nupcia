package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.BudgetRebalancer
import com.example.domain.DefaultWeddingCatalog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialName: String,
    isBusy: Boolean,
    onSwitchToVendor: () -> Unit,
    onCompleteCoupleOnboarding: (
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
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var brideName by remember { mutableStateOf(initialName.substringBefore(" ").ifBlank { "Emma" }) }
    var partnerName by remember { mutableStateOf("Liam") }
    var weddingDate by remember { mutableStateOf("2027-06-19") }
    var selectedCity by remember { mutableStateOf(DefaultWeddingCatalog.CITIES.first()) }
    var budgetText by remember { mutableStateOf("35000") }
    var guestsText by remember { mutableStateOf("140") }

    val selectedStyles = remember { mutableStateListOf("Garden", "Classic") }
    var selectedPaletteName by remember { mutableStateOf(DefaultWeddingCatalog.COLOR_PALETTES.keys.first()) }
    var selectedCeremony by remember { mutableStateOf(DefaultWeddingCatalog.CEREMONY_TYPES.first()) }
    var visionExpectations by remember {
        mutableStateOf("A luminous mountain-view garden celebration with seasonal dining and timeless editorial photography.")
    }

    val priorityOrder = remember {
        mutableStateListOf<String>().apply { addAll(DefaultWeddingCatalog.CATEGORIES) }
    }

    val parsedBudget = budgetText.toDoubleOrNull()?.coerceIn(1000.0, 50000000.0) ?: 35000.0
    val previewAllocations = remember(parsedBudget, priorityOrder.toList()) {
        BudgetRebalancer.computeInitialAllocations(parsedBudget, priorityOrder.toList())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top bar with role switch option (RF-01)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step ${step + 1} of 3",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = onSwitchToVendor,
                    modifier = Modifier.testTag("onboarding_vendor_switch")
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("I'm a Vendor")
                }
            }

            LinearProgressIndicator(
                progress = { (step + 1) / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                label = "onboarding_step"
            ) { currentStep ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (currentStep) {
                        0 -> {
                            Text(
                                text = "Let's start with your story",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "These details calibrate your smart wedding budget, Utah County vendor map, and wedding website.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = brideName,
                                    onValueChange = { brideName = it },
                                    label = { Text("Your name") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_bride_name")
                                )
                                OutlinedTextField(
                                    value = partnerName,
                                    onValueChange = { partnerName = it },
                                    label = { Text("Partner's name") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_partner_name")
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = weddingDate,
                                    onValueChange = { weddingDate = it },
                                    label = { Text("Date (YYYY-MM-DD)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = guestsText,
                                    onValueChange = { guestsText = it.filter(Char::isDigit) },
                                    label = { Text("Est. guests") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = budgetText,
                                onValueChange = { budgetText = it.filter(Char::isDigit) },
                                label = { Text("Estimated total budget (USD)") },
                                singleLine = true,
                                prefix = { Text("$") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_total_budget")
                            )

                            Text(
                                text = "Utah County wedding location",
                                style = MaterialTheme.typography.titleSmall
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DefaultWeddingCatalog.CITIES.forEach { city ->
                                    FilterChip(
                                        selected = selectedCity.name == city.name,
                                        onClick = { selectedCity = city },
                                        label = { Text(city.name) }
                                    )
                                }
                            }
                        }

                        1 -> {
                            Text(
                                text = "Aesthetic vision & expectations",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "We tailor your stationery suite, wedding website, and vendor affinity scores to match your style.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text("Wedding styles you love", style = MaterialTheme.typography.titleSmall)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DefaultWeddingCatalog.STYLE_TAGS.forEach { tag ->
                                    val isSelected = selectedStyles.contains(tag)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected && selectedStyles.size > 1) selectedStyles.remove(tag)
                                            else if (!isSelected && selectedStyles.size < 4) selectedStyles.add(tag)
                                        },
                                        label = { Text(tag) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }

                            Text("Color palette", style = MaterialTheme.typography.titleSmall)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DefaultWeddingCatalog.COLOR_PALETTES.forEach { (name, hexList) ->
                                    val active = selectedPaletteName == name
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = if (active) 2.dp else 1.dp,
                                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { selectedPaletteName = name }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(name, style = MaterialTheme.typography.titleSmall)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                hexList.forEach { hex ->
                                                    val parsedColor = remember(hex) {
                                                        Color(android.graphics.Color.parseColor(hex))
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(parsedColor)
                                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Text("Ceremony setting", style = MaterialTheme.typography.titleSmall)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DefaultWeddingCatalog.CEREMONY_TYPES.forEach { cType ->
                                    FilterChip(
                                        selected = selectedCeremony == cType,
                                        onClick = { selectedCeremony = cType },
                                        label = { Text(cType) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = visionExpectations,
                                onValueChange = { visionExpectations = it },
                                label = { Text("What do you dream of remembering most?") },
                                minLines = 3,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        else -> {
                            Text(
                                text = "Priority Ranking",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Rank categories from highest to lowest importance. Our engine recalculates your allocations in real time without exceeding your budget.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val matchedSuite = DefaultWeddingCatalog.suiteForStyles(selectedStyles)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "Matched suite: ${matchedSuite.name}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            matchedSuite.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            priorityOrder.forEachIndexed { idx, cat ->
                                val previewAmount = previewAllocations.firstOrNull { it.category == cat }?.allocatedAmount ?: 0.0
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (idx < 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (idx < 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cat, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = "Suggested allocation: ${BudgetRebalancer.formatCurrency(previewAmount)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                if (idx > 0) {
                                                    val item = priorityOrder.removeAt(idx)
                                                    priorityOrder.add(idx - 1, item)
                                                }
                                            },
                                            enabled = idx > 0
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move priority up")
                                        }
                                        IconButton(
                                            onClick = {
                                                if (idx < priorityOrder.lastIndex) {
                                                    val item = priorityOrder.removeAt(idx)
                                                    priorityOrder.add(idx + 1, item)
                                                }
                                            },
                                            enabled = idx < priorityOrder.lastIndex
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move priority down")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step -= 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        if (step < 2) {
                            step += 1
                        } else {
                            onCompleteCoupleOnboarding(
                                brideName,
                                partnerName,
                                weddingDate,
                                selectedCity.name,
                                selectedCity.lat,
                                selectedCity.lng,
                                parsedBudget,
                                guestsText.toLongOrNull()?.coerceIn(2L, 5000L) ?: 140L,
                                selectedStyles.toList(),
                                DefaultWeddingCatalog.COLOR_PALETTES[selectedPaletteName]
                                    ?: listOf("#7B2D3B", "#FAF6F1", "#536B5E"),
                                selectedCeremony,
                                visionExpectations,
                                priorityOrder.toList()
                            )
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(52.dp)
                        .testTag("onboarding_next_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (step < 2) "Continue" else "Create My Wedding Plan")
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            if (step < 2) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                            null,
                            Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
