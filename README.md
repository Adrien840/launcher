# Glass Home 2.0

A native Android launcher for Android 13+ with a real refraction/dispersion dock powered by [AndroidLiquidGlassView](https://github.com/QmDeve/AndroidLiquidGlassView).

## Features
- Real-time Liquid Glass sampling from the launcher content using QmDeve's LiquidGlassView (not a decorative translucent overlay).
- Up-swipe from the home screen to open the app drawer; down-swipe to return.
- Searchable installed-app drawer and native app icons.
- Seven built-in gradient wallpapers plus photo selection from the gallery.
- Persistent wallpaper selection, app-drawer column count (4/5), and compact icon preference.
- SF-inspired typography using Android's native sans-serif family; no proprietary Apple font files are redistributed.
- Glass dock with Phone, Messages, Browser, Camera shortcuts and app-drawer control.

## Build
- Android Studio, JDK 17, Android SDK Platform 35.
- Open this repository and build the `app` debug variant.
- Minimum SDK 33 (Android 13), required by the library for full Liquid Glass rendering.
- Dependency: `com.qmdeve.liquidglass:core:1.1.0` from Maven Central.
- On the S22+ (Android 13+), set Glass Home as the default Home app in Settings → Apps → Choose default apps → Home app.

## How the glass works
The dock is a real `com.qmdeve.liquidglass.widget.LiquidGlassView`. Its sampling source is the launcher content container behind it. The library applies its AGSL/RenderEffect pipeline for refractive distortion, chromatic dispersion and configurable tint/blur. The earlier hand-written shader has been removed.

## Limitations
- Android launcher shortcuts and widgets are not implemented yet.
- App drawer supports search and a 4/5-column preference.
- Photo wallpapers are read through a persisted gallery URI.
