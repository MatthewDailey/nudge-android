package com.reactiverobot.nudge;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;

import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class OnboardingActivity extends AppCompatActivity {

    @Inject
    Prefs prefs;

    public static final int REQUEST_USAGE_ACCESS = 0;
    public static final int REQUEST_SELECT_BAD_HABITS = 1;
    public static final int REQUEST_SELECT_BETTER_OPTIONS = 2;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_USAGE_ACCESS) {
            // Make sure the request was successful
            if (prefs.isUsageAccessGranted()) {
                changeToScene(R.layout.activity_onboarding_3);
            }
        }

        if (requestCode == REQUEST_SELECT_BAD_HABITS
                && !prefs.getSelectedPackages(PackageType.BAD_HABIT).isEmpty()) {
            changeToScene(R.layout.activity_onboarding_4);
        }

        if (requestCode == REQUEST_SELECT_BETTER_OPTIONS
                && !prefs.getSelectedPackages(PackageType.GOOD_OPTION).isEmpty()) {
            changeToScene(R.layout.activity_onboarding_5);
        }
    }

    private void changeToScene(int activity_resource) {
        final ConstraintLayout constraintLayout = findViewById(R.id.onboarding_contraint_layout);

        final ConstraintSet constraintSet3 = new ConstraintSet();
        constraintSet3.clone(getApplicationContext(), activity_resource);

        TransitionManager.beginDelayedTransition(constraintLayout);
        constraintSet3.applyTo(constraintLayout);
    }

    private void launchSelectPackagesActivity(PackageType packageType, int requestCode) {
        Intent intent = new Intent(this, SelectPackagesActivity.class);
        intent.putExtra("packageType", packageType.name());
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_onboarding_1);

        findViewById(R.id.button_get_started).setOnClickListener(v -> {
            changeToScene(R.layout.activity_onboarding_2);

            if (prefs.isUsageAccessGranted()) {
                changeToScene(R.layout.activity_onboarding_3);
            }
        });

        findViewById(R.id.button_open_settings).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, REQUEST_USAGE_ACCESS);
        });

        findViewById(R.id.button_bad_habits)
                .setOnClickListener(v -> launchSelectPackagesActivity(PackageType.BAD_HABIT, REQUEST_SELECT_BAD_HABITS));

        findViewById(R.id.button_better_options)
                .setOnClickListener(v -> launchSelectPackagesActivity(PackageType.GOOD_OPTION, REQUEST_SELECT_BETTER_OPTIONS));

        findViewById(R.id.button_enable).setOnClickListener(v -> {
            prefs.completeOnboarding();
            prefs.setCheckActiveEnabled(true);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

}
