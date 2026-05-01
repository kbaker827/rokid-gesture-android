package com.rokid.gesture.ui

import android.graphics.PointF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rokid.gesture.data.*
import com.rokid.gesture.viewmodel.GestureViewModel
import com.rokid.gesture.vision.HandLandmarkHelper

// ── Colour palette for the HUD ────────────────────────────────────────────────────────────────
// On Rokid's OLED HUD display, black pixels are transparent (emit no light).
// Everything drawn here floats as AR text/shapes over the real world.

private val HudGold    = Color(0xFFFFD600)     // selected item / cursor
private val HudWhite   = Color(0xFFFFFFFF)     // normal items
private val HudDim     = Color(0x99FFFFFF)     // secondary text
private val HudGreen   = Color(0xFF00E676)     // confirmation / active indicator
private val HudRed     = Color(0xFFEF5350)     // stop / error
private val HudCyan    = Color(0xFF80E5FF)     // gesture label
private val HudPanel   = Color(0xBB000000)     // semi-opaque panel backing

private val HudTextShadow = Shadow(Color.Black, Offset(1f, 1f), blurRadius = 4f)

@Composable
fun HudScreen(vm: GestureViewModel = viewModel()) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val handPoints     by vm.handPoints.collectAsState()
    val currentGesture by vm.currentGesture.collectAsState()
    val lastFiredLabel by vm.lastFiredLabel.collectAsState()
    val isDetecting    by vm.isDetecting.collectAsState()
    val menu           by vm.menu.collectAsState()
    val format         by vm.glassesFormat.collectAsState()

    var showSkeleton   by remember { mutableStateOf(true) }

    // Camera runs silently — no preview surface rendered on the HUD
    val helper = remember {
        HandLandmarkHelper(
            context  = context,
            onResult = { pts -> vm.processHandPoints(pts) },
            onError  = { /* surface non-critical errors as HUD text if needed */ }
        )
    }
    DisposableEffect(Unit) { onDispose { helper.close() } }

    LaunchedEffect(isDetecting) {
        if (isDetecting) helper.bindAnalysisOnly(lifecycleOwner)
    }

    // ── Root: black = transparent on OLED HUD ────────────────────────────────────────────────
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)   // transparent on Rokid OLED HUD
    ) {

        // ── Optional skeleton overlay (calibration aid) ───────────────────────────────────────
        if (showSkeleton && handPoints.isNotEmpty()) {
            HudSkeletonOverlay(pts = handPoints, modifier = Modifier.fillMaxSize())
        }

        // ── Main menu panel (left-centre, slight inset) ───────────────────────────────────────
        AnimatedVisibility(
            visible = isDetecting,
            enter   = fadeIn() + slideInVertically { -it / 4 },
            exit    = fadeOut() + slideOutVertically { -it / 4 },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
        ) {
            HudMenuPanel(menu = menu, format = format)
        }

        // ── Gesture label (bottom-left) ───────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = isDetecting && lastFiredLabel.isNotEmpty(),
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, bottom = 48.dp)
        ) {
            HudGestureLabel(label = lastFiredLabel, emoji = currentGesture.emoji)
        }

        // ── Current gesture badge (right side, large emoji) ───────────────────────────────────
        AnimatedVisibility(
            visible  = isDetecting && currentGesture != GestureType.NONE,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
        ) {
            Text(
                text     = currentGesture.emoji,
                fontSize = 64.sp,
                style    = TextStyle(shadow = HudTextShadow)
            )
        }

        // ── Status indicator (top-left) ───────────────────────────────────────────────────────
        HudStatusChip(
            isDetecting  = isDetecting,
            modifier     = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 16.dp)
        )

        // ── Controls (top-right) ──────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Skeleton toggle
            TextButton(
                onClick = { showSkeleton = !showSkeleton },
                colors  = ButtonDefaults.textButtonColors(contentColor = HudDim)
            ) {
                Text(if (showSkeleton) "Hide Skeleton" else "Show Skeleton", fontSize = 11.sp)
            }

            // Start / Stop detection
            FloatingActionButton(
                onClick          = { vm.setDetecting(!isDetecting) },
                containerColor   = if (isDetecting) HudRed else HudGreen,
                contentColor     = Color.Black,
                modifier         = Modifier.size(44.dp),
                shape            = CircleShape
            ) {
                Icon(
                    imageVector     = if (isDetecting) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isDetecting) "Stop" else "Start"
                )
            }
        }

        // ── "Not detecting" prompt ────────────────────────────────────────────────────────────
        if (!isDetecting) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HudPanel
                ) {
                    Column(
                        modifier                = Modifier.padding(36.dp),
                        horizontalAlignment     = Alignment.CenterHorizontally,
                        verticalArrangement     = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🖐", fontSize = 56.sp)
                        Text(
                            text  = "Tap ▶ to activate gesture detection",
                            color = HudWhite,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text  = "Hold your hand 30–60 cm from the glasses camera",
                            color = HudDim,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// ── HUD menu panel ────────────────────────────────────────────────────────────────────────────

@Composable
fun HudMenuPanel(menu: AppMenu, format: GlassesFormat) {
    when (format) {
        GlassesFormat.FULL -> {
            // Show all items with cursor — the classic HUD list
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HudPanel
            ) {
                Column(
                    modifier            = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    menu.items.forEachIndexed { index, item ->
                        val isSelected = index == menu.selectedIndex
                        Row(
                            verticalAlignment  = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text     = if (isSelected) "▶" else "  ",
                                color    = HudGold,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Monospace,
                                style    = TextStyle(shadow = HudTextShadow)
                            )
                            Text(
                                text       = item.title,
                                color      = if (isSelected) HudGold else HudWhite,
                                fontSize   = if (isSelected) 22.sp else 18.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Default,
                                style      = TextStyle(shadow = HudTextShadow)
                            )
                        }
                    }
                }
            }
        }

        GlassesFormat.COMPACT -> {
            // Single-line counter + title
            Surface(shape = RoundedCornerShape(8.dp), color = HudPanel) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text       = "[${menu.selectedIndex + 1}/${menu.items.size}]",
                        color      = HudDim,
                        fontSize   = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        style      = TextStyle(shadow = HudTextShadow)
                    )
                    Text(
                        text       = menu.items.getOrNull(menu.selectedIndex)?.title ?: "",
                        color      = HudGold,
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.Bold,
                        style      = TextStyle(shadow = HudTextShadow)
                    )
                }
            }
        }

        GlassesFormat.MINIMAL -> {
            // Just the current item name — maximum clarity
            Text(
                text       = menu.items.getOrNull(menu.selectedIndex)?.title ?: "",
                color      = HudGold,
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold,
                style      = TextStyle(shadow = HudTextShadow)
            )
        }
    }
}

// ── Gesture label bar ─────────────────────────────────────────────────────────────────────────

@Composable
fun HudGestureLabel(label: String, emoji: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (emoji.isNotEmpty()) {
            Text(text = emoji, fontSize = 28.sp, style = TextStyle(shadow = HudTextShadow))
        }
        Text(
            text     = label,
            color    = HudCyan,
            fontSize = 16.sp,
            style    = TextStyle(
                fontWeight = FontWeight.Medium,
                shadow     = HudTextShadow
            )
        )
    }
}

// ── Status chip ───────────────────────────────────────────────────────────────────────────────

@Composable
fun HudStatusChip(isDetecting: Boolean, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue   = 1f,
        targetValue    = 0.3f,
        animationSpec  = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label          = "alpha"
    )

    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    color = if (isDetecting) HudGreen.copy(alpha = alpha) else HudRed,
                    shape = CircleShape
                )
        )
        Text(
            text     = if (isDetecting) "Detecting" else "Stopped",
            color    = if (isDetecting) HudGreen else HudRed,
            fontSize = 12.sp,
            style    = TextStyle(shadow = HudTextShadow)
        )
    }
}

// ── Skeleton overlay (HUD calibration aid) ────────────────────────────────────────────────────
// Drawn in the lower-right corner at 25 % scale so it doesn't dominate the HUD.

@Composable
fun HudSkeletonOverlay(pts: List<PointF?>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // Confine skeleton to bottom-right quadrant at 30% size
        val inset  = 24.dp.toPx()
        val sw     = size.width  * 0.28f
        val sh     = size.height * 0.28f
        val ox     = size.width  - sw - inset
        val oy     = size.height - sh - inset

        fun toOffset(pt: PointF?): Offset? =
            pt?.let { Offset(ox + it.x * sw, oy + it.y * sh) }

        // Bone chains — subtle white lines
        for (chain in BONE_CHAINS) {
            for (i in 0 until chain.size - 1) {
                val a = toOffset(pts.getOrNull(chain[i]))     ?: continue
                val b = toOffset(pts.getOrNull(chain[i + 1])) ?: continue
                drawLine(color = Color.White.copy(alpha = 0.45f), start = a, end = b, strokeWidth = 2f)
            }
        }

        // Joints — tips in gold, others in white
        for (i in 0..20) {
            val c = toOffset(pts.getOrNull(i)) ?: continue
            if (i in TIP_INDICES) {
                drawCircle(color = HudGold.copy(alpha = 0.6f), radius = 5f, center = c)
            } else {
                drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 3f, center = c)
            }
        }
    }
}
