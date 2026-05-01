# Rokid Gesture HUD (Android)

Navigate menus on your **Rokid AI glasses** using hand gestures detected by the glasses' own camera — no iPhone, no external device.

Built for the Rokid AI glasses' **transparent OLED HUD display**: black pixels are off (transparent), so every coloured element renders as a floating AR overlay in your field of view. The camera feed is never shown on the HUD; the real world remains visible through the lenses.

```
Glasses Camera (CameraX — analysis only, no preview rendered)
    ↓
MediaPipe HandLandmarker (21-joint hand skeleton, ~15 fps)
    ↓
GestureClassifier → GestureType → NavAction
    ↓  (also: wrist movement history → SwipeGesture)
GestureViewModel
    ↓
HUD overlay — floating AR text on transparent OLED display
  ▶ Home           ✌️  ← Previous Item
    Notifications
    Apps
    Settings
```

---

## How the HUD Display Works

On Rokid AI glasses the display is an OLED micro-display:

| Pixel colour | What you see |
|---|---|
| **Black** `#000000` | Nothing — the real world shows through |
| **White / Gold / Cyan** | Floating text / shape in AR |
| **Semi-transparent panel** `#BB000000` | Slight dim + floating panel |

The app uses a fully black window background (`windowIsTranslucent = true`). Only the menu text, gesture badges, and skeleton overlay are coloured — everything else is invisible.

---

## Gestures

| Gesture | Default Action | Notes |
|---------|---------------|-------|
| ✊ **Fist** | Back / Cancel | All fingers curled |
| 🖐 **Open Palm** | Select / Confirm | All 5 extended |
| ☝️ **Point** | Next Item | Index finger only |
| ✌️ **Peace Sign** | Previous Item | Index + middle |
| 👍 **Thumbs Up** | Scroll to First | Thumb up, others curled |
| 👎 **Thumbs Down** | Scroll to Last | Thumb down, others curled |
| → **Swipe Right** | Next Item | Wrist moves right |
| ← **Swipe Left** | Previous Item | Wrist moves left |
| ↑ **Swipe Up** | Scroll to First | Wrist moves up |
| ↓ **Swipe Down** | Scroll to Last | Wrist moves down |

Every mapping is customisable in **Settings → Gesture → Action Mapping**.

---

## HUD Display Formats

Switch in **Settings → Glasses Display Format**:

| Format | What appears on the HUD |
|--------|------------------------|
| **Full** | All 8 items with `▶` cursor, selected in gold |
| **Compact** | `[1/8] Home` — single line, large text |
| **Minimal** | `Home` — just the selected item, maximum size |

---

## Quick Start

### 1. Download the MediaPipe model

```bash
curl -Lo app/src/main/assets/hand_landmarker.task \
  "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"
```

### 2. Build & sideload onto the glasses

```bash
# Connect glasses via USB, enable ADB
adb devices                        # confirm glasses appear
./gradlew installDebug             # build + deploy
```

### 3. Use the HUD

- Tap **▶** (top-right) to start detection
- A pulsing green dot confirms detection is active
- Hold your hand 30–60 cm from the forward-facing camera
- The 21-joint skeleton appears in the **bottom-right corner** (tap "Hide Skeleton" to remove it)
- The detected gesture emoji appears on the **right side**
- The fired action label (`✌️ Peace Sign → Previous Item`) appears **bottom-left**
- Navigate to **Menu** or **Settings** tabs to configure

---

## How the Classifier Works

MediaPipe `HandLandmarker` returns 21 normalized landmarks (x, y ∈ [0, 1], origin top-left).

**Static gestures** (single frame):
- **Finger extended** = `dist(tip, wrist) / dist(MCP, wrist) > 1.35`
- **Thumb extended** = `dist(thumbTip, wrist) / dist(thumbCMC, wrist) > 1.15`
- **Thumbs up/down** = `thumbTip.y < thumbMCP.y` (MediaPipe y increases downward)

**Dynamic gestures** (wrist movement over 0.5 s):
- Track last 500 ms of wrist positions
- If `max(|Δx|, |Δy|) > swipeThreshold` (default 0.18), fire on dominant axis
- History cleared after swipe to prevent repeats

**Cooldown** (default 1.0 s): prevents a held gesture from firing repeatedly. Adjustable 0.3 – 3.0 s in Settings.

---

## Requirements

| Component | Requirement |
|-----------|-------------|
| Device | Rokid AI glasses (Android-based, API 26+) |
| Android Studio | Hedgehog 2023.1.1+ / Gradle 8.9 |
| Camera permission | Required |
| MediaPipe model | `hand_landmarker.task` in `app/src/main/assets/` — see Quick Start |

---

## Project Structure

```
rokid-gesture-android/
└── app/src/main/
    ├── AndroidManifest.xml              ← landscape, fullscreen, camera permission
    ├── assets/
    │   └── hand_landmarker.task         ← download separately (see Quick Start)
    ├── res/values/themes.xml            ← windowIsTranslucent + black bg for HUD
    └── java/com/rokid/gesture/
        ├── GestureApp.kt
        ├── MainActivity.kt              ← immersive mode, landscape, screen-on
        ├── data/
        │   └── GestureModels.kt         ← LM indices, GestureType, NavAction, AppMenu
        ├── vision/
        │   ├── GestureClassifier.kt     ← 21-joint geometry → GestureType
        │   └── HandLandmarkHelper.kt    ← CameraX analysis-only (no preview on HUD)
        ├── viewmodel/
        │   └── GestureViewModel.kt      ← wrist history, cooldown, menu navigation
        └── ui/
            ├── HudScreen.kt             ← transparent AR overlay: menu + gesture + skeleton
            ├── MenuScreen.kt            ← add/edit/reorder menu items
            └── SettingsScreen.kt        ← gesture→action mapping, cooldown, swipe sensitivity
```

---

## Key Difference from `rokid-gesture-ios`

| | [rokid-gesture-ios](https://github.com/kbaker827/rokid-gesture-ios) | rokid-gesture-android (this) |
|---|---|---|
| Platform | iPhone (iOS 17) | Rokid AI glasses (Android) |
| Camera | iPhone camera | **Glasses' own camera** |
| Detection | Apple Vision framework | MediaPipe HandLandmarker |
| Display | Menu sent over TCP :8104 | **Direct AR overlay on HUD** |
| iPhone required | Yes | **No — fully standalone** |
| Camera preview | Shown on iPhone screen | **Never shown — real world visible** |

---

## Part of the Rokid Suite

| App | Platform | Source | Port |
|-----|----------|--------|------|
| [rokid-claude-ios](https://github.com/kbaker827/rokid-claude-ios) | iOS | Claude AI | :8095 |
| [rokid-chatgpt-ios](https://github.com/kbaker827/rokid-chatgpt-ios) | iOS | ChatGPT | :8096 |
| [rokid-lansweeper-ios](https://github.com/kbaker827/rokid-lansweeper-ios) | iOS | Lansweeper | :8097 |
| [rokid-teams-ios](https://github.com/kbaker827/rokid-teams-ios) | iOS | MS Teams | :8098 |
| [rokid-outlook-ios](https://github.com/kbaker827/rokid-outlook-ios) | iOS | Outlook | :8099 |
| [rokid-compass-ios](https://github.com/kbaker827/rokid-compass-ios) | iOS | Compass | :8100 |
| [rokid-powershell-ios](https://github.com/kbaker827/rokid-powershell-ios) | iOS | PowerShell + AI Voice | :8101/:8102 |
| [rokid-govee-ios](https://github.com/kbaker827/rokid-govee-ios) | iOS | Govee Lights | :8103 |
| [rokid-gesture-ios](https://github.com/kbaker827/rokid-gesture-ios) | iOS | Gestures (iPhone camera) | :8104 |
| **rokid-gesture-android** | **Android / Rokid AI glasses** | **Gestures (glasses camera + HUD)** | **on-device** |
