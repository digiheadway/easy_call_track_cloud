# Comprehensive Project Audit & Improvement Proposal

This document provides a detailed analysis of the current state of the Call Tracking application and proposes specific improvements across performance, sync reliability, UX/UI, and code architecture.

---

## 1. Findings: Bugs & Issues

### 1.1 Redundant Sync Triggers
- **Issue:** In `CallSyncWorker`, when a person's note/label is updated, an additional call to `updateNote` is made for the `lastCallCompositeId`.
- **Impact:** Increases network traffic and server load.
- **Fix:** Consolidate person-level updates on the server. The server should automatically propagate person labels to associated historical calls if required, or handle it via a single batch update.

### 1.2 "Ghost" Flow Emissions
- **Issue:** `SettingsRepository` flows (e.g., `getTrackStartDateFlow()`) emit values whenever the underlying SharedPreferences change, even if the specific key hasn't changed.
- **Problem:** Triggers expensive `triggerFilter()` logic in `HomeViewModel` unnecessarily.
- **Fix:** Apply `.distinctUntilChanged()` to all shared preference flows in the Repository layer.

### 1.3 Normalization Inconsistency
- **Issue:** `normalizePhoneNumber` uses `TelephonyManager`, which is a system service call. Calling this in loops (like in `processCallFilters`) is inefficient.
- **Problem:** Potential for minor differences in normalization logic between Local (App) and Remote (Server).
- **Fix:** Implement a thread-safe `NormalizationCache` (LRU or Map) and ensure the logic strictly matches the server-side normalization.

---

## 2. Performance: "Heavy Things" & Slow Queries

### 2.1 Reports Screen Logic (Critical)
- **Heavy Thing:** `ReportsScreen.kt` currently calculates complex statistics (Top Callers, Daily Breakdown, Duration Distribution) inside a `remember` block within the Composable.
- **Problem:** As the call log grows (thousands of entries), these O(N) or O(N log N) operations happen on the UI thread during recomposition or data updates, causing visible frame drops.
- **Optimization:** Move all statistical calculations to `HomeViewModel` (or a dedicated `ReportsViewModel`) and execute them on `Dispatchers.Default`. Expose the result as a single `StateFlow<ReportStats>`.

### 2.2 Database Query Optimization
- **Slow Query Potential:** The `@Query` in `CallDataDao.getAllCallsFlow()` joins `call_data` with `person_data` on `phoneNumber`. 
- **Findings:** While indexes exist, joining on every fetch of the main list adds overhead.
- **Optimization:** Denormalize critical UI flags (like `isExcluded`) into the `call_data` table during the import/sync process to avoid the JOIN for the main list view.

### 2.3 Incremental System Import
- **Issue:** `syncFromSystemCallLog` performs a "Smart Check" but currently fetches a 2-day buffer by default.
- **Optimization:** Use a more surgical query for the system `CallLog` provider. Instead of fetching a window, strictly query for IDs/Dates *greater than* the current max local ID/Date.

---

## 3. Syncing: Stability & Improvements

### 3.1 Unreliable Recording Status
- **Issue:** If a recording is not found initially, it is marked as `NOT_FOUND`. 
- **Problem:** Some recording apps might delay file creation or use temporary paths.
- **Improvement:** Implement a "Retry with Backoff" for `NOT_FOUND` recordings. Instead of giving up immediately, re-scan a few times over the next hour before final failure.

### 3.2 Parallel Uploads
- **Current State:** `RecordingUploadWorker` processes uploads sequentially.
- **Improvement:** Implement parallel uploads with a threshold (e.g., 2-3 concurrent uploads). This significantly reduces the time to clear a large sync queue on fast networks.

### 3.3 Large Batch Conflict Resolution
- **Issue:** Bidirectional sync (`pullServerUpdates`) might overwrite local notes if the server timestamp is newer, even if the user just edited the note offline.
- **Improvement:** Implement a "Dirty" flag for local edits. If a record is locally dirty, do not overwrite it with server data until the local change has successfully pushed.

---

## 4. UX/UI Betterment

### 4.1 Enhanced Loading Experience
- **UI Issue:** The app uses a simple "Syncing..." status bar or loading spinner.
- **Better UX:** Implement **Shimmer Placeholders** for the Call List and Person List. This gives the user a sense of "content is coming" and reduces perceived wait time.

### 4.2 Interactive Reports
- **UI Issue:** Reports are static cards.
- **Better UX:** Make stats cards **clickable**.
  - Clicking "Missed Calls" in the report should navigate to the Calls tab with the "Missed" filter pre-applied.
  - Clicking a "Top Caller" should open that person's interaction history.

### 4.3 Tab Overload
- **UX Issue:** Having both "Calls" and "Persons" tabs with sub-filters can be confusing to navigate.
- **Optimization:** Consolidate the "Ignored" tab into a "Manage Exclusions" screen accessible from Settings or as a bottom sheet, keeping the main feed cleaner.

---

## 5. Architecture & Code Structure

### 5.1 Repository Separation
- **Current State:** `CallDataRepository` is a "God Repository" (3200+ lines).
- **Better Method:** Split into:
  - `CallSyncManager`: Orchestrates foreground/background syncing logic.
  - `CallLocalSource`: Pure Room DB operations.
  - `RecordingProvider`: Logic for finding and compressing files.

### 5.2 Hardcoded Values to Enums
- **Issue:** Logic like `callType == 6` or strings like `"Sim1"` are scattered.
- **Better Method:** Standardize on sealed classes or robust enums for all call types and SIM configurations to prevent "Magic Number" bugs.

---

## 6. Management of Features: Add/Remove

### 6.1 Add: Smart Search History
- **New Feature:** Users often search for the same numbers or tags. Add a "Recent Searches" chip row under the search bar.

### 6.2 Add: Call Duration Alerts
- **New Feature:** For sales tracking, highlight calls that are "Too Short" (e.g., under 10 seconds) as they might indicate low engagement.

### 6.3 Remove: Duplicate Recording Scans
- **Remove:** The logic that re-scans for recordings in every `syncFromSystem` even if they were already marked as `NOT_APPLICABLE` for non-voice calls.

---

## Summary Action Plan

| Rank | Task | Impact | Difficulty | Status |
| :--- | :--- | :--- | :--- | :--- |
| 1 | Move Report calculations to ViewModel | High | Medium | ✅ Done |
| 2 | Implement Shimmer Loading UI | High | Medium | ✅ Done |
| 3 | Apply `.distinctUntilChanged()` to settings | Medium | Easy | ✅ Done |
| 4.1 | Enhanced Loading Experience (Shimmer) | High | Medium | ✅ Done |
| 4.2 | Interactive Reports (Clickable Stats) | Medium | Medium | ✅ Done |
| 5.2 | Hardcoded Values to Enums | Medium | Easy | ✅ Done |
| 6.1 | Add Smart Search History | Low | Easy | ✅ Done |
| 6.2 | Add Call Duration Alerts | Low | Easy | ✅ Done |
| 4 | Batch Server Sync Updates | Medium | Hard | Pending |
| 5.1 | Refactor CallDataRepository into smaller units | Low | Hard | Pending |
| 6.3 | Remove Duplicate Recording Scans | Low | Medium | Pending |

---
