# Firestore Setup Instructions

## Issue Fixed
The app was experiencing a race condition when signing in with Google. The backend Cloud Function creates the user document asynchronously, but the app was trying to fetch it immediately, resulting in "User document not found" errors.

## Solution Implemented
1. **Retry Logic with Exponential Backoff**: The app now waits up to ~13 seconds (5 retries: 1s, 1.5s, 2.25s, 3.37s, 5s) for the backend to create the user document
2. **Enhanced Logging**: Detailed logs to track the entire sign-in and user creation flow
3. **Fallback User Creation**: If backend doesn't create user after retries, app creates it locally

## Firestore Security Rules
The `firestore.rules` file has been created. Deploy it to Firebase using one of these methods:

### Option 1: Firebase Console (Manual)
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project
3. Navigate to **Firestore Database** → **Rules**
4. Copy the contents of `firestore.rules` and paste it
5. Click **Publish**

### Option 2: Firebase CLI (Recommended)
```bash
# Install Firebase CLI if not already installed
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize Firebase in your project (if not already done)
firebase init firestore

# Deploy the rules
firebase deploy --only firestore:rules
```

## Testing
After deployment, test the Google sign-in flow:
1. Clear Firestore data (if testing fresh)
2. Sign in with Google
3. Check logcat for these logs:
   - ✅ "Backend user document found" (success)
   - ⚠️ "Waiting for backend to create user" (backend delay)
   - ❌ "Backend didn't create user after 5 attempts" (backend issue)

## Notes
- The retry logic ensures the app waits for backend user creation
- Local user creation is only a fallback if backend fails
- Security rules allow authenticated users to manage their own data
