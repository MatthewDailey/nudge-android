package com.reactiverobot.nudge;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;

import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.job.CheckActiveAppJobScheduler;
import com.reactiverobot.nudge.prefs.Prefs;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.reactiverobot.nudge.OnboardingActivity.REQUEST_USAGE_ACCESS;

public class MainActivity extends AppCompatActivity {

    @Inject
    Prefs prefs;
    @Inject
    CheckActiveAppJobScheduler jobScheduler;
    @Inject
    PackageInfoManager packageInfoManager;

    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onResume() {
        super.onResume();

        Switch enableServiceSwitch = findViewById(R.id.switch_enable_service);
        if (prefs.isUsageAccessGranted()) {
            enableServiceSwitch.setChecked(prefs.getCheckActiveEnabled());
        } else {
            enableServiceSwitch.setChecked(false);
        }
    }

    private void showOpenSettingsAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Before you can enable Nudge, you need to grant it App Usage Access.")
                .setTitle("App Usage Access is required");

        builder.setPositiveButton("Open Settings",
                (dialog, id) -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        builder.setNegativeButton("Cancel", (dialog, id) -> {
            // User cancelled the dialog
        });

        builder.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        if (!prefs.hasCompletedOnboarding()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupTabsAndTitle();

        PackageArrayAdapter.attach(this, PackageType.BAD_HABIT, packageInfoManager, prefs, R.id.list_view_bad_habits, R.id.search_bad_habits);
        PackageArrayAdapter.attach(this, PackageType.GOOD_OPTION, packageInfoManager, prefs, R.id.list_view_good_options, R.id.search_good_options);

        Switch enableServiceSwitch = findViewById(R.id.switch_enable_service);
        enableServiceSwitch.setOnCheckedChangeListener((compoundButton, isEnabled) -> {
            if (prefs.isUsageAccessGranted()) {
                if (isEnabled) {
                    jobScheduler.scheduleJob();
                } else {
                    jobScheduler.cancelJob();
                }
            } else {
                enableServiceSwitch.setChecked(false);
                showOpenSettingsAlertDialog();
            }
        });

    }

    private void setupTabsAndTitle() {
        TabHost host = findViewById(R.id.tabs_main);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Tab One");
        spec.setContent(R.id.tab1);
        spec.setIndicator("BAD HABITS");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("Tab Two");
        spec.setContent(R.id.tab2);
        spec.setIndicator("GOOD OPTIONS");
        host.addTab(spec);

        TextView titleView = findViewById(R.id.title_text_view);
        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/Pacifico-Regular.ttf");
        titleView.setTypeface(typeFace);
    }
}
