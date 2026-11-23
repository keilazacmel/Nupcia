package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.BudgetRebalancer
import com.example.domain.DefaultWeddingCatalog
import com.example.domain.ScoredVendor
import com.example.domain.VendorProfile
import com.example.domain.Wedding
import com.example.ui.theme.MapCanvasBottom
import com.example.ui.theme.MapCanvasTop
import com.example.ui.theme.MapCorridorStroke
import com.example.ui.theme.MapFoothillContour
import com.example.ui.theme.MapGridStroke
import com.example.ui.theme.MapOverlayScrim
import com.example.ui.theme.MapOverlayText
import com.example.ui.theme.MapPinInBudget
import com.example.ui.theme.MapPinPremium
import com.example.ui.theme.MapPinWedding
import com.example.ui.theme.MapUtahLakeFill
import com.example.ui.theme.MapUtahLakeStroke
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
    var selectedCategory by remember { mutableStateOf("All") }
    var onlyInBudget by remember { mutableStateOf(false) }
    var onlyInRadius by remember { mutableStateOf(false) }
    var selectedVendor by remember { mutableStateOf<ScoredVendor?>(null) }
    var inquiryTarget by remember { mutableStateOf<ScoredVendor?>(null) }

    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val filtered = remember(scoredVendors, selectedCategory, onlyInBudget, onlyInRadius) {
        scoredVendors.filter { sv ->
            val catOk = selectedCategory == "All" || sv.vendor.category.equals(selectedCategory, ignoreCase = true)
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

    // Default to Provo, Utah County, UT
    val centerLat = wedding?.lat ?: 40.2338
    val centerLng = wedding?.lng ?: -111.6585

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Bar (RF-07)
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("Utah County Vendor Map", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Wedding venue: ${wedding?.city ?: "Provo, UT"} (${"%.3f".format(centerLat)}, ${"%.3f".format(centerLng)})",
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
                        label = { Text("In budget") }
                    )
                    FilterChip(
                        selected = onlyInRadius,
                        onClick = { onlyInRadius = !onlyInRadius },
                        label = { Text("Covers venue") }
                    )
                    (listOf("All") + DefaultWeddingCatalog.CATEGORIES.take(6)).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        }

        // High-Contrast Wasatch Slate-Teal Cartographic Map Canvas
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(264.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp,
            color = MapCanvasBottom
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(MapCanvasTop, MapCanvasBottom)))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                val baseScale = 860f

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("geospatial_map_canvas")
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                zoom = (zoom * gestureZoom).coerceIn(0.6f, 2.8f)
                                panOffset = Offset(
                                    (panOffset.x + pan.x).coerceIn(-280f, 280f),
                                    (panOffset.y + pan.y).coerceIn(-220f, 220f)
                                )
                            }
                        }
                        .pointerInput(filtered, zoom, panOffset, centerLat, centerLng) {
                            detectTapGestures { tap ->
                                val cx = size.width / 2f + panOffset.x
                                val cy = size.height / 2f + panOffset.y
                                val scale = baseScale * zoom
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
                    val scale = baseScale * zoom

                    fun geoToOffset(lat: Double, lng: Double): Offset = Offset(
                        x = cx + ((lng - centerLng) * scale).toFloat(),
                        y = cy - ((lat - centerLat) * scale).toFloat()
                    )

                    // 1. Subtle Topographic Cartographic Grid
                    val step = 36.dp.toPx()
                    var x = (panOffset.x % step)
                    while (x < size.width) {
                        drawLine(MapGridStroke.copy(alpha = 0.28f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        x += step
                    }
                    var y = (panOffset.y % step)
                    while (y < size.height) {
                        drawLine(MapGridStroke.copy(alpha = 0.28f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += step
                    }

                    // 2. Stylized Utah Lake Water Body (West Valley)
                    val lakeCoords = listOf(
                        40.355 to -111.860,
                        40.350 to -111.790,
                        40.290 to -111.745,
                        40.225 to -111.735,
                        40.165 to -111.740,
                        40.095 to -111.795,
                        40.075 to -111.860,
                        40.145 to -111.895,
                        40.245 to -111.890
                    )
                    val lakePath = Path().apply {
                        lakeCoords.forEachIndexed { idx, (lat, lng) ->
                            val pt = geoToOffset(lat, lng)
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                        close()
                    }
                    drawPath(lakePath, color = MapUtahLakeFill)
                    drawPath(lakePath, color = MapUtahLakeStroke.copy(alpha = 0.75f), style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))

                    // 3. Wasatch Front / Mount Timpanogos Foothill Contours (East)
                    val ridgeOuter = listOf(
                        40.48 to -111.61,
                        40.40 to -111.57,
                        40.33 to -111.60,
                        40.25 to -111.58,
                        40.17 to -111.56,
                        40.08 to -111.59,
                        40.08 to -111.35,
                        40.48 to -111.35
                    )
                    val ridgePath = Path().apply {
                        ridgeOuter.forEachIndexed { idx, (lat, lng) ->
                            val pt = geoToOffset(lat, lng)
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                        close()
                    }
                    drawPath(ridgePath, color = MapFoothillContour.copy(alpha = 0.55f))
                    drawPath(
                        ridgePath,
                        color = MapCorridorStroke.copy(alpha = 0.35f),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f)))
                    )

                    // 4. I-15 Valley Corridor & US-189 Provo Canyon Road
                    val i15Points = listOf(
                        40.1150 to -111.6549, // Spanish Fork
                        40.1652 to -111.6108, // Springville
                        40.2338 to -111.6585, // Provo
                        40.2969 to -111.6946, // Orem
                        40.3641 to -111.7385, // Pleasant Grove
                        40.3769 to -111.7958, // American Fork
                        40.3916 to -111.8508  // Lehi
                    )
                    val i15Path = Path().apply {
                        i15Points.forEachIndexed { idx, (lat, lng) ->
                            val pt = geoToOffset(lat, lng)
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        i15Path,
                        color = MapCorridorStroke.copy(alpha = 0.65f),
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    val canyonPath = Path().apply {
                        val start = geoToOffset(40.2969, -111.6946)
                        val mid = geoToOffset(40.3350, -111.6350)
                        val end = geoToOffset(40.3934, -111.5888)
                        moveTo(start.x, start.y)
                        lineTo(mid.x, mid.y)
                        lineTo(end.x, end.y)
                    }
                    drawPath(
                        canyonPath,
                        color = MapCorridorStroke.copy(alpha = 0.5f),
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 5. Concentric Geospatial Distance Rings Around Wedding Venue (5 mi, 12 mi)
                    listOf(0.0725f * scale, 0.174f * scale).forEach { r ->
                        drawCircle(
                            color = MapOverlayText.copy(alpha = 0.28f),
                            radius = r,
                            center = Offset(cx, cy),
                            style = Stroke(
                                width = 1.6f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                            )
                        )
                    }

                    // 6. Wedding Venue Origin Pin (Vibrant Coral-Rose)
                    drawCircle(color = MapPinWedding.copy(alpha = 0.25f), radius = 22.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(color = MapOverlayText, radius = 11.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(color = MapPinWedding, radius = 8.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(color = MapOverlayText, radius = 2.8.dp.toPx(), center = Offset(cx, cy))

                    // 7. Vendor Pins (High-Contrast Mint-Sage & Champagne Gold)
                    filtered.forEach { sv ->
                        val pos = geoToOffset(sv.vendor.lat, sv.vendor.lng)
                        val isSelected = selectedVendor?.vendor?.id == sv.vendor.id
                        val pinColor = if (sv.withinCategoryBudget) MapPinInBudget else MapPinPremium

                        if (isSelected) {
                            drawCircle(
                                color = pinColor.copy(alpha = 0.28f),
                                radius = 32.dp.toPx(),
                                center = pos
                            )
                            drawCircle(
                                color = MapOverlayText,
                                radius = 14.dp.toPx(),
                                center = pos
                            )
                            drawCircle(
                                color = pinColor,
                                radius = 10.5.dp.toPx(),
                                center = pos
                            )
                        } else {
                            drawCircle(color = MapCanvasBottom, radius = 11.dp.toPx(), center = pos)
                            drawCircle(color = MapOverlayText.copy(alpha = 0.9f), radius = 9.5.dp.toPx(), center = pos)
                            drawCircle(color = pinColor, radius = 7.dp.toPx(), center = pos)
                        }
                    }
                }

                // Frosted Dark-Glass Legend Overlay (Top-Left)
                Surface(
                    color = MapOverlayScrim,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        LegendDot(MapPinWedding, "Venue")
                        LegendDot(MapPinInBudget, "In budget")
                        LegendDot(MapPinPremium, "Premium")
                    }
                }

                // Region Badge (Top-Right)
                Surface(
                    color = MapOverlayScrim,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MapPinInBudget,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            "Utah County, UT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MapOverlayText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Selected Vendor Quick Callout Chip (Bottom-Left)
                selectedVendor?.let { sv ->
                    Surface(
                        color = MapOverlayScrim,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 10.dp, bottom = 10.dp, end = 64.dp)
                            .clickable { inquiryTarget = sv }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(if (sv.withinCategoryBudget) MapPinInBudget else MapPinPremium)
                            )
                            Column {
                                Text(
                                    text = sv.vendor.businessName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MapOverlayText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${sv.vendor.city} • ${"%.1f".format(sv.distanceMiles)} mi • ${sv.affinityScore}% match",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MapPinInBudget
                                )
                            }
                        }
                    }
                }

                // High-Contrast Zoom & Recenter Controls (Bottom-Right)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(shape = CircleShape, color = MapOverlayScrim) {
                        IconButton(
                            onClick = { zoom = (zoom + 0.3f).coerceAtMost(2.8f) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom in", tint = MapOverlayText)
                        }
                    }
                    Surface(shape = CircleShape, color = MapOverlayScrim) {
                        IconButton(
                            onClick = { zoom = (zoom - 0.3f).coerceAtLeast(0.6f) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom out", tint = MapOverlayText)
                        }
                    }
                    Surface(shape = CircleShape, color = MapOverlayScrim) {
                        IconButton(
                            onClick = { zoom = 1f; panOffset = Offset.Zero },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Center on wedding venue", tint = MapPinWedding)
                        }
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
                        Text("No vendors match these filters yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Publish your own wedding business in the Vendor Portal or load 5 sample Utah County vendors near ${wedding?.city ?: "Provo, UT"} to explore the map and AI Assistant.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onLoadDemoVendors,
                                enabled = !isBusy,
                                modifier = Modifier.testTag("load_demo_vendors_button")
                            ) {
                                Text("Load 5 Utah vendors")
                            }
                            OutlinedButton(onClick = onSwitchToVendorPortal) {
                                Icon(Icons.Default.Storefront, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Vendor Portal")
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = MapOverlayText)
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
                        "${v.category} • ${v.city} • ${"%.1f".format(scored.distanceMiles)} mi (${scored.coverageRadiusMiles} mi radius)",
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
                    Text("${v.ratingAvg} (${v.completedEventsCount} weddings)", style = MaterialTheme.typography.labelSmall)
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
                    Text("Inquire", style = MaterialTheme.typography.labelMedium)
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
    var pkgTitle by remember { mutableStateOf("Full ${v.category} Package") }
    var budgetText by remember {
        mutableStateOf(scored.categoryBudgetAllocated.coerceIn(v.minPrice, v.maxPrice.coerceAtLeast(v.minPrice)).toLong().toString())
    }
    var notes by remember { mutableStateOf("We love your work! Are you available on our wedding date in Utah County?") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inquire with ${v.businessName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Your allocated budget for ${v.category}: ${BudgetRebalancer.formatCurrency(scored.categoryBudgetAllocated)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = pkgTitle,
                    onValueChange = { pkgTitle = it },
                    label = { Text("Desired service / package") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter(Char::isDigit) },
                    label = { Text("Proposed budget (USD)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes for vendor") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(pkgTitle, budgetText.toDoubleOrNull() ?: v.minPrice, notes)
            }) { Text("Send inquiry") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
