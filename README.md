# Pivot

Orientation control for Android. Lock or force any screen direction system wide, choose exactly which directions are allowed, and switch modes from a notification or a quick settings tile.

## Stack (checked November 2026)

Kotlin 2.4.0, Android Gradle Plugin 9.2.1 with built-in Kotlin, Gradle 9.4.1, Jetpack Compose BOM 2026.09.00, compileSdk 37, targetSdk 36, minSdk 26, JDK 21 on CI.

## Build the APK on GitHub

1. Create a new GitHub repository and push this folder to the `main` branch.
2. Open the Actions tab and run Build APK, or just push. The APK appears as the artifact named Pivot-apk.
3. For a tagged release, push a tag such as `v1.0.0`. The APK is attached to the release.

The APK is signed with the debug key unless you add these repository secrets: `PIVOT_KEYSTORE_B64` (base64 of your .jks), `PIVOT_KEYSTORE_PASSWORD`, `PIVOT_KEY_ALIAS`, `PIVOT_KEY_PASSWORD`.

## First run on the phone

1. Install the APK.
2. Open Pivot and allow Display over other apps. If the switch is greyed out, open App info, tap the three dot menu and choose Allow restricted settings. Android applies this to apps installed outside the Play Store.
3. Allow notifications.
4. Pick a mode. Add the Pivot tile from the quick settings edit screen if you want a one tap cycle.

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
  service/        foreground service, controller, boot receiver, quick settings tile
  notification/   channel and notification builder
  data/           synchronous preference store
  ui/             shadcn style charcoal design system and the home screen
```
