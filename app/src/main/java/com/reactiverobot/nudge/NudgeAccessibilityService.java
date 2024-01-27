package com.reactiverobot.nudge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.reactiverobot.nudge.checker.ActivePackageChecker;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static android.view.accessibility.AccessibilityNodeInfo.FOCUS_ACCESSIBILITY;
import static com.reactiverobot.nudge.prefs.PrefsImpl.TEMP_UNBLOCK_SEC;

public class NudgeAccessibilityService extends AccessibilityService {
    private final static String TAG = NudgeAccessibilityService.class.getName();

    @Inject
    Prefs prefs;
    @Inject
    ActivePackageChecker packageChecker;

    AtomicReference<String> lastEventPackage = new AtomicReference<>(null);

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();

        if (packageName == null) {
            Log.d(TAG, "Saw event with no package name, doing nothing");
            return;
        }

        String eventPackageName = packageName.toString();
        lastEventPackage.set(eventPackageName);

        int eventType = event.getEventType();
        int contentChangeType = event.getContentChangeTypes();
        int action = event.getAction();
        AccessibilityNodeInfo source = event.getSource();
        Log.d(TAG, "Saw packageName=" + eventPackageName + " eventType=" + eventType
                + " contentChangeType=" + contentChangeType
                + " action=" + action + " source=" + source);

        if (contentChangeType != AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED) {
            Log.d(TAG, "Content change type is not CONTENT_CHANGE_TYPE_PANE_APPEARED, doing nothing");
            return;
        }

        if (prefs.isTemporarilyUnblocked(eventPackageName)) {
            new Handler().postDelayed(() -> {
                Log.d("handler", "Calling handler " + eventPackageName);
                if (eventPackageName.equals(lastEventPackage.get())) {
                    packageChecker.launchSuggestActivityIfBlocked(eventPackageName);
                }
            }, (TEMP_UNBLOCK_SEC + 1) * 1000);
        } else {
            packageChecker.launchSuggestActivityIfBlocked(eventPackageName);
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
