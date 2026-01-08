# Rejection Logic Verification

## 1. Problem
- User request: "why even scanning for 0 duration why waste resource... attaching recording to missed calls".
- **Cause**: The re-attach worker was blindly iterating through ALL calls in the database, including missed calls and those with 0 seconds duration.
- **Impact**: 
  - Wasted CPU/Battery checking files for calls that obviously have no recording (missed/rejected).
  - Risk of "false positives" (attaching a file to a missed call if timing was coincidental).

## 2. Solution: Hard Filtering
- **ReattachRecordingsWorker (Global)**:
  - Added filter: `if (log.duration <= 0 || log.callType >= 3) return@forEach`.
  - **Result**: Completely skips:
    - Missed Calls (Type 3)
    - Voicemails (Type 4)
    - Rejected Calls (Type 5)
    - Blocked Calls (Type 6)
    - Any call with 0 duration.
  - Only processes **Incoming (1)** and **Outgoing (2)** calls with actual talk time.

- **HomeViewModel (Individual)**:
  - Applied the same filter to the `reAttachRecordingsForPhone` function.

## 3. Verification
- Build passed successfully (`./gradlew assembleDebug`).
- This change heavily reduces the workload (skipping all missed calls) and eliminates the possibility of attaching recordings to non-connected calls.
