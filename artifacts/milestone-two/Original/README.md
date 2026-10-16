# MultiTasked

MultiTasked is a task management app built with Jetpack Compose and Firebase.

## Features

*   Create, edit, and delete task boards.
*   Create, edit, and delete tasks within boards.
*   Multiple color themes.


## Technologies Used

*   **Kotlin:** The entire application is written in Kotlin, a modern, concise, and safe programming language.
*   **Jetpack Compose:** The UI is built entirely with Jetpack Compose, which allows for a declarative and reactive UI development process.
*   **Firebase Authentication:** Used for user management. The app checks if a user is signed in to determine the initial screen.
*   **Firebase Firestore:** Used to store and sync data (task boards and tasks) in real-time across devices.
*   **Hilt:** Used for dependency injection to manage dependencies and simplify testing and maintenance.
*   **AndroidX Libraries (Navigation, Lifecycle, DataStore):** Used for managing navigation, UI state, and persisting user preferences.

## How Technologies Are Used

This application leverages a modern Android architecture to create a robust and maintainable user experience.

*   **UI Layer (Jetpack Compose):** The entire UI is built with Jetpack Compose, starting from the `MainActivity`. This provides a declarative approach to UI, making the code more intuitive and easier to manage. The app's theming is also handled by Compose, allowing for dynamic theme changes.
*   **Dependency Injection (Hilt):** Hilt is used to provide dependencies throughout the app. For example, in `MainActivity`, the `AuthRepository` is injected, decoupling the Activity from the concrete implementation of the repository. This is crucial for testability and scalability.
*   **Navigation (Jetpack Navigation):** The app uses a `NavHost` to manage navigation between different composable screens. This provides a structured and predictable way to handle the app's UI flow.
*   **Data and Business Logic (Repositories and ViewModels):** The app follows a repository pattern, where repositories (like `AuthRepository`) are responsible for handling data operations. They abstract the data sources (Firebase) from the rest of the app. ViewModels will then use these repositories to expose data to the UI and handle business logic.

## Scalability

The architecture of this app is designed to be scalable for a large user base.

*   **Firebase Backend:** Firebase services like Authentication and Firestore are serverless, managed services that automatically scale with your user base. You don't need to worry about managing servers or infrastructure as your app grows. Firestore's real-time capabilities ensure a seamless experience for users, even with a large number of concurrent connections.
*   **Clean Architecture:** The use of dependency injection and the repository pattern promotes a clean and decoupled architecture. This makes the app:
    *   **Easier to maintain:** With clear separation of concerns, it's easier to modify or add features without breaking existing functionality.
    *   **More testable:** Individual components can be tested in isolation, leading to a more stable app.
    *   **Ready for modularization:** As the app grows, features can be split into separate Gradle modules, improving build times and code organization.
*   **Performant UI:** Jetpack Compose is designed for performance. Its smart recomposition system ensures that only the necessary parts of the UI are redrawn, leading to a smooth user experience even with complex UIs.

