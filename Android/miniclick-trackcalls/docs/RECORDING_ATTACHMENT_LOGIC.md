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

## 3. Implemented Solutions ✅

> **Last Updated:** 2026-01-09 - MediaStore query and tiered approach implemented

### 3.1 ✅ Solution: MediaStore Query (Primary Method) — IMPLEMENTED

Instead of guessing file paths, we now ask the Android OS for "Audio files created around this time."

**Implementation:**
- Added `findRecordingViaMediaStore()` method in `RecordingRepository.kt`
- Queries `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` with `DATE_ADDED` filter
- Works on Android 10+ regardless of where the file is stored
- Returns `content://` URIs that can be read directly

### 3.2 ✅ Solution: Tiered Fallback Approach — IMPLEMENTED

The `findRecording()` method now uses a 5-tier approach:

```
Tier 1: CallCloud Backup Folder (fastest, our managed folder)
Tier 2: Learned Folder (previous successful match location)
Tier 3: MediaStore Query ±5 min window (works on Android 10+)
Tier 4: Traditional File Path Scan (legacy fallback)
Tier 5: MediaStore Query ±30 min window (last resort)
```

### 3.3 ✅ Solution: Learning System — IMPLEMENTED

- When a match is found, we save the parent folder to preferences
- Next time, we check this "learned folder" first for faster matching
- Key: `KEY_LEARNED_FOLDER` in SharedPreferences

### 3.4 ✅ Solution: Expanded Device Path Coverage — IMPLEMENTED

Added paths for:
- Samsung OneUI 4+ (`Recordings/Call/`, `DCIM/Call/`)
- Vivo FuntouchOS (`VoiceRecorder/Calls/`)
- Huawei backup (`HuaweiBackup/CallRecord/`)
- ColorOS 12+ hidden (`Android/media/com.coloros.soundrecorder/`)
- Additional third-party recorders (Blackbox, IntCall, ACR variants)

---

## 4. Implementation Details

**MediaStore Query Logic (from RecordingRepository.kt):**

```kotlin
private fun findRecordingViaMediaStore(
    callDate: Long, 
    durationSec: Long,
    bufferSeconds: Int = 300
): List<RecordingSourceFile> {
    val callDateSeconds = callDate / 1000
    val startWindow = callDateSeconds - bufferSeconds
    val endWindow = callDateSeconds + durationSec + bufferSeconds
    
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
    
    val selection = "${MediaStore.Audio.Media.DATE_ADDED} >= ? AND ${MediaStore.Audio.Media.DATE_ADDED} <= ?"
    val selectionArgs = arrayOf(startWindow.toString(), endWindow.toString())
    
    // Query and return results...
}
```

---

## 5. Remaining To-Do

| Task | Status | Priority |
|------|--------|----------|
| MediaStore Query | ✅ Done | Critical |
| Tiered Fallback | ✅ Done | Critical |
| Learning System | ✅ Done | High |
| Expanded Paths | ✅ Done | Medium |
| Manual Attachment UI | ❌ Not Done | Medium |
| Compression Before Upload | ❌ Not Done | Medium |
| Device Permission Guides | ❌ Not Done | Low |

---

*Document Updated: 2026-01-09*

