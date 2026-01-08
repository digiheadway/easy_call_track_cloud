# ODialer / OnePlus Format Support Verification

## 1. Explicit Date Parsing Implemented
- Added `extractDateFromFilename(fileName)` to `RecordingRepository`.
- **Pattern Supported**: `yyMMddHHmm` (e.g., `2601090249`).
  - This parses `2026-01-09 02:49:00`.
- **Pattern Supported**: `yyyyMMdd_HHmmss` (Standard).

## 2. Robust Scoring Update
- The scoring logic now calls `extractDateFromFilename`.
- **Scenario**: 
  - Call Log: `02:50` (Start time)
  - Recording Filename: `Mohit-2601090249` (Parsed as `02:49`).
  - **Old Logic**: Simple substring match for "0250" or "02-50" would FAIL.
  - **New Logic**: 
    - Extracts `02:49` from filename.
    - Calculates `diff = abs(02:50 - 02:49) = 1 minute`.
    - Checks `diff <= 5 minutes` -> **YES**.
    - Grants **+40 Score** (Filename Date Match).
    - Updates `timeDiff` to 1 minute for tie-breaking.
- This effectively solves the issue where file names are slightly offset from call log times (common with incoming calls vs recording start time).

## 3. Verification
- Build passed successfully (`./gradlew assembleDebug`).
- This change robustly handles the "ODialer" format and the "1 minute offset" issue mentioned by the user.
