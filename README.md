# Glass Home 2.0

A native Android launcher for Android 13+ with a real refractive dock powered by [QmDeve/AndroidLiquidGlassView](https://github.com/QmDeve/AndroidLiquidGlassView).

## Features
- Real Liquid Glass sampling from the launcher content behind the dock, using QmDeve's AGSL/RenderEffect pipeline.
- Swipe up on the home screen to open the app drawer.
- Searchable installed-app drawer with native app icons.
- Seven built-in gradient wallpapers and gallery photo selection.
- Persistent wallpaper, app-drawer column count (4/5), and compact-icon preference.
- SF-inspired typography using Android's native sans-serif family; no proprietary Apple font files are redistributed.
- Floating dock with Phone, Messages, Browser, Camera shortcuts and app-drawer control.

## Build
- Android Studio, JDK 17, Android SDK Platform 35.
- Open this repository and build the `app` debug variant.
- Minimum SDK 33 (Android 13), required for the full liquid-glass rendering pipeline.
- The QmDeve library source is vendored under `app/src/main/java/com/qmdeve/liquidglass` and its AGSL shader is under `app/src/main/res/raw`. This avoids requiring the upstream AAR's Android API 37 compile SDK metadata; the engine uses Android 13+ rendering APIs.
- Set Glass Home as the default Home app in Settings → Apps → Choose default apps → Home app.

## How the glass works
The dock is a real `com.qmdeve.liquidglass.widget.LiquidGlassView`. It binds to the launcher content container behind it and uses the upstream AGSL shader with Android RenderEffect/RenderNode to refract and disperse the sampled background. The earlier hand-written approximation has been removed.

## Customization
Tap the sparkle icon in the top-right corner to select a built-in gradient or a photo from the gallery. The same panel changes app-drawer columns and icon density.

## Limitations
- Android launcher widgets, notification badges, folders, and drag-to-rearrange shortcuts are not implemented yet.
- The gallery image is used as the home wallpaper and the Liquid Glass view samples the rendered launcher content behind the dock.
