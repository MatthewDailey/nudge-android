package com.reactiverobot.nudge;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;

import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageInfoManagerImpl;
import com.reactiverobot.nudge.info.PackageListManagerImpl;
import com.reactiverobot.nudge.job.CheckActiveAppJobScheduler;
import com.reactiverobot.nudge.prefs.Prefs;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class MainActivity extends AppCompatActivity {

    @Inject
    Prefs prefs;
    @Inject
    CheckActiveAppJobScheduler jobScheduler;

    private static final String TAG = MainActivity.class.getName();

    private PackageInfoManager packageInfoManager;

    PackageArrayAdapter badHabitPackageAdapter;
    PackageArrayAdapter goodOptionPackageAdapter;

    @Override
    protected void onResume() {
        super.onResume();

        Switch enableServiceSwitch = findViewById(R.id.switch_enable_service);
        enableServiceSwitch.setChecked(prefs.getCheckActiveEnabled());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        packageInfoManager = new PackageInfoManagerImpl(this, prefs);

        setupTabsAndTitle();

        // BAD HABITS
        badHabitPackageAdapter = new PackageArrayAdapter(
                this,
                (packageInfo, isChecked) -> {
                    packageInfo.badHabit = isChecked;
                    prefs.setPackageBadHabit(packageInfo.packageName, isChecked);
                });

        ListView badHabitsList = findViewById(R.id.list_view_bad_habits);
        badHabitsList.setAdapter(badHabitPackageAdapter);

        PackageListManagerImpl packageListManager = new PackageListManagerImpl(
                getPackageManager(),
                packageInfoManager,
                prefs::getPinnedBadHabitPackages);
        packageListManager.subscribe(badHabitPackageAdapter);
        packageListManager.initialize();
        packageInfoManager.subscribe(packageListManager);

        prefs.subscribeBadHabits(packageListManager);
        prefs.subscribeBadHabits(badHabitPackageAdapter);
        //------

        // GOOD OPTIONS
        goodOptionPackageAdapter = new PackageArrayAdapter(
                this,
                (packageInfo, isChecked) -> {
                    packageInfo.goodOption = isChecked;
                    prefs.setPackageGoodOption(packageInfo.packageName, isChecked);
                });
        ListView goodOptionsList = findViewById(R.id.list_view_good_options);
        goodOptionsList.setAdapter(goodOptionPackageAdapter);

        PackageListManagerImpl packageListManagerGoodOptions = new PackageListManagerImpl(
                getPackageManager(),
                packageInfoManager,
                prefs::getPinnedGoodOptionPackages);
        packageListManagerGoodOptions.subscribe(goodOptionPackageAdapter);
        packageListManagerGoodOptions.initialize();

        packageInfoManager.subscribe(packageListManagerGoodOptions);

        prefs.subscribeGoodOptions(packageListManagerGoodOptions);
        prefs.subscribeGoodOptions(goodOptionPackageAdapter);
        //------

        Switch enableServiceSwitch = findViewById(R.id.switch_enable_service);
        enableServiceSwitch.setOnCheckedChangeListener((compoundButton, isEnabled) -> {
            if (isEnabled) {
                jobScheduler.scheduleJob();
            } else {
                jobScheduler.cancelJob();
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
