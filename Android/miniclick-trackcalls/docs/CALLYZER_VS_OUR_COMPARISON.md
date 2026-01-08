# Callyzer vs Our App: Recording Attachment & System Comparison

> **Created:** 2026-01-09  
> **Purpose:** Comprehensive comparison analysis for learning and improvement planning

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

| Aspect | Callyzer Pro | Our App (CallCloud) |
|--------|--------------|---------------------|
| **Primary Detection** | MediaStore Query + File Scan | File System Scan Only |
| **Matching Strategy** | Tiered (MediaStore → Path Scan → Full Scan) | Single-tier (Path Scan + Score) |
| **History Back-Scan** | "Zipper" Algorithm (sorted merge) | Not Explicitly Implemented |
| **Confidence Scoring** | Weight-based (100/80/50/30 etc.) | Score-based (40/30/25/10 etc.) |
| **Fallback Layers** | 6 layers (Manual as final) | 2-3 layers (No manual fallback) |
| **Storage Handling** | SAF + MediaStore + MANAGE_EXTERNAL_STORAGE | File API + Basic SAF |
| **Compression** | Built-in with configurable levels | Not Implemented |
| **Background Work** | WorkManager with detailed constraints | WorkManager (basic implementation) |

### Key Insight
**Callyzer uses a multi-layered detection strategy with MediaStore as the PRIMARY method**, while we rely primarily on direct file system scanning which is increasingly broken on Android 10+.

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
1. Query MediaStore (Primary)     ← WE DON'T HAVE THIS
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

6. Manual Attachment (Final)      ← WE DON'T HAVE THIS
   └── User browses and selects
```

#### Our App's Approach (Flat)
```
1. Scan Primary Path (Custom or Detected)
   ├── File.listFiles() - BROKEN on Android 11+
   └── DocumentFile for content:// URIs

2. Scan CallCloud Backup Folder
   └── Our public folder for imported recordings

3. Score-Based Matching
   ├── Phone number in filename
   ├── Contact name in filename
   ├── Time window matching
   └── Duration matching
```

### 2.3 MediaStore Usage Gap

| Callyzer | Our App |
|----------|---------|
| `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` query | ❌ Not implemented |
| Filter by `DATE_ADDED` ±buffer | ❌ Not implemented |
| Returns `content://` URIs | ❌ Not utilizing |
| Works regardless of file location | ❌ Path-dependent |

**CRITICAL GAP**: Callyzer's MediaStore approach works on **ALL Android versions** and doesn't require knowing the file path. Our file-based approach **FAILS on Android 10+** for many directories.

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
| Exact phone match | +40 | |
| Partial phone match (9 digits) | +20 | |
| Contact name match | +25 | |
| Date in filename match | +40 | |
| Perfect time (≤2 min) | +30 | |
| Good time (≤15 min) | +15 | |
| Acceptable time (≤1 hour) | +5 | |
| Duration exact (≤1s diff) | +50 | |
| Duration close (≤5s diff) | +30 | |
| Duration decent (≤10s diff) | +10 | |
| Duration mismatch (>1 min) | -20 | Penalty |

**Threshold**: Files scoring at least 30 are attached.

### 3.2 Comparison Analysis

| Aspect | Callyzer | Our App | Winner |
|--------|----------|---------|--------|
| Threshold Strictness | 100+ | 30+ | 🏆 Callyzer (less false positives) |
| Time Weight Priority | Strongest (100) | Strong but equal (40/30) | 🏆 Callyzer (more accurate) |
| Duration Verification | Secondary (40) | Primary (50) | 🟡 Depends on use case |
| Folder Context | Uses (+30) | Doesn't use | 🏆 Callyzer |
| Rejection Logic | Implicit via threshold | Explicit (4h/24h rules) | 🏆 Our App (more robust) |

### 3.3 Our Unique Strengths
- **Explicit Rejection Rules**: We have specific rules like:
  - "No identity match + >5 min time diff → REJECT"
  - "Identity match but >4 hours + no date in filename → REJECT"
  - These prevent many false positives that a pure scoring system might miss.

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

#### Our App's 2-Layer Fallback
```
┌─────────────────────────────────────────────────────────────────┐
│ Layer 1: Scan Primary Path (Custom/Detected + CallCloud)        │
│    ↓ FAIL                                                       │
│ Layer 2: Return null (Recording marked as not found)            │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 Gap Analysis

| Fallback | Callyzer | Our App | Priority to Add |
|----------|----------|---------|-----------------|
| MediaStore Query | ✅ Primary | ❌ Missing | 🔴 CRITICAL |
| Wider Time Window Retry | ✅ Yes | ❌ No | 🟡 Medium |
| Full Storage Scan | ✅ Yes | ❌ No | 🟢 Low (expensive) |
| Manual Attachment UI | ✅ Yes | ❌ No | 🟡 Medium |
| "Learning" System | ✅ Saves successful folders | ❌ No | 🟡 Medium |

---

## 5. Device & Manufacturer Handling

### 5.1 Path Coverage Comparison

| Manufacturer | Callyzer Paths | Our Paths | Gap |
|--------------|----------------|-----------|-----|
| **Samsung** | `/Recordings/Call/`, `/Call/`, `/Recordings/Voice Recorder/` | `/Call/`, `/Recordings/Voice Recorder/` | ❌ Missing `/Recordings/Call/` |
| **Xiaomi/MIUI** | `/MIUI/sound_recorder/call_rec/` | `/MIUI/sound_recorder/call_rec/` | ✅ Same |
| **OnePlus** | `/Recordings/PhoneRecord/`, `/Record/PhoneRecord/` | `/Record/Call/`, `/Record/PhoneRecord/` | ✅ Similar |
| **Oppo/Realme** | `/ColorOS/Recordings/`, `/Recordings/Call/`, `/DCIM/Recorder/` | `/Music/Recordings/Call Recordings/`, `/Recordings/Call Recordings/`, `/ColorOS/PhoneRecord/` | 🟡 Different but covered |
| **Vivo** | `/Recordings/`, `/VoiceRecorder/Calls/` | `/Record/Call/` | ❌ Missing Vivo-specific |
| **Huawei** | `/Sounds/CallRecord/`, `/Record/`, `/HuaweiBackup/CallRecord/` | `/Sounds/CallRecord/`, `/record/` | ❌ Missing backup path |
| **Pixel/Stock** | `/Recordings/`, `/Download/` | `/Recordings/Call recordings/`, `/Recordings/` | ✅ Good coverage |

### 5.2 Third-Party App Paths

| App | Callyzer | Our App |
|-----|----------|---------|
| ACR Phone | `/ACR/` | `/ACRCalls/` | 🟡 Different naming |
| Cube ACR | `/CubeCallRecorder/All/` | `/CubeCallRecorder/All/`, `/CubeCallRecorder/Recordings/` | ✅ Better |
| Truecaller | `/Truecaller/Recording/` | `/Truecaller/recordings/` | ❓ Case sensitivity issue |
| Boldbeast | `/BoldBeast/` | `/Boldbeast/` | ❓ Case sensitivity issue |
| Blackbox | `/.blackbox/` (hidden) | ❌ Missing | ❌ Gap |

### 5.3 Filename Pattern Parsing

| Pattern Type | Callyzer | Our App |
|--------------|----------|---------|
| `Call Recording {Name} {Number} {Date}` | ✅ Regex | 🟡 Substring match |
| `{Number}_{Date}_{Time}.mp3` | ✅ Regex | ✅ Substring match |
| `yyMMddHHmm` (OnePlus) | ❓ Not documented | ✅ Explicit parser |
| `yyyyMMdd_HHmmss` | ✅ Standard | ✅ Explicit parser |

---

## 6. Android Version Compatibility

### 6.1 Storage Permission Strategy

| Android | Callyzer | Our App | Gap |
|---------|----------|---------|-----|
| **≤9 (API ≤28)** | `READ_EXTERNAL_STORAGE` + File API | Same | ✅ OK |
| **10 (API 29)** | `requestLegacyExternalStorage` + MediaStore | Likely legacy flag | ⚠️ Should use MediaStore |
| **11+ (API 30+)** | `MANAGE_EXTERNAL_STORAGE` OR SAF + MediaStore | SAF only? | 🔴 MediaStore missing |
| **13+ (API 33+)** | `READ_MEDIA_AUDIO` + MediaStore | ❓ Unclear | 🔴 Need verification |

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

#### Our App's Approach
```kotlin
// Current implementation
// 1. Try File API (FAILS on 10+ for many paths)
// 2. Support content:// URIs via DocumentFile (SAF)
// 3. MediaStore NOT USED as primary
```

### 6.3 Critical Compatibility Issues in Our App

| Issue | Severity | Description |
|-------|----------|-------------|
| No MediaStore Query | 🔴 Critical | On Android 10+, file paths like `/MIUI/sound_recorder/` are invisible |
| Legacy File API | 🔴 Critical | `dir.listFiles()` returns null for many directories on 11+ |
| No `MANAGE_EXTERNAL_STORAGE` handling | 🟡 Medium | Some users might have granted this via other apps |
| Missing `READ_MEDIA_AUDIO` for Android 13+ | 🔴 Critical | May break on newest devices |

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

## 9. What They Do Better

### 9.1 Critical Technical Advantages

| Feature | Impact | Difficulty to Implement |
|---------|--------|------------------------|
| **MediaStore as Primary** | Works on Android 10+ regardless of path | 🟡 Medium (2-3 days) |
| **6-Layer Fallback** | Much higher success rate finding recordings | 🟢 Easy (1-2 days) |
| **Manual Attachment UI** | Users can fix when auto-match fails | 🟢 Easy (1 day) |
| **Compression** | 90% smaller uploads, faster sync | 🟡 Medium (2-3 days) |
| **"Learning" System** | Prioritizes folders that worked before | 🟢 Easy (1 day) |
| **Device-Specific Fixes** | Better compatibility, fewer crashes | 🟡 Medium (ongoing) |

### 9.2 UX Advantages

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

- We always check our own `/Recordings/CallCloud/` folder
- Recordings shared from Google Dialer are saved here
- Survives reinstall/path changes

### 10.4 Content URI Support

- We already support `content://` URIs via DocumentFile
- Can handle SAF-selected folders

---

## 11. What We Should Implement

### 11.1 Priority 1: Critical (Must Add)

| Feature | Reason | Effort |
|---------|--------|--------|
| **MediaStore Query** | Core functionality broken on Android 10+ | 3 days |
| **READ_MEDIA_AUDIO** permission | Android 13+ requirement | 1 day |
| **Wider Retry Window** | Improve success rate if first pass fails | 0.5 days |

### 11.2 Priority 2: High (Should Add)

| Feature | Reason | Effort |
|---------|--------|--------|
| **Manual Attachment UI** | User can fix when auto fails | 2 days |
| **"Learning" System** | Optimize for user's specific device | 1 day |
| **Compression Before Upload** | Huge UX improvement for uploads | 3 days |
| **Device Permission Guides** | Reduce support tickets | 2 days |

### 11.3 Priority 3: Medium (Nice to Have)

| Feature | Reason | Effort |
|---------|--------|--------|
| **Webhook Events** | Enterprise feature | 3 days |
| **Full Storage Scan** | Last-resort fallback | 1 day |
| **Higher Score Threshold** | Reduce false positives further | 0.5 days |

---

## 12. Immediate Action Items

### 12.1 This Week (Critical Fixes)

```markdown
1. [ ] Add MediaStore Query Method
   - File: RecordingRepository.kt
   - Function: findRecordingViaMediaStore()
   - Query: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
   - Filter: DATE_ADDED within ±5 min of callDate

2. [ ] Update findRecording() to use tiered approach:
   a. Try CallCloud folder first (fastest)
   b. Try MediaStore query (works on Android 10+)
   c. Try current file scan (fallback for old devices)
   d. Try SAF/custom path (user-configured)

3. [ ] Add READ_MEDIA_AUDIO permission for Android 13+
   - Update AndroidManifest.xml
   - Update permission request flow
```

### 12.2 Next Week (High Priority)

```markdown
4. [ ] Implement "Learning" System
   - When match found, save parent folder to preferences
   - On next search, check learned folder first

5. [ ] Add Manual Attachment UI
   - "Recording not found? Tap to browse"
   - Use SAF file picker (ACTION_OPEN_DOCUMENT)
   - Save selection to CallCloud folder

6. [ ] Add Retry with Wider Window
   - First attempt: ±5 minutes
   - Retry: ±30 minutes (if first fails)
   - Final: ±60 minutes (if still failing)
```

### 12.3 This Month (Improvements)

```markdown
7. [ ] Audio Compression
   - Use MediaCodec for AAC encoding
   - Target: 32kbps, 16000Hz, Mono
   - Compress before upload

8. [ ] Device-Specific Permission Guides
   - Detect device manufacturer
   - Show tailored instructions for:
     - Xiaomi: Autostart + Battery
     - Oppo: Battery optimization
     - Samsung: Background restrictions

9. [ ] Higher Confidence Threshold
   - Consider raising minimum score from 30 to 50
   - Or add "confidence level" indicator in UI
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
