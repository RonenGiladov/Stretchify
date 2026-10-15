# Stretchify showcase

Public website: https://ronengiladov.github.io/Stretchify/

The static website lives in `docs/` and is published by GitHub Pages from the
`main` branch's `/docs` directory. It uses HTML, CSS, JavaScript, the bundled
Nunito font, and real Android app captures. There is no website build step,
package installation, analytics, or sign-in requirement.

## Preview locally

Serve `docs/` with a static HTTP server, for example:

```powershell
python -m http.server 8765 --directory docs
```

Open `http://localhost:8765`. Using HTTP also allows the browser to load the
video caption track. The website supports deployment under the repository
subdirectory, so keep asset paths relative.

## Enable the interactive demo later

Appetize setup is intentionally deferred. The website currently presents the
video, screenshots, and source links. No Appetize request is made on page load.

1. Build the app with JDK 17 using `.\gradlew.bat :app:assembleDebug`.
2. Upload `app/build/outputs/apk/debug/app-debug.apk` to your free Appetize account.
3. Copy the build ID from its public `https://appetize.io/app/BUILD_ID` sharing link.
   Do not use an account-restricted `/apps/android/...` link or an API token.
4. Set `window.stretchifyDemoBuildId` in `docs/demo-config.js` to that build ID.
5. Commit and push the change. GitHub Pages will publish it automatically.
6. Check the page in a signed-out browser. The “Try in browser” action should
   open an emulator, and “Open demo separately” should require no sign-in.

The emulator is created only when requested and removed when its dialog closes.
Escape closes the dialog. The video fallback is always available. A successful
iframe load does not guarantee streaming capacity; Appetize displays its own
availability or quota messages inside the frame.

The free tier has a limited monthly streaming allowance. Check the current
[Appetize pricing](https://appetize.io/pricing) before changing plans. No paid
subscription is required for this website.

## Media

The walkthrough and screenshots were captured from the real Android app on a
dedicated emulator. Its activity history is demonstration data, identified on
the page and video. The walkthrough is silent and has English WebVTT captions
and an HTML transcript. It is edited for pacing.

When refreshing media, use the same filenames in `docs/assets/`, keep the
walkthrough between 30 and 45 seconds, and update the captions and transcript
to match. Do not include personal session data in public captures.

Keep `nunito-ofl.txt` alongside the bundled font. The project's third-party
notices cover the app components shown in the captures.

## Before publishing updates

- Check desktop and narrow mobile layouts for overflow and unreadable text.
- Play the video, enable captions, and open each full-size screenshot.
- Navigate with the keyboard and check the skip link and visible focus styles.
- Verify the source links, social preview image, and browser console.
- If Appetize is configured, test dialog close/reopen, Escape, and the video fallback.
- Verify the deployed page while signed out.

## Resume entry

**Stretchify — Android movement app** · Kotlin, Jetpack Compose

Built guided exercise sessions with background timers, customizable routines,
progress tracking, and automated tests.
[Demo & source](https://ronengiladov.github.io/Stretchify/)
