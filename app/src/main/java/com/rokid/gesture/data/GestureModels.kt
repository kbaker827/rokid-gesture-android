package com.rokid.gesture.data

// ── MediaPipe hand-landmark indices (matches iOS Vision JointName layout) ─────────────────────
object LM {
    const val WRIST      = 0
    const val THUMB_CMC  = 1;  const val THUMB_MCP  = 2;  const val THUMB_IP   = 3;  const val THUMB_TIP  = 4
    const val INDEX_MCP  = 5;  const val INDEX_PIP  = 6;  const val INDEX_DIP  = 7;  const val INDEX_TIP  = 8
    const val MIDDLE_MCP = 9;  const val MIDDLE_PIP = 10; const val MIDDLE_DIP = 11; const val MIDDLE_TIP = 12
    const val RING_MCP   = 13; const val RING_PIP   = 14; const val RING_DIP   = 15; const val RING_TIP   = 16
    const val PINKY_MCP  = 17; const val PINKY_PIP  = 18; const val PINKY_DIP  = 19; const val PINKY_TIP  = 20
}

/** Bone chains for drawing the 21-joint hand skeleton */
val BONE_CHAINS: List<List<Int>> = listOf(
    listOf(0, 1, 2, 3, 4),          // thumb
    listOf(0, 5, 6, 7, 8),          // index
    listOf(0, 9, 10, 11, 12),       // middle
    listOf(0, 13, 14, 15, 16),      // ring
    listOf(0, 17, 18, 19, 20),      // pinky
    listOf(5, 9, 13, 17)            // knuckle bar
)

val TIP_INDICES: Set<Int> = setOf(4, 8, 12, 16, 20)

// ── Gesture enums ─────────────────────────────────────────────────────────────────────────────

enum class GestureType(val emoji: String, val label: String) {
    FIST(       "✊", "Fist"),
    OPEN_PALM(  "🖐", "Open Palm"),
    POINT_ONE(  "☝️", "Point"),
    PEACE_SIGN( "✌️", "Peace Sign"),
    THUMBS_UP(  "👍", "Thumbs Up"),
    THUMBS_DOWN("👎", "Thumbs Down"),
    NONE(       "",   "—")
}

enum class SwipeGesture(val arrow: String, val label: String) {
    LEFT("←", "Left"), RIGHT("→", "Right"), UP("↑", "Up"), DOWN("↓", "Down")
}

enum class NavAction(val label: String) {
    NEXT("Next Item"),
    PREVIOUS("Previous Item"),
    SELECT("Select / Confirm"),
    BACK("Back / Cancel"),
    SCROLL_UP("Scroll to First"),
    SCROLL_DOWN("Scroll to Last"),
    NONE("No Action")
}

// ── Customisable gesture → action mapping ─────────────────────────────────────────────────────

data class GestureMapping(
    val fist:       NavAction = NavAction.BACK,
    val openPalm:   NavAction = NavAction.SELECT,
    val pointOne:   NavAction = NavAction.NEXT,
    val peaceSign:  NavAction = NavAction.PREVIOUS,
    val thumbsUp:   NavAction = NavAction.SCROLL_UP,
    val thumbsDown: NavAction = NavAction.SCROLL_DOWN,
    val swipeLeft:  NavAction = NavAction.PREVIOUS,
    val swipeRight: NavAction = NavAction.NEXT,
    val swipeUp:    NavAction = NavAction.SCROLL_UP,
    val swipeDown:  NavAction = NavAction.SCROLL_DOWN
)

// ── Menu ─────────────────────────────────────────────────────────────────────────────────────

data class MenuItem(
    val id:       String = java.util.UUID.randomUUID().toString(),
    val title:    String,
    val iconName: String = "circle",   // informational; used by UI icons
    val subtitle: String = ""
)

enum class GlassesFormat { FULL, COMPACT, MINIMAL }

data class AppMenu(
    val items:         List<MenuItem> = DEFAULT_ITEMS,
    val selectedIndex: Int = 0
) {
    fun glassesText(format: GlassesFormat): String = when (format) {
        GlassesFormat.FULL -> items.mapIndexed { i, item ->
            if (i == selectedIndex) "▶ ${item.title}" else "  ${item.title}"
        }.joinToString("\n")
        GlassesFormat.COMPACT -> "[${selectedIndex + 1}/${items.size}] ${items.getOrNull(selectedIndex)?.title ?: ""}"
        GlassesFormat.MINIMAL -> items.getOrNull(selectedIndex)?.title ?: ""
    }

    fun moveNext()  = if (items.isEmpty()) this else copy(selectedIndex = (selectedIndex + 1) % items.size)
    fun movePrev()  = if (items.isEmpty()) this else copy(selectedIndex = (selectedIndex - 1 + items.size) % items.size)
    fun moveFirst() = copy(selectedIndex = 0)
    fun moveLast()  = if (items.isEmpty()) this else copy(selectedIndex = items.size - 1)

    companion object {
        val DEFAULT_ITEMS = listOf(
            MenuItem(title = "Home",          iconName = "home"),
            MenuItem(title = "Notifications", iconName = "notifications"),
            MenuItem(title = "Apps",          iconName = "apps"),
            MenuItem(title = "Settings",      iconName = "settings"),
            MenuItem(title = "Messages",      iconName = "message"),
            MenuItem(title = "Camera",        iconName = "camera_alt"),
            MenuItem(title = "Maps",          iconName = "map"),
            MenuItem(title = "Music",         iconName = "music_note")
        )
    }
}
