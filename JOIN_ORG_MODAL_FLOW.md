# Join Organisation Modal - 3-Step Verification Flow

## Date: 2026-01-01

## Overview
Redesigned the Join Organisation modal with a progressive 3-step verification flow that validates, verifies, then connects.

---

## New User Flow

### Step 1: Format Validation (Client-Side)
**State:** Button disabled ("Connect Organisation")

User enters pairing code → Real-time format validation:
- ✅ Must contain hyphen `-`
- ✅ Must have exactly 2 parts (ORGID-USERID)
- ✅ ORGID: Letters and numbers only (A-Z, 0-9)
- ✅ USERID: Numbers only (0-9)
- ✅ Both parts must be non-empty

**UI Feedback:**
- ❌ Invalid format → Red error text: "Format: ORGID-USERID (e.g., GOOGLE-123)"
- ✅ Valid format → Green text: "Format looks good! Click to verify →"
- Button changes to: **"Check Pairing Code"** (enabled)

---

### Step 2: Backend Verification
**State:** User clicks "Check Pairing Code"

**What happens:**
1. Shows loading spinner
2. Button text: "Verifying..."
3. Button disabled during verification
4. Calls `viewModel.verifyPairingCodeOnly(pairingCode)`
5. Backend checks:
   - Employee exists
   - Organization valid
   - Device authorization

**On Success:**
- ✅ Green card appears showing:
  - Organisation name (ORGID)
  - Employee name (from backend)
- Button changes to: **"Connect"** (enabled)
- Pairing code saved in UI state (not repository yet)

**On Failure:**
- ❌ Error toast with specific message
- TextField shows error state (red border)
- Button returns to "Check Pairing Code"

---

### Step 3: Connect (Save)
**State:** User clicks "Connect"

**What happens:**
1. Calls `viewModel.connectVerifiedOrganisation()`
2. Saves organization ID and user ID to repository
3. Shows success toast: "✓ Connected to {OrgName}!"
4. Modal closes automatically
5. User is now connected to organization

---

## Component Structure

### JoinOrgModal.kt

#### State Management
```kotlin
val uiState by viewModel.uiState.collectAsState()
var pairingCode by remember { mutableStateOf("") }
```

#### Format Validation (Reactive)
```kotlin
val isFormatValid = remember(pairingCode) {
    val trimmed = pairingCode.trim().uppercase()
    if (!trimmed.contains("-")) return@remember false
    val parts = trimmed.split("-", limit = 2)
    if (parts.size != 2) return@remember false
    val orgId = parts[0].trim()
    val userId = parts[1].trim()
    orgId.isNotEmpty() && userId.isNotEmpty() && 
    orgId.matches(Regex("^[A-Z0-9]+$")) && 
    userId.matches(Regex("^[0-9]+$"))
}
```

#### Button Text Logic
```kotlin
val buttonText = when {
    uiState.isVerifying -> "Verifying..."
    uiState.verificationStatus == "verified" -> "Connect"
    isFormatValid -> "Check Pairing Code"
    else -> "Connect Organisation"
}
```

#### Button Enabled Logic
```kotlin
val buttonEnabled = when {
    uiState.isVerifying -> false
    uiState.verificationStatus == "verified" -> true
    else -> isFormatValid
}
```

#### Button Click Handler
```kotlin
Button(
    onClick = {
        when {
            uiState.verificationStatus == "verified" -> {
                // Step 3: Connect (save)
                viewModel.connectVerifiedOrganisation(onSuccess = onDismiss)
            }
            else -> {
                // Step 2: Verify with backend
                viewModel.verifyPairingCodeOnly(pairingCode)
            }
        }
    },
    enabled = buttonEnabled
)
```

---

## ViewModel Functions

### 1. `verifyPairingCodeOnly(pairingCode: String)`
**Purpose:** Verify pairing code with backend WITHOUT saving

**Process:**
1. Reset previous verification state
2. Client-side validation (format)
3. Call backend API `verifyPairingCode`
4. On success:
   - Set `verificationStatus = "verified"`
   - Store `verifiedOrgName` and `verifiedEmployeeName`
   - Update `pairingCode` in UI state
5. On failure:
   - Set `verificationStatus = "failed"`
   - Show error toast

**Key Point:** Does NOT save to repository!

---

### 2. `connectVerifiedOrganisation(onSuccess: () -> Unit)`
**Purpose:** Save verified pairing code to repository

**Process:**
1. Check if already verified (`verificationStatus == "verified"`)
2. Parse pairing code
3. Save to repository:
   - `settingsRepository.setOrganisationId(orgId)`
   - `settingsRepository.setUserId(userId)`
4. Update UI state
5. Show success toast
6. Call `onSuccess()` callback

**Key Point:** Only called after successful verification!

---

### 3. `resetVerificationState()`
**Purpose:** Clear verification state when user edits pairing code

**Process:**
```kotlin
_uiState.update {
    it.copy(
        verificationStatus = null,
        verifiedOrgName = null,
        verifiedEmployeeName = null
    )
}
```

---

## UI Components

### Pairing Code TextField
```kotlin
OutlinedTextField(
    value = pairingCode,
    onValueChange = { 
        pairingCode = it.uppercase()  // Auto-uppercase
        viewModel.resetVerificationState()  // Clear on edit
    },
    enabled = !uiState.isVerifying,  // Disable during verification
    supportingText = {
        when {
            !isFormatValid && pairingCode.isNotEmpty() -> {
                Text("Format: ORGID-USERID (e.g., GOOGLE-123)", color = Error)
            }
            isFormatValid && uiState.verificationStatus == null -> {
                Text("Format looks good! Click to verify →", color = Primary)
            }
        }
    },
    isError = uiState.verificationStatus == "failed"
)
```

### Verification Success Card
```kotlin
if (uiState.verificationStatus == "verified") {
    Card {
        Column {
            Icon(CheckCircle) + Text("Verified Successfully")
            
            Row {
                Column {
                    Text("Organisation")
                    Text(uiState.verifiedOrgName)
                }
                Column {
                    Text("Employee")
                    Text(uiState.verifiedEmployeeName)
                }
            }
        }
    }
}
```

---

## State Flow Diagram

```
┌─────────────────────────────────────────────────────┐
│ Initial State                                       │
│ • verificationStatus = null                         │
│ • Button: "Connect Organisation" (disabled)         │
└────────────────────┬────────────────────────────────┘
                     │
         User enters pairing code
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│ Format Valid                                        │
│ • isFormatValid = true                              │
│ • Button: "Check Pairing Code" (enabled)            │
└────────────────────┬────────────────────────────────┘
                     │
           User clicks "Check"
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│ Verifying                                           │
│ • isVerifying = true                                │
│ • Button: "Verifying..." (disabled)                 │
└────────┬─────────────────────┬──────────────────────┘
         │                     │
    ┌────▼──────┐        ┌─────▼──────┐
    │ Success   │        │ Failed     │
    └────┬──────┘        └─────┬──────┘
         │                     │
         ▼                     ▼
┌──────────────────┐  ┌─────────────────────┐
│ Verified         │  │ Error State         │
│ • status = "✓"   │  │ • status = "failed" │
│ • Show org info  │  │ • Red border        │
│ • Button: "Con   │  │ • Toast error       │
│   nect" (enabled)│  │ • Reset to check    │
└────┬─────────────┘  └─────────────────────┘
     │
 User clicks "Connect"
     │
     ▼
┌──────────────────────┐
│ Connected            │
│ • Data saved         │
│ • Modal closes       │
│ • Success toast      │
└──────────────────────┘
```

---

## Validation Rules

### Client-Side (Real-time)
1. **Input auto-uppercase:** Converts to uppercase as user types
2. **Format check:** ORGID-USERID pattern
3. **ORGID validation:** `^[A-Z0-9]+$`
4. **USERID validation:** `^[0-9]+$`
5. **Button state:** Disabled until format valid

### Server-Side
6. **Employee exists:** Checks database
7. **Organization valid:** Matches org_id
8. **Device authorization:** Handles device linking/switching

---

## User Experience Features

### Real-time Feedback
- ✅ Format validation as user types
- ✅ Auto-uppercase conversion
- ✅ Color-coded helper text
- ✅ Button text changes based on state

### Progressive Disclosure
- 👁️ Only shows verification card after success
- 👁️ Clear button states guide user
- 👁️ Helpful error messages

### Error Handling
- ❌ Format errors: Inline helper text
- ❌ Verification errors: Toast + error state
- ❌ Network errors: Clear error messages

### Success Feedback
- ✅ Green verification card
- ✅ Shows organization and employee info
- ✅ Clear "Connect" action
- ✅ Success toast on completion

---

## Files Modified

1. ✅ `SettingsViewModel.kt`:
   - Added `verificationStatus`, `verifiedOrgName`, `verifiedEmployeeName` to state
   - Added `verifyPairingCodeOnly()` function
   - Added `connectVerifiedOrganisation()` function
   - Added `resetVerificationState()` function

2. ✅ `SettingsScreen.kt`:
   - Completely redesigned `JoinOrgModal` composable
   - Added client-side format validation
   - Added verification success card
   - Added progressive button states
   - Added real-time UI feedback

---

## Benefits

### Security
- ✅ Nothing saved until verified
- ✅ Strict format validation
- ✅ Backend verification required
- ✅ Device binding enforced

### User Experience
- ✅ Clear 3-step process
- ✅ Real-time feedback
- ✅ No guesswork
- ✅ Helpful error messages

### Developer Experience
- ✅ Clean separation of concerns
- ✅ Reactive state management
- ✅ Easy to test
- ✅ Easy to extend

---

## Testing Checklist

- [ ] Test with empty input (button disabled)
- [ ] Test with invalid format (shows error helper)
- [ ] Test with valid format (button enabled)
- [ ] Test verification with wrong credentials
- [ ] Test verification with correct credentials
- [ ] Test verification success card displays
- [ ] Test connect button saves data
- [ ] Test modal closes on success
- [ ] Test editing after verification resets state
- [ ] Test loading states during verification
- [ ] Test network error handling

---

## Summary

The Join Organisation modal now provides a **smooth, guided 3-step experience**:

1. **Format Validation** → Button says "Check Pairing Code"
2. **Backend Verification** → Shows org/employee info
3. **Connect** → Saves and closes

This prevents saving invalid data, provides clear feedback at every step, and creates a professional, polished user experience! 🎯
