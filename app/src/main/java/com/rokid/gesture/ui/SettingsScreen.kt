package com.rokid.gesture.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rokid.gesture.data.*
import com.rokid.gesture.viewmodel.GestureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: GestureViewModel = viewModel()) {
    val cooldown       by vm.cooldownSec.collectAsState()
    val swipeThreshold by vm.swipeThreshold.collectAsState()
    val mapping        by vm.mapping.collectAsState()
    val format         by vm.glassesFormat.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Gesture Timing ────────────────────────────────────────────────────────────────
            SettingsSection("Gesture Timing") {
                SliderRow(
                    label     = "Cooldown",
                    value     = cooldown,
                    range     = 0.3f..3.0f,
                    display   = "%.1fs".format(cooldown),
                    onChange  = { vm.setCooldown(it) }
                )
                SliderRow(
                    label     = "Swipe Sensitivity",
                    value     = swipeThreshold,
                    range     = 0.05f..0.35f,
                    display   = "%.2f".format(swipeThreshold),
                    onChange  = { vm.setSwipeThreshold(it) }
                )
                Text(
                    "Lower sensitivity = easier to trigger swipes  ·  Higher = requires bigger wrist motion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Glasses Display ───────────────────────────────────────────────────────────────
            SettingsSection("Glasses Display Format") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    GlassesFormat.entries.forEach { f ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(f.name.lowercase().replaceFirstChar { it.uppercase() })
                                val example = when (f) {
                                    GlassesFormat.FULL    -> "▶ Home  /  Notifications  /  …"
                                    GlassesFormat.COMPACT -> "[1/8] Home"
                                    GlassesFormat.MINIMAL -> "Home"
                                }
                                Text(example, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(selected = format == f, onClick = { vm.setGlassesFormat(f) })
                        }
                    }
                }
            }

            // ── Gesture → Action Mapping ──────────────────────────────────────────────────────
            SettingsSection("Gesture → Action Mapping") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GestureActionRow("✊ Fist",        mapping.fist)       { vm.setMapping(mapping.copy(fist        = it)) }
                    GestureActionRow("🖐 Open Palm",   mapping.openPalm)   { vm.setMapping(mapping.copy(openPalm   = it)) }
                    GestureActionRow("☝️ Point",       mapping.pointOne)   { vm.setMapping(mapping.copy(pointOne   = it)) }
                    GestureActionRow("✌️ Peace Sign",  mapping.peaceSign)  { vm.setMapping(mapping.copy(peaceSign  = it)) }
                    GestureActionRow("👍 Thumbs Up",   mapping.thumbsUp)   { vm.setMapping(mapping.copy(thumbsUp   = it)) }
                    GestureActionRow("👎 Thumbs Down", mapping.thumbsDown) { vm.setMapping(mapping.copy(thumbsDown = it)) }
                    HorizontalDivider()
                    GestureActionRow("→ Swipe Right",  mapping.swipeRight) { vm.setMapping(mapping.copy(swipeRight = it)) }
                    GestureActionRow("← Swipe Left",   mapping.swipeLeft)  { vm.setMapping(mapping.copy(swipeLeft  = it)) }
                    GestureActionRow("↑ Swipe Up",     mapping.swipeUp)    { vm.setMapping(mapping.copy(swipeUp    = it)) }
                    GestureActionRow("↓ Swipe Down",   mapping.swipeDown)  { vm.setMapping(mapping.copy(swipeDown  = it)) }
                }
            }

            // ── Tips ──────────────────────────────────────────────────────────────────────────
            SettingsSection("Tips for Best Results") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Hold your hand 30–60 cm from the glasses camera",
                        "Palm facing the camera with fingers pointing upward",
                        "For swipes, a quick wrist flick — no need to move your whole arm",
                        "Good lighting significantly improves MediaPipe accuracy",
                        "Landmarks below 0.5 confidence are automatically ignored",
                        "Use 'World' camera (back of glasses) for best hand detection while wearing"
                    ).forEach { tip ->
                        Text("• $tip",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────────────────────

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title,
            style    = MaterialTheme.typography.titleSmall,
            color    = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
    HorizontalDivider(Modifier.padding(vertical = 2.dp))
}

@Composable
fun SliderRow(
    label:    String,
    value:    Float,
    range:    ClosedFloatingPointRange<Float>,
    display:  String,
    onChange: (Float) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Text(display, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary)
    }
    Slider(value = value, onValueChange = onChange, valueRange = range)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureActionRow(label: String, current: NavAction, onSelect: (NavAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it },
            modifier = Modifier.widthIn(max = 200.dp)) {
            OutlinedTextField(
                value         = current.label,
                onValueChange = {},
                readOnly      = true,
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier      = Modifier.menuAnchor(),
                textStyle     = MaterialTheme.typography.bodySmall,
                colors        = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                NavAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text    = { Text(action.label) },
                        onClick = { onSelect(action); expanded = false }
                    )
                }
            }
        }
    }
}
