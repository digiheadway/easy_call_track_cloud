package tera.videodownloader.box.Activity;


import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static tera.videodownloader.box.Config.ADMOB_APP_OPEN_AD_ID;
import static tera.videodownloader.box.Config.ADMOB_BANNER_ID;
import static tera.videodownloader.box.Config.ADMOB_INTERSTITIAL_ID;
import static tera.videodownloader.box.Config.ADMOB_NATIVE_ID;
import static tera.videodownloader.box.Config.AD_NETWORK;
import static tera.videodownloader.box.Config.AD_STATUS;
import static tera.videodownloader.box.Config.BACKUP_AD_NETWORK;
import static tera.videodownloader.box.Config.INTERSTITIAL_AD_INTERVAL;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import tera.videodownloader.box.R;
import tera.videodownloader.box.Utils.AdsManager;
import tera.videodownloader.box.Utils.AdsPref;
import com.ymg.ymgdevelopers.PrefManager;


public class SplashActivity extends AppCompatActivity {

    private TextView developers;


    private RelativeLayout parentLayout;
    private AppCompatImageView logo;
    public static final String TAG = "ActivitySplash";
    private AdsPref adsPref;


    private com.ymg.ymgdevelopers.PrefManager prf;

    private AdsManager adsManager;

    boolean isForceOpenAds = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        adsPref = new AdsPref(this);
        adsManager = new AdsManager(this);
        prf = new PrefManager(this);
        prf.setString("dev", "YMG-Developers");

        getConfig();
    }

    private void getConfig() {

        adsPref.saveAds(
                AD_STATUS,
                AD_NETWORK,
                BACKUP_AD_NETWORK,
                "",
                ADMOB_BANNER_ID,
                ADMOB_INTERSTITIAL_ID,
                ADMOB_NATIVE_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                ADMOB_APP_OPEN_AD_ID,
                INTERSTITIAL_AD_INTERVAL,
                INTERSTITIAL_AD_INTERVAL
        );

        showAppOpenAdIfAvailable();

    }

    private void showAppOpenAdIfAvailable() {
        if (isForceOpenAds) {
            if ("ymg".equals("ymg")) {
                adsManager.loadAppOpenAd(true, this::startMainActivity);
            } else {
                startMainActivity();
            }
        } else {
            if (adsPref.getAdStatus().equals("1")) {
                switch (adsPref.getMainAds()) {
                    case ADMOB:
                        if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                            ((MyApplication) getApplication()).showAdIfAvailable(SplashActivity.this, this::startMainActivity);
                        } else {
                            startMainActivity();
                        }
                        break;
                    default:
                        startMainActivity();
                        break;
                }
            } else {
                startMainActivity();
            }
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

}
