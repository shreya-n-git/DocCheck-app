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

---

## Running & Building

```bash
./gradlew assembleDebug
```

Out of the box, `BuildConfig.USE_MOCK` is `true`, so authentication, document forensic scoring, and RAG chat streaming all work offline with realistic mock data.

---

## Plugging In Real Backend & Firebase Keys

1. **Backend URL & Mock Toggle** (`app/build.gradle.kts`):
   - Set `buildConfigField("boolean", "USE_MOCK", "false")`
   - Set `buildConfigField("String", "BASE_URL", "\"https://your-fastapi-host.com/\"")`
2. **Firebase Authentication** (`AuthRepository.kt`):
   - Create a Firebase project and download `google-services.json` into `app/google-services.json`.
   - Add the `com.google.gms.google-services` Gradle plugin in `build.gradle.kts` and `app/build.gradle.kts`.
   - Enable Email/Password and Google Sign-In providers in Firebase Console and configure your Web Client ID in `AuthFragment` / `AuthRepository`.

---

## Backend Contract (FastAPI + RAG Pipeline Reference)

The Android client expects the following Python FastAPI endpoints when `USE_MOCK = false`:

1. **`POST /api/analyze`** (`multipart/form-data`)
   - Request: `file` (PNG/JPG image), optional `module` (`COPY_MOVE | ELA | METADATA | SIGNATURE`)
   - Response:
     ```json
     {
       "id": "uuid",
       "tamper_score": 78,
       "findings": [
         { "module": "Error Level (ELA)", "severity": "HIGH", "description": "..." }
       ],
       "timestamp": 1732440000000
     }
     ```
2. **`GET /api/stats`**
   - Response: `{ "scanned": 142, "tampered": 11, "accuracy": 99.4 }`
3. **`POST /api/chat`** (and optional SSE **`POST /api/chat/stream`**)
   - Request: `{ "session_id": "...", "message": "...", "scan_context": { "scan_id": "...", "tamper_score": 78, "findings": [...] } }`
   - Pipeline: Embed user query → retrieve top-$k$ forensic knowledge chunks from Chroma/FAISS vector store → generate grounded LLM response with citations.
   - Response:
     ```json
     {
       "answer": "...",
       "sources": [
         { "title": "ELA_guide.pdf", "page": 4, "snippet": "..." }
       ]
     }
     ```

