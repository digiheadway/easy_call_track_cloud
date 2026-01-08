# Speed Optimization Verification

## 1. Problem
- User reported "still scanning very slow around 40 in 5-10 second" (approx 4-8 calls/second).
- **Cause Analysis**: While filesystem I/O was optimized, the matching logic was still **O(M * N)** (checking every call against every file). 
- With 1000 recordings, checking 40 calls meant 40,000 file comparisons. Plus, Regex compilation was happening inside the loop.

## 2. Optimizations Implemented
1.  **Bucketed Search (Divide & Conquer)**:
    -   Grouped all recordings by **Day ID** (start of day millis).
    -   For each call, we generate its Day ID.
    -   We **only check files from Day-1, Day, Day+1**.
    -   **Impact**: Instead of searching 1000 files per call, we now search ~10 files (assuming roughly 10 calls/day). 
    -   This reduces complexity from **O(M * N)** to **O(M * (N/Days))**. This is a massive theoretical speedup (100x).

2.  **Regex Pre-compilation**:
    -   Moved `Pattern.compile` to static `companion object` in `RecordingRepository`.
    -   Avoids compiling the Regex pattern for every single file check.
    -   Important for the `extractDateFromFilename` method which runs frequently.

3.  **UI Updates**:
    -   Reduced notification update frequency from every 20 items to every 40 items to reduce NotificationManager overhead.

## 3. Expected Result
- The scanning speed should increase from ~8 calls/sec to **hundreds of calls/sec**.
- The limiting factor is now just the simple integer hashing of the date and list lookup.

## 4. Verification
- Build passed successfully (`./gradlew assembleDebug`).
- The buckets cover Day-1 and Day+1 to safely handle timezone shifts or midnight calls.
