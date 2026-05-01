package com.rokid.gesture.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rokid.gesture.data.*
import com.rokid.gesture.viewmodel.GestureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(vm: GestureViewModel = viewModel()) {
    val menu   by vm.menu.collectAsState()
    val format by vm.glassesFormat.collectAsState()

    var showAddSheet   by remember { mutableStateOf(false) }
    var editingIndex   by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu Builder") },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add item")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Glasses display preview ───────────────────────────────────────────────────────
            GlassesPreview(text = menu.glassesText(format))
            HorizontalDivider()

            // ── Menu item list ────────────────────────────────────────────────────────────────
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(menu.items, key = { _, item -> item.id }) { index, item ->
                    SwipeableMenuItemRow(
                        item       = item,
                        isSelected = index == menu.selectedIndex,
                        onTap      = { vm.updateMenu { copy(selectedIndex = index) } },
                        onEdit     = { editingIndex = index },
                        onDelete   = {
                            vm.updateMenu {
                                val newItems = items.toMutableList().also { it.removeAt(index) }
                                copy(
                                    items         = newItems,
                                    selectedIndex = selectedIndex.coerceAtMost((newItems.size - 1).coerceAtLeast(0))
                                )
                            }
                        }
                    )
                }
                item {
                    if (menu.items.size < 8) {
                        TextButton(
                            onClick  = { showAddSheet = true },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add item  (${menu.items.size}/8)")
                        }
                    }
                }
            }

            // ── Quick action bar ──────────────────────────────────────────────────────────────
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { vm.applyAction(NavAction.SCROLL_UP)  }) { Text("⇈ First") }
                    TextButton(onClick = { vm.applyAction(NavAction.PREVIOUS)   }) { Text("◀ Prev") }
                    TextButton(onClick = { vm.applyAction(NavAction.SELECT)     }) { Text("✓ Select") }
                    TextButton(onClick = { vm.applyAction(NavAction.NEXT)       }) { Text("Next ▶") }
                    TextButton(onClick = { vm.applyAction(NavAction.SCROLL_DOWN)}) { Text("⇊ Last") }
                }
            }
        }
    }

    // ── Sheets ────────────────────────────────────────────────────────────────────────────────
    if (showAddSheet) {
        ItemEditorSheet(
            item      = null,
            onDismiss = { showAddSheet = false },
            onSave    = { newItem ->
                vm.updateMenu { copy(items = items + newItem) }
                showAddSheet = false
            }
        )
    }
    editingIndex?.let { idx ->
        menu.items.getOrNull(idx)?.let { item ->
            ItemEditorSheet(
                item      = item,
                onDismiss = { editingIndex = null },
                onSave    = { updated ->
                    vm.updateMenu { copy(items = items.toMutableList().also { it[idx] = updated }) }
                    editingIndex = null
                }
            )
        }
    }
}

// ── Glasses preview ───────────────────────────────────────────────────────────────────────────

@Composable
fun GlassesPreview(text: String) {
    Surface(
        modifier      = Modifier.fillMaxWidth().padding(12.dp),
        shape         = RoundedCornerShape(10.dp),
        color         = Color.Black,
        tonalElevation = 4.dp
    ) {
        Text(
            text       = text.ifEmpty { "— empty menu —" },
            color      = Color(0xFF00E676),
            fontFamily = FontFamily.Monospace,
            fontSize   = 13.sp,
            modifier   = Modifier.padding(12.dp)
        )
    }
}

// ── Swipeable list row ────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableMenuItemRow(
    item:       MenuItem,
    isSelected: Boolean,
    onTap:      () -> Unit,
    onEdit:     () -> Unit,
    onDelete:   () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )

    SwipeToDismissBox(
        state                   = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(Color(0xFFB71C1C)).padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.White)
            }
        }
    ) {
        ListItem(
            headlineContent   = { Text(item.title) },
            supportingContent = { if (item.subtitle.isNotEmpty()) Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingContent    = {
                Icon(
                    imageVector = if (isSelected) Icons.Default.ChevronRight else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent   = {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
            },
            modifier = Modifier
                .clickable(onClick = onTap)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
    HorizontalDivider()
}

// ── Item editor bottom sheet ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditorSheet(item: MenuItem?, onDismiss: () -> Unit, onSave: (MenuItem) -> Unit) {
    var title    by remember { mutableStateOf(item?.title    ?: "") }
    var subtitle by remember { mutableStateOf(item?.subtitle ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (item == null) "Add Item" else "Edit Item",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Title") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )
            OutlinedTextField(
                value         = subtitle,
                onValueChange = { subtitle = it },
                label         = { Text("Subtitle (optional)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(
                                item?.copy(title = title.trim(), subtitle = subtitle.trim())
                                    ?: MenuItem(title = title.trim(), subtitle = subtitle.trim())
                            )
                        }
                    },
                    enabled = title.isNotBlank()
                ) { Text("Save") }
            }
        }
    }
}
