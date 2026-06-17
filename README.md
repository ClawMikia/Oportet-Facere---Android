# Oportet Facere (ReqSync)

An Android application for intelligent requirements tracking and mission management. Paste raw employment requirement lists and let the app auto-organize them into structured, trackable missions with gamified progress tracking.

## Features

- **Smart Parsing** — Paste raw requirement text and auto-organize into categorized missions using pattern recognition.
- **Mission Checklist** — Expandable category-based checklist with search, filter, swipe-to-delete, and archive support.
- **Gamification** — Earn XP, level up through ranks, maintain streaks, and unlock achievements as you complete requirements.
- **Notes** — Attach notes to individual requirement items for context and documentation.
- **Reminders** — Schedule notification reminders for pending requirements via WorkManager.
- **Statistics** — Track completion rates, category breakdowns, streaks, and overall progress.
- **Offline-First** — Full local data persistence using Room database with no network dependency.
- **Cyberpunk UI** — Dark-themed, neon-accented interface with custom animations.

## Tech Stack

- **Language:** Kotlin
- **Architecture:** MVVM with Repository pattern
- **UI:** Android Views with ViewBinding, Material Components
- **Database:** Room (with KSP annotation processing)
- **Async:** Kotlin Coroutines + Flow
- **Preferences:** Jetpack DataStore
- **Notifications:** WorkManager for scheduled reminders
- **Navigation:** Jetpack Navigation Component

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK API 34 (compileSdk)
- Minimum SDK 26 (Android 8.0)

## Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/yourusername/Oportet-Facere.git
   ```

2. Open the project in Android Studio.

3. Sync the project with Gradle files.

4. Build and run on an emulator or physical device (API 26+).

## Project Structure

```
Oportet-Facere/
├── app/
│   ├── src/main/
│   │   ├── java/com/reqsync/app/
│   │   │   ├── adapters/          # RecyclerView adapters (Checklist, Achievements, Notes, etc.)
│   │   │   ├── data/
│   │   │   │   ├── database/      # Room database, DAOs, entities, migrations
│   │   │   │   └── repository/    # Data repositories (Requirement, Gamification, Note, Reminder)
│   │   │   ├── ui/                # Fragments and Activities (Dashboard, Checklist, Paste, etc.)
│   │   │   ├── utils/             # Helpers (Parser, Notifications, Preferences, Extensions)
│   │   │   ├── viewmodels/        # ViewModels (Dashboard, Checklist, Parse, Details, etc.)
│   │   │   └── ReqSyncApp.kt      # Application class (manual DI container)
│   │   └── res/                   # Layouts, drawables, animations, themes, strings
│   └── build.gradle
├── gradle/
├── build.gradle
└── settings.gradle
```

## Dependencies

| Library | Purpose |
|---------|---------|
| AndroidX Core KTX | Kotlin extensions for Android core |
| Material Components | Cyberpunk-themed UI components |
| Room + KSP | Local SQLite database with compile-time query validation |
| Lifecycle + ViewModel | MVVM architecture with coroutine scopes |
| Navigation Component | Fragment navigation with safe args |
| Coroutines | Asynchronous programming with Flow |
| DataStore Preferences | Modern replacement for SharedPreferences |
| WorkManager | Scheduled background reminder notifications |
| ViewPager2 | Onboarding swipe pages |

## Contributing

1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/NewFeature`).
3. Commit your changes (`git commit -m 'Add NewFeature'`).
4. Push to the branch (`git push origin feature/NewFeature`).
5. Open a Pull Request.

## License

This project is licensed under the MIT License.

---
*Oportet Facere — Track. Parse. Complete.*
