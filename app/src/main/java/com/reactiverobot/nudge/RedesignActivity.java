package com.reactiverobot.nudge;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.reactiverobot.nudge.checker.ActivePackageChecker;
import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageListManagerSupplier;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class RedesignActivity extends AppCompatActivity {

    @Inject
    Prefs prefs;
    @Inject
    ActivePackageChecker packageChecker;
    @Inject
    PackageInfoManager packageInfoManager;
    @Inject
    PackageListManagerSupplier packageListManagerSupplier;

    private static final String TAG = RedesignActivity.class.getName();

    private void showOpenBrieflyDialog(PackageType packageType, PackageInfo packageInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("This will open and unblock " + packageInfo.name + " for 60 seconds.")
                .setIcon(packageInfo.iconDrawable)
                .setTitle("Open " + packageInfo.name + "?");

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

    private void showUnpinDialog(PackageType packageType, PackageInfo packageInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("This package will no longer appear in this list. " +
                "You can always add it back with the '+' button.")
                .setIcon(packageInfo.iconDrawable)
                .setTitle("Remove " + packageInfo.name + "?");

        builder.setPositiveButton("Remove",
                (dialog, id) -> prefs.unpinPackage(packageType, packageInfo.packageName));
        builder.setNegativeButton("Cancel", (dialog, id) -> {
            // User cancelled the dialog
        });

        builder.create().show();
    }

    private void showOpenSettingsAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Before you can enable Nudge, you need to grant it Accessibility Access. This permission will allow Nudge to view all content on your screen and is necessary for Nudge to block apps. None of your app usage data will be stored or transmitted from your device.")
                .setTitle("Accessibility Access is required");

        builder.setPositiveButton("Grant Access",
                (dialog, id) -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        builder.setNegativeButton("Cancel", (dialog, id) -> {
            // User cancelled the dialog
        });

        builder.create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateEnabledViews();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        if (!prefs.hasCompletedOnboarding()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        PackageArrayAdapter.Builder builder = PackageArrayAdapter
                .builder(packageListManagerSupplier, prefs)
                .onLongPress((packageType, packageInfo) -> showUnpinDialog(packageType, packageInfo))
                .onShowBriefly((packageType, packageInfo) -> showOpenBrieflyDialog(packageType, packageInfo))
                .withCheckbox();

        setupGroup(findViewById(R.id.group_blocked), PackageType.BAD_HABIT, "BLOCKED", builder.attach(this, PackageType.BAD_HABIT));
        setupGroup(findViewById(R.id.group_suggested), PackageType.GOOD_OPTION, "SUGGESTED", builder.attach(this, PackageType.GOOD_OPTION));

        Switch enableServiceSwitch = findViewById(R.id.switch_enable_service);

        final Consumer<Boolean> setServiceEnabled = (Boolean isEnabled) -> {
            if (prefs.isAccessibilityAccessGranted()) {
                prefs.setCheckActiveEnabled(isEnabled);
            } else {
                showOpenSettingsAlertDialog();
            }
            updateEnabledViews();
        };

        enableServiceSwitch.setOnCheckedChangeListener((compoundButton, isEnabled) -> {
            setServiceEnabled.accept(isEnabled);
        });

        Button enableServiceButton = findViewById(R.id.button_enable_service);
        enableServiceButton.setOnClickListener((view) -> setServiceEnabled.accept(true));

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ImageButton drawerButton = findViewById(R.id.button_drawer);
        drawerButton.setOnClickListener((view) -> drawer.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.navigation_main);
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    drawer.closeDrawers();
                    menuItem.setCheckable(false);

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
//                    case R.id.nav_about:
//                        // TODO: Open simple about activity
                        default:
                            return true;
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
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:matt@reactiverobot.com?subject=" + Uri.encode("Feedback about Nudge App")));
        startActivity(Intent.createChooser(emailIntent, "Send Email"));
    }

    private void setupGroup(View groupView, PackageType packageType, String title, PackageArrayAdapter adapter) {
        ListView blockedPackagesList = groupView.findViewById(R.id.list_packages);
        blockedPackagesList.setAdapter(adapter);

        TextView sectionTitle = groupView.findViewById(R.id.section_title);
        sectionTitle.setText(title);

        ImageButton buttonAdd = groupView.findViewById(R.id.button_add);
        buttonAdd.setOnClickListener((view) -> {
            Intent intent = new Intent(this, ChooseOnePackageActivity.class);
            intent.putExtra("packageType", packageType.name());
            startActivity(intent);
        });
    }

    private void updateEnabledViews() {
        Switch enableServiceSwitch = findViewById(R.id.switch_enable_service);
        View disabledView = findViewById(R.id.disabled_header);

        if (prefs.isAccessibilityAccessGranted() && prefs.getCheckActiveEnabled()) {
            enableServiceSwitch.setChecked(true);
            disabledView.setVisibility(View.GONE);
        } else {
            enableServiceSwitch.setChecked(false);
            disabledView.setVisibility(View.VISIBLE);
        }
    }
}
