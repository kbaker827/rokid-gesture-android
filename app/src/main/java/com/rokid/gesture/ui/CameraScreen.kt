package com.rokid.gesture.ui

import android.graphics.PointF
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rokid.gesture.data.*
import com.rokid.gesture.viewmodel.GestureViewModel
import com.rokid.gesture.vision.HandLandmarkHelper
import androidx.camera.core.CameraSelector

@Composable
fun CameraScreen(vm: GestureViewModel = viewModel()) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val handPoints     by vm.handPoints.collectAsState()
    val currentGesture by vm.currentGesture.collectAsState()
    val lastFiredLabel by vm.lastFiredLabel.collectAsState()
    val isDetecting    by vm.isDetecting.collectAsState()

    // Toggle between back (world-facing) and front camera
    var useFrontCamera by remember { mutableStateOf(false) }
    val cameraSelector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                         else                CameraSelector.DEFAULT_BACK_CAMERA

    val helper = remember(useFrontCamera) {
        HandLandmarkHelper(
            context        = context,
            cameraSelector = cameraSelector,
            onResult       = { pts -> vm.processHandPoints(pts) },
            onError        = { /* snackbar in production */ }
        )
    }
    DisposableEffect(useFrontCamera) { onDispose { helper.close() } }

    // Restart camera when detection toggled on
    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera preview ────────────────────────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewViewRef.value = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { pv ->
                if (isDetecting) helper.bindCamera(lifecycleOwner, pv)
            }
        )

        // ── Hand skeleton overlay ─────────────────────────────────────────────────────────────
        if (handPoints.isNotEmpty()) {
            HandSkeletonOverlay(pts = handPoints, modifier = Modifier.fillMaxSize())
        }

        // ── Gesture badge ─────────────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(
                visible = currentGesture != GestureType.NONE,
                enter = fadeIn(), exit = fadeOut()
            ) {
                Text(text = currentGesture.emoji, fontSize = 72.sp)
            }
            AnimatedVisibility(
                visible = lastFiredLabel.isNotEmpty(),
                enter = fadeIn(), exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xCC000000)
                ) {
                    Text(
                        text = lastFiredLabel,
                        color = Color.Yellow,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // ── Toolbar (top-right) ───────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Start / Stop detection
            FloatingActionButton(
                onClick = { vm.setDetecting(!isDetecting) },
                containerColor = if (isDetecting) Color(0xFFEF5350) else Color(0xFF66BB6A),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = if (isDetecting) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isDetecting) "Stop detection" else "Start detection",
                    tint = Color.White
                )
            }

            // Flip camera
            FloatingActionButton(
                onClick = { useFrontCamera = !useFrontCamera },
                containerColor = Color(0xFF37474F),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Flip camera", tint = Color.White)
            }
        }

        // ── "Not detecting" overlay ───────────────────────────────────────────────────────────
        if (!isDetecting) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xAA000000)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🖐", fontSize = 48.sp)
                        Text(
                            text = "Tap ▶ to start gesture detection",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Hold your hand 30–60 cm from the glasses camera",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // ── Camera label chip ─────────────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(50),
            color = Color(0x99000000)
        ) {
            Text(
                text = if (useFrontCamera) "📷 Front" else "📷 World",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

// ── Hand skeleton overlay ─────────────────────────────────────────────────────────────────────

@Composable
fun HandSkeletonOverlay(pts: List<PointF?>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun toOffset(pt: PointF?): Offset? =
            pt?.let { Offset(it.x * w, it.y * h) }   // MediaPipe y already goes top→bottom

        // Bone chains
        for (chain in BONE_CHAINS) {
            for (i in 0 until chain.size - 1) {
                val a = toOffset(pts.getOrNull(chain[i]))     ?: continue
                val b = toOffset(pts.getOrNull(chain[i + 1])) ?: continue
                drawLine(color = Color.White.copy(alpha = 0.55f), start = a, end = b, strokeWidth = 3f)
            }
        }

        // Joints
        for (i in 0..20) {
            val c = toOffset(pts.getOrNull(i)) ?: continue
            val isTip = i in TIP_INDICES
            if (isTip) {
                drawCircle(color = Color.Yellow.copy(alpha = 0.25f), radius = 14f, center = c)
                drawCircle(color = Color.Yellow, radius = 7f, center = c)
            } else {
                drawCircle(color = Color.White, radius = 5f, center = c)
            }
        }
    }
}
