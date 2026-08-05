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

1. Создайте проект Firebase.
2. Добавьте Android-приложение с package name `com.listeik.familyapp`.
3. Скачайте `google-services.json`.
4. Поместите файл в `app/google-services.json`.
5. Включите анонимный способ входа в Firebase Authentication.
6. Разверните правила Firestore из репозитория backend.

Без `google-services.json` проект всё равно собирается, но вместо подключения к Firebase показывает экран настройки.

## Локальная сборка

Откройте проект в Android Studio и выполните синхронизацию Gradle либо запустите:

```powershell
.\gradlew.bat assembleDebug
```

Debug APK появится в `app/build/outputs/apk/debug/app-debug.apk`. Секреты и `google-services.json` намеренно не хранятся в Git.
