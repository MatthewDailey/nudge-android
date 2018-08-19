package com.reactiverobot.nudge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;
import com.reactiverobot.nudge.prefs.PrefsImpl;

import java.util.Set;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class NudgeAccessibilityService extends AccessibilityService {
    private final static String TAG = NudgeAccessibilityService.class.getName();

    @Inject
    Prefs prefs;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName().equals("com.reactiverobot.nudge")) {
            Log.d(TAG, "Saw self, doing nothing." );
            return;
        }

        if (!prefs.getCheckActiveEnabled()) {
            Log.d(TAG, "Check is not enabled.");
            return;
        }

        Set<String> blockedPackages = PrefsImpl.from(this).getSelectedPackages(PackageType.BAD_HABIT);

        if (blockedPackages.contains(event.getPackageName())) {
            Intent intent = new Intent(getApplicationContext(), SuggestChangeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Got interrupt");
    }

    @Override
    public void onServiceConnected() {
        AndroidInjection.inject(this);
        Log.d(TAG, "Connecting service");
        super.onServiceConnected();

        AccessibilityServiceInfo info = getServiceInfo();

        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.packageNames = null;
        info.flags = AccessibilityServiceInfo.DEFAULT;

        this.setServiceInfo(info);
    }
}
