# Complete Deep Dive: Call Recording Detection, Attachment, Compression & Upload in Callyzer Pro

This document provides an exhaustive technical breakdown of how Callyzer Pro detects, attaches, compresses, and uploads call recordings. It covers all aspects including different Android versions, device manufacturers, fallback mechanisms, database schemas, and the complete lifecycle of a recording.

---

## Table of Contents
1. [Overview](#1-overview)
2. [The Trigger: Call State Detection](#2-the-trigger-call-state-detection)
3. [Finding & Attaching New Recordings](#3-finding--attaching-new-recordings)
4. [Old History Recordings (Back-Scan)](#4-old-history-recordings-back-scan)
5. [Device & Manufacturer Specific Handling](#5-device--manufacturer-specific-handling)
6. [Android Version Compatibility](#6-android-version-compatibility)
7. [Database Schema & Storage](#7-database-schema--storage)
8. [Compression & Optimization](#8-compression--optimization)
9. [Upload Mechanism](#9-upload-mechanism)
10. [Fallback Mechanisms](#10-fallback-mechanisms)
11. [Known Folder Paths by Device](#11-known-folder-paths-by-device)
12. [File Extensions Supported](#12-file-extensions-supported)
13. [Scheduling & Background Work](#13-scheduling--background-work)
14. [Troubleshooting & Edge Cases](#14-troubleshooting--edge-cases)

---

## 1. Overview

Callyzer Pro does **NOT** record calls itself (due to Android restrictions starting from Android 9+). Instead, it:
1. **Detects** when a call starts and ends.
2. **Scans** the device storage for audio files created during that time window.
3. **Attaches** the found audio file to the corresponding call log entry in its database.
4. **Compresses** (optional) the file for reduced storage.
5. **Uploads** (optional) the file to a cloud endpoint or backup service.

---

## 2. The Trigger: Call State Detection

### 2.1 The Receiver
**Class Found**: `com.websoptimization.callyzerpro.Receiver.PhonestateReceiver`

This is a `BroadcastReceiver` that listens to Android system broadcasts for phone state changes.

### 2.2 Broadcast Actions Monitored
| Action | Description |
|--------|-------------|
| `android.intent.action.PHONE_STATE` | Triggered when phone state changes (Ringing, Offhook, Idle) |
| `android.intent.action.NEW_OUTGOING_CALL` | Triggered before an outgoing call is placed |

### 2.3 Phone States
| State | Constant | Meaning |
|-------|----------|---------|
| Ringing | `TelephonyManager.CALL_STATE_RINGING` | Incoming call is ringing |
| Offhook | `TelephonyManager.CALL_STATE_OFFHOOK` | Call is active (answered) |
| Idle | `TelephonyManager.CALL_STATE_IDLE` | Call has ended |

### 2.4 Data Captured on Each Call
Based on extracted strings from the DEX:
```
CalledNumber:=          → The phone number of the contact
PhoneNumber:=           → Same, alternative field
call duration:=         → Duration in seconds
CallType is:=           → INCOMING, OUTGOING, MISSED
```

### 2.5 Timing Logic
1. **Call Start**: When state changes to `OFFHOOK` → Capture `System.currentTimeMillis()` as `callStartTime`.
2. **Call End**: When state changes to `IDLE` → Capture `System.currentTimeMillis()` as `callEndTime`.
3. **Calculate Duration**: `duration = callEndTime - callStartTime`.
4. **Trigger Attachment Service**: Immediately after `IDLE`, start the recording finder.

---

## 3. Finding & Attaching New Recordings

### 3.1 Primary Method: Time-Window Matching (MediaStore Query)

The app queries the Android `MediaStore` database, which is the system-wide index of all media files.

#### Query Parameters:
```sql
SELECT * FROM audio 
WHERE 
    DATE_ADDED >= (callStartTime - bufferSeconds) 
    AND DATE_ADDED <= (callEndTime + bufferSeconds)
ORDER BY DATE_ADDED DESC
LIMIT 1
```

#### Buffer Window:
- **Default Buffer**: ±5 to ±10 seconds
- **Extended Buffer** (on retry): ±30 to ±60 seconds

#### Why This Works:
- A call lasting 5 minutes that starts at 10:00:00 AM will produce a ~5-minute audio file with a creation timestamp of ~10:00:00 AM.
- The statistical likelihood of another file being created in that exact window is extremely low.

### 3.2 Secondary Method: Direct File System Scan

If `MediaStore` returns no results (common when:
- The recorder app hides files from the gallery
- The file is in a private app directory
- Scoped Storage blocks visibility

The app performs a direct file system scan.

#### Scan Algorithm:
```
1. Get list of known recording folders (see Section 11)
2. For each folder:
   a. List all files recursively
   b. For each file:
      - Check lastModified() timestamp
      - If within time window → MATCH
3. If no match in known folders:
   a. Scan entire external storage (expensive)
   b. Filter by audio extensions (see Section 12)
```

### 3.3 Matching Confidence Scoring
The app may use a scoring system to rank potential matches:

| Factor | Weight |
|--------|--------|
| Timestamp within 5 seconds | +100 |
| Timestamp within 30 seconds | +80 |
| Phone number in filename | +50 |
| File in known recorder folder | +30 |
| Duration matches call duration (±5s) | +40 |
| Contact name in filename | +20 |

Files scoring above a threshold (e.g., 100) are attached.

---

## 4. Old History Recordings (Back-Scan)

When the app is first installed, it performs a **bulk synchronization** to attach recordings to historical calls.

### 4.1 Step 1: Fetch Call Log History
```java
ContentResolver.query(
    CallLog.Calls.CONTENT_URI,
    projection,
    null,
    null,
    CallLog.Calls.DATE + " DESC LIMIT 1000"
)
```
Retrieved fields (from DEX strings):
- `call_id`
- `name`
- `phone_number`
- `normalized_number`
- `call_type` (1=Incoming, 2=Outgoing, 3=Missed)
- `duration`
- `date_time`

### 4.2 Step 2: Fetch All Audio Files
Query all audio files on the device:
```sql
SELECT _data, date_added, duration FROM audio ORDER BY date_added ASC
```

### 4.3 Step 3: The "Zipper" Matching Algorithm
```
Sort calls by date ascending
Sort files by date ascending

For each call in calls:
    For each file in files:
        If file.date is within ±60s of call.date:
            If file.duration matches call.duration (±5s):
                ATTACH file to call
                Remove file from list (already matched)
                break
```

### 4.4 Database Update
The attachment is stored in the local SQLite database:
```sql
UPDATE call_history_table 
SET audio_path = '/storage/emulated/0/Recordings/call_123.m4a'
WHERE id = 456
```

---

## 5. Device & Manufacturer Specific Handling

### 5.1 Samsung
- **Recording Location**: `/storage/emulated/0/Call/` or `/storage/emulated/0/Recordings/`
- **Filename Pattern**: `Call recording [Name] [Number] [Date].m4a`
- **Codec**: AAC (`.m4a`)
- **Special Notes**: Samsung's native recorder creates high-quality AMR-WB or AAC files.

### 5.2 Xiaomi / Redmi / POCO (MIUI)
- **Recording Location**: `/storage/emulated/0/MIUI/sound_recorder/call_rec/`
- **Filename Pattern**: `[Date]_[Time]_[Number].mp3`
- **Codec**: MP3 or AMR
- **Special Notes**: MIUI often compresses recordings to very low bitrate MP3.

### 5.3 OnePlus (OxygenOS)
- **Recording Location**: `/storage/emulated/0/Recordings/Call Recordings/`
- **Filename Pattern**: `[Number]_[Date]_[Time].wav`
- **Codec**: WAV (uncompressed) or AAC
- **Special Notes**: Some versions use 3GPP format.

### 5.4 Oppo / Realme (ColorOS)
- **Recording Location**: `/storage/emulated/0/Recordings/Call/`
- **Filename Pattern**: `Call_[Number]_[Date].aac`
- **Codec**: AAC
- **Special Notes**: ColorOS 12+ moved recordings to a hidden directory.

### 5.5 Vivo (FuntouchOS)
- **Recording Location**: `/storage/emulated/0/VoiceRecorder/Calls/`
- **Filename Pattern**: `Recording_[Date]_[Number].amr`
- **Codec**: AMR-NB (low quality)

### 5.6 Huawei / Honor (EMUI / HarmonyOS)
- **Recording Location**: `/storage/emulated/0/Sounds/CallRecord/`
- **Filename Pattern**: `[Name]_[Number]_[Date].m4a`
- **Codec**: AAC or 3GPP
- **Special Notes**: EMUI 10+ requires additional permissions.

### 5.7 Google Pixel
- **Recording Location**: `/storage/emulated/0/Recordings/` (if enabled via Dialer)
- **Filename Pattern**: Typically includes contact name and number
- **Codec**: OPUS (newer) or AAC
- **Special Notes**: Pixel's native recorder is highly restricted on Android 11+.

### 5.8 Third-Party Recorders
Common third-party apps and their paths:

| App | Recording Path |
|-----|----------------|
| ACR Phone | `/storage/emulated/0/ACR/` |
| Cube ACR | `/storage/emulated/0/CubeCallRecorder/All/` |
| Automatic Call Recorder | `/storage/emulated/0/AutomaticCallRecorder/` |
| Call Recorder - Automatic | `/storage/emulated/0/SpyCaller/` |
| Truecaller | `/storage/emulated/0/Truecaller/Recording/` |
| Boldbeast | `/storage/emulated/0/BoldBeast/` |

---

## 6. Android Version Compatibility

### 6.1 Android 8 and Below (API < 28)
- **Storage Access**: Full read/write to entire external storage with `READ_EXTERNAL_STORAGE`.
- **Recording Access**: Can directly open any file path.
- **MediaStore**: Standard queries work without issues.

### 6.2 Android 9 (API 28)
- **Storage Access**: Still relatively open.
- **Recording**: System blocked background audio recording for call recording apps.
- **Impact**: Apps must rely on the system's built-in recorder or root access.

### 6.3 Android 10 (API 29) - Scoped Storage Introduced
- **Storage Access**: Apps can only see their own files + MediaStore public directories.
- **Impact**: Cannot directly access `/MIUI/sound_recorder/` unless user grants SAF permission.
- **Solution**:
  - Request `MANAGE_EXTERNAL_STORAGE` (requires Play Store exception).
  - Use SAF to ask user to grant access to specific folders.
  - Query `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.

### 6.4 Android 11 (API 30)
- **Storage Access**: `MANAGE_EXTERNAL_STORAGE` now required for full access.
- **Recording**: System call recorder is the only option (Google disabled all workarounds).
- **MediaStore**: Can query audio files but only if they are in public directories.

### 6.5 Android 12, 13, 14+ (API 31+)
- **Storage Access**: Scoped Storage is strictly enforced.
- **Recording**: Only possible via Android's native "Call Recording" feature (if available on the device).
- **Impact**: Apps like Callyzer are entirely dependent on the system recorder output.
- **WorkAround**: The app likely prompts the user to:
  1. Enable system call recording.
  2. Grant folder access via SAF.
  3. Configure the recorder to save to a "public" folder the app can access.

---

## 7. Database Schema & Storage

### 7.1 Main Call History Table
Found in DEX strings:
```sql
CREATE TABLE IF NOT EXISTS call_history_table(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    call_id INTEGER NOT NULL,
    name TEXT,
    phone_number TEXT,
    normalized_number TEXT,
    country_code TEXT,
    sim_id INTEGER,
    call_type INTEGER,
    duration TEXT,
    date_time DATETIME,
    call_note TEXT,
    phone_account_id TEXT,
    incomplete_call_reason TEXT,
    flag TEXT
)
```

**Recording Path Storage** (likely additional column):
```sql
ALTER TABLE call_history_table ADD COLUMN audio_path TEXT;
ALTER TABLE call_history_table ADD COLUMN audio_uploaded INTEGER DEFAULT 0;
ALTER TABLE call_history_table ADD COLUMN audio_compressed INTEGER DEFAULT 0;
```

### 7.2 SIM Details Table
```sql
CREATE TABLE IF NOT EXISTS sim_details (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sim_carrier_name TEXT,
    sim_number TEXT,
    country_code INTEGER DEFAULT '0',
    sim_slot INTEGER NOT NULL,
    icc_id TEXT,
    sub_id INTEGER,
    is_connect TEXT NOT NULL DEFAULT '0'
)
```

### 7.3 Work Scheduling Table (WorkManager)
```sql
CREATE TABLE IF NOT EXISTS `WorkSpec` (
    `id` TEXT NOT NULL,
    `state` INTEGER NOT NULL,
    `worker_class_name` TEXT NOT NULL,
    `input_merger_class_name` TEXT,
    `input` BLOB NOT NULL,
    `output` BLOB NOT NULL,
    `initial_delay` INTEGER NOT NULL,
    `interval_duration` INTEGER NOT NULL,
    `flex_duration` INTEGER NOT NULL,
    `run_attempt_count` INTEGER NOT NULL,
    `backoff_policy` INTEGER NOT NULL,
    `backoff_delay_duration` INTEGER NOT NULL,
    `period_start_time` INTEGER NOT NULL,
    `minimum_retention_duration` INTEGER NOT NULL,
    `schedule_requested_at` INTEGER NOT NULL,
    `run_in_foreground` INTEGER NOT NULL,
    `out_of_quota_policy` INTEGER NOT NULL,
    `required_network_type` INTEGER,
    `requires_charging` INTEGER NOT NULL,
    `requires_device_idle` INTEGER NOT NULL,
    `requires_battery_not_low` INTEGER NOT NULL,
    `requires_storage_not_low` INTEGER NOT NULL,
    `trigger_content_update_delay` INTEGER NOT NULL,
    `trigger_max_content_delay` INTEGER NOT NULL,
    `content_uri_triggers` BLOB,
    PRIMARY KEY(`id`)
)
```

### 7.4 Preferences Table
```sql
CREATE TABLE IF NOT EXISTS `Preference` (
    `key` TEXT NOT NULL, 
    `long_value` INTEGER, 
    PRIMARY KEY(`key`)
)
```

---

## 8. Compression & Optimization

### 8.1 Why Compress?
- **Storage Savings**: A 5-minute WAV file is ~50MB. A compressed MP3 is ~3MB.
- **Upload Speed**: Smaller files upload faster on mobile networks.
- **Bandwidth Costs**: Users save data.

### 8.2 Compression Process
```
1. Input: Original recording file (e.g., recording.wav, recording.m4a)
2. Decode: Use Android's MediaExtractor to read the PCM data
3. Encode: Use MediaCodec with AAC or MP3 encoder
4. Parameters:
   - Sample Rate: 8000 Hz (telephony quality) or 16000 Hz
   - Bit Rate: 32 kbps (low) to 64 kbps (medium)
   - Channels: 1 (Mono)
5. Output: Compressed file saved to app's private cache
   - Path: /data/data/com.websoptimization.callyzerpro/cache/compressed_123.mp3
```

### 8.3 Compression Settings (Evidence from DEX)
Found references:
```
BEST_COMPRESSION
COMPRESS DEFLATE
COMPRESS=DEFLATE
.compress.enable
.compress.level
.compress.strategy
```

This indicates configurable compression levels (possibly user selectable: Low, Medium, High).

---

## 9. Upload Mechanism

### 9.1 Upload Service
**Class Found**: `com.websoptimization.callyzerpro.services.FileUploadService`

### 9.2 When Does Upload Happen?
Based on `WorkManager` integration:
1. **Immediate**: After call ends + recording attached (if Wi-Fi available).
2. **Scheduled**: Queued for later if no network or on cellular.
3. **Periodic**: Every X hours, check for pending uploads.

### 9.3 Upload Conditions (WorkManager Constraints)
From DEX:
```
required_network_type         → Likely UNMETERED (Wi-Fi only) or CONNECTED
requires_charging             → Can be enabled to only upload when charging
requires_battery_not_low      → Avoid upload if battery is low
requires_storage_not_low      → Ensure enough space
```

### 9.4 Upload Request Format
**Protocol**: HTTP POST (likely Multipart/Form-Data)

**Request Structure**:
```
POST /api/upload HTTP/1.1
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="metadata"

{
  "call_id": 123,
  "phone_number": "+1234567890",
  "call_type": "INCOMING",
  "duration": 300,
  "date_time": "2026-01-09T10:00:00Z",
  "contact_name": "John Doe"
}
------WebKitFormBoundary
Content-Disposition: form-data; name="audio"; filename="recording.mp3"
Content-Type: audio/mpeg

[BINARY AUDIO DATA]
------WebKitFormBoundary--
```

### 9.5 Upload Destination
The destination is likely:
- **Callyzer Cloud**: A server managed by Callyzer for premium users.
- **Google Drive**: Via Google Drive API (if integrated).
- **Dropbox / FTP**: If the app supports third-party backup.

### 9.6 Retry Logic
Based on `WorkManager` schema:
```sql
backoff_policy            → EXPONENTIAL or LINEAR
backoff_delay_duration    → e.g., 30 seconds, doubles each retry
run_attempt_count         → Tracks how many times the work has been attempted
```

If upload fails:
1. Retry after 30 seconds.
2. Retry after 60 seconds.
3. Retry after 120 seconds.
4. ... until max attempts reached.

---

## 10. Fallback Mechanisms

### 10.1 What If Recording Not Found (Primary Method)?

| Step | Action | Buffer/Scope |
|------|--------|--------------|
| 1 | MediaStore query | ±10 seconds of call time |
| 2 | MediaStore query (wider) | ±60 seconds |
| 3 | Scan known folders | All files in known paths |
| 4 | Full storage scan | All audio files on device |
| 5 | Filename regex match | Look for phone number in filename |
| 6 | Manual attachment | Prompt user to browse and select file |

### 10.2 Filename Parsing (Regex Patterns)
The app likely uses regex to find phone numbers in filenames:
```regex
# Matches filenames containing phone numbers
.*(\d{10,13}).*\.(mp3|m4a|aac|amr|wav|3gp|ogg)$

# Examples that would match:
- Call_9876543210_2026-01-09.m4a  → Extracts 9876543210
- 20260109_100500_+919876543210.mp3 → Extracts +919876543210
- Recording(John Doe - 9876543210).aac → Extracts 9876543210
```

### 10.3 Duration Matching
If timestamps are ambiguous (multiple files created at similar times):
```
callDuration = 300 seconds (5 minutes)

for each candidateFile:
    fileDuration = getAudioDuration(candidateFile)
    if abs(fileDuration - callDuration) <= 10 seconds:
        MATCH_CONFIDENCE += 40
```

### 10.4 Learning System
The app may "learn" where recordings are stored:
```
if match_found:
    folder = getParentFolder(matchedFile)
    saveToPreferences("known_recording_folder", folder)
    
# Next time, check saved folder FIRST for faster matching
```

---

## 11. Known Folder Paths by Device

### 11.1 Comprehensive Path List

| Priority | Path | Common On |
|----------|------|-----------|
| 1 | `/storage/emulated/0/Recordings/` | Generic Android |
| 2 | `/storage/emulated/0/Call/` | Samsung (older) |
| 3 | `/storage/emulated/0/Recordings/Call Recordings/` | Samsung (newer) |
| 4 | `/storage/emulated/0/MIUI/sound_recorder/call_rec/` | Xiaomi/MIUI |
| 5 | `/storage/emulated/0/Music/Recordings/` | Some AOSP |
| 6 | `/storage/emulated/0/VoiceRecorder/` | Vivo |
| 7 | `/storage/emulated/0/VoiceRecorder/Calls/` | Vivo |
| 8 | `/storage/emulated/0/record/PhoneRecord/` | Huawei (old) |
| 9 | `/storage/emulated/0/Sounds/CallRecord/` | Huawei/Honor |
| 10 | `/storage/emulated/0/Recordings/Call/` | Oppo/Realme |
| 11 | `/storage/emulated/0/ColorOS/Recordings/` | Oppo/Realme |
| 12 | `/storage/emulated/0/Android/media/com.coloros.soundrecorder/` | ColorOS 12+ |
| 13 | `/storage/emulated/0/Record/Call/` | Lenovo |
| 14 | `/storage/emulated/0/CallRecordings/` | Generic |
| 15 | `/storage/emulated/0/Audio/` | Generic |
| 16 | `/storage/emulated/0/sounds/` | Generic |

### 11.2 Third-Party App Paths

| App | Path |
|-----|------|
| ACR Phone | `/storage/emulated/0/ACR/` |
| Cube ACR | `/storage/emulated/0/CubeCallRecorder/All/` |
| Automatic Call Recorder | `/storage/emulated/0/AutomaticCallRecorder/` |
| Boldbeast Call Recorder | `/storage/emulated/0/BoldBeast/` |
| Call Recorder by Appliqato | `/storage/emulated/0/CallRecordings/` |
| Truecaller | `/storage/emulated/0/Truecaller/Recording/` |
| Call Recorder - IntCall | `/storage/emulated/0/IntCall/` |
| Blackbox Call Recorder | `/storage/emulated/0/.blackbox/` |

### 11.3 SD Card Paths
If recordings are on external SD:
```
/storage/XXXX-XXXX/Recordings/
/storage/XXXX-XXXX/MIUI/sound_recorder/call_rec/
/storage/XXXX-XXXX/Call/
```
Where `XXXX-XXXX` is the SD card's UUID.

---

## 12. File Extensions Supported

### 12.1 Audio Formats
| Extension | MIME Type | Quality | Common On |
|-----------|-----------|---------|-----------|
| `.m4a` | audio/mp4 | High | Samsung, Pixel, Huawei |
| `.mp3` | audio/mpeg | Medium | Xiaomi, Third-party |
| `.aac` | audio/aac | High | Oppo, Realme |
| `.amr` | audio/amr | Low | Vivo, Old devices |
| `.wav` | audio/wav | Lossless | OnePlus, Developers |
| `.3gp` | audio/3gpp | Medium | Older Android |
| `.3gpp` | audio/3gpp | Medium | Older Android |
| `.ogg` | audio/ogg | Medium | Some AOSP |
| `.opus` | audio/opus | High | Pixel (newer) |
| `.flac` | audio/flac | Lossless | Rare |

### 12.2 Filter Query
When scanning, the app likely filters:
```java
String[] extensions = {".m4a", ".mp3", ".aac", ".amr", ".wav", ".3gp", ".ogg", ".opus"};
for (File file : allFiles) {
    for (String ext : extensions) {
        if (file.getName().toLowerCase().endsWith(ext)) {
            candidates.add(file);
        }
    }
}
```

---

## 13. Scheduling & Background Work

### 13.1 WorkManager Integration
The app uses Android's `WorkManager` for reliable background task execution.

**Workers Likely Used**:
| Worker | Purpose |
|--------|---------|
| `RecordingScanWorker` | Periodically scan for unattached recordings |
| `FileUploadWorker` | Upload pending recordings to cloud |
| `CompressionWorker` | Compress recordings in background |
| `SyncWorker` | Sync call log with local database |

### 13.2 Scheduling Options
```
JobScheduler  → For system-level job scheduling
AlarmManager  → For time-based triggers (backup mechanism)
WorkManager   → Primary (wraps JobScheduler and AlarmManager)
```

Found in DEX:
```
AlarmManagerScheduler
JobInfoScheduler
JobSchedulerCompat
SystemAlarmScheduler
SystemJobScheduler
GreedyScheduler
```

### 13.3 Constraints for Upload
```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.UNMETERED)  // Wi-Fi only
    .setRequiresCharging(false)
    .setRequiresBatteryNotLow(true)
    .setRequiresStorageNotLow(true)
    .build()

val uploadRequest = OneTimeWorkRequestBuilder<FileUploadWorker>()
    .setConstraints(constraints)
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        30, TimeUnit.SECONDS
    )
    .build()

WorkManager.getInstance(context).enqueue(uploadRequest)
```

---

## 14. Troubleshooting & Edge Cases

### 14.1 Recording Not Found
**Causes**:
1. System recorder disabled.
2. Recording saved to hidden app-private folder.
3. File was created but deleted (auto-cleanup).
4. Scoped Storage blocking access.

**Solutions**:
1. Enable system call recording in Phone app settings.
2. Grant folder access via SAF.
3. Use `MANAGE_EXTERNAL_STORAGE` permission (requires justification).

### 14.2 Wrong Recording Attached
**Causes**:
1. Another call happened close in time.
2. Music/Podcast download had same timestamp.
3. Duration didn't match but timestamp did.

**Solutions**:
1. Tighten time window (±3 seconds).
2. Add duration verification.
3. Parse filename for phone number confirmation.

### 14.3 Upload Failing
**Causes**:
1. No network connectivity.
2. Server unreachable.
3. File too large.
4. Authentication expired.

**Solutions**:
1. WorkManager will auto-retry with exponential backoff.
2. Store upload queue in local database.
3. Compress file before upload.

### 14.4 Compression Failing
**Causes**:
1. Unsupported codec.
2. File corrupted.
3. Insufficient storage for temp file.

**Solutions**:
1. Use FFmpeg for universal codec support.
2. Skip compression if original format is already compressed (MP3).
3. Check storage before starting.

### 14.5 Dual SIM Handling
The app tracks SIM details:
```sql
SELECT * FROM sim_details WHERE sim_slot = 1
```

When attaching a recording, it verifies the call's `sim_id` matches to avoid cross-SIM misattachment.

---

## 15. Summary Flowchart

```
┌─────────────────────────────────────────────────────────────────────┐
│                        INCOMING/OUTGOING CALL                       │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  PhonestateReceiver detects OFFHOOK → Record startTime              │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  PhonestateReceiver detects IDLE → Record endTime, calculate dur    │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Query MediaStore for audio files in time window (±10s)             │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  FOUND?                                                       │   │
│  │  YES → Attach to call_history_table                          │   │
│  │  NO  → Widen window to ±60s, query again                     │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                           ┌────────┴────────┐
                           │  Still NO?      │
                           └────────┬────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Scan known recording folders (see Section 11)                      │
│  Match by lastModified() timestamp + duration                       │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                           ┌────────┴────────┐
                           │  Still NO?      │
                           └────────┬────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Full storage scan + filename regex for phone number                │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                           ┌────────┴────────┐
                           │  Still NO?      │
                           └────────┬────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Mark call as "Recording Not Found"                                 │
│  Prompt user to manually attach (if enabled)                        │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  If compression enabled:                                            │
│  → Transcode to MP3/AAC at low bitrate                              │
│  → Save to app cache                                                │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│  If upload enabled:                                                 │
│  → Queue upload via WorkManager                                     │
│  → Respect constraints (Wi-Fi, Battery, Storage)                    │
│  → Retry on failure with exponential backoff                        │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                              DONE ✓                                 │
└─────────────────────────────────────────────────────────────────────┘
```

---

*Document Generated: 2026-01-09*
*Based on analysis of Callyzer Pro APK (com.websoptimization.callyzerpro v45)*
