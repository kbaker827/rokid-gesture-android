# Rokid Gesture — Glasses Camera Edition (Android)

Navigate your glasses menu using hand gestures detected by **the glasses' own camera** — no iPhone required.

Runs directly on **Rokid Station** or any Android-powered AR device. Uses [MediaPipe HandLandmarker](https://ai.google.dev/edge/mediapipe/solutions/vision/hand_landmarker) to detect a 21-joint hand skeleton from the live camera feed and classifies poses into navigation actions.

```
Glasses Camera (CameraX)
    ↓
MediaPipe HandLandmarker (21-joint hand skeleton)
    ↓
GestureClassifier → GestureType → NavAction
    ↓  (also: wrist movement history → SwipeGesture)
GestureViewModel
    ↓
Menu overlay rendered directly on the glasses display
  ▶ Home
    Notifications
    Apps
    Settings
```

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

Every mapping is fully customisable in **Settings → Gesture → Action Mapping**.

---

## Quick Start

### 1. Download the MediaPipe model

Before building, download `hand_landmarker.task` and place it in `app/src/main/assets/`:

```bash
curl -Lo app/src/main/assets/hand_landmarker.task \
  "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"
```

### 2. Build & deploy

```bash
# Open in Android Studio, or:
./gradlew installDebug
```

Grant **Camera** permission when prompted.

### 3. Gestures tab

Tap **▶** to start detection. Hold your hand 30–60 cm from the camera.  
The 21-joint skeleton is drawn in real time. The detected gesture appears as a large emoji.  
Use **📷 World / Front** toggle to switch between the forward-facing (world) and inward-facing camera.

### 4. Menu tab

- Default 8-item menu pre-loaded (Home, Notifications, Apps, Settings…)
- Tap any row to jump the cursor
- Swipe left on a row to delete; tap ✏️ to edit
- Quick action bar at the bottom for manual navigation

---

## How the Classifier Works

MediaPipe's `HandLandmarker` returns 21 normalized landmarks per hand (x, y in [0, 1], origin top-left).

**Static gestures** (single frame):
- **Finger extended** = `dist(tip, wrist) / dist(MCP, wrist) > 1.35`
- **Thumb extended** = `dist(thumbTip, wrist) / dist(thumbCMC, wrist) > 1.15`
- **Thumbs up/down** direction = `thumbTip.y < thumbMCP.y` (MediaPipe y increases downward)

**Dynamic gestures** (wrist movement over 0.5 s):
- Last 500 ms of wrist positions tracked
- If `max(|Δx|, |Δy|) > swipeThreshold` (default 0.18), fire swipe on dominant axis
- History cleared after swipe fires to prevent repeat triggers

**Cooldown** (default 1.0 s): prevents a held gesture from firing repeatedly.

---

## Differences from the iOS Version

| Feature | iOS (`rokid-gesture-ios`) | Android (this app) |
|---------|--------------------------|-------------------|
| Camera | iPhone front/back camera | Glasses world/front camera |
| Detection | Apple Vision `VNDetectHumanHandPoseRequest` | MediaPipe `HandLandmarker` |
| Transport | TCP :8104 → glasses | Direct on-device overlay |
| Platform | SwiftUI + iOS 17 | Jetpack Compose + Android API 26 |
| Coordinate origin | Bottom-left (y-up) | Top-left (y-down) |
| iPhone required | Yes | No |

Both use identical geometry-based classification and the same 6 gesture + 4 swipe model.

---

## Tips for Best Results

- **Camera** — use the world-facing (back) camera; it points where you look
- **Distance** — 30–60 cm from the glasses
- **Lighting** — well-lit environment improves landmark confidence
- **Steady wrist** — hold still between gestures; swipe is a quick wrist flick
- **Confidence** — landmarks below 0.5 are ignored automatically
- **Cooldown** — adjust in Settings if gestures fire too fast or too slow

---

## Requirements

| Component | Requirement |
|-----------|-------------|
| Device | Rokid Station or any Android device running API 26+ (Android 8.0+) |
| Android Studio | Hedgehog (2023.1.1)+ / Gradle 8.9+ |
| Camera permission | Required |
| MediaPipe model | `hand_landmarker.task` in `app/src/main/assets/` (not included — see Quick Start) |

---

## Project Structure

```
rokid-gesture-android/
└── app/src/main/
    ├── AndroidManifest.xml
    ├── assets/
    │   └── hand_landmarker.task          ← download separately (see Quick Start)
    └── java/com/rokid/gesture/
        ├── GestureApp.kt                 ← Application subclass
        ├── MainActivity.kt               ← entry point + bottom-nav scaffold
        ├── data/
        │   └── GestureModels.kt          ← LM indices, GestureType, NavAction, AppMenu
        ├── vision/
        │   ├── GestureClassifier.kt      ← 21-joint geometry → GestureType
        │   └── HandLandmarkHelper.kt     ← CameraX + MediaPipe HandLandmarker
        ├── viewmodel/
        │   └── GestureViewModel.kt       ← wrist history, cooldown, menu navigation
        └── ui/
            ├── CameraScreen.kt           ← live preview + skeleton overlay + gesture badge
            ├── MenuScreen.kt             ← add/edit/reorder menu items
            └── SettingsScreen.kt         ← gesture→action mapping, cooldown, swipe sensitivity
```

---

## Part of the Rokid iOS/Android Bridge Suite

| App | Platform | Source | Port |
|-----|----------|--------|------|
| [rokid-claude-ios](https://github.com/kbaker827/rokid-claude-ios) | iOS | Claude AI | :8095 |
| [rokid-chatgpt-ios](https://github.com/kbaker827/rokid-chatgpt-ios) | iOS | ChatGPT | :8096 |
| [rokid-lansweeper-ios](https://github.com/kbaker827/rokid-lansweeper-ios) | iOS | Lansweeper | :8097 |
| [rokid-teams-ios](https://github.com/kbaker827/rokid-teams-ios) | iOS | MS Teams | :8098 |
| [rokid-outlook-ios](https://github.com/kbaker827/rokid-outlook-ios) | iOS | Outlook | :8099 |
| [rokid-compass-ios](https://github.com/kbaker827/rokid-compass-ios) | iOS | Compass | :8100 |
| [rokid-powershell-ios](https://github.com/kbaker827/rokid-powershell-ios) | iOS | PowerShell + AI | :8101/:8102 |
| [rokid-govee-ios](https://github.com/kbaker827/rokid-govee-ios) | iOS | Govee Lights | :8103 |
| [rokid-gesture-ios](https://github.com/kbaker827/rokid-gesture-ios) | iOS | Hand Gestures (iPhone camera) | :8104 |
| **rokid-gesture-android** | **Android** | **Hand Gestures (glasses camera)** | **on-device** |
