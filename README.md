# Glass Home — native Android launcher

Glass Home is a native Kotlin + Jetpack Compose Android HOME app. It discovers installed launcher activities and uses each app's real launcher icon. The interface has an iOS-inspired four-icon dock, a searchable app drawer, and a Liquid Glass-inspired refractive dock.

## Build on Android Studio

1. Install Android Studio with Android SDK Platform 35 and Build Tools 35.0.0.
2. Open this project directory, named GlassHomeLauncher.
3. Use JDK 17 and allow Gradle sync to complete. The project uses Android Gradle Plugin 8.7.3, Kotlin 2.0.21 and Compose BOM 2024.12.01.
4. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Install the generated debug APK on the phone.
6. Open **Settings → Apps → Choose default apps → Home app** and select **Glass Home**.

No root access is required. The launcher targets Android 15 (API 35) and supports Android 9 (API 28) or newer.

## What is implemented

- Native Android HOME activity, selectable as the device's default launcher.
- Installed app discovery through PackageManager, with real launcher icons and labels.
- Suggested dock apps matching Phone, Messages, Browser/Chrome and Camera when available.
- iOS-inspired floating dock and a searchable 4-column app drawer.
- Device wallpaper displayed as the home background when Android exposes a static wallpaper bitmap.
- Android 13+ AGSL RuntimeShader that samples the wallpaper texture under the dock, distorts the rim, separates RGB samples for a restrained prism fringe, and overlays a glass tint/highlight.
- A translucent Compose fallback on Android versions without AGSL or when wallpaper sampling is unavailable.
- GitHub Actions workflow to build the debug APK on pushes and pull requests.

## Liquid Glass source adaptation

Optical reference: https://github.com/deepika-builds/liquid-glass (MIT; see THIRD_PARTY_NOTICES.md). Its browser implementation generates a two-axis displacement map: red carries horizontal displacement, blue carries vertical displacement, and a blurred neutral-gray inset suppresses distortion through the centre. It then uses three channel-isolated SVG displacement passes with slightly different scales to create a subtle RGB fringe.

The web project depends on SVG feDisplacementMap and CSS backdrop-filter, which are not a native Android rendering path. Glass Home translates the same optical structure into AGSL: an analytic neutral-centre/rim displacement field samples the displayed wallpaper through an Android RuntimeShader, with staggered red/blue sampling. This is a native adaptation of the algorithmic approach, not a port of the JavaScript/SVG implementation.

The current refraction samples the launcher wallpaper image. It does not capture arbitrary live content or a live-wallpaper animation behind the dock. Devices below API 33 use the translucent glass fallback.

## Project structure

- .github/workflows/android-build.yml
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/example/glasshome/LauncherActivity.kt
- app/build.gradle.kts
- build.gradle.kts
- settings.gradle.kts
- gradle.properties
- THIRD_PARTY_NOTICES.md
- README.md

## Limitations

This is a focused launcher foundation rather than a One UI replacement. Home-screen widgets, notification badges, icon packs, folders, per-icon editing, live wallpaper capture, and advanced gesture customization are not included yet. QUERY_ALL_PACKAGES is declared because a launcher needs to enumerate installed apps; Google Play distribution requires compliance with its package-visibility policy.
