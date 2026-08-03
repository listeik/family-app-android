# Family App Android

Android MVP for a private family coordination app: food in the fridge, shopping, household tasks, wishes, activity updates, and a small family chat.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- Firebase Auth
- Cloud Firestore realtime listeners
- Firebase Cloud Messaging-ready member tokens

## Firebase setup

1. Create a Firebase project.
2. Add an Android app with package `com.listeik.familyapp`.
3. Download `google-services.json`.
4. Place it at `app/google-services.json`.
5. Enable Firebase Authentication anonymous sign-in.
6. Deploy the Firestore rules from the companion `family-app-firebase` repository.

Without `google-services.json`, the app builds but shows a setup screen instead of connecting to Firebase.

## MVP scope

- Anonymous Firebase sign-in per device.
- Create a family and share invite code.
- Join family by invite code.
- Create family items by category: food, shopping, task, wish.
- Realtime list updates through Firestore.
- Status transitions with activity history.
- Family chat backed by Firestore.

## Local build

Open the project in Android Studio and sync Gradle. This repository intentionally does not include secrets or `google-services.json`.
