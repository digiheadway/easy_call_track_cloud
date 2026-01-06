package tera.videodownloader.box.Activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.onesignal.Continue;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import tera.videodownloader.box.Config;
import tera.videodownloader.box.Fragment.DownloadFragment;
import tera.videodownloader.box.Fragment.HistoryFragment;
import tera.videodownloader.box.R;
import tera.videodownloader.box.Utils.AdsManager;
import com.ymg.ymgdevelopers.YmgTools;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    DrawerLayout drawerLayout;
    Toolbar toolBar;
    NavigationView navigationView;
    ActionBarDrawerToggle actionBarDrawerToggle;
    ViewPager viewPager;
    private AdsManager adsManager;
    private BottomNavigationView navigation;
    int pager_number = 2;
    private String TAG = "MainActivity";

    private static final String ONESIGNAL_APP_ID = "23897ee6-1da1-4b9e-b20b-8ad4b68ea56d";

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolBar = findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        adsManager = new AdsManager(this);
        adsManager.initializeAd();
        adsManager.updateConsentStatus();

        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
        OneSignal.getNotifications().requestPermission(false, Continue.none());

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolBar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        actionBarDrawerToggle.syncState();
        toolBar.setNavigationIcon(R.drawable.ic_action_action);

        initViewPager();

    }



    @SuppressWarnings("deprecation")
    public void initViewPager() {
        viewPager = findViewById(R.id.viewPager);
        navigation = findViewById(R.id.navigation);
        viewPager.setAdapter(new MyAdapter(getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(pager_number);
        navigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_email) {
                viewPager.setCurrentItem(0);
                setTitle(R.string.app_name);
                return true;
            } else if (itemId == R.id.navigation_inbox) {
                viewPager.setCurrentItem(1);
                setTitle("Downloads");
                return true;
            }
            return false;
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 0){
                    setTitle(R.string.app_name);
                }else {
                    setTitle("Downloads");
                }
            }

            @Override
            public void onPageSelected(int position) {
                navigation.getMenu().getItem(position).setChecked(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    public class MyAdapter extends FragmentPagerAdapter {

        MyAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {

            if (position == 0) {
                return new HistoryFragment();
            } else if (position == 1) {
                return new DownloadFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return pager_number;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            YmgTools.shareApp(this,getPackageName());
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        drawerLayout.closeDrawer(GravityCompat.START);


        if (menuItem.getItemId() == R.id.nav_home) {

        }
        if (menuItem.getItemId() == R.id.nav_about) {
            showAboutDialog();
        }
        if (menuItem.getItemId() == R.id.nav_contact) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.your_email)});
            i.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name));
            i.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.email_text));
            try {
                startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }
        }
        if (menuItem.getItemId() == R.id.nav_rate) {
            YmgTools.rateApp(this, getPackageName());
        }
        if (menuItem.getItemId() == R.id.nav_share) {
            YmgTools.shareApp(this, getPackageName());
        }
        if (menuItem.getItemId() == R.id.nav_more) {
            YmgTools.openUrl(this, Config.MORE_APP_URL);
        }
        if (menuItem.getItemId() == R.id.nav_privacy) {
            Intent intent = new Intent(MainActivity.this, PrivacyActivity.class);
            intent.putExtra("query", "Privacy Policy");
            startActivity(intent);
        }
        if (menuItem.getItemId() == R.id.nav_terms) {
            Intent intent = new Intent(MainActivity.this, TermsActivity.class);
            intent.putExtra("query", "Terms and Condition");
            startActivity(intent);
        }
        return false;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            showExitDialog();
        }
    }

    private void showAboutDialog() {
        final Dialog dialog = new Dialog(this, R.style.DialogCustomTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(R.layout.dialog_about);

        AppCompatButton btn_done = dialog.findViewById(R.id.btn_done);
        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void swipeToSecondFragment() {
        viewPager.setCurrentItem(1, true);  // 1 is for the second fragment (index starts from 0)
    }

    private void showExitDialog() {
        final Dialog dialog = new Dialog(MainActivity.this, R.style.DialogCustomTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(R.layout.dialog_exit);

        LinearLayout mbtnYes = dialog.findViewById(R.id.mbtnYes);
        LinearLayout mbtnNo = dialog.findViewById(R.id.mbtnNo);
        mbtnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                finish();
            }
        });
        mbtnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

}