# DocCheck — Document Tampering Detection Android App

DocCheck is a native Kotlin + XML Android application (no Jetpack Compose) styled with a soft pastel-lilac glassmorphic design system. It implements the complete 4-screen flow (**Welcome → Login / Sign Up → Home → RAG Chatbot**) plus **Result**, **History**, and **Settings** screens, connected via Jetpack Navigation with Safe Args.

---

## Architecture & Tech Stack

- **Language:** Kotlin, min SDK 26, target SDK 35
- **UI:** XML Layouts, ViewBinding, Material Components (Material 3), ConstraintLayout, RecyclerView, ViewPager2, NestedScrollView
- **Architecture:** Single-Activity (`MainActivity`), MVVM, Repository Pattern, Hilt DI
- **Navigation:** Jetpack Navigation Component (`nav_graph.xml`) with Safe Args
- **Async:** Kotlin Coroutines + `StateFlow` / `SharedFlow`
- **Local Storage:** DataStore Preferences (onboarding flag, dark theme, user session) + Room (`scan_results`, `chat_messages`)
- **Networking:** Retrofit + OkHttp + Gson (`DocCheckApiService`)
- **Images:** Coil + Android Photo Picker (`ActivityResultContracts.PickVisualMedia`), restricted to PNG/JPG up to 25 MB
- **Mock Mode:** Enabled via `BuildConfig.USE_MOCK = true` in `app/build.gradle.kts`, allowing end-to-end execution with zero backend or Firebase keys.

---

## Project Structure

```text
com.doccheck.app
 ├─ data
 │   ├─ local        (DocCheckDatabase, DAOs, Room entities, SessionPreferences)
 │   ├─ remote       (DocCheckApiService, DTOs)
 │   ├─ repository   (AuthRepository, ScanRepository, ChatRepository)
 │   └─ model        (User, AuthResult, ScanResult, Finding, ChatMessage, Source, HomeStats)
 ├─ di               (NetworkModule, DatabaseModule, RepositoryModule)
 ├─ ui
 │   ├─ splash       (RouterFragment)
 │   ├─ welcome      (WelcomeFragment)
 │   ├─ auth         (AuthFragment, AuthViewModel)
 │   ├─ home         (HomeFragment, HomeViewModel, ForensicToolkitAdapter, ResultFragment)
 │   ├─ chat         (ChatFragment, ChatViewModel, ChatAdapter, SourceBottomSheetDialogFragment)
 │   ├─ history      (HistoryFragment)
 │   ├─ settings     (SettingsFragment)
 │   └─ common       (FloatingBottomNavView)
 └─ util             (Constants, ValidationUtils, FileUtils, GlassBlurHelper)
```




     ```

