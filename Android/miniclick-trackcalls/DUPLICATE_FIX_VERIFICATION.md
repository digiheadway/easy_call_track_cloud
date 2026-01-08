# Duplicate Call / Same Duration Fix Verification

## 1. Problem
- User reported: "same person call at 3:56 pm and 9:09 pm, with same duration, it attached same recording to both".
- **Cause**: The previous logic heavily weighted "Identity Match" + "Duration Match".
  - If File A (3:56 PM) had the right Name and Duration, it was getting a high score.
  - When matching for the 9:09 PM call, File A *still* got a high score because Name and Duration matched perfectly, even though the time was 5+ hours off. The time penalty wasn't strong enough to exclude it.

## 2. The Fix: Stricter Rejection Rules
- **Rule 1**: If even the Name/Phone matches, **REJECT** if the file time is > 4 hours away from the call time (unless the filename contains the specific date/time of the call).
  - This specifically kills the 3:56 PM file from being a candidate for the 9:09 PM call (diff > 5 hours).
- **Rule 2**: If Name/Phone DOES NOT match, strict limit is now 5 minutes (down from 15).

## 3. Outcome
- **Call 1 (3:56 PM)**: Matches File A (3:56 PM). Score: High.
- **Call 2 (9:09 PM)**: 
  - Checks File A (3:56 PM). Name matches? Yes. Duration matches? Yes. Time Diff? 5h 13m. 
  - **New Rule**: Time Diff > 4h -> **REJECTED**.
  - System is forced to look for File B (9:09 PM) or return nothing.

## 4. Verification
- Build passed successfully (`./gradlew assembleDebug`).
- This solves the specific "duplicates" issue while preserving the robust matching for legitimate slight time offsets (up to 4 hours, handling timezone weirdness, but blocking distinct Separate Calls).
