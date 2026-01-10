# Google Ad Manager Configuration Backup

## Network Details
- **Display Name**: The Mak Media
- **Network Code**: 22649815059
- **Currency**: US Dollar
- **Time Zone**: Asia/Kolkata

## AdMob App (linked)
- **App ID**: ca-app-pub-2930566069243303~7469083365
- **Package Name**: com.clicktoearn.linkbox

## Ad Unit IDs (Ad Manager Format)
| Ad Type | Ad Unit Path |
|---------|-------------|
| App Open | `/22649815059/linkbox_app/interstitial` |
| Banner | `/22649815059/linkbox_app/banner` |
| Interstitial | `/22649815059/linkbox_app/interstitial` |
| Native | `/22649815059/linkbox_app/native` |
| Rewarded | `/22649815059/linkbox_app/rewarded` |
| Rewarded Interstitial | `/22649815059/linkbox_app/rewarded_interstitial` |

## How to Revert to Ad Manager

### 1. Update strings.xml
```xml
<string name="admob_app_id">ca-app-pub-2930566069243303~7469083365</string>
```

### 2. Update AndroidManifest.xml
Add inside `<application>`:
```xml
<meta-data
    android:name="com.google.android.gms.ads.AD_MANAGER_APP"
    android:value="true"/>
```

### 3. Update AdsManager.kt
Replace AdMob IDs with Ad Manager IDs:
```kotlin
private const val GAM_APP_OPEN_ID = "/22649815059/linkbox_app/interstitial"
private const val GAM_BANNER_ID = "/22649815059/linkbox_app/banner"
private const val GAM_INTERSTITIAL_ID = "/22649815059/linkbox_app/interstitial"
private const val GAM_NATIVE_ID = "/22649815059/linkbox_app/native"
private const val GAM_REWARDED_INTERSTITIAL_ID = "/22649815059/linkbox_app/rewarded_interstitial"
private const val GAM_REWARDED_ID = "/22649815059/linkbox_app/rewarded"
```

### 4. Use Ad Manager Classes
- `AdManagerAdRequest` instead of `AdRequest`
- `AdManagerAdView` instead of `AdView`
- `AdManagerInterstitialAd` instead of `InterstitialAd`

## Notes
- Requires active line items OR AdSense backfill enabled in Ad Manager
- Ad units must have "Maximize revenue" or "AdSense targeting" enabled
