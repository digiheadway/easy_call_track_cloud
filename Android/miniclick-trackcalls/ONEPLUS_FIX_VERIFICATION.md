# OnePlus Pattern & Duration Matching Fixes Verification

## 1. Filename Pattern Added
- Added `yyMMddHHmm` pattern (e.g., `2601090249`) to `RecordingRepository.kt`.
- This ensures dates are extracted from filenames like `Mohit-2601090249`, improving the **Time Match Score**.

## 2. Scoring Logic Refinement
- **Duration Match Priority**: Increased the score for exact duration matches (<1s diff) from **+30** to **+50**.
- **Reasoning**: Even if a file's timestamp is 1 minute off (common system drift), an exact duration match (e.g., 13s vs 13s) is a much stronger signal than a timestamp match with a wrong duration (e.g., 13s vs 3s).
- **Outcome**: The file `Mohit...248` (13s) should now correctly attach to the call at `02:49` (13s) instead of the call at `02:48` (3s), because the Duration Bonus (+50) will outweigh the minor Time Penalty of being 1 minute off.

## 3. Verification
- Build passed successfully (`./gradlew assembleDebug`).
- The logic aligns with the specific user report involving OnePlus devices and multiple calls in close succession.
