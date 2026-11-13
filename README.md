# Pivot

Orientation control for Android. Lock or force any screen direction system wide, choose exactly which directions are allowed, and switch modes from a notification or a quick settings tile.

## Stack (checked November 2026)

Kotlin 2.4.0, Android Gradle Plugin 9.2.1 with built-in Kotlin, Gradle 9.4.1, Jetpack Compose BOM 2026.09.00 with Material 3, compileSdk 37, targetSdk 36, minSdk 26, JDK 21 on CI.

## Build the APK on GitHub

1. Create a new GitHub repository and push this folder to the `main` branch.
2. Open the Actions tab and run Build APK, or just push. The APK appears as the artifact named Pivot-apk.
3. For a tagged release, push a tag such as `v1.0.0`. The APK is attached to the release.

The APK is signed with the debug key unless you add these repository secrets: `PIVOT_KEYSTORE_B64` (base64 of your .jks), `PIVOT_KEYSTORE_PASSWORD`, `PIVOT_KEY_ALIAS`, `PIVOT_KEY_PASSWORD`.

## First run on the phone

1. Install the APK.
2. Open Pivot and allow Display over other apps. If the switch is greyed out, open App info, tap the three dot menu and choose Allow restricted settings. Android applies this to apps installed outside the Play Store.
3. Open the Settings tab and work through Keep Pivot running: notifications, background use, and your phone brand's autostart screen. This is what keeps Pivot alive after you close the app.
4. Pick a mode on Home. Add the Pivot tile from the Shortcuts tab (Android 13 and newer) or from the quick settings edit screen.

## Staying alive after you close the app

Pivot holds the screen direction with a foreground service, so it must survive the app being closed. What the app does about that:

| Problem | What Pivot does |
| --- | --- |
| Android 14 and newer let you swipe away a foreground notification, which looks like Pivot stopped | The notification is put back while Show controls in notification is on |
| Notification buttons could fail when the process had been stopped | Buttons start the service with a foreground service PendingIntent |
| Android 12 and newer refuse foreground service starts from the background | The refusal is caught. Opening Pivot, or tapping the tile, starts the service again |
| Stop from the Active apps list kills the whole process with no callback | Detected on next launch from the process exit history and shown on Home |
| Phone makers stop background apps | Settings shows the battery optimization exemption and opens the maker's autostart screen when it is known |
| Tile showed an old mode after a change from the app or notification | The service refreshes the tile whenever the stored state changes |

Some phones (Xiaomi, Oppo, Realme, Vivo, OnePlus, Huawei, Samsung) stop apps more aggressively than Android requires. The autostart screens are maker specific and are not part of Android, so they may not open on every version. App info is the fallback.

If Pivot still stops, check Settings, Keep Pivot running, for the item that is not green.

## How it works

Orientation is forced with one invisible 1 pixel overlay window whose `screenOrientation` attribute the window manager obeys. Changing mode updates that single attribute.

| Mode | Realised as |
| --- | --- |
| Portrait, Landscape, Reverse portrait, Reverse landscape | fixed value |
| Portrait flip, Landscape flip, All four | sensor values built into Android |
| Freeze | locked to the current rotation |
| Custom, one direction | fixed value |
| Custom, all four, or both portraits, or both landscapes | sensor values built into Android |
| Custom, any other mix such as portrait plus landscape only | screen-on-only motion sensor with a settle delay and dead zones |

## Battery

No polling, no wake locks, no network, no continuous animation. The foreground service does nothing between commands. The motion sensor is registered only for custom mixes Android cannot express, only while the screen is on, and is released the moment the screen turns off.

## Notification

Android allows three action buttons, so you choose which three modes appear. The body shows the live mode and opens the app. The quick settings tile cycles through the same three.

## Layout

```
app/src/main/java/app/pivot/orientation
  model/          Side, Mode and the planner that picks the cheapest realisation
  engine/         overlay window and the screen-on-only sensor
  service/        foreground service, controller, boot and notification dismissed receivers, quick settings tile
  notification/   channel and notification builder
  data/           synchronous preference store
  health/         checks for what keeps Pivot alive, and the buttons that fix them
  ui/             Material 3 app: Home, Shortcuts and Settings tabs, light and dark, Material You colors
```
