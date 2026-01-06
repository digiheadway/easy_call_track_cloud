# Guest Account Linking & Profile Photo Implementation

## Summary of Changes

This implementation fixes guest account linking to Google and adds profile photo support for users who sign in with Google.

## Key Changes Made

### 1. **Data Models Updated**

#### FirestoreUser (Remote Model)
- Added `photoUrl: String` field to store Google profile photo URL
- Added `isGuest: Boolean` field to track guest accounts

#### UserEntity (Local Model)
- Added `photoUrl: String` field
- Added `isGuest: Boolean` field

### 2. **Database Migration**
- Incremented database version from 8 to 9
- Room will handle migration automatically with fallback to recreate

### 3. **Authentication Flow Improvements**

#### Guest Account Creation (`createGuestUser`)
- Now uses Firebase Anonymous Authentication
- Creates a Firebase Auth anonymous user first
- Uses the Firebase Auth UID as the user ID in Firestore
- Marks the account with `isGuest = true`
- This enables proper account linking later

#### Google Sign-In (`signInWithGoogle`)
- Fetches and stores the user's Google profile photo URL
- Updates existing users with photo URL if not already set
- Properly marks non-guest accounts with `isGuest = false`

#### Account Linking (`linkWithGoogle`)
- **FIXED**: Now properly links anonymous Firebase Auth accounts to Google
- Checks if current user is authenticated
- Links the Google credential to the anonymous account
- Updates Firestore with:
  - Email from Google account
  - Profile photo URL
  - Username (if current is guest-like)
  - Sets `isGuest = false`
- Refreshes the user profile after linking

### 4. **UI Enhancements**

#### ProfileScreen
- Added Coil image loading library dependency
- Displays circular profile photo if available
- Falls back to default icon if no photo URL
- Uses `isGuest` field instead of username prefix to detect guest accounts
- Shows "Link Google Account" card for guest users

### 5. **Dependencies Added**
- `io.coil-kt:coil-compose:2.5.0` for image loading

## How It Works

### Guest Login Flow
1. User clicks "Continue as Guest"
2. System generates random username (e.g., "guest_123456")
3. Firebase Anonymous Auth creates an anonymous user
4. Firestore user document created with `isGuest = true`
5. User can now use the app as a guest

### Linking Guest to Google
1. Guest user navigates to Profile screen
2. Sees "Link Google Account" card
3. Clicks to link account
4. Google Sign-In flow starts
5. On success, `linkWithGoogle` is called
6. Firebase Auth links the Google credential to the anonymous account
7. Firestore updated with Google account info and photo
8. User is now a full Google-authenticated user with all their guest data preserved

### Google Sign-In Flow (New Users)
1. User clicks "Sign in with Google" on login screen
2. Google Sign-In flow completes
3. System fetches user's profile photo from Google account
4. Creates Firestore user with photo URL
5. Profile screen displays the photo

## Testing Checklist

- [ ] Guest login creates anonymous Firebase Auth user
- [ ] Guest account shows "Link Google Account" option
- [ ] Linking guest to Google preserves all user data
- [ ] Profile photo displays correctly after Google sign-in
- [ ] Profile photo displays correctly after linking
- [ ] Fallback icon shows when no photo available
- [ ] Database migration completes without errors

## Notes

- The key fix was using Firebase Anonymous Authentication for guest users instead of just creating Firestore documents
- This enables proper account linking via `linkWithCredential()`
- Profile photos are loaded asynchronously using Coil
- Photos are displayed in a circular shape for better aesthetics
