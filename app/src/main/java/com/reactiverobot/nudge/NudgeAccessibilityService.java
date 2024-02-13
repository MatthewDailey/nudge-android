package com.reactiverobot.nudge;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.reactiverobot.nudge.checker.ActivePackageChecker;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.reactiverobot.nudge.prefs.PrefsImpl.TEMP_UNBLOCK_SEC;

public class NudgeAccessibilityService extends AccessibilityService {
    private final static String TAG = NudgeAccessibilityService.class.getName();

    @Inject
    Prefs prefs;
    @Inject
    ActivePackageChecker packageChecker;

    AtomicReference<String> lastEventPackage = new AtomicReference<>(null);
    AtomicReference<Map<String, View>> viewMapRef = new AtomicReference<>(new HashMap<>());

    private void addCoverViewForAccessibilityNode(AccessibilityNodeInfo source, Map<String, View> oldViewMap, Map<String, View> newViewMap) {
        String viewKey = "viewKey://" + source.getClassName() + "/" + source.getContentDescription();
        AccessibilityWindowInfo window = source.getWindow();
        Log.d(TAG, "window=" + window);
        Log.d(TAG, "source=" + source);
        if (window == null) {
            Log.d(TAG, "Window is null, doing nothing");
            return;
        }
        Rect rect = new Rect();
        source.getBoundsInWindow(rect);
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = rect.left; // X position
        params.y = rect.top; // Y position
        params.height = rect.height();
        params.width = rect.width();

        Log.d(TAG, "Adding new view");
        View view = new RedRectangleView(getApplicationContext());
        newViewMap.put(viewKey, view);
        windowManager.addView(view, params);
    }

    private void traverseAccessibilityNodes(AccessibilityNodeInfo source, Map<String, View> oldViewMap, Map<String, View> newViewMap) {
        if (source == null) {
            return;
        }

        CharSequence text = source.getText();
        CharSequence contentDescription = source.getContentDescription();


        if (text != null && contentDescription != null
                && text.toString().equals("Shorts") && contentDescription.toString().equals("Shorts")) {
            Log.d(TAG, "Shorts title found.");
            addCoverViewForAccessibilityNode(source, oldViewMap, newViewMap);
        }

        if (text == null && contentDescription != null && contentDescription.toString().contains("play Short")) {
            Log.d(TAG, "Short link found.");
            addCoverViewForAccessibilityNode(source, oldViewMap, newViewMap);
        }

        for (int i = 0; i < source.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = source.getChild(i);
            if (source != null) {
                traverseAccessibilityNodes(nodeInfo, oldViewMap, newViewMap);
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

        if (eventPackageName.equals("com.google.android.youtube")) {
            synchronized (this) {
                Map<String, View> oldViewMap = viewMapRef.get();
                Map<String, View> newViewMap = new HashMap<>();
                if (event.getSource() != null && event.getSource().getWindow() != null) {
                    AccessibilityNodeInfo root = event.getSource().getWindow().getRoot();
                    traverseAccessibilityNodes(root, oldViewMap, newViewMap);
                    viewMapRef.set(newViewMap);
                }
                Log.d(TAG, "Old view map size: " + oldViewMap.size());
                Log.d(TAG, "New view map size: " + newViewMap.size());
                Log.d(TAG, "Old view map: " + oldViewMap.keySet());
                // Remove views that were not found in the traversal.
                oldViewMap.values().forEach(view -> {
                    WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                    try {
                        windowManager.removeView(view);
                    } catch (Exception e) {
                        Log.e(TAG, "View did not exist when trying to cleanup.", e);
                    }

                });
            }
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
