package com.reactiverobot.nudge;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;

import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageListManagerSupplier;
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
    @Inject
    PackageListManagerSupplier packageListManagerSupplier;

    private static final String TAG = MainActivity.class.getName();

    private PackageType currentFocusPackageType = PackageType.BAD_HABIT;

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

    private void showUnpinDialog(PackageType packageType, PackageInfo packageInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("This package will no longer appear in this list. " +
                "You can always add it back with the '+' button.")
                .setTitle("Remove '" + packageInfo.name + "'?");

        builder.setPositiveButton("Remove",
                (dialog, id) -> prefs.unpinPackage(packageType, packageInfo.packageName));
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
        setContentView(R.layout.activity_main_no_search);

        PackageArrayAdapter.Builder builder = PackageArrayAdapter
                .builder(packageListManagerSupplier, prefs)
                .onLongPress((packageType, packageInfo) -> showUnpinDialog(packageType, packageInfo))
                .withCheckbox();
        ((ListView) findViewById(R.id.list_view_bad_habits)).setAdapter(builder.attach(this, PackageType.BAD_HABIT));
        ((ListView) findViewById(R.id.list_view_good_options)).setAdapter(builder.attach(this, PackageType.GOOD_OPTION));

        setupTabsAndTitle();

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

        findViewById(R.id.fab_select_package).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickFab();
            }
        });
    }

    private void onClickFab() {
        Intent intent = new Intent(this, ChooseOnePackageActivity.class);
        intent.putExtra("packageType", currentFocusPackageType.name());
        startActivity(intent);
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

        host.setOnTabChangedListener(s -> {
            if (s == "Tab One") {
                currentFocusPackageType = PackageType.BAD_HABIT;
                animateFab(0);
            } else {
                currentFocusPackageType = PackageType.GOOD_OPTION;
                animateFab(1);
            }
        });
    }

    int[] colorIntArray = {R.color.colorPrimary, R.color.colorPrimary};
    int[] iconIntArray = {R.drawable.ic_action_add, R.drawable.ic_action_add};

    protected void animateFab(final int position) {
        FloatingActionButton fab = findViewById(R.id.fab_select_package);

        fab.clearAnimation();
        // Scale down animation
        ScaleAnimation shrink =  new ScaleAnimation(1f, 0.2f, 1f, 0.2f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        shrink.setDuration(150);     // animation duration in milliseconds
        shrink.setInterpolator(new DecelerateInterpolator());
        shrink.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Change FAB color and icon
                fab.setBackgroundTintList(getResources().getColorStateList(colorIntArray[position]));
                fab.setImageDrawable(getResources().getDrawable(iconIntArray[position], null));

                // Scale up animation
                ScaleAnimation expand =  new ScaleAnimation(0.2f, 1f, 0.2f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                expand.setDuration(100);     // animation duration in milliseconds
                expand.setInterpolator(new AccelerateInterpolator());
                fab.startAnimation(expand);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        fab.startAnimation(shrink);
    }
}
