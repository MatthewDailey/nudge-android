package com.reactiverobot.nudge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.reactiverobot.nudge.checker.ActivePackageChecker;
import com.reactiverobot.nudge.prefs.Prefs;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class NudgeAccessibilityService extends AccessibilityService {
    private final static String TAG = NudgeAccessibilityService.class.getName();

    @Inject
    Prefs prefs;
    @Inject
    ActivePackageChecker packageChecker;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        if (packageName == null) {
            Log.d(TAG, "Saw event with no package name, doing nothing");
            return;
        }

        String eventPackageName = packageName.toString();
        Log.d(TAG, "Saw package : " + eventPackageName);
        packageChecker.launchSuggestActivityIfBlocked(eventPackageName);
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
