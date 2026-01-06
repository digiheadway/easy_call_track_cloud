package tera.videodownloader.box.Utils;

import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;
import static com.solodroid.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;
import static com.solodroid.ads.sdk.util.Constant.IRONSOURCE;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import com.solodroid.ads.sdk.BuildConfig;
import com.solodroid.ads.sdk.format.AdNetwork;
import com.solodroid.ads.sdk.format.AppOpenAd;
import com.solodroid.ads.sdk.format.BannerAd;
import com.solodroid.ads.sdk.format.InterstitialAd;
import com.solodroid.ads.sdk.format.NativeAd;
import com.solodroid.ads.sdk.format.NativeAdFragment;
import com.solodroid.ads.sdk.format.RewardedAd;
import com.solodroid.ads.sdk.gdpr.GDPR;
import com.solodroid.ads.sdk.util.OnRewardedAdCompleteListener;
import com.solodroid.ads.sdk.util.OnRewardedAdDismissedListener;
import com.solodroid.ads.sdk.util.OnRewardedAdErrorListener;
import com.solodroid.ads.sdk.util.OnShowAdCompleteListener;
import tera.videodownloader.box.Config;
import tera.videodownloader.box.R;

public class AdsManager {

    Activity activity;
    AdNetwork.Initialize adNetwork;
    AppOpenAd.Builder appOpenAd;
    BannerAd.Builder bannerAd;
    InterstitialAd.Builder interstitialAd;
    NativeAd.Builder nativeAd;
    RewardedAd.Builder rewardedAd;
    AdsPref adsPref;
    GDPR gdpr;

    NativeAdFragment.Builder nativeAd2;

    public AdsManager(Activity activity) {
        this.activity = activity;

        this.adsPref = new AdsPref(activity);
        this.gdpr = new GDPR(activity);
        adNetwork = new AdNetwork.Initialize(activity);
        appOpenAd = new AppOpenAd.Builder(activity);
        bannerAd = new BannerAd.Builder(activity);
        interstitialAd = new InterstitialAd.Builder(activity);
        nativeAd = new NativeAd.Builder(activity);
        nativeAd2 = new NativeAdFragment.Builder(activity);
        rewardedAd = new RewardedAd.Builder(activity);
    }

    public void initializeAd() {
        adNetwork.setAdStatus(adsPref.getAdStatus())
                .setAdNetwork(adsPref.getMainAds())
                .setBackupAdNetwork(adsPref.getBackupAds())
                .setStartappAppId(adsPref.getStartappAppId())
                .setUnityGameId(adsPref.getUnityGameId())
                .setIronSourceAppKey(adsPref.getIronSourceAppKey())
                .setWortiseAppId(adsPref.getWortiseAppId())
                .setDebug(BuildConfig.DEBUG)
                .build();
    }

    public void loadAppOpenAd(boolean placement, OnShowAdCompleteListener onShowAdCompleteListener) {
        if (placement) {
            appOpenAd = new AppOpenAd.Builder(activity)
                    .setAdStatus(adsPref.getAdStatus())
                    .setAdNetwork(adsPref.getMainAds())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setAdMobAppOpenId(adsPref.getAdMobAppOpenAdId())
                    .setAdManagerAppOpenId(adsPref.getAdManagerAppOpenAdId())
                    .setApplovinAppOpenId(adsPref.getAppLovinAppOpenAdUnitId())
                    .setWortiseAppOpenId(adsPref.getWortiseAppOpenId())
                    .build(onShowAdCompleteListener);
        } else {
            onShowAdCompleteListener.onShowAdComplete();
        }
    }

    public void loadAppOpenAd(boolean placement) {
        if (placement) {
            appOpenAd = new AppOpenAd.Builder(activity)
                    .setAdStatus(adsPref.getAdStatus())
                    .setAdNetwork(adsPref.getMainAds())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setAdMobAppOpenId(adsPref.getAdMobAppOpenAdId())
                    .setAdManagerAppOpenId(adsPref.getAdManagerAppOpenAdId())
                    .setApplovinAppOpenId(adsPref.getAppLovinAppOpenAdUnitId())
                    .setWortiseAppOpenId(adsPref.getWortiseAppOpenId())
                    .build();
        }
    }

    public void showAppOpenAd(boolean placement) {
        if (placement) {
            appOpenAd.show();
        }
    }

    public void destroyAppOpenAd(boolean placement) {
        if (placement) {
            appOpenAd.destroyOpenAd();
        }
    }

    public void loadBannerAd(boolean placement) {
        if (placement) {
            bannerAd.setAdStatus(adsPref.getAdStatus())
                    .setAdNetwork(adsPref.getMainAds())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setAdMobBannerId(adsPref.getAdMobBannerId())
                    .setGoogleAdManagerBannerId(adsPref.getAdManagerBannerId())
                    .setFanBannerId(adsPref.getFanBannerId())
                    .setUnityBannerId(adsPref.getUnityBannerPlacementId())
                    .setAppLovinBannerId(adsPref.getAppLovinBannerAdUnitId())
                    .setAppLovinBannerZoneId(adsPref.getAppLovinBannerZoneId())
                    .setIronSourceBannerId(adsPref.getIronSourceBannerId())
                    .setWortiseBannerId(adsPref.getWortiseBannerId())
                    .setDarkTheme(false)
                    .setPlacementStatus(1)
                    .build();
        }
    }

    public void loadInterstitialAd(boolean placement, int interval) {
        if (placement) {
            interstitialAd.setAdStatus(adsPref.getAdStatus())
                    .setAdNetwork(adsPref.getMainAds())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setAdMobInterstitialId(adsPref.getAdMobInterstitialId())
                    .setGoogleAdManagerInterstitialId(adsPref.getAdManagerInterstitialId())
                    .setFanInterstitialId(adsPref.getFanInterstitialId())
                    .setUnityInterstitialId(adsPref.getUnityInterstitialPlacementId())
                    .setAppLovinInterstitialId(adsPref.getAppLovinInterstitialAdUnitId())
                    .setAppLovinInterstitialZoneId(adsPref.getAppLovinInterstitialZoneId())
                    .setIronSourceInterstitialId(adsPref.getIronSourceInterstitialId())
                    .setWortiseInterstitialId(adsPref.getWortiseInterstitialId())
                    .setInterval(interval)
                    .setPlacementStatus(1)
                    .build();
        }
    }

    public void loadNativeAd(boolean placement) {
        if (placement) {
            nativeAd.setAdStatus(adsPref.getAdStatus())
                    .setAdNetwork(adsPref.getMainAds())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setAdMobNativeId(adsPref.getAdMobNativeId())
                    .setAdManagerNativeId(adsPref.getAdManagerNativeId())
                    .setFanNativeId(adsPref.getFanNativeId())
                    .setAppLovinNativeId(adsPref.getAppLovinNativeAdManualUnitId())
                    .setAppLovinDiscoveryMrecZoneId(adsPref.getAppLovinBannerMrecZoneId())
                    .setWortiseNativeId(adsPref.getWortiseNativeId())
                    .setPlacementStatus(1)
                    .setDarkTheme(false)
                    .setNativeAdBackgroundColor(R.color.white, R.color.white)
                    .setNativeAdStyle(adsPref.getNativeAdStylePostDetails())
                    .build();
            nativeAd.setNativeAdPadding(
                    activity.getResources().getDimensionPixelSize(R.dimen.item_post_padding_small),
                    activity.getResources().getDimensionPixelSize(R.dimen.item_post_padding_small),
                    activity.getResources().getDimensionPixelSize(R.dimen.item_post_padding_small),
                    activity.getResources().getDimensionPixelSize(R.dimen.item_post_padding_small)
            );
            nativeAd.setNativeAdMargin(
                    activity.getResources().getDimensionPixelSize(R.dimen.no_padding),
                    activity.getResources().getDimensionPixelSize(R.dimen.item_post_padding_small),
                    activity.getResources().getDimensionPixelSize(R.dimen.no_padding),
                    activity.getResources().getDimensionPixelSize(R.dimen.item_post_padding_small)
            );
        }
    }

    public void loadNativeAdView(boolean placement) {
        if (placement) {
            nativeAd.setAdStatus(adsPref.getAdStatus())
                    .setAdNetwork(adsPref.getMainAds())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setAdMobNativeId(adsPref.getAdMobNativeId())
                    .setAdManagerNativeId(adsPref.getAdManagerNativeId())
                    .setFanNativeId(adsPref.getFanNativeId())
                    .setAppLovinNativeId(adsPref.getAppLovinNativeAdManualUnitId())
                    .setAppLovinDiscoveryMrecZoneId(adsPref.getAppLovinBannerMrecZoneId())
                    .setWortiseNativeId(adsPref.getWortiseNativeId())
                    .setPlacementStatus(1)
                    .setDarkTheme(false)

                    .setNativeAdBackgroundColor(R.color.white, R.color.white)
                    .setNativeAdStyle(adsPref.getNativeAdStyleExitDialog())
                    .build();
        }
    }

    public void loadNativeAdViewFrgment(boolean placement, View view) {
        if (placement) {
            nativeAd2.setAdStatus(adsPref.getAdStatus())
                    .setAdNetwork(adsPref.getMainAds())
                    .setBackupAdNetwork(adsPref.getBackupAds())
                    .setAdMobNativeId(adsPref.getAdMobNativeId())
                    .setAdManagerNativeId(adsPref.getAdManagerNativeId())
                    .setFanNativeId(adsPref.getFanNativeId())
                    .setAppLovinNativeId(adsPref.getAppLovinNativeAdManualUnitId())
                    .setAppLovinDiscoveryMrecZoneId(adsPref.getAppLovinBannerMrecZoneId())
                    .setView(view)
                    .setPlacementStatus(1)
                    .setDarkTheme(false)

                    .setNativeAdBackgroundColor(R.color.white, R.color.white)
                    .setNativeAdStyle(adsPref.getNativeAdStyleExitDialog())
                    .build();
        }
    }


    public void loadRewardedAd() {
        rewardedAd
                .setAdStatus(adsPref.getAdStatus())
                .setMainAds(adsPref.getMainAds())
                .setBackupAds(adsPref.getBackupAds())
                .setAdMobRewardedId("ca-app-pub-3639394690716281/8387512124")
                .setAdManagerRewardedId("")
                .setFanRewardedId("")
                .setUnityRewardedId("")
                .setApplovinMaxRewardedId("")
                .setApplovinDiscRewardedZoneId("")
                .setIronSourceRewardedId("")
                .setWortiseRewardedId("")
                .build(new OnRewardedAdCompleteListener() {
                    @Override
                    public void onRewardedAdComplete() {
                        Log.d("ymgssssss", "onRewardedAdComplete");

                    }
                }, new OnRewardedAdDismissedListener() {
                    @Override
                    public void onRewardedAdDismissed() {


                    }
                });
    }

    public void showRewardedAd() {
        rewardedAd.show(new OnRewardedAdCompleteListener() {
            @Override
            public void onRewardedAdComplete() {
                Log.d("ymgssssss", "onRewardedAdComplete 2");

            }
        }, new OnRewardedAdDismissedListener() {
            @Override
            public void onRewardedAdDismissed() {
                Log.d("ymgssssss", "onRewardedAdDismissed 2");

            }
        }, new OnRewardedAdErrorListener() {
            @Override
            public void onRewardedAdError() {

            }
        });
    }


    public void showInterstitialAd() {
        interstitialAd.show();
    }

    public void destroyBannerAd() {
        bannerAd.destroyAndDetachBanner();
    }

    public void resumeBannerAd(boolean placement) {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && !adsPref.getIronSourceBannerId().equals("0")) {
            if (adsPref.getMainAds().equals(IRONSOURCE) || adsPref.getBackupAds().equals(IRONSOURCE)) {
                loadBannerAd(placement);
            }
        }
    }

    public void updateConsentStatus() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            if (adsPref.getMainAds().equals(ADMOB) || adsPref.getMainAds().equals(GOOGLE_AD_MANAGER)) {
                if (Config.ENABLE_GDPR_UMP_SDK) {
                    gdpr.updateGDPRConsentStatus(adsPref.getMainAds(), false, false);
                }
            }
        }
    }

}
