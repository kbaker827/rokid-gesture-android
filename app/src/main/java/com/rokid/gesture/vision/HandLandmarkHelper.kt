package com.rokid.gesture.vision

import android.content.Context
import android.graphics.PointF
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.Executors

private const val TAG         = "HandLandmarkHelper"
private const val MODEL_FILE  = "hand_landmarker.task"

/**
 * Wraps CameraX + MediaPipe HandLandmarker (LIVE_STREAM mode).
 *
 * Uses the **back (world-facing) camera** — on Rokid AR glasses that is the
 * forward-looking camera pointing at the user's hand in front of them.
 *
 * Switch [cameraSelector] to [CameraSelector.DEFAULT_FRONT_CAMERA] if your
 * hardware places the hand-tracking camera on the other side.
 */
class HandLandmarkHelper(
    private val context:         Context,
    private val cameraSelector:  CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    private val onResult:        (List<PointF?>) -> Unit,
    private val onError:         (String) -> Unit
) : AutoCloseable {

    private var landmarker:   HandLandmarker? = null
    private val cameraExec    = Executors.newSingleThreadExecutor()
    private var frameCounter  = 0

    init { setupLandmarker() }

    // ── Setup ─────────────────────────────────────────────────────────────────────────────────

    private fun setupLandmarker() {
        try {
            val base = BaseOptions.builder()
                .setModelAssetPath(MODEL_FILE)
                .build()

            val opts = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ -> dispatchResult(result) }
                .setErrorListener  { err -> onError(err.message ?: "MediaPipe error") }
                .build()

            landmarker = HandLandmarker.createFromOptions(context, opts)
        } catch (e: Exception) {
            Log.e(TAG, "Landmarker init failed", e)
            onError("Hand landmark model init failed: ${e.message}")
        }
    }

    // ── Camera binding ────────────────────────────────────────────────────────────────────────

    /**
     * HUD mode — camera runs silently in the background for gesture detection only.
     * No [PreviewView] is used; the camera feed is never rendered on the HUD display.
     * This is the correct mode for Rokid AI glasses where black = transparent on the OLED HUD.
     */
    fun bindAnalysisOnly(owner: LifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(cameraExec) { proxy -> processFrame(proxy) } }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(owner, cameraSelector, analysis)
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                onError("Camera error: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Preview mode — binds both a [PreviewView] and the analysis pipeline.
     * Use this on a phone/tablet for development and calibration.
     */
    fun bindCamera(owner: LifecycleOwner, previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()

            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(cameraExec) { proxy -> processFrame(proxy) } }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(owner, cameraSelector, preview, analysis)
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                onError("Camera error: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // ── Frame processing ──────────────────────────────────────────────────────────────────────

    private fun processFrame(proxy: ImageProxy) {
        // Process every 2nd frame → ~15 classifications/sec at 30 fps
        frameCounter++
        if (frameCounter % 2 != 0) { proxy.close(); return }

        val bmp = proxy.toBitmap()
        val ts  = proxy.imageInfo.timestamp / 1_000_000L   // nanoseconds → milliseconds
        proxy.close()

        try {
            landmarker?.detectAsync(BitmapImageBuilder(bmp).build(), ts)
        } catch (e: Exception) {
            Log.e(TAG, "detectAsync failed", e)
        }
    }

    private fun dispatchResult(result: HandLandmarkerResult) {
        if (result.landmarks().isEmpty()) {
            onResult(emptyList())
            return
        }
        // First hand only; MediaPipe always returns exactly 21 landmarks
        val raw = result.landmarks()[0]
        val pts = List(21) { i -> raw.getOrNull(i)?.let { PointF(it.x(), it.y()) } }
        onResult(pts)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────────────────────

    override fun close() {
        landmarker?.close()
        cameraExec.shutdown()
    }
}
