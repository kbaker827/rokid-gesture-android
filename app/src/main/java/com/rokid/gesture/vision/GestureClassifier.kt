package com.rokid.gesture.vision

import android.graphics.PointF
import com.rokid.gesture.data.GestureType
import com.rokid.gesture.data.LM
import kotlin.math.sqrt

/**
 * Pure geometry classifier — same ratio-based logic as the iOS GestureClassifier.
 *
 * MediaPipe coordinate space: x=0 left → 1 right, y=0 top → 1 bottom.
 * (Opposite y-direction from Apple Vision, so thumbs-up check is inverted.)
 */
class GestureClassifier {

    fun classify(pts: List<PointF?>): GestureType {
        val wrist = pts.getOrNull(LM.WRIST) ?: return GestureType.NONE

        val indexExt  = extended(pts.getOrNull(LM.INDEX_TIP),  pts.getOrNull(LM.INDEX_MCP),  wrist)
        val middleExt = extended(pts.getOrNull(LM.MIDDLE_TIP), pts.getOrNull(LM.MIDDLE_MCP), wrist)
        val ringExt   = extended(pts.getOrNull(LM.RING_TIP),   pts.getOrNull(LM.RING_MCP),   wrist)
        val pinkyExt  = extended(pts.getOrNull(LM.PINKY_TIP),  pts.getOrNull(LM.PINKY_MCP),  wrist)
        val thumbExt  = thumbExtended(pts, wrist)

        return when {
            !indexExt && !middleExt && !ringExt && !pinkyExt && !thumbExt -> GestureType.FIST
            indexExt && middleExt && ringExt && pinkyExt                  -> GestureType.OPEN_PALM
            indexExt && !middleExt && !ringExt && !pinkyExt               -> GestureType.POINT_ONE
            indexExt && middleExt && !ringExt && !pinkyExt                -> GestureType.PEACE_SIGN
            thumbExt && !indexExt && !middleExt && !ringExt && !pinkyExt  -> {
                val tip = pts.getOrNull(LM.THUMB_TIP)
                val mp  = pts.getOrNull(LM.THUMB_MCP)
                // MediaPipe y increases downward: tip.y < mp.y → thumb pointing UP
                if (tip != null && mp != null && tip.y < mp.y) GestureType.THUMBS_UP
                else GestureType.THUMBS_DOWN
            }
            else -> GestureType.NONE
        }
    }

    // dist(tip, wrist) / dist(MCP, wrist) > 1.35 → finger extended
    private fun extended(tip: PointF?, mcp: PointF?, wrist: PointF): Boolean {
        tip ?: return false; mcp ?: return false
        val dMcp = dist(mcp, wrist)
        return dMcp > 0f && dist(tip, wrist) / dMcp > 1.35f
    }

    // dist(thumbTip, wrist) / dist(thumbCMC, wrist) > 1.15 → thumb extended
    private fun thumbExtended(pts: List<PointF?>, wrist: PointF): Boolean {
        val tip = pts.getOrNull(LM.THUMB_TIP) ?: return false
        val cmc = pts.getOrNull(LM.THUMB_CMC) ?: return false
        val dCmc = dist(cmc, wrist)
        return dCmc > 0f && dist(tip, wrist) / dCmc > 1.15f
    }

    private fun dist(a: PointF, b: PointF): Float {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
