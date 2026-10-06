# WhatsApp Profile Hider

Android Accessibility app that hides WhatsApp profile pictures without modifying WhatsApp.

## Improved Redmi Note 14 detection — always-on mode

The current version is tuned for modern Android/MIUI/HyperOS displays and:
- Uses density-independent dp measurements instead of fixed pixel sizes.
- Checks ImageView/ImageButton semantics and resource/content-description names when available.
- Supports both left-to-right and right-to-left (Hebrew) WhatsApp layouts.
- Uses position and size scoring to distinguish avatars from toolbar icons and message images.
- Covers both the chat-list avatar area and the conversation header avatar.
- Uses circular masks rather than square rectangles.
- Debounces WhatsApp UI updates to reduce flicker.
- Reuses the overlay when the layout has not changed.

## Build

Open this folder in Android Studio and build `app`.

The APK can then be installed on the Redmi Note 14. Android Settings > Accessibility > Installed apps > WhatsApp Profile Hider must be enabled.

## Important limitation

WhatsApp changes its internal accessibility tree between versions. No third-party accessibility overlay can guarantee 100% detection across every WhatsApp release. The app is configured as an always-on hider whenever its accessibility service is enabled. If a particular screen is missed, the detection rules can be adjusted for that WhatsApp version.
