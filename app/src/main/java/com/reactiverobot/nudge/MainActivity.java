package com.reactiverobot.nudge;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TabHost;

import com.reactiverobot.nudge.checker.ActivePackageChecker;
import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageListManagerSupplier;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class MainActivity extends AppCompatActivity {

    @Inject
    Prefs prefs;
    @Inject
    ActivePackageChecker packageChecker;
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
        if (prefs.isAccessibilityAccessGranted()) {
            enableServiceSwitch.setChecked(prefs.getCheckActiveEnabled());
        } else {
            enableServiceSwitch.setChecked(false);
        }
    }

    private void showOpenSettingsAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Before you can enable Nudge, you need to grant it Accessibility Access.")
                .setTitle("Accessibility Access is required");

        builder.setPositiveButton("Open Settings",
                (dialog, id) -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
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

    private void showOpenBrieflyDialog(PackageType packageType, PackageInfo packageInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("This will open and unblock " + packageInfo.name + " for 60 seconds.")
                .setTitle("Open '" + packageInfo.name + "'?");

        builder.setPositiveButton("Open",
                (dialog, id) -> {
                    Log.d("handler", "opening temp");
                    prefs.setTemporarilyUnblocked(packageInfo.packageName, true);
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageInfo.packageName);
                    startActivity(launchIntent);
                });
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
                .onShowBriefly((packageType, packageInfo) -> showOpenBrieflyDialog(packageType, packageInfo))
                .withCheckbox();
        ((ListView) findViewById(R.id.list_view_bad_habits)).setAdapter(builder.attach(this, PackageType.BAD_HABIT));
        ((ListView) findViewById(R.id.list_view_good_options)).setAdapter(builder.attach(this, PackageType.GOOD_OPTION));

        setupTabsAndTitle();

        Switch enableServiceSwitch = findViewById(R.id.switch_enable_service);
        enableServiceSwitch.setOnCheckedChangeListener((compoundButton, isEnabled) -> {
            if (prefs.isAccessibilityAccessGranted()) {
                prefs.setCheckActiveEnabled(isEnabled);
            } else {
                enableServiceSwitch.setChecked(false);
                showOpenSettingsAlertDialog();
            }
        });

        findViewById(R.id.fab_select_package).setOnClickListener(view -> onClickFab());
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
        spec.setIndicator("BLOCKED");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("Tab Two");
        spec.setContent(R.id.tab2);
        spec.setIndicator("SUGGESTED");
        host.addTab(spec);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ImageButton drawerButton = findViewById(R.id.button_drawer);
        drawerButton.setOnClickListener((view) -> drawer.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.navigation_main);
        navigationView.setNavigationItemSelectedListener(
            menuItem -> {
                menuItem.setChecked(true);
                drawer.closeDrawers();

                switch (menuItem.getItemId()) {
                    case R.id.nav_rate:
                        prefs.openPlayStore();
                        return true;
                    case R.id.nav_share:
                        startShareAppIntent();
                        return true;
                    case R.id.nav_feedback:
                        startSendFeedbackActivity();
                        return true;
                    case R.id.nav_about:
                        // TODO: Open simple about activity
                    default:
                        return true;
                }
            });

        host.setOnTabChangedListener(s -> {
            if (s == "Tab One") {
                currentFocusPackageType = PackageType.BAD_HABIT;
                animateFab();
            } else {
                currentFocusPackageType = PackageType.GOOD_OPTION;
                animateFab();
            }
        });
    }

    private void startShareAppIntent() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                "Hey check out Nudge to block distracting apps at: https://play.google.com/store/apps/details?id=com.reactiverobot.nudge");
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    private void startSendFeedbackActivity() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setType("text/html");
        intent.putExtra(Intent.EXTRA_EMAIL, "matt@reactiverobot.com");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback about Nudge App");
        startActivity(Intent.createChooser(intent, "Send Email"));
    }

    protected void animateFab() {
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
                fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary, null)));
                fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_add, null));

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
