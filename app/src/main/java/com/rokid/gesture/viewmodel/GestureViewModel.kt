package com.rokid.gesture.viewmodel

import android.graphics.PointF
import androidx.lifecycle.ViewModel
import com.rokid.gesture.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

class GestureViewModel : ViewModel() {

    // ── Observable state ─────────────────────────────────────────────────────────────────────

    private val _handPoints     = MutableStateFlow<List<PointF?>>(emptyList())
    val handPoints: StateFlow<List<PointF?>> = _handPoints.asStateFlow()

    private val _currentGesture = MutableStateFlow(GestureType.NONE)
    val currentGesture: StateFlow<GestureType> = _currentGesture.asStateFlow()

    private val _lastFiredLabel = MutableStateFlow("")
    val lastFiredLabel: StateFlow<String> = _lastFiredLabel.asStateFlow()

    private val _isDetecting    = MutableStateFlow(false)
    val isDetecting: StateFlow<Boolean> = _isDetecting.asStateFlow()

    private val _menu           = MutableStateFlow(AppMenu())
    val menu: StateFlow<AppMenu> = _menu.asStateFlow()

    private val _cooldownSec    = MutableStateFlow(1.0f)
    val cooldownSec: StateFlow<Float> = _cooldownSec.asStateFlow()

    private val _swipeThreshold = MutableStateFlow(0.18f)
    val swipeThreshold: StateFlow<Float> = _swipeThreshold.asStateFlow()

    private val _mapping        = MutableStateFlow(GestureMapping())
    val mapping: StateFlow<GestureMapping> = _mapping.asStateFlow()

    private val _glassesFormat  = MutableStateFlow(GlassesFormat.FULL)
    val glassesFormat: StateFlow<GlassesFormat> = _glassesFormat.asStateFlow()

    // ── Internal ──────────────────────────────────────────────────────────────────────────────

    private val classifier  = GestureClassifier()
    private var lastFireMs  = 0L

    private data class WristSample(val pos: PointF, val time: Long)
    private val wristHistory = ArrayDeque<WristSample>()   // 0.5 s rolling window

    // ── Settings ─────────────────────────────────────────────────────────────────────────────

    fun setDetecting(v: Boolean)             { _isDetecting.update { v } }
    fun setCooldown(v: Float)                { _cooldownSec.update { v } }
    fun setSwipeThreshold(v: Float)          { _swipeThreshold.update { v } }
    fun setMapping(m: GestureMapping)        { _mapping.update { m } }
    fun setGlassesFormat(f: GlassesFormat)   { _glassesFormat.update { f } }
    fun updateMenu(fn: (AppMenu) -> AppMenu) { _menu.update(fn) }

    // ── Core processing ───────────────────────────────────────────────────────────────────────

    /**
     * Called from [HandLandmarkHelper] result callback — may arrive on any thread.
     * StateFlow.value assignments are thread-safe.
     */
    fun processHandPoints(pts: List<PointF?>) {
        _handPoints.value = pts
        if (!_isDetecting.value || pts.isEmpty()) return

        val wrist = pts.getOrNull(LM.WRIST) ?: return
        val now   = System.currentTimeMillis()

        // Maintain 0.5 s wrist history for swipe detection
        wristHistory += WristSample(wrist, now)
        wristHistory.removeAll { now - it.time > 500L }

        val gesture = classifier.classify(pts)
        _currentGesture.value = gesture

        if (detectSwipe()) return   // dynamic wins over static

        if (gesture != GestureType.NONE && cooldownOk()) {
            val action = mappedAction(gesture)
            if (action != NavAction.NONE) fire(action, "${gesture.emoji} ${gesture.label}")
        }
    }

    // ── Swipe detection ───────────────────────────────────────────────────────────────────────

    private fun detectSwipe(): Boolean {
        if (wristHistory.size < 3) return false
        val dx = wristHistory.last().pos.x - wristHistory.first().pos.x
        val dy = wristHistory.last().pos.y - wristHistory.first().pos.y
        if (maxOf(abs(dx), abs(dy)) < _swipeThreshold.value) return false
        if (!cooldownOk()) return false

        // MediaPipe y increases downward
        val dir = if (abs(dx) > abs(dy)) {
            if (dx > 0) SwipeGesture.RIGHT else SwipeGesture.LEFT
        } else {
            if (dy > 0) SwipeGesture.DOWN else SwipeGesture.UP
        }

        val m = _mapping.value
        val action = when (dir) {
            SwipeGesture.LEFT  -> m.swipeLeft
            SwipeGesture.RIGHT -> m.swipeRight
            SwipeGesture.UP    -> m.swipeUp
            SwipeGesture.DOWN  -> m.swipeDown
        }
        if (action == NavAction.NONE) return false

        fire(action, "${dir.arrow} Swipe ${dir.label}")
        wristHistory.clear()
        return true
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────────────────

    private fun mappedAction(g: GestureType): NavAction {
        val m = _mapping.value
        return when (g) {
            GestureType.FIST        -> m.fist
            GestureType.OPEN_PALM   -> m.openPalm
            GestureType.POINT_ONE   -> m.pointOne
            GestureType.PEACE_SIGN  -> m.peaceSign
            GestureType.THUMBS_UP   -> m.thumbsUp
            GestureType.THUMBS_DOWN -> m.thumbsDown
            GestureType.NONE        -> NavAction.NONE
        }
    }

    private fun cooldownOk(): Boolean =
        System.currentTimeMillis() - lastFireMs > (_cooldownSec.value * 1000).toLong()

    private fun fire(action: NavAction, label: String) {
        lastFireMs          = System.currentTimeMillis()
        _lastFiredLabel.value = label
        applyAction(action)
    }

    fun applyAction(action: NavAction) {
        _menu.update { menu ->
            when (action) {
                NavAction.NEXT        -> menu.moveNext()
                NavAction.PREVIOUS    -> menu.movePrev()
                NavAction.SCROLL_UP   -> menu.moveFirst()
                NavAction.SCROLL_DOWN -> menu.moveLast()
                NavAction.SELECT,
                NavAction.BACK,
                NavAction.NONE        -> menu     // SELECT/BACK handled as UI events
            }
        }
    }
}
