# Rewarded Ad Testing Guide

## Overview
The rewarded ad functionality has been enhanced with comprehensive logging and proper test mode support. This guide will help you test the rewarded ad implementation.

## What Was Changed

### Enhanced AdsManager.kt
1. **Improved Logging**: Added detailed logs at every step of the ad lifecycle
   - ✅ Success indicators (green checkmarks in logs)
   - ❌ Error indicators (red X marks in logs)
   - ⚠️ Warning indicators (yellow warnings in logs)

2. **Better Error Handling**: 
   - Detailed error codes and messages
   - Proper state management
   - Automatic retry on failure

3. **Test Mode Support**:
   - Using Google's official test ad unit ID: `ca-app-pub-3940256099942544/5224354917`
   - Set `IS_TEST_MODE = true` in AdsManager.kt

## How Rewarded Ads Work

### Loading Flow
1. App starts → `MainActivity.onCreate()` calls `AdsManager.loadRewardedAd()`
2. Ad loads in background (takes 2-5 seconds typically)
3. Once loaded, ad is ready to show

### Showing Flow
1. User triggers action (e.g., "Watch Ad to Earn Point" button)
2. `AdsManager.showRewardedAd()` is called
3. If ad is ready → Shows full-screen video ad
4. User watches ad completely → Reward callback fires
5. App gives user 1 point
6. New ad starts loading automatically

## Where Rewarded Ads Are Used

The app uses rewarded ads in these locations:

1. **InsufficientPointsSheet** - When user doesn't have enough points
2. **WalletBottomSheet** - "Watch Ad to Earn" option
3. **SharedFolderScreen** - Earning points for access
4. **DeepLinkScreen** - Earning points for content access
5. **HistoryScreen** - Earning points

## Testing Instructions

### Step 1: Check Logcat
Open Android Studio Logcat and filter by "AdsManager" to see detailed logs:

```
AdsManager: Starting to load RewardedAd with ID: ca-app-pub-3940256099942544/5224354917
AdsManager: ✅ RewardedAd loaded successfully!
AdsManager: showRewardedAd called - Ad ready: true, Enabled: true
AdsManager: Showing RewardedAd...
AdsManager: RewardedAd showed full screen content
AdsManager: ✅ User earned reward! Type: Reward, Amount: 1
AdsManager: RewardedAd dismissed
```

### Step 2: Test Scenarios

#### Scenario A: Successful Ad View
1. Launch the app
2. Wait 3-5 seconds for ad to load (check logcat)
3. Navigate to Profile → My Points → "Watch Ad to Earn"
4. Click the button
5. Watch the test ad completely
6. Verify you receive 1 point

#### Scenario B: Ad Not Ready
1. Launch app
2. Immediately click "Watch Ad to Earn" (before ad loads)
3. Should see log: "⚠️ RewardedAd not ready yet, loading now..."
4. Wait a few seconds and try again

#### Scenario C: Multiple Ads
1. Watch first ad → Get reward
2. Wait 5-10 seconds for next ad to load
3. Watch second ad → Get reward
4. Verify points increase each time

### Step 3: Common Issues & Solutions

#### Issue: "Ad failed to load"
**Possible Causes:**
- No internet connection
- AdMob account not set up
- Test device not configured

**Solution:**
- Check internet connection
- Verify test ad unit IDs are being used
- Check logcat for specific error codes

#### Issue: "Ad shows but no reward"
**Possible Causes:**
- User closed ad before completion
- Callback not properly set up

**Solution:**
- Watch ad completely until it shows "Close" button
- Check logcat for "User earned reward!" message

#### Issue: "Ad never loads"
**Possible Causes:**
- Remote Config disabled ads
- Network issues
- AdMob initialization failed

**Solution:**
- Check Remote Config settings
- Verify `AdsManager.init()` is called in Application class
- Check logcat for initialization errors

## Test Ad Behavior

Google's test rewarded ads:
- Load quickly (2-5 seconds)
- Show a simple video (usually 15-30 seconds)
- Always grant rewards when completed
- Can be closed after 5 seconds (but won't grant reward)
- Unlimited impressions for testing

## Production Checklist

Before releasing to production:

- [ ] Replace test ad unit IDs with real AdMob IDs
- [ ] Set `IS_TEST_MODE = false`
- [ ] Test on multiple devices
- [ ] Verify Remote Config is working
- [ ] Test with poor network conditions
- [ ] Verify points are awarded correctly
- [ ] Test ad frequency limits
- [ ] Check analytics/reporting in AdMob console

## Monitoring in Production

Key metrics to watch:
1. **Ad Request Fill Rate**: Should be >80%
2. **Show Rate**: Percentage of loaded ads that are shown
3. **Reward Grant Rate**: Should be 100% for completed views
4. **eCPM**: Revenue per 1000 impressions

## Debug Commands

To test specific scenarios, you can add these temporary buttons:

```kotlin
// Force load ad
Button(onClick = { AdsManager.loadRewardedAd(context) }) {
    Text("Force Load Ad")
}

// Check ad status
Button(onClick = { 
    Log.d("TEST", "Ad ready: ${AdsManager.rewardedAd != null}")
}) {
    Text("Check Ad Status")
}
```

## Support

If issues persist:
1. Check AdMob dashboard for account status
2. Verify app is registered in AdMob
3. Check Firebase Remote Config values
4. Review full logcat output
5. Test on different devices/Android versions

## Current Configuration

- **Test Mode**: Enabled (`IS_TEST_MODE = true`)
- **Rewarded Ad Unit ID**: `ca-app-pub-3940256099942544/5224354917` (Google Test ID)
- **Remote Config Key**: `show_rewarded` (default: true)
- **Reward Amount**: 1 point per ad view
- **Auto-reload**: Yes (new ad loads after each view)

---

**Note**: The enhanced logging will help you track exactly what's happening with the rewarded ads. Always check logcat when testing!
