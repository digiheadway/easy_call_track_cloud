# Background Re-attach & Feedback Verification

## 1. Background Process (Global Sync)
- **Implemented `ReattachRecordingsWorker`**:
  - Runs in the background (WorkManager).
  - Displays a persistent notification ("Scanning recordings 50/200...").
  - Safely iterates through all call logs using `RecordingRepository`.
  - Is resistant to app closures or navigation changes.
- **Integration**:
  - `SettingsViewModel.reAttachAllRecordings()` now launches this worker immediately.
  - The blocking modal dialog in `ExtrasScreen` is no longer needed (replaced by non-blocking notification).

## 2. Individual Re-attach Feedback
- **Update in `HomeViewModel.reAttachRecordingsForPhone`**:
  - Shows "Scanning recordings for [Phone]..." toast immediately.
  - Shows "Updated [N] recordings for [Phone]" toast on completion.
  - Ensures user knows something is happening when they click the option.

## 3. Verification
- Build passed successfully (`./gradlew assembleDebug`).
- This meets the user requests:
  - "Run in background?" -> Yes, via WorkManager.
  - "In global sync?" -> It's now a robust background task that behaves like a sync.
  - "Individual call attach showing no loading" -> Fixed with Toasts.
