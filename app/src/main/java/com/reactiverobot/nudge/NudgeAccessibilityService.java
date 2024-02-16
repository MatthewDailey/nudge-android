package com.reactiverobot.nudge;

import android.accessibilityservice.AccessibilityService;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.reactiverobot.nudge.prefs.PrefsImpl.TEMP_UNBLOCK_SEC;

public class NudgeAccessibilityService extends AccessibilityService {
    private final static String TAG = NudgeAccessibilityService.class.getName();
    private final static String BACKGROUND = NudgeAccessibilityService.class.getName() + "-background";

    @Inject
    Prefs prefs;
    @Inject
    ActivePackageChecker packageChecker;

    AtomicReference<String> lastEventPackage = new AtomicReference<>(null);
    AtomicReference<Map<String, View>> viewMapRef = new AtomicReference<>(new HashMap<>());

    ExecutorService executorService = Executors.newFixedThreadPool(1);


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

    private void drawRectAt(int x, int y) {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        params.x = x; // X position
        params.y = y; // Y position
        params.height = 20;
        params.width = 20;
        View view = new RedRectangleView(getApplicationContext());
        windowManager.addView(view, params);
    }

    private int getStatusBarHeight() {
        Resources resources = getResources();
        int identifier = resources.getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight = resources.getDimensionPixelSize(identifier);
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // TODO (mjd): properly detect if there is a cutout and the size of the cutout.
//        DisplayCutout displayCutout = (new WindowInsets.Builder().build()).getDisplayCutout();
//        Log.d(TAG, "Display cutout: " + displayCutout);
//        if (displayCutout != null) {
//            return statusBarHeight;
//        }
//        return 0;
        return statusBarHeight;
    }

    private String getViewKey(AccessibilityNodeInfo source) {
        return "viewKey://" + source.getClassName() + "/" + source.getText() + "/" + source.getContentDescription();
    }

    private WindowManager.LayoutParams computeParamsForNode(AccessibilityNodeInfo source) {
        Rect rect = new Rect();
        source.getBoundsInScreen(rect);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        int statusBarHeight = getStatusBarHeight();
        if (isSourceFromNavBar(source)) {
            Rect toCoverNavBar = rectToCoverNavBarButton(source);
            params.x = toCoverNavBar.left; // X position
            params.y = toCoverNavBar.top - statusBarHeight; // Y position
            params.height = toCoverNavBar.height();
            params.width = toCoverNavBar.width();
        } else {
            params.x = rect.left; // X position
            params.y = rect.top - statusBarHeight; // Y position
            params.height = getHeightAccountingForNavBar(source, rect.top, rect.height());
            params.width = rect.width();
        }
        return params;
    }

    private void updateCoverViewForAccessibilityNode(String logTag, AccessibilityNodeInfo source, View view) {
        AccessibilityWindowInfo window = source.getWindow();
        Log.d(logTag, "window=" + window);
        Log.d(logTag, "source=" + source);
        if (window == null) {
            Log.d(logTag, "Window is null, doing nothing");
            return;
        }
        if (view == null) {
            Log.d(logTag, "View is null, doing nothing");
            return;
        }

        WindowManager.LayoutParams params = computeParamsForNode(source);
        if (params.height > 0) {
            Log.d(logTag, "Updating view with params: " + params);
            new Handler(Looper.getMainLooper()).post(() -> {
                WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                windowManager.updateViewLayout(view, params);
            });
        } else {
            Log.d(logTag, " Skipping because of <0 height. params: " + params);
        }
    }

    private synchronized void traverseAccessibilityNodes(String logTag, AccessibilityNodeInfo source, Function<AccessibilityNodeInfo, Void> f) {
        if (source == null) {
            return;
        }
        CharSequence text = source.getText();
        CharSequence contentDescription = source.getContentDescription();

        if (text != null || contentDescription != null) {
            Log.d(logTag, "Text: " + text + ", ContentDescription: " + contentDescription);
        }

        if ((text != null && text.toString().equals("Shorts"))
                || (contentDescription != null && contentDescription.toString().equals("Shorts"))) {
            Log.d(logTag, "Shorts title found.");
            f.apply(source);
        }

        if (text == null && contentDescription != null && contentDescription.toString().contains("play Short")) {
            Log.d(logTag, "Short link found.");
            f.apply(source);
        }

        for (int i = 0; i < source.getChildCount(); i++) {
            traverseAccessibilityNodes(logTag, source.getChild(i), f);
        }
    }

    class ViewAndNode {
        public Optional<View> view;
        final public AccessibilityNodeInfo node;

        public ViewAndNode(Optional<View> view, AccessibilityNodeInfo node) {
            this.view = view;
            this.node = node;
        }
    }
    ConcurrentMap<String, ViewAndNode> viewKeyToViewMap = new ConcurrentHashMap<>();
    AtomicBoolean isBackgroundThreadRunning = new AtomicBoolean(false);

    Function<AccessibilityNodeInfo, Void> saveNodesToCover = (node) -> {
        viewKeyToViewMap.putIfAbsent(getViewKey(node), new ViewAndNode(Optional.empty(), node));
        return null;
    };

    private synchronized int iterateOverKnownNodes(String logTag) {
        AtomicInteger numViews = new AtomicInteger(0);
        Handler handler = new Handler(Looper.getMainLooper());
        viewKeyToViewMap.values().forEach(viewAndNode -> {
            // TODO (mjd): Animate the changes in location so it's not sudden and jarring. https://stackoverflow.com/questions/8664939/animate-view-added-on-windowmanagera
            boolean refreshSucceeded = viewAndNode.node.refresh();
            if (viewAndNode != null && viewAndNode.view.isPresent()) {
                Log.d(logTag, "Updating existing view for node: " + viewAndNode.node);
                if (refreshSucceeded) {
                    updateCoverViewForAccessibilityNode(logTag, viewAndNode.node, viewAndNode.view.get());
                } else {
                    handler.post(() -> {
                        Log.d(logTag, "[post to main thread] Creating new view for node: " + getViewKey(viewAndNode.node) + " " + viewAndNode.node);
                        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                        windowManager.removeView(viewAndNode.view.get());
                    });
                }
            } else {
                View newView = new RedRectangleView(getApplicationContext());
                viewAndNode.view = Optional.of(newView);
                handler.post(() -> {
                    Log.d(logTag, "[post to main thread] Creating new view for node: " + getViewKey(viewAndNode.node) + " " + viewAndNode.node);
                    WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                    WindowManager.LayoutParams params = computeParamsForNode(viewAndNode.node);
                    windowManager.addView(newView, params);
                });
            }
            numViews.incrementAndGet();
        });

        Log.d(logTag, "Num views: " + numViews.get() +  " keysInMap: " + viewKeyToViewMap.keySet());
        return numViews.get();
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

        NudgeAccessibilityService service = this;
        if (eventPackageName.equals("com.google.android.youtube")) {
            traverseAccessibilityNodes(TAG, getRootInActiveWindow(), saveNodesToCover);

            Log.d(TAG, "Event for youtube backgroundThreadRunning=" + isBackgroundThreadRunning.get());
            if (!isBackgroundThreadRunning.getAndSet(true)) {
                Log.d(TAG, "Starting background thread");
                executorService.submit(() -> {
                    Log.d(BACKGROUND, "Background thread started.");
                    try {
                        while(iterateOverKnownNodes(BACKGROUND) > 0) {
                            Log.d(BACKGROUND, "Finished traversal, sleeping.");
                            try {
                                Thread.sleep(5);
                                Log.d(BACKGROUND, "Woke up from sleep.");
                            } catch (Exception e) {
                                Log.e(BACKGROUND, "Error sleeping", e);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(BACKGROUND, "Error in background thread", e);
                    }
                    isBackgroundThreadRunning.set(false);
                });
                return;
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
