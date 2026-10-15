# d3tr01t Holdem

Offline Texas Hold'em tournament trainer, packaged as an Android WebView app.

## Android APK
Every push to `main` builds a debug APK using GitHub Actions. Open **Actions → Build d3tr01t Holdem APK → latest successful run → Artifacts → d3tr01t-holdem-apk** to download the APK.

## App behavior
- Locked to landscape orientation.
- Immersive full-screen layout.
- Offline HTML/JavaScript game bundled from root `index.html`.
- WebView DOM storage enabled for locally persisted player statistics.
- No network permission required at runtime.

## Build locally
Requires JDK 17, Android SDK platform 35, and Gradle 8.9:
`gradle assembleDebug`

The bot logic is heuristic and should not be treated as a GTO solver.