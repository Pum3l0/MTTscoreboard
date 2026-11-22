# Публикация ScoreBoard на GitHub

## Подготовка версии

1. Проверь `versionName` и `versionCode` в [app/build.gradle.kts](app/build.gradle.kts). Для первой публикации в проекте заданы `0.1.0` и `1`. Для следующих версий увеличивай `versionCode`.
2. Выполни `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
3. Проверь на Android 11 начало и продолжение матча, счёт и отмену, обе ориентации, запись результата, импорт и экспорт, озвучку и отправку карточки результата.
4. Исходники распространяются по [GPL версии 3](LICENSE). Лицензии используемых шрифтов уже лежат в [assets/licenses](app/src/main/assets/licenses).

## Подписанный APK

1. В Android Studio открой **Build → Generate Signed App Bundle or APK**, выбери **APK**, модуль `app` и вариант `release`.
2. Для первой публикации создай ключ подписи и сохрани хранилище ключей и его резервную копию вне репозитория. Для следующих сборок GitHub Releases используй тот же ключ, чтобы пользователи могли обновляться с сохранением данных. [Подпись приложения в Android Studio](https://developer.android.com/studio/publish/app-signing)
3. Переименуй итоговый файл в `ScoreBoard-v0.1.0.apk` и проверь его перед загрузкой. Команда `apksigner` находится в каталоге `build-tools/<версия>/` Android SDK.

```bash
apksigner verify --verbose --print-certs ScoreBoard-v0.1.0.apk
sha256sum ScoreBoard-v0.1.0.apk > ScoreBoard-v0.1.0.apk.sha256
adb install -r ScoreBoard-v0.1.0.apk
```

Если на телефоне установлена отладочная сборка с тем же идентификатором приложения и другой подписью, Android отклонит обновление. Перед удалением отладочной сборки экспортируй историю. Незавершённый матч в экспорт истории не входит.

## Подпись через Gradle

Для локальной автоматической подписи создай в корне проекта игнорируемый Git файл `signing.properties` с полями `storeFile`, `storePassword`, `keyAlias` и `keyPassword`. Укажи абсолютный путь к хранилищу ключей и ограничь доступ к файлу командой `chmod 600 signing.properties`. Затем выполни `./gradlew :app:assembleRelease`. В чистом checkout сборка выпускает неподписанный APK.

## Черновик релиза

После первого пуша открой **Releases → Draft a new release**. Выбери тег `v0.1.0` на коммите, из которого собран APK. Добавь описание на русском, минимальную версию Android, инструкцию по установке и ссылку на исходники соответствующего тега. Прикрепи подписанный APK и файл SHA-256. Сохрани черновик и проверь вложения перед публикацией. [Управление релизами GitHub](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)
