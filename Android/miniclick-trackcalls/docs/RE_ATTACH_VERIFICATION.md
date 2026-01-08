# Re-attach Recordings Implementation Verification

## 1. UI Additions
- **Call Lists**: Long-pressing a call card now shows a "Re-scan Recordings" option in the "More Options" menu. This triggers a targeted re-scan for that phone number.
- **Person Lists**: Long-pressing a person card also shows the "Re-scan Recordings" option.
- **Troubleshooting**: In Settings -> More (Extras), a new "Re-attach Recordings" option has been added under the Troubleshooting section. This triggers a full re-scan for ALL calls.

## 2. Logic Implementation
- **Targeted Re-attach (`reAttachRecordingsForPhone`)**: 
  - Fetches all logs for the specific phone number.
  - Uses the enhanced `findRecording` logic (scoring based on name, phone, time, duration) to find the best match.
  - Updates the recording path if a better match is found or if one wasn't attached.
- **Full Re-attach (`reAttachAllRecordings`)**: 
  - Iterates through ALL call logs in the database.
  - Attempts to find matching recordings for each.
  - Provides a toast summary of how many recordings were updated.

## 3. Data Integrity
- The new logic uses `CallDataRepository` and `RecordingRepository` ensuring consistency with existing data patterns.
- It respects the improved matching algorithm (lazy duration check, time penalties, etc.) added in the previous step.

## 4. Verification
- Build passed successfully (`./gradlew assembleDebug`).
- No compilation errors in ViewModels or Repositories.
