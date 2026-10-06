# WhatsApp Profile Hider — Cloud APK Build

This project includes a GitHub Actions workflow that builds the Android APK in GitHub's cloud.

## How to build the APK

1. Create/sign in to a GitHub account.
2. Create a new **private** repository.
3. Upload the contents of this ZIP to the repository (not the ZIP file itself).
4. Open the **Actions** tab.
5. Select **Build Android APK**.
6. Click **Run workflow**.
7. When it finishes, open the workflow run and download the artifact named **WhatsAppProfileHider-debug-apk**.
8. Extract the downloaded artifact and install `app-debug.apk` on the Android phone.

The workflow uses Java 17 and Gradle 8.9 and runs `assembleDebug`.
