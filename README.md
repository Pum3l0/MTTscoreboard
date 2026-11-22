[Русский](README.ru.md)

# ScoreBoard

![ScoreBoard banner](artwork/screenshots/banner.jpg)

An offline table tennis scoreboard for Android 8.0+.

- Track points, games, and service in singles matches. Undo points and pick up where you left off.
- Use the scoreboard in portrait or landscape with custom colors, fonts, timers, and optional English or Russian announcements.
- Keep match history on your device, back it up as JSON, and share results as images.

**[Download APK](https://github.com/Pum3l0/MTTscoreboard/releases)** · [All screenshots](artwork/screenshots) · [GPL-3.0](LICENSE)

## Screenshots

<p align="center">
  <img src="artwork/screenshots/horizontal_board.jpg" alt="Landscape scoreboard" width="720">
</p>

<p align="center">
  <img src="artwork/screenshots/menu.jpg" alt="Home" width="170">
  <img src="artwork/screenshots/vertical_board.jpg" alt="Portrait scoreboard" width="170">
  <img src="artwork/screenshots/final_matchscore.jpg" alt="Match result" width="170">
  <img src="artwork/screenshots/match_history.jpg" alt="Match history" width="170">
</p>

## Build

With JDK 17 and Android SDK 37 installed, open the project in Android Studio or run:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

By [Pum3l0](https://pumelo.site)
