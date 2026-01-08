# Dialer & In-Call Improvement Proposal

This document outlines potential improvements, identified bugs, and structural issues within the dialer and in-call systems of the application.

## 1. Structural & Architectural Issues

### A. Polling vs. Reactive State
- **Current Issue**: `InCallActivity.kt` uses a `while(isActive) { delay(500) }` loop to poll the call state from `CallTrackInCallService.currentCall`. 
- **Impact**: Unnecessary CPU usage, potential UI lag (up to 500ms delay in state updates), and a "jumpy" user experience.
- **Improvement**: Implement a `StateFlow` in `CallTrackInCallService` that emits the current call state and duration. The UI should collect this flow.

### B. Static Variable Dependency
- **Current Issue**: `CallTrackInCallService` relies on a static `currentCall` variable and a weak reference to its `instance`.
- **Impact**: Brittle architecture, difficult to unit test, and prone to memory leaks if not handled carefully.
- **Improvement**: Use a proper `Binder` or a shared `ViewModel` scope (if possible) or a singleton `CallManager` repository that manages call states independently of the Service lifecycle.

### C. Hardcoded Logic & Strings
- **Current Issue**: State transitions and UI branching rely on hardcoded strings like `"Incoming Call"` or `"Active"`.
- **Impact**: Localization is difficult, and any change in string values will break the UI logic.
- **Improvement**: Use Enums or the `android.telecom.Call.STATE_*` constants directly to drive UI logic.

---

## 2. UI/UX Enhancements

### A. Advanced Dialer (T9 Search)
- **Current Issue**: The `DialerScreen.kt` is a simple keypad. Keyboards on mobile devices typically support T9 search (e.g., typing '5-6-4' to find "JOHN").
- **Improvement**: Implement a real-time list below the dialer number that filters contacts as the user types.

### B. Rich In-Call Notification
- **Current Issue**: The notification in `CallTrackInCallService` is very basic.
- **Improvement**: Add Action Buttons (Answer, Decline, Mute) to the notification. Display the contact name and a timer in the notification content.

### C. Audio Feedback
- **Current Issue**: Pressing dialer keys provides no audio feedback.
- **Improvement**: Add standard DTMF tones or click sounds to both the Dialer and In-Call keypad to confirm user interaction.

### D. Dual-SIM Clarity
- **Current Issue**: The SIM picker appears *after* pressing the call button.
- **Improvement**: Indicate the currently active/preferred SIM on the dialer screen itself before the call is placed.

---

## 3. Potential Bugs & Edge Cases

### A. International Number Formatting
- **Issue**: Number matching for `PersonGroup` uses `normalizePhoneNumber(phoneNumber)`. If the number in the database is local and the incoming call is international (+code), matching might fail.
- **Improvement**: Use `PhoneNumberUtils.compare()` or a consistent global format (E.164) for all database entries.

### B. Keypad (DTMF) Timing
- **Issue**: The current DTMF logic plays for exactly 300ms. Some automated systems (IVRs) might require shorter or longer tones, or tones that last as long as the user holds the button.
- **Improvement**: Use `MotionEvent` to play tone on `ACTION_DOWN` and stop on `ACTION_UP`.

### C. Call Waiting / Multiple Calls
- **Issue**: `InCallActivity` assumes only one active call. If a second call comes in, the UI may break or override the first.
- **Improvement**: Implement a pager or a list to handle multiple calls, allowing the user to swap between them.

### D. Activity Launch from Background
- **Issue**: Launching `InCallActivity` from the service on Android 10+ requires the `SYSTEM_ALERT_WINDOW` permission or specific "launch from background" permissions.
- **Improvement**: Ensure the app gracefully handles cases where it cannot launch the full-screen UI (fallback to a high-priority notification).

---

## 4. Code Cleanliness (Bad Code Samples)

### Polling Loop (InCallActivity.kt:117-155)
```kotlin
LaunchedEffect(Unit) {
    while (isActive) {
        val call = CallTrackInCallService.currentCall
        // ... update states ...
        delay(500)
    }
}
```
*Why it's bad*: This is an anti-pattern in Compose. It should be an event-driven flow.

### Broad Exception Catching (InCallActivity.kt:408-413)
```kotlin
onKeyPress = { char ->
    try {
        // ... keypad logic ...
    } catch (e: Exception) {
        // Silent failure or log
    }
}
```
*Why it's bad*: It hides legitimate logic errors (like null calls) instead of handling them explicitly.

### Static Instance Usage (CallTrackInCallService.kt:18-28)
```kotlin
fun setAudioRoute(route: Int) {
    instance?.get()?.setAudioRoute(route)
}
```
*Why it's bad*: This creates a tight coupling between the UI and the process-global state of the Service.
