# Recording Attachment & Path Detection: Analysis and Improvement Plan

## 1. Current Problem Analysis

The current implementation of call recording attachment in `RecordingRepository.kt` and `RecordingUploadWorker.kt` faces significant challenges due to modern Android storage restrictions (Scoped Storage).

### 1.1 Compatibility with Android 10+ (API 29+)
The application targets **SDK 35 (Android 15)**. Since Android 10, the "Scoped Storage" model has been enforced, which fundamentally changes how apps access files.

*   **Legacy `File` API Failure:** The current detection logic relies heavily on `java.io.File` and `Environment.getExternalStorageDirectory()` to scan hardcoded paths (e.g., `Recordings/Call recordings`).
    *   **Issue:** On Android 11+, apps cannot access files in the public external storage or other apps' directories using standard `File` paths, even with `READ_EXTERNAL_STORAGE`.
    *   **Result:** `dir.exists()` or `dir.listFiles()` will often return `false` or `null`, making the app "blind" to recordings that actually exist on the disk.

### 1.2 Fragile Path Hardcoding
The repository maintains a list of `DEVICE_DEFAULT_PATHS` and `THIRD_PARTY_PATHS`.
*   **Issue:** This list requires constant maintenance. If a recording app changes its folder name or structure, our app fails to find the files.
*   **Issue:** Some manufacturers (e.g., Samsung, Xiaomi) might change their default recorder paths with OS updates.

### 1.3 Matching Logic Limitations
*   **Time-Based Matching:** The current logic uses a +/- 5-minute window (`tolerance`). While reasonable, it can lead to false positives if multiple short calls occur in quick succession.
*   **Filename Matching:** It attempts to match phone numbers in filenames. This fails if the recording app uses opaque naming conventions (e.g., "Voice 001.mp3" or just a timestamp).

---

## 2. Potential Future Issues

1.  **Total Access Block:** Future Android versions may further restrict `READ_EXTERNAL_STORAGE`, deprecating it entirely for `READ_MEDIA_AUDIO` only. The current codebase already handles the permission request, but the *files access logic* is outdated.
2.  **Private Storage Migration:** More third-party call recorders are moving to `Android/data/[package_name]/files` to avoid permission issues themselves. Our app **cannot** access these directories without root or specific vulnerability exploits (which we must avoid).
3.  **Metadata Stripping:** Audio files shared or moved might lose their "Date Modified" timestamp, breaking the time-based matching logic.

---

## 3. Proposed Solutions & Improvements

To resolve these bugs and "future-proof" the app, we must shift from a "File System Scanner" approach to a "Media Query" approach.

### 3.1 Solution: Implement `MediaStore` Query (Primary Method)
Instead of guessing file paths, we should ask the Android OS for "Audio files created around this time."

**Mechanism:**
1.  Query `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.
2.  Filter by `DATE_ADDED` or `DATE_MODIFIED` within the call start/end window.
3.  The OS returns a `content://` URI (e.g., `content://media/external/audio/media/1054`) which we can read directly, regardless of where the file is actually stored (as long as it's in a shared collection).

**Advantages:**
*   Works on all Android versions (10, 11, 12, 13+).
*   No need to know the specific folder path (e.g., "MIUI/sound_recorder").
*   Respects Scoped Storage permissions naturally.

### 3.2 Solution: Robust Storage Access Framework (SAF)
For users who save recordings in non-standard folders or folders the MediaStore doesn't index effectively.

**Mechanism:**
1.  Enhance the "Custom Path" feature to force the use of `ACTION_OPEN_DOCUMENT_TREE`.
2.  Persist access permissions (already partially implemented).
3.  Use `DocumentFile` traversal instead of `File` traversal for these custom paths (already partially implemented but needs to be the standard).

### 3.3 Solution: "Accessibility Service" Path Sniffer (Advanced)
*Use only if `MediaStore` fails.*
If the user uses a third-party recorder that saves to a private directory, we can't read the file. However, if we identify the recorder app, we could potentially guide the user to "Share" the recording to our app immediately after the call.

### 3.4 Logic Improvement Plan (Step-by-Step)

#### Step 1: Refactor `RecordingRepository.kt`
*   Add a `findRecordingViaMediaStore()` method.
*   Update `findRecording()` to chain strategies:
    1.  **Strategy A:** Check `CallCloud` public folder (internal backup).
    2.  **Strategy B:** Query `MediaStore` for audio files in the time window (New Standard).
    3.  **Strategy C:** Scan "Custom Path" using `DocumentFile` (User-defined).
    4.  **Strategy D:** Legacy `File` scan (Keep for Android 9 and below only).

#### Step 2: Update `RecordingUploadWorker.kt`
*   Ensure it can handle `content://` URIs natively without trying to convert them to `File` paths unless necessary for upload libraries. (Current code has some support, but needs verification).

#### Step 3: Improve Matching Heuristics
*   **Duration Matching:** If metadata is available, compare the file's duration with the call log duration. An exact match (or within 1-2 seconds) is a very strong signal.
*   **Smart Filename Parsers:** Add regex parsers for common recorder filename patterns (e.g., `Call_with_[NUMBER]_[DATE]`).

---

## 4. Implementation Details (Code Snippet)

**New `MediaStore` Query Logic:**

```kotlin
fun findRecordingViaMediaStore(callDate: Long, durationSec: Long): List<RecordingSourceFile> {
    val windowSeconds = 300 // 5 minutes
    val startWindow = (callDate / 1000) - windowSeconds
    val endWindow = (callDate / 1000) + durationSec + windowSeconds

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA // Deprecated in Q, but still useful for debug/legacy
    )

    val selection = "${MediaStore.Audio.Media.DATE_ADDED} >= ? AND ${MediaStore.Audio.Media.DATE_ADDED} <= ?"
    val selectionArgs = arrayOf(startWindow.toString(), endWindow.toString())

    val results = mutableListOf<RecordingSourceFile>()
    
    context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val dateAdded = cursor.getLong(dateColumn)
            val contentUri = ContentUris.withAppendedId(collection, id)
            
            results.add(RecordingSourceFile(
                name = name,
                lastModified = dateAdded * 1000,
                absolutePath = contentUri.toString(), // Store URI as path
                isLocal = false // It's a content URI
            ))
        }
    }
    return results
}
```
