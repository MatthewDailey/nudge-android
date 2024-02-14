package com.reactiverobot.nudge;

import android.accessibilityservice.AccessibilityService;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
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

    private int getScreenHeight(AccessibilityNodeInfo source) {
        Rect screenRect = new Rect();
        AccessibilityWindowInfo window = source.getWindow();
        window.getBoundsInScreen(screenRect);
        return screenRect.height();
    }

    private int getScreenWidth(AccessibilityNodeInfo source) {
        Rect screenRect = new Rect();
        AccessibilityWindowInfo window = source.getWindow();
        window.getBoundsInScreen(screenRect);
        return screenRect.width();
    }

    private int navBarHeightPx() {
        float dip = 72f;
        Resources r = getResources();
        float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                r.getDisplayMetrics()
        );
        return (int) px;
    }

    private int bottomLatchHeightPx() {
        float dip = 24f;
        Resources r = getResources();
        float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                r.getDisplayMetrics()
        );
        return (int) px;
    }

    private boolean isSourceFromNavBar(AccessibilityNodeInfo source) {
        int px = navBarHeightPx();
        int screenHeight = getScreenHeight(source);
        Rect rect = new Rect();
        source.getBoundsInWindow(rect);

        return rect.top > screenHeight - px;
    }
    private int getHeightAccountingForNavBar(AccessibilityNodeInfo source, int y, int initialHeight) {
        int px = navBarHeightPx();
        int screenHeight = getScreenHeight(source);

        if (y + initialHeight > screenHeight - px) {
            return screenHeight - y - px;
        }
        return initialHeight;
    }

    private Rect rectToCoverNavBarButton(AccessibilityNodeInfo source) {
        int px = navBarHeightPx();
        int screenHeight = getScreenHeight(source);
        int screenWidth = getScreenWidth(source);
        int buttonWidth = screenWidth / 4;

        Rect rect = new Rect();
        source.getBoundsInWindow(rect);
        int centerX = rect.centerX();
        rect.left = centerX - buttonWidth / 2;
        rect.right = centerX + buttonWidth / 2;
        rect.top = screenHeight - px;
        rect.bottom = screenHeight - bottomLatchHeightPx();
        return rect;
    }

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
        source.getBoundsInScreen(rect); // TODO: try wthis on phone
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        if (isSourceFromNavBar(source)) {
            Rect toCoverNavBar = rectToCoverNavBarButton(source);
            params.x = toCoverNavBar.left; // X position
            params.y = toCoverNavBar.top; // Y position
            params.height = toCoverNavBar.height();
            params.width = toCoverNavBar.width();
        } else {
            params.x = rect.left; // X position
            params.y = rect.top; // Y position
            params.height = getHeightAccountingForNavBar(source, rect.top, rect.height());
            params.width = rect.width();
        }
        if (params.height > 0) {
            View view = oldViewMap.get(viewKey);
            if (view != null) {
                windowManager.updateViewLayout(view, params);
            } else {
                view = new RedRectangleView(getApplicationContext());
                windowManager.addView(view, params);
            }
            newViewMap.put(viewKey, view);
        }
    }

    private void traverseAccessibilityNodes(AccessibilityNodeInfo source, Map<String, View> oldViewMap, Map<String, View> newViewMap) {
        if (source == null) {
            return;
        }

        CharSequence text = source.getText();
        CharSequence contentDescription = source.getContentDescription();

        if (text != null || contentDescription != null) {
            Log.d(TAG, "Text: " + text + ", ContentDescription: " + contentDescription);
        }

        if ((text != null && text.toString().equals("Shorts"))
                || (contentDescription != null && contentDescription.toString().equals("Shorts"))) {
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
        if (packageName == null) {
            return;
        }
        Log.d(TAG, "===============================================");
        Log.d(TAG, event.toString());

        String eventPackageName = packageName.toString();
        lastEventPackage.set(eventPackageName);
        int contentChangeType = event.getContentChangeTypes();

        // TODO (mjd): Traverse more frequently than events.
        // TODO (mjd): Only cover the parts of the views that are visible.
        if (eventPackageName.equals("com.google.android.youtube")) {
                Map<String, View> oldViewMap = viewMapRef.get();
                Map<String, View> newViewMap = new HashMap<>();
                if (event.getSource() != null && event.getSource().getWindow() != null) {
                    getWindows().forEach(window -> Log.d(TAG, "Window: " + window));
                    AccessibilityWindowInfo eventWindow = event.getSource().getWindow();
                    Log.d(TAG, "Event window: " + eventWindow);
                    Log.d(TAG, "Event window root: " + eventWindow.getRoot());
                    Log.d(TAG, "Event source: " + event.getSource());
                    AccessibilityNodeInfo root = eventWindow.getRoot();
                    traverseAccessibilityNodes(root, oldViewMap, newViewMap);
                    viewMapRef.set(newViewMap);
                }
                Log.d(TAG, "Old view map size: " + oldViewMap.size());
                Log.d(TAG, "New view map size: " + newViewMap.size());
                Log.d(TAG, "Old view map: " + oldViewMap.keySet());
                // Remove views that were not found in the traversal.
//                oldViewMap.values().forEach(view -> {
//                    WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
//                    try {
//                        windowManager.removeView(view);
//                    } catch (Exception e) {
//                        Log.e(TAG, "View did not exist when trying to cleanup.", e);
//                    }
//                });
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
