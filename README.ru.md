[English](README.md)

# ScoreBoard

![Баннер ScoreBoard](artwork/screenshots/banner.jpg)

Офлайн-табло для настольного тенниса на Android 8.0 и новее.

- Веди счёт, следи за партиями и подачей в одиночных матчах. Отменяй очки и продолжай незаконченные матчи.
- Используй табло в любой ориентации. Настраивай цвета, шрифты, таймеры и озвучку на русском или английском.
- Храни историю на устройстве, сохраняй резервную копию в JSON и делись результатом картинкой.

**[Скачать APK](https://github.com/Pum3l0/MTTscoreboard/releases)** · [Все скриншоты](artwork/screenshots) · [GPL-3.0](LICENSE)

## Скриншоты

<p align="center">
  <img src="artwork/screenshots/horizontal_board.jpg" alt="Горизонтальное табло" width="720">
</p>

<p align="center">
  <img src="artwork/screenshots/menu.jpg" alt="Главная" width="170">
  <img src="artwork/screenshots/vertical_board.jpg" alt="Вертикальное табло" width="170">
  <img src="artwork/screenshots/final_matchscore.jpg" alt="Результат матча" width="170">
  <img src="artwork/screenshots/match_history.jpg" alt="История матчей" width="170">
</p>

## Сборка

С установленными JDK 17 и Android SDK 37 открой проект в Android Studio или запусти:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Автор [Pum3l0](https://pumelo.site)
