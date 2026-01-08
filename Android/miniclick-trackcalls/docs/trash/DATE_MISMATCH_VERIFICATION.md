# Different Date / Metadata Mismatch Verification

## 1. Problem
- User reported: "i see even different date recording are matched".
- **Cause**: This happens when a file's **System Last Modified Date** is incorrect (e.g., all files "touched" today during a restore/copy) BUT the **Filename** contains the correct, old date.
- **Scenario**: 
  - Call: Today (Jan 9).
  - File: `20240101_Call.mp3` (Jan 1, 2024).
  - File Metadata: Modified Today (Jan 9) [Because user copied it today].
  - **Result**: The app trusted the metadata (Today matches Today) and ignored the filename date because it "didn't match". It saw a perfect time match + likely name/phone match.

## 2. The Fix: Explicit Filename Trust
- I added a rule: **Trust the Filename Date above all else.**
- Logic:
  1. Extract date from filename (e.g., `2024-01-01`).
  2. Compare with Call Date (e.g., `2026-01-09`).
  3. Difference = 2 years.
  4. **Rule**: If Difference > 2 hours -> **STOP. REJECT IMMEDIATELY.**
  - It returns `null` instantly, preventing any further matching based on the misleading metadata.

## 3. Verification
- Build passed successfully (`./gradlew assembleDebug`).
- This specifically targets recovered/copied files where metadata is lost but filenames are preserved.
- It ensures `2024...mp3` never attaches to a `2026` call.

## 4. Summary of All Matches Logic
1.  **Optimization**: Group by Day (Speed boost).
2.  **Filter**: Skip Missed/Rejected/Zero-duration calls (No false positives).
3.  **Strictness**: Min Score 30 (Must have Phone OR Date OR Perfect Time).
4.  **Duplicates**: Reject if Time Diff > 4 hours even if name matches (Fixes 3PM vs 9PM).
5.  **Metadata Trust**: Reject if Filename Date conflicts with Call Date (Fixes "Different Date matched").
