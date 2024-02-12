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

    private void logEventSource(AccessibilityNodeInfo source, int depth) {
        if (source == null) {
            return;
        }
        CharSequence text = source.getText();
        CharSequence contentDescription = source.getContentDescription();
        if (text != null || contentDescription != null) {
//            Log.d(TAG, "source=" + source);
//            Log.d(TAG, "text=" + text + " contentDescription=" + contentDescription);
        }

        if (text != null && contentDescription != null
                && text.toString().equals("Shorts") && contentDescription.toString().equals("Shorts")) {
            Log.d(TAG, "Shorts title found.");
            Log.d(TAG, "window=" + source.getWindow());
            Log.d(TAG, "source=" + source);
        }

        if (text == null && contentDescription != null && contentDescription.toString().contains("play Short")) {
            Log.d(TAG, "Short link found.");
            Log.d(TAG, "window=" + source.getWindow());
            Log.d(TAG, "source=" + source);
        }

        for (int i = 0; i < source.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = source.getChild(i);
            if (source != null) {
                logEventSource(nodeInfo, depth + 1);
            }
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        Log.d(TAG, "===============================================");
        Log.d(TAG, event.toString());
        if (packageName == null) {
            Log.d(TAG, "Saw event with no package name, doing nothing");
            return;
        }

        String eventPackageName = packageName.toString();
        lastEventPackage.set(eventPackageName);

        int contentChangeType = event.getContentChangeTypes();
        AccessibilityNodeInfo source = event.getSource();

        if (event.getSource() != null) {
            logEventSource(event.getSource(), 0);
        }

        if (contentChangeType != AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED && contentChangeType != AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED) {
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
    }
}
