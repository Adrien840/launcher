# Glass Home

A native Android launcher for Android 13+ that brings a One UI-inspired layout together with refractive Liquid Glass dock rendering.

## Current experience
- One UI-inspired home screen with a large clock/date, glance cards, search, app pages and a floating glass dock.
- Swipe up to open the searchable app drawer; swipe horizontally to move between home pages.
- Uses the actual installed Android app icons and labels.
- Personalization menu with Home screen, Wallpaper, Apps and About sections.
- Seven built-in gradient wallpapers and a persistent gallery-photo wallpaper option.
- App grid density and compact icon controls.
- Dock rendered with the vendored QmDeve AndroidLiquidGlassView engine and AGSL shader.

## Build
- Android Studio, JDK 17, Android SDK Platform 35.
- Open this repository and build the `app` debug variant.
- Minimum SDK 33 (Android 13).
- QmDeve's LiquidGlassView source is vendored under `app/src/main/java/com/qmdeve/liquidglass`; its AGSL shader is under `app/src/main/res/raw`.
- GitHub Actions builds the debug APK on each push and publishes it as the `glass-home-debug-apk` workflow artifact.

## Important boundaries
This is a third-party launcher, not a replacement for Samsung's One UI system framework. Android continues to own the notification shade, Quick Settings, lock screen, Recents screen and system Settings. The launcher can provide its own home screen, app drawer, dock and personalization menus, but it cannot faithfully replace those privileged system surfaces without Samsung/Android system-level integration.

Widgets, folders, notification badges, drag-to-rearrange icons and full One UI launcher parity are not implemented yet.
