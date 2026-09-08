LEKHO APP — STEP 1

This project wraps the existing Lekho v37 HTML app in an Android app shell.

Already included:
- applicationId: com.lekho.app
- targetSdk/compileSdk: 36
- Firebase google-services.json from the existing Lekho project
- Firebase Auth + Firestore dependencies
- Android INTERNET + RECORD_AUDIO permissions
- HTTPS-like WebViewAssetLoader origin for the bundled app
- Existing Lekho v37 HTML copied as app/src/main/assets/index.html

NEXT STEP:
1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Build/run on an Android phone.
4. Confirm the existing v37 UI opens and microphone permission can be requested.
5. Then integrate Firebase Phone OTP and move app data from localStorage to Firestore.

IMPORTANT:
The current HTML still uses localStorage/password login. Firebase OTP is NOT claimed to be integrated yet.
