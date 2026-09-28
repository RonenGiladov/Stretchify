# Stretchify

![Stretchify wordmark](assets/branding/stretchify-wordmark.png)

Stretchify is a native Android stretching app built with Kotlin and Jetpack
Compose. It provides guided routines, timed stretch and rest phases, progress
tracking, reminders, a home-screen widget, and a selection of visual themes.

## Requirements

- Android Studio with Android SDK 35
- JDK 17
- Android 8.0 (API 26) or newer for the app
- Android 13 (API 33) or newer for the animated liquid shader; older versions
  use a gradient fallback

## Build

Clone the repository, open it in Android Studio, and allow Gradle to synchronize.
To build from the command line on Windows:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```sh
./gradlew assembleDebug
```

## Test

Run local unit tests with:

```powershell
.\gradlew.bat testDebugUnitTest
```

Instrumentation and Compose UI tests require an emulator or connected Android
device:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## License and acknowledgements

Stretchify is available under the [MIT License](LICENSE). Bundled and adapted
third-party work remains subject to its original license. See
[Third-Party Notices](THIRD_PARTY_NOTICES.md) for the liquid shader, Nunito,
and Haze acknowledgements.
