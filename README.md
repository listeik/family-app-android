# Family App для Android

Семейное Android-приложение для общих дел, покупок, еды в холодильнике, желаний, ленты активности и небольшого чата.

Связанный backend: [listeik/family-app-firebase](https://github.com/listeik/family-app-firebase).

## Технологии

- Kotlin
- Jetpack Compose и Material 3
- Firebase Authentication
- Cloud Firestore с обновлениями в реальном времени
- Firebase Cloud Messaging для push-уведомлений

## Возможности MVP

- анонимный вход Firebase для каждого устройства;
- создание семьи и приглашение по шестизначному коду;
- вход в существующую семью по коду;
- семейные карточки категорий «Еда», «Покупки», «Дела» и «Хотелки»;
- смена статусов и учёт оставшихся порций еды;
- общая лента событий;
- семейный чат;
- сохранение FCM-токена для push-уведомлений.

## Подключение Firebase

Рабочее приложение зарегистрировано в Firebase-проекте `family-app-listeik` с package name `com.listeik.familyapp`. Анонимный вход и Firestore уже настроены, правила опубликованы из репозитория backend.

Для локального запуска скачайте актуальный `google-services.json` из Firebase Console и поместите его в `app/google-services.json`.

Без `google-services.json` проект всё равно собирается, но вместо подключения к Firebase показывает экран настройки.

## Локальная сборка

Откройте проект в Android Studio и выполните синхронизацию Gradle либо запустите:

```powershell
.\gradlew.bat assembleDebug
```

Debug APK появится в `app/build/outputs/apk/debug/app-debug.apk`. Секреты и `google-services.json` намеренно не хранятся в Git.
