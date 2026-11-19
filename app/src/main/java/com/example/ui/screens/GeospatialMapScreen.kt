package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.BudgetRebalancer
import com.example.domain.DefaultWeddingCatalog
import com.example.domain.ScoredVendor
import com.example.domain.VendorProfile
import com.example.domain.Wedding
import kotlin.math.hypot

@Composable
fun GeospatialMapScreen(
    wedding: Wedding?,
    scoredVendors: List<ScoredVendor>,
    highlightedVendorId: String?,
    isBusy: Boolean,
    onClearHighlight: () -> Unit,
    onSendInquiry: (vendor: VendorProfile, packageTitle: String, budget: Double, notes: String) -> Unit,
    onLoadDemoVendors: () -> Unit,
    onSwitchToVendorPortal: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Todos") }
    var onlyInBudget by remember { mutableStateOf(false) }
    var onlyInRadius by remember { mutableStateOf(false) }
    var selectedVendor by remember { mutableStateOf<ScoredVendor?>(null) }
    var inquiryTarget by remember { mutableStateOf<ScoredVendor?>(null) }

    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val filtered = remember(scoredVendors, selectedCategory, onlyInBudget, onlyInRadius) {
        scoredVendors.filter { sv ->
            val catOk = selectedCategory == "Todos" || sv.vendor.category.equals(selectedCategory, ignoreCase = true)
            val budgetOk = !onlyInBudget || sv.withinCategoryBudget
            val radiusOk = !onlyInRadius || sv.coversWeddingLocation
            catOk && budgetOk && radiusOk
        }
    }

    LaunchedEffect(highlightedVendorId, scoredVendors) {
        if (!highlightedVendorId.isNullOrBlank()) {
            selectedVendor = scoredVendors.firstOrNull { it.vendor.id == highlightedVendorId }
            onClearHighlight()
        }
    }

    val centerLat = wedding?.lat ?: 19.4326
    val centerLng = wedding?.lng ?: -99.1332

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Bar (RF-07)
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("Mapa Geoespacial de Proveedores", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Ubicación de boda: ${wedding?.city ?: "CDMX"} (${"%.3f".format(centerLat)}, ${"%.3f".format(centerLng)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = onlyInBudget,
                        onClick = { onlyInBudget = !onlyInBudget },
                        label = { Text("En presupuesto") }
                    )
                    FilterChip(
                        selected = onlyInRadius,
                        onClick = { onlyInRadius = !onlyInRadius },
                        label = { Text("Cubre mi zona") }
                    )
                    (listOf("Todos") + DefaultWeddingCatalog.CATEGORIES.take(6)).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        }

        // Interactive Geospatial Map Canvas (Pan + Zoom + Coverage Rings + Pin Selection)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(248.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
            val surfaceColor = MaterialTheme.colorScheme.surface

            // Scale factor: 0.1 degree ~ 11 km
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(0.6f, 2.8f)
                            panOffset = Offset(
                                (panOffset.x + pan.x).coerceIn(-280f, 280f),
                                (panOffset.y + pan.y).coerceIn(-200f, 200f)
                            )
                        }
                    }
                    .pointerInput(filtered, zoom, panOffset) {
                        detectTapGestures { tap ->
                            val cx = size.width / 2f + panOffset.x
                            val cy = size.height / 2f + panOffset.y
                            val scale = 1400f * zoom
                            val hit = filtered.minByOrNull { sv ->
                                val px = cx + ((sv.vendor.lng - centerLng) * scale).toFloat()
                                val py = cy - ((sv.vendor.lat - centerLat) * scale).toFloat()
                                hypot(tap.x - px, tap.y - py)
                            }
                            if (hit != null) {
                                val px = cx + ((hit.vendor.lng - centerLng) * scale).toFloat()
                                val py = cy - ((hit.vendor.lat - centerLat) * scale).toFloat()
                                if (hypot(tap.x - px, tap.y - py) <= 44.dp.toPx()) {
                                    selectedVendor = hit
                                }
                            }
                        }
                    }
            ) {
                val cx = size.width / 2f + panOffset.x
                val cy = size.height / 2f + panOffset.y
                val scale = 1400f * zoom

                // Topographic / Street Grid
                val step = 36.dp.toPx()
                var x = (panOffset.x % step)
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = (panOffset.y % step)
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }

                // Concentric geospatial distance rings around wedding location (5 km, 12 km)
                listOf(65f * zoom, 130f * zoom).forEach { r ->
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.25f),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        )
                    )
                }

                // Wedding Venue Origin Pin
                drawCircle(color = primaryColor.copy(alpha = 0.18f), radius = 22.dp.toPx(), center = Offset(cx, cy))
                drawCircle(color = primaryColor, radius = 9.dp.toPx(), center = Offset(cx, cy))
                drawCircle(color = surfaceColor, radius = 3.5.dp.toPx(), center = Offset(cx, cy))

                // Vendor Pins + Selected Coverage Radius Halo
                filtered.forEach { sv ->
                    val px = cx + ((sv.vendor.lng - centerLng) * scale).toFloat()
                    val py = cy - ((sv.vendor.lat - centerLat) * scale).toFloat()
                    val isSelected = selectedVendor?.vendor?.id == sv.vendor.id
                    val pinColor = when {
                        isSelected -> primaryColor
                        sv.withinCategoryBudget -> secondaryColor
                        else -> tertiaryColor
                    }

                    if (isSelected) {
                        drawCircle(
                            color = pinColor.copy(alpha = 0.16f),
                            radius = 36.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                    drawCircle(color = surfaceColor, radius = if (isSelected) 13.dp.toPx() else 10.dp.toPx(), center = Offset(px, py))
                    drawCircle(color = pinColor, radius = if (isSelected) 9.dp.toPx() else 7.dp.toPx(), center = Offset(px, py))
                }
            }

            // Map Legend Overlay
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LegendDot(MaterialTheme.colorScheme.primary, "Tu boda")
                    LegendDot(MaterialTheme.colorScheme.secondary, "En presupuesto")
                    LegendDot(MaterialTheme.colorScheme.tertiary, "Premium")
                }
            }

            // Zoom & Center Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(shape = CircleShape, tonalElevation = 3.dp) {
                    IconButton(onClick = { zoom = (zoom + 0.3f).coerceAtMost(2.8f) }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Acercar mapa")
                    }
                }
                Surface(shape = CircleShape, tonalElevation = 3.dp) {
                    IconButton(onClick = { zoom = (zoom - 0.3f).coerceAtLeast(0.6f) }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Alejar mapa")
                    }
                }
                Surface(shape = CircleShape, tonalElevation = 3.dp) {
                    IconButton(onClick = { zoom = 1f; panOffset = Offset.Zero }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Centrar en mi boda")
                    }
                }
            }
        }

        // Split List of Matching Vendors
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text("Aún no hay proveedores visibles con estos filtros", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Puedes publicar tu propio negocio en el Portal de Proveedor o cargar un directorio de prueba en ${wedding?.city ?: "tu ciudad"} para explorar el mapa y el Asistente IA.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onLoadDemoVendors,
                                enabled = !isBusy,
                                modifier = Modifier.testTag("load_demo_vendors_button")
                            ) {
                                Text("Cargar 5 proveedores demo")
                            }
                            OutlinedButton(onClick = onSwitchToVendorPortal) {
                                Icon(Icons.Default.Storefront, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Portal Proveedor")
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.vendor.id }) { sv ->
                    val isSelected = selectedVendor?.vendor?.id == sv.vendor.id
                    VendorDiscoveryCard(
                        scored = sv,
                        selected = isSelected,
                        onSelect = { selectedVendor = sv },
                        onRequestQuote = { inquiryTarget = sv }
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }

    inquiryTarget?.let { sv ->
        SendInquiryDialog(
            scored = sv,
            onDismiss = { inquiryTarget = null },
            onConfirm = { pkgTitle, budget, notes ->
                onSendInquiry(sv.vendor, pkgTitle, budget, notes)
                inquiryTarget = null
            }
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun VendorDiscoveryCard(
    scored: ScoredVendor,
    selected: Boolean,
    onSelect: () -> Unit,
    onRequestQuote: () -> Unit
) {
    val v = scored.vendor
    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(v.businessName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${v.category} • ${v.city} • ${"%.1f".format(scored.distanceKm)} km (radio ${v.coverageRadiusKm.toInt()} km)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "${scored.affinityScore}% match",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (v.description.isNotBlank()) {
                Text(v.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                    Text("${v.ratingAvg} (${v.completedEventsCount} bodas)", style = MaterialTheme.typography.labelSmall)
                    Text("•", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "${BudgetRebalancer.formatCurrency(v.minPrice)}–${BudgetRebalancer.formatCurrency(v.maxPrice)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = onRequestQuote,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Solicitar", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SendInquiryDialog(
    scored: ScoredVendor,
    onDismiss: () -> Unit,
    onConfirm: (pkgTitle: String, budget: Double, notes: String) -> Unit
) {
    val v = scored.vendor
    var pkgTitle by remember { mutableStateOf("Paquete Integral de ${v.category}") }
    var budgetText by remember {
        mutableStateOf(scored.categoryBudgetAllocated.coerceIn(v.minPrice, v.maxPrice.coerceAtLeast(v.minPrice)).toLong().toString())
    }
    var notes by remember { mutableStateOf("Nos encantó su estilo. ¿Tienen disponibilidad para nuestra fecha?") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Solicitar a ${v.businessName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Presupuesto asignado en tu plan para ${v.category}: ${BudgetRebalancer.formatCurrency(scored.categoryBudgetAllocated)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = pkgTitle,
                    onValueChange = { pkgTitle = it },
                    label = { Text("Servicio / Paquete deseado") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter(Char::isDigit) },
                    label = { Text("Presupuesto propuesto") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas para el proveedor") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(pkgTitle, budgetText.toDoubleOrNull() ?: v.minPrice, notes)
            }) { Text("Enviar solicitud") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
