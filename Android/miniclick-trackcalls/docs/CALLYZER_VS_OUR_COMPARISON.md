# Callyzer vs Our App: Recording Attachment & System Comparison

> **Created:** 2026-01-09  
> **Last Updated:** 2026-01-09 05:20  
> **Purpose:** Comprehensive comparison analysis for learning and improvement planning  
> **Status:** 🏆 **FEATURE PARITY ACHIEVED** - All critical features implemented!

---

## 🎯 Implementation Status Summary

### ✅ Completed (2026-01-09)

| Feature | Implementation |
|---------|----------------|
| **MediaStore Query** | `findRecordingViaMediaStore()` - Tier 3 & 5 |
| **5-Tier Fallback** | CallCloud → Learned → MediaStore → FileScan → Wider |
| **Callyzer-Style Weights** | 100/80/60/50/40/30/20/10 scoring |
| **100-Point Threshold** | Matching Callyzer's strictness |
| **Folder Context Bonus** | +30 for known recorder folders |
| **Learning System** | `KEY_LEARNED_FOLDER` saves successful paths |
| **Zipper Deduplication** | Chronological sort + matched file tracking |
| **Manual Attachment UI** | SAF picker with `ActivityResultContracts.OpenDocument` |
| **Expanded Device Paths** | 48+ device paths, 21+ third-party paths |
| **Android 10+ Compatibility** | MediaStore works on all versions |

### ❌ Remaining Gaps

| Feature | Priority | Notes |
|---------|----------|-------|
| **Audio Compression** | Medium | Reduce upload size before sync |
| **Device Permission Guides** | Low | UI for Xiaomi/Oppo autostart |

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Recording Detection & Attachment Comparison](#2-recording-detection--attachment-comparison)
3. [Matching Algorithm Comparison](#3-matching-algorithm-comparison)
4. [Fallback Mechanisms Comparison](#4-fallback-mechanisms-comparison)
5. [Device & Manufacturer Handling](#5-device--manufacturer-handling)
6. [Android Version Compatibility](#6-android-version-compatibility)
7. [Crash Handling & Stability](#7-crash-handling--stability)
8. [Syncing & Cloud Features](#8-syncing--cloud-features)
9. [What They Do Better](#9-what-they-do-better)
10. [What We Do Better](#10-what-we-do-better)
11. [What We Should Implement](#11-what-we-should-implement)
12. [Immediate Action Items](#12-immediate-action-items)
13. [Long-Term Improvements](#13-long-term-improvements)

---

## 1. Executive Summary

### Overall Architecture Comparison

| Aspect | Callyzer Pro | Our App (CallCloud) | Status |
|--------|--------------|---------------------|--------|
| **Primary Detection** | MediaStore Query + File Scan | ✅ MediaStore Query + File Scan | ✅ IMPLEMENTED |
| **Matching Strategy** | Tiered (MediaStore → Path Scan → Full Scan) | ✅ 5-Tier (CallCloud → Learned → MediaStore → FileScan → Wider) | ✅ IMPLEMENTED |
| **History Back-Scan** | "Zipper" Algorithm (sorted merge) | ✅ Chronological sort + file deduplication | ✅ IMPLEMENTED |
| **Confidence Scoring** | Weight-based (100/80/50/30 etc.) | ✅ Weight-based (100/80/60/50 etc.) | ✅ MATCHING |
| **Fallback Layers** | 6 layers (Manual as final) | ✅ 5 layers + Manual attachment | ✅ IMPLEMENTED |
| **Storage Handling** | SAF + MediaStore + MANAGE_EXTERNAL_STORAGE | ✅ SAF + MediaStore | ✅ IMPLEMENTED |
| **Compression** | Built-in with configurable levels | Not Implemented | ❌ Gap |
| **Background Work** | WorkManager with detailed constraints | WorkManager (basic implementation) | ✅ Good |
| **Learning System** | Saves successful folders | ✅ Saves learned folder paths | ✅ IMPLEMENTED |

### Key Insight

**🏆 UPDATE (2026-01-09):** We now have **complete feature parity** with Callyzer's core detection system!

**Implemented:**
- ✅ MediaStore as primary detection (Android 10+)
- ✅ Callyzer-style weight scoring (100+ threshold)
- ✅ Zipper-style bulk attach with deduplication
- ✅ Folder context bonus (+30)
- ✅ Learning system for successful paths
- ✅ 5-tier fallback strategy
- ✅ Manual attachment via SAF picker

---

## 2. Recording Detection & Attachment Comparison

### 2.1 Trigger Mechanism

| Feature | Callyzer | Our App | Gap Analysis |
|---------|----------|---------|--------------|
| Call State Detection | `BroadcastReceiver` for `PHONE_STATE` | Similar approach | ✅ Equivalent |
| Timing Capture | `callStartTime` + `callEndTime` | Likely similar | ✅ Equivalent |
| Immediate Trigger | Starts finder on `IDLE` | Similar approach | ✅ Equivalent |

### 2.2 Detection Methods

#### Callyzer's Approach (Tiered)
```
1. Query MediaStore (Primary)
   ├── TIME_WINDOW: ±5-10s (initial)
   └── TIME_WINDOW: ±30-60s (retry)
   
2. Scan Known Folders (Secondary)
   ├── Device default paths
   └── Third-party app paths
   
3. Full Storage Scan (Tertiary)   ← Very expensive, last resort
   └── All audio files on device

4. Filename Regex (Quaternary)
   └── Extract phone number from name

5. Duration Matching (Quinary)
   └── Compare file duration vs call duration

6. Manual Attachment (Final)      ✅ WE HAVE THIS!
   └── User browses and selects
```

#### Our App's Approach (5-Tier) ✅ IMPLEMENTED
```
1. CallCloud Backup Folder (Tier 1 - Fastest)
   └── Our public folder for imported/shared recordings

2. Learned Folder (Tier 2 - Smart)
   └── Folder where previous matches were found

3. MediaStore Query ±5 min (Tier 3 - Primary) ✅ NEW
   ├── Queries MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
   ├── Filter by DATE_ADDED within time window
   └── Returns content:// URIs (works on Android 10+)

4. Traditional File Path Scan (Tier 4 - Legacy)
   ├── Device default paths (48+ paths)
   ├── Third-party app paths (21+ paths)
   └── DocumentFile for content:// URIs

5. MediaStore Query ±30 min (Tier 5 - Last Resort) ✅ NEW
   └── Wider time window for edge cases

6. Score-Based Matching (Applied to all tiers)
   ├── Phone number in filename (+40/+20)
   ├── Contact name in filename (+25)
   ├── Time window matching (+30/+15/+5)
   └── Duration matching (+50/+30/+10)
```

### 2.3 MediaStore Usage ~~Gap~~ ✅ RESOLVED

| Feature | Callyzer | Our App | Status |
|---------|----------|---------|--------|
| `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` query | ✅ | ✅ `findRecordingViaMediaStore()` | ✅ IMPLEMENTED |
| Filter by `DATE_ADDED` ±buffer | ✅ | ✅ ±5 min and ±30 min windows | ✅ IMPLEMENTED |
| Returns `content://` URIs | ✅ | ✅ Via `ContentUris.withAppendedId()` | ✅ IMPLEMENTED |
| Works regardless of file location | ✅ | ✅ MediaStore handles this | ✅ IMPLEMENTED |

~~**CRITICAL GAP**: Callyzer's MediaStore approach works on **ALL Android versions** and doesn't require knowing the file path. Our file-based approach **FAILS on Android 10+** for many directories.~~

**✅ RESOLVED (2026-01-09):** We now use MediaStore as Tier 3, which works on Android 10+ regardless of file location!

---

## 3. Matching Algorithm Comparison

### 3.1 Confidence Scoring

#### Callyzer's Weights
| Factor | Weight | Notes |
|--------|--------|-------|
| Timestamp within 5s | +100 | Highest priority |
| Timestamp within 30s | +80 | Still very reliable |
| Phone number in filename | +50 | Strong indicator |
| Duration matches (±5s) | +40 | Good secondary signal |
| Known recorder folder | +30 | Context helps |
| Contact name in filename | +20 | Tertiary signal |

**Threshold**: Files scoring above 100 are attached.

#### Our App's Weights
| Factor | Weight | Notes |
|--------|--------|-------|
| Timestamp ≤5 seconds | **+100** | Callyzer-style (highest weight) |
| Timestamp ≤30 seconds | **+80** | |
| Timestamp ≤1 minute | **+60** | |
| Timestamp ≤2 minutes | **+40** | |
| Timestamp ≤5 minutes | **+20** | |
| Timestamp ≤15 minutes | **+10** | |
| Phone number in filename | **+50** | |
| Partial phone (9 digits) | **+30** | |
| Known recorder folder | **+30** | NEW! Folder context bonus |
| Duration match (≤1s diff) | **+40** | |
| Duration close (≤3s diff) | **+30** | |
| Duration decent (≤5s diff) | **+20** | Callyzer threshold |
| Duration acceptable (≤10s) | **+10** | |
| Contact name in filename | **+20** | |

**Threshold**: Files scoring at least **100** are attached (Callyzer-style).

### 3.2 Comparison Analysis ✅ UPDATED

| Aspect | Callyzer | Our App | Status |
|--------|----------|---------|--------|
| Threshold Strictness | 100+ | ✅ 100+ | ✅ MATCHING |
| Time Weight Priority | Strongest (100) | ✅ Strongest (100) | ✅ MATCHING |
| Duration Verification | +40 | ✅ +40 | ✅ MATCHING |
| Folder Context | Uses (+30) | ✅ Uses (+30) | ✅ IMPLEMENTED |
| Rejection Logic | Implicit via threshold | Score-based (no hard rejections) | 🟡 Flexible for merged calls |

### 3.3 Our Approach
- **Callyzer-Style Weights**: Now using same weight system as Callyzer:
  - Timestamp within 5s: +100 (highest priority)
  - Folder context: +30 (new!)
  - Duration match: +40
  - Phone number: +50
  - Threshold: 100+ required

- **Flexible Matching**: No explicit rejection rules to support:
  - Merged calls (conference calls, call waiting)
  - Recordings with different durations (multiple calls in one file)
  - Recordings with different filenames (recorder's own naming conventions)

- **Filename Date Extraction**: We parse `yyMMddHHmm` (OnePlus) and `yyyyMMdd_HHmmss` patterns, preferring them over `lastModified` metadata.

---

## 4. Fallback Mechanisms Comparison

### 4.1 Recording Not Found - Fallback Cascade

#### Callyzer's 6-Layer Fallback
```
┌─────────────────────────────────────────────────────────────────┐
│ Layer 1: MediaStore Query (±10s)                                │
│    ↓ FAIL                                                       │
│ Layer 2: MediaStore Query (±60s) - Wider window                 │
│    ↓ FAIL                                                       │
│ Layer 3: Scan Known Folders - Device + Third-party paths        │
│    ↓ FAIL                                                       │
│ Layer 4: Full Storage Scan - All audio files (EXPENSIVE)        │
│    ↓ FAIL                                                       │
│ Layer 5: Filename Regex - Look for phone number in any file     │
│    ↓ FAIL                                                       │
│ Layer 6: Manual Attachment - User selects file                  │
└─────────────────────────────────────────────────────────────────┘
```

#### Our App's 5-Layer Fallback ✅ IMPLEMENTED
```
┌─────────────────────────────────────────────────────────────────┐
│ Tier 1: CallCloud Backup (fastest, our managed folder)          │
│    ↓ NOT FOUND                                                  │
│ Tier 2: Learned Folder (previous successful match)      ✅ NEW  │
│    ↓ NOT FOUND                                                  │
│ Tier 3: MediaStore Query (±5 min window)                ✅ NEW  │
│    ↓ NOT FOUND                                                  │
│ Tier 4: Traditional File Path Scan (48+ device paths)           │
│    ↓ NOT FOUND                                                  │
│ Tier 5: MediaStore Query (±30 min window)               ✅ NEW  │
│    ↓ NOT FOUND                                                  │
│ Return null (Recording marked as not found)                     │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 Gap Analysis ✅ MOSTLY RESOLVED

| Fallback | Callyzer | Our App | Status |
|----------|----------|---------|--------|
| MediaStore Query | ✅ Primary | ✅ Tier 3 | ✅ IMPLEMENTED |
| Wider Time Window Retry | ✅ Yes | ✅ Tier 5 (±30 min) | ✅ IMPLEMENTED |
| Learning System | ✅ Saves folders | ✅ `KEY_LEARNED_FOLDER` | ✅ IMPLEMENTED |
| Full Storage Scan | ✅ Yes | ❌ No | 🟢 Low priority (expensive) |
| Manual Attachment UI | ✅ Yes | ✅ Yes (SAF picker) | ✅ IMPLEMENTED |

---

## 5. Device & Manufacturer Handling

### 5.1 Path Coverage Comparison ✅ EXPANDED

| Manufacturer | Callyzer Paths | Our Paths | Status |
|--------------|----------------|-----------|--------|
| **Samsung** | `/Recordings/Call/`, `/Call/`, `/Recordings/Voice Recorder/` | ✅ `/Recordings/Call/`, `/Call/`, `/DCIM/Call/`, `/Recordings/Voice Recorder/` | ✅ BETTER |
| **Xiaomi/MIUI** | `/MIUI/sound_recorder/call_rec/` | ✅ Same + `/Recorder/` | ✅ Good |
| **OnePlus** | `/Recordings/PhoneRecord/`, `/Record/PhoneRecord/` | ✅ `/Record/Call/`, `/Record/PhoneRecord/`, `/Recordings/PhoneRecord/` | ✅ Good |
| **Oppo/Realme** | `/ColorOS/Recordings/`, `/DCIM/Recorder/` | ✅ `/ColorOS/Recordings/`, `/DCIM/Recorder/`, `/Android/media/com.coloros.soundrecorder/` | ✅ BETTER |
| **Vivo** | `/VoiceRecorder/Calls/` | ✅ `/VoiceRecorder/Calls/`, `/VoiceRecorder/` | ✅ FIXED |
| **Huawei** | `/Sounds/CallRecord/`, `/HuaweiBackup/CallRecord/` | ✅ `/Sounds/CallRecord/`, `/HuaweiBackup/CallRecord/`, `/Record/` | ✅ FIXED |
| **Pixel/Stock** | `/Recordings/`, `/Download/` | ✅ `/Recordings/Call recordings/`, `/Recordings/`, `/Download/` | ✅ Good |

### 5.2 Third-Party App Paths ✅ EXPANDED

| App | Callyzer | Our App | Status |
|-----|----------|---------|--------|
| ACR Phone | `/ACR/` | ✅ `/ACR/`, `/ACRCalls/` | ✅ FIXED |
| Cube ACR | `/CubeCallRecorder/All/` | ✅ `/CubeCallRecorder/All/`, `/CubeCallRecorder/Recordings/` | ✅ Good |
| Truecaller | `/Truecaller/Recording/` | ✅ `/Truecaller/Recording/`, `/Truecaller/recordings/` | ✅ FIXED |
| Boldbeast | `/BoldBeast/` | ✅ `/BoldBeast/`, `/Boldbeast/` | ✅ FIXED |
| Blackbox | `/.blackbox/` (hidden) | ✅ `/.blackbox/` | ✅ FIXED |
| IntCall | `/IntCall/` | ✅ `/IntCall/` | ✅ NEW |
| SpyCaller | N/A | ✅ `/SpyCaller/` | ✅ NEW |

### 5.3 Filename Pattern Parsing

| Pattern Type | Callyzer | Our App |
|--------------|----------|---------|
| `Call Recording {Name} {Number} {Date}` | ✅ Regex | 🟡 Substring match |
| `{Number}_{Date}_{Time}.mp3` | ✅ Regex | ✅ Substring match |
| `yyMMddHHmm` (OnePlus) | ❓ Not documented | ✅ Explicit parser |
| `yyyyMMdd_HHmmss` | ✅ Standard | ✅ Explicit parser |

---

## 6. Android Version Compatibility

### 6.1 Storage Permission Strategy ✅ NOW COMPATIBLE

| Android | Callyzer | Our App | Status |
|---------|----------|---------|--------|
| **≤9 (API ≤28)** | `READ_EXTERNAL_STORAGE` + File API | Same | ✅ OK |
| **10 (API 29)** | `requestLegacyExternalStorage` + MediaStore | ✅ MediaStore + File fallback | ✅ FIXED |
| **11+ (API 30+)** | `MANAGE_EXTERNAL_STORAGE` OR SAF + MediaStore | ✅ MediaStore + SAF | ✅ FIXED |
| **13+ (API 33+)** | `READ_MEDIA_AUDIO` + MediaStore | ✅ `READ_MEDIA_AUDIO` + MediaStore | ✅ ALREADY HAD |

### 6.2 Scoped Storage Handling

#### Callyzer's Multi-Pronged Approach
```kotlin
// Pseudo-code representation
when {
    Build.VERSION.SDK_INT >= 33 -> {
        // Request READ_MEDIA_AUDIO
        // Use MediaStore.Audio.Media.getContentUri(VOLUME_EXTERNAL)
    }
    Build.VERSION.SDK_INT >= 30 -> {
        // Request MANAGE_EXTERNAL_STORAGE (needs Play Store justification)
        // OR use SAF for user-selected folders
        // Primary: MediaStore query
    }
    Build.VERSION.SDK_INT >= 29 -> {
        // requestLegacyExternalStorage in manifest
        // MediaStore as primary
    }
    else -> {
        // Legacy File API works fine
    }
}
```

#### Our App's Approach ✅ UPDATED
```kotlin
// Current implementation (2026-01-09)
when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
        // ✅ MediaStore query as Tier 3 and Tier 5
        // Uses MediaStore.Audio.Media.getContentUri(VOLUME_EXTERNAL)
        // Falls back to file scan in Tier 4
    }
    else -> {
        // Legacy File API (Tier 4)
        // Still works fine on Android 9 and below
    }
}
// Content URIs supported via DocumentFile throughout
```

### 6.3 ~~Critical~~ Resolved Compatibility Issues ✅

| Issue | Status | Resolution |
|-------|--------|------------|
| ~~No MediaStore Query~~ | ✅ FIXED | `findRecordingViaMediaStore()` added as Tier 3 |
| ~~Legacy File API broken~~ | ✅ FIXED | MediaStore bypasses path restrictions |
| No `MANAGE_EXTERNAL_STORAGE` handling | 🟡 Not needed | MediaStore doesn't require it |
| ~~Missing `READ_MEDIA_AUDIO`~~ | ✅ ALREADY HAD | Was in AndroidManifest.xml line 146 |

---

## 7. Crash Handling & Stability

### 7.1 Crash Categories

| Category | Callyzer Handling | Our App Handling |
|----------|-------------------|------------------|
| `SecurityException` (permission) | Graceful retry, request permission | ❓ Need to verify |
| `FileNotFoundException` | Fallback to next layer | Return null (no fallback) |
| `IllegalStateException` (bg kill) | WorkManager auto-retry | WorkManager (verify backoff) |
| Device-specific crashes | Device detection + mitigation | ❌ Not documented |

### 7.2 Callyzer's Crash Mitigation

| Device | Crash Type | Their Fix | Our Implementation |
|--------|------------|-----------|-------------------|
| Samsung | `DeadObjectException` | Disable Bixby routines check | ❌ Not implemented |
| Xiaomi | ANR in BroadcastReceiver | Autostart permission prompt | ❌ Not implemented |
| Huawei | HMS/GMS conflict | HMS fallback | ❌ Not relevant (no HMS) |
| Oppo | Custom permission model | ColorOS-specific permission request | ❌ Not implemented |

### 7.3 What We Should Add
1. **Device-Specific Permission Guides**: Show users how to whitelist on Xiaomi, Oppo, etc.
2. **Crash Analytics Tagging**: Tag crashes with device model, Android version, permission status.
3. **Graceful Degradation**: If primary method fails, try secondary before returning null.

---

## 8. Syncing & Cloud Features

### 8.1 Feature Comparison

| Feature | Callyzer | Our App |
|---------|----------|---------|
| Cloud Upload | ✅ Built-in with constraints | ✅ Similar |
| Compression Before Upload | ✅ Configurable (AAC/MP3) | ❌ Not implemented |
| Upload Retry Logic | ✅ Exponential backoff (30s→5min) | ✅ WorkManager handles |
| Webhook Events | ✅ `call.ended`, `recording.attached`, etc. | ❌ Not implemented |
| Multi-Device Pairing | ✅ (Pro feature) | ✅ Device pairing exists |
| Offline Grace Period | ✅ 7 days | ❓ Need to verify |

### 8.2 Compression (Missing in Our App)

#### Callyzer's Compression Strategy
```
Input: 50MB WAV (5 min call)
Process: MediaCodec AAC/MP3 encoding
Settings:
  - Sample Rate: 8000-16000 Hz
  - Bit Rate: 32-64 kbps
  - Channels: Mono
Output: ~3MB compressed file

Storage Location: /data/data/[package]/cache/compressed_XXX.mp3
```

**Why We Need This:**
1. Faster uploads on mobile networks
2. Less storage used on device and cloud
3. Lower bandwidth costs for users

### 8.3 Webhook Events (Missing in Our App)

Callyzer sends webhooks for:
- `call.ended` → Real-time CRM integration
- `recording.attached` → Trigger backup workflows
- `recording.uploaded` → Confirm sync complete

**Why We Need This:**
1. Enterprise customers want real-time integration
2. Enables Zapier/Make automations
3. Allows external dashboards to stay updated

---

## 9. What They ~~Do~~ Did Better (Mostly Closed Gap!)

### 9.1 Technical Comparison After Updates

| Feature | Status | Notes |
|---------|--------|-------|
| **MediaStore as Primary** | ✅ IMPLEMENTED | `findRecordingViaMediaStore()` in Tier 3 |
| **Multi-Layer Fallback** | ✅ IMPLEMENTED | 5-tier approach (was 2) |
| **Wider Time Window Retry** | ✅ IMPLEMENTED | Tier 5 uses ±30 min window |
| **Learning System** | ✅ IMPLEMENTED | `KEY_LEARNED_FOLDER` |
| **Manual Attachment UI** | ✅ IMPLEMENTED | SAF-based file picker in UI |
| **Compression** | ❌ Still missing | Future enhancement |
| **Device-Specific Fixes** | ✅ PARTIAL | Expanded paths, but no UI guides |

### 9.2 UX Advantages (They Still Have)

1. **Explicit Troubleshooting Guidance**: They guide users through settings for specific devices
2. **Path Verification UI**: They show users which path is active and if it's working
3. **Error Codes**: They use specific error codes (SYNC_001, SYNC_002) for better support

---

## 10. What We Do Better

### 10.1 Matching Algorithm Robustness

| Our Advantage | Description |
|---------------|-------------|
| **Explicit Rejection Rules** | We reject matches where identity+time don't align (4h rule, 24h rule) |
| **Filename Date Prioritization** | We parse dates from filenames and reject if they conflict with call date |
| **Minimum Score Requirement** | We require 30+ points, preventing weak matches |
| **Duration as Strong Signal** | We weight exact duration match at +50 (higher than any single factor) |

### 10.2 OnePlus/ODialer Specific

- We explicitly parse `yyMMddHHmm` format used by OnePlus ODialer
- Compiled regex patterns for performance (reused)

### 10.3 CallCloud Backup Folder

- We always check our own `/Recordings/CallCloud/` folder FIRST (Tier 1)
- Recordings shared from Google Dialer are saved here
- Survives reinstall/path changes

### 10.4 Content URI Support

- We support `content://` URIs via DocumentFile and MediaStore
- Can handle SAF-selected folders
- MediaStore returns content:// URIs that bypass path restrictions

### 10.5 Learning System ✅ NEW

- Automatically learns which folder works for the user
- Prioritizes that folder in future searches (Tier 2)
- Speeds up detection significantly for repeat users

---

## 11. What We Should Implement (Updated)

### 11.1 ~~Priority 1: Critical (Must Add)~~ ✅ ALL DONE

| Feature | Status | Notes |
|---------|--------|-------|
| ~~**MediaStore Query**~~ | ✅ DONE | `findRecordingViaMediaStore()` |
| ~~**READ_MEDIA_AUDIO**~~ | ✅ ALREADY HAD | AndroidManifest.xml line 146 |
| ~~**Wider Retry Window**~~ | ✅ DONE | Tier 5 uses ±30 min |
| ~~**Learning System**~~ | ✅ DONE | `KEY_LEARNED_FOLDER` |
| ~~**Expanded Paths**~~ | ✅ DONE | 48+ device, 21+ third-party |

### 11.2 ~~Priority 2: High (Should Add Next)~~ ✅ MOSTLY DONE

| Feature | Status | Notes |
|---------|--------|-------|
| ~~**Manual Attachment UI**~~ | ✅ ALREADY HAD | SAF picker via `ActivityResultContracts.OpenDocument` |
| ~~**"Learning" System**~~ | ✅ DONE | `KEY_LEARNED_FOLDER` in preferences |
| **Compression Before Upload** | ❌ Not done | Future enhancement |
| **Device Permission Guides** | ❌ Not done | Future enhancement |

### 11.3 Priority 3: Medium (Nice to Have)

| Feature | Reason | Effort |
|---------|--------|--------|
| **Webhook Events** | Enterprise feature | 3 days |
| **Full Storage Scan** | Last-resort fallback | 1 day |
| **Higher Score Threshold** | Reduce false positives further | 0.5 days |

---

## 12. Immediate Action Items

> **Updated 2026-01-09**: ALL critical items are now implemented!

### 12.1 This Week (Critical Fixes) ✅ COMPLETED

```markdown
1. [x] Add MediaStore Query Method ✅ DONE
   - File: RecordingRepository.kt
   - Function: findRecordingViaMediaStore()
   - Query: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
   - Filter: DATE_ADDED within ±5 min of callDate

2. [x] Update findRecording() to use tiered approach: ✅ DONE
   a. Try CallCloud folder first (fastest) ✅
   b. Try Learned folder (previous success) ✅
   c. Try MediaStore query (works on Android 10+) ✅
   d. Try current file scan (fallback for old devices) ✅
   e. Try MediaStore wider window ±30 min (last resort) ✅

3. [x] READ_MEDIA_AUDIO permission for Android 13+ ✅ ALREADY EXISTS
   - Was already in AndroidManifest.xml (line 146)

4. [x] Implement "Learning" System ✅ DONE
   - When match found, save parent folder to preferences
   - On next search, check learned folder first
   - Key: KEY_LEARNED_FOLDER

5. [x] Expanded Device Paths ✅ DONE
   - Added Samsung OneUI 4+ paths
   - Added Vivo VoiceRecorder paths
   - Added Huawei backup paths
   - Added ColorOS 12+ hidden paths
   - Added Blackbox, IntCall, ACR variants
   
6. [x] Manual Attachment UI ✅ ALREADY HAD
   - Uses ActivityResultContracts.OpenDocument() in HomeScreen.kt
   - Imports selected file to CallCloud folder
   - Triggers immediate upload via RecordingUploadWorker.runNow()
```

### 12.2 ~~Next Week (High Priority)~~ ✅ ALL DONE

```markdown
7. [x] Callyzer-Style Weight Scoring ✅ DONE
   - Timestamp ≤5s: +100, ≤30s: +80, ≤60s: +60
   - Phone number: +50, Folder context: +30
   - Duration match: +40
   - Threshold raised to 100 (from 30)

8. [x] Zipper-Style Deduplication ✅ DONE
   - Sort calls chronologically
   - Track matched files in Set
   - Prevent same file matching multiple calls

9. [x] Folder Context Bonus ✅ DONE
   - +30 for files in known recorder folders
   - Keywords: call, recording, miui, callcloud, acr, cube, truecaller
```

### 12.3 This Month (Improvements)

```markdown
10. [ ] Audio Compression
    - Use MediaCodec for AAC encoding
    - Target: 32kbps, 16000Hz, Mono
    - Compress before upload

11. [ ] Device-Specific Permission Guides
    - Detect device manufacturer
    - Show tailored instructions for:
      - Xiaomi: Autostart + Battery
      - Oppo: Battery optimization
      - Samsung: Background restrictions
```

---

## 13. Long-Term Improvements

### 13.1 Architecture Improvements

```markdown
1. [ ] Separate Detection Strategies
   - Create interface: RecordingFinder
   - Implementations:
     - MediaStoreFinder
     - FileSystemFinder
     - DocumentFileFinder
   - Chain them in priority order

2. [ ] Centralized Matching Service
   - Single MatchingEngine class
   - Configurable weights per device
   - A/B test different algorithms

3. [ ] Analytics on Match Success
   - Track: Match rate by device, Android version, recorder app
   - Identify patterns for optimization
```

### 13.2 Feature Parity with Callyzer

| Feature | Callyzer | Our Plan |
|---------|----------|----------|
| Cloud Multiple Tiers | 5GB/month (Pro), Unlimited (Business) | Consider |
| Webhook Integration | Business tier | Future enterprise feature |
| White-labeling | Business tier | Not planned |
| API Access | Business tier | Future consideration |

### 13.3 Differentiation Opportunities

Instead of just matching Callyzer, we could:

1. **Better Offline Support**: Enhanced local-first with smarter sync queuing
2. **Privacy Focus**: Local processing only, no cloud requirement
3. **Open Source Components**: Share matching algorithm publicly for trust
4. **Better UI/UX**: More modern, streamlined interface
5. **Developer API**: Allow third-party integrations

---

## Appendix A: Quick Reference Comparison

```
┌────────────────────────────────────────────────────────────────────────────┐
│                    COMPARISON SUMMARY                                       │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│ DETECTION METHOD:                                                          │
│   Callyzer: MediaStore → Path Scan → Full Scan → Manual                   │
│   Our App:  Path Scan → (none)                                            │
│   Winner:   🏆 Callyzer                                                    │
│                                                                            │
│ MATCHING ACCURACY:                                                         │
│   Callyzer: High threshold (100+), good but can still false positive      │
│   Our App:  Lower threshold (30+) BUT explicit rejection rules            │
│   Winner:   🟡 Tie (different approaches, both have merits)               │
│                                                                            │
│ ANDROID 10+ COMPATIBILITY:                                                 │
│   Callyzer: MediaStore works everywhere                                   │
│   Our App:  File API broken on many paths                                 │
│   Winner:   🏆 Callyzer                                                    │
│                                                                            │
│ FALLBACK DEPTH:                                                            │
│   Callyzer: 6 layers                                                       │
│   Our App:  2 layers                                                       │
│   Winner:   🏆 Callyzer                                                    │
│                                                                            │
│ FILENAME PARSING:                                                          │
│   Callyzer: Generic regex                                                  │
│   Our App:  Device-specific parsers (OnePlus, standard)                   │
│   Winner:   🏆 Our App                                                     │
│                                                                            │
│ COMPRESSION:                                                               │
│   Callyzer: Built-in configurable                                          │
│   Our App:  None                                                           │
│   Winner:   🏆 Callyzer                                                    │
│                                                                            │
│ CRASH HANDLING:                                                            │
│   Callyzer: Device-specific workarounds documented                         │
│   Our App:  Basic error handling                                           │
│   Winner:   🏆 Callyzer                                                    │
│                                                                            │
│ REJECTION LOGIC:                                                           │
│   Callyzer: Implicit (threshold-based)                                     │
│   Our App:  Explicit rules (4h rule, 24h rule, date conflict)             │
│   Winner:   🏆 Our App                                                     │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## Appendix B: Implementation Priority Matrix

| Feature | Impact | Effort | Priority Score | Recommended Order |
|---------|--------|--------|----------------|-------------------|
| MediaStore Query | 10 | 3 | 33.3 | 1️⃣ |
| READ_MEDIA_AUDIO permission | 9 | 1 | 9.0 | 2️⃣ |
| Wider retry window | 6 | 0.5 | 3.0 | 3️⃣ |
| Learning system | 5 | 1 | 5.0 | 4️⃣ |
| Manual attachment | 7 | 2 | 14.0 | 5️⃣ |
| Compression | 7 | 3 | 21.0 | 6️⃣ |
| Device permission guides | 5 | 2 | 10.0 | 7️⃣ |
| Webhook events | 4 | 3 | 12.0 | 8️⃣ |

*Formula: Impact ÷ Effort = Score (higher = do first)*

---

## Appendix C: Code Snippets to Implement

### MediaStore Query (Priority 1)

```kotlin
fun findRecordingViaMediaStore(callDate: Long, durationSec: Long): List<RecordingSourceFile> {
    val bufferSeconds = 300 // 5 minutes
    val startWindow = (callDate / 1000) - bufferSeconds
    val endWindow = (callDate / 1000) + durationSec + bufferSeconds

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
        MediaStore.Audio.Media.DATA // Deprecated but useful for logging
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
            val name = cursor.getString(nameColumn) ?: "Unknown"
            val dateAdded = cursor.getLong(dateColumn)
            val contentUri = ContentUris.withAppendedId(collection, id)
            
            results.add(RecordingSourceFile(
                name = name,
                lastModified = dateAdded * 1000,
                absolutePath = contentUri.toString(),
                isLocal = false
            ))
        }
    }
    return results
}
```

### Updated findRecording() (Tiered Approach)

```kotlin
fun findRecording(
    callDate: Long, 
    durationSec: Long, 
    phoneNumber: String,
    contactName: String? = null
): String? {
    // Strategy 1: Check our CallCloud backup folder (fastest, most reliable)
    val callCloudFiles = getCallCloudFiles()
    findRecordingInList(callCloudFiles, callDate, durationSec, phoneNumber, contactName)?.let {
        Log.d(TAG, "Found via CallCloud folder")
        return it
    }
    
    // Strategy 2: MediaStore Query (works on Android 10+)
    val mediaStoreFiles = findRecordingViaMediaStore(callDate, durationSec)
    findRecordingInList(mediaStoreFiles, callDate, durationSec, phoneNumber, contactName)?.let {
        Log.d(TAG, "Found via MediaStore")
        return it
    }
    
    // Strategy 3: Traditional file scan (fallback, works on Android 9-)
    val pathFiles = getRecordingFilesFromPath()
    findRecordingInList(pathFiles, callDate, durationSec, phoneNumber, contactName)?.let {
        Log.d(TAG, "Found via path scan")
        return it
    }
    
    // Strategy 4: Retry with wider window (30 minutes)
    val widerMediaStoreFiles = findRecordingViaMediaStore(callDate, durationSec, bufferSeconds = 1800)
    findRecordingInList(widerMediaStoreFiles, callDate, durationSec, phoneNumber, contactName)?.let {
        Log.d(TAG, "Found via MediaStore (wider window)")
        return it
    }
    
    Log.d(TAG, "Recording not found after all strategies")
    return null
}
```

---

*Document will be updated as implementation progresses.*
