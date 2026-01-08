# Rules Display Verification

## 1. Problem
- User request: "add rules currently following, scan folders, etc".
- Interpretation: The user wants to SEE the rules and scope before running the matching process.

## 2. The Explanation Dialog
- Modified `ExtrasScreen.kt` to show an `AlertDialog` when clicking "Re-attach Recordings".
- **Content**:
  - **Active Matching Rules list**:
    1.  Skip Missed/Rejected Calls (0s duration ignored)
    2.  Prioritize Exact Phone/Name Match
    3.  Trust Filename Date (rejects if >2h different)
    4.  Strict Duration Matching (higher score)
    5.  Separates Duplicate Calls (Same day, diff time)
    6.  Optimized Bucketed Search (High speed)
  - **Scan Scope**:
    - "System Detected Recording Folders + Subdirectories"
- **Actions**:
  - "Start Scan": Launches the background process.
  - "Cancel": Aborts.

## 3. Verification
- Build passed successfully (`./gradlew assembleDebug`).
- The UI now transparently shows exactly what logic is being used, fulfilling the user's request to "add rules currently following".
