package com.reactiverobot.nudge.youtubeshorts;

import static android.content.Context.WINDOW_SERVICE;

import android.accessibilityservice.AccessibilityService;
import android.animation.ValueAnimator;
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

import com.reactiverobot.nudge.RectangleView;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class YoutubeShortBlocker implements YoutubeAccessibilityEventListener {

    private static final String TAG = YoutubeAccessibilityEventListener.class.getName();
    private final static int INTERVAL_UPDATE_THREAD_SLEEP = 10;
    private final static int DURATION_ANIMATE_COVER = 10;
    private final AccessibilityService accessibilityService;
    private final Prefs prefs;

    private final AtomicBoolean isYoutubeNavBarVisible = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    private final ConcurrentMap<String, ViewAndNode> viewKeyToViewMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isBackgroundThreadRunning = new AtomicBoolean(false);

    private static class ViewAndNode {
        public Optional<View> view;
        final public AccessibilityNodeInfo node;

        public ViewAndNode(Optional<View> view, AccessibilityNodeInfo node) {
            this.view = view;
            this.node = node;
        }
    }

    private Function<AccessibilityNodeInfo, Void> saveNodesToCover = (node) -> {
        viewKeyToViewMap.putIfAbsent(getViewKey(node), new ViewAndNode(Optional.empty(), node));
        return null;
    };

    public YoutubeShortBlocker(AccessibilityService accessibilityService, Prefs prefs) {
        this.accessibilityService = accessibilityService;
        this.prefs = prefs;
    }

    public void onYoutubeEvent(AccessibilityEvent event) {
        if (!prefs.isBlockShortsEnabled()) {
            return;
        }

        traverseAccessibilityNodesForNodesToCover(accessibilityService.getRootInActiveWindow(),
                saveNodesToCover);

        Log.d(TAG, "Event for youtube backgroundThreadRunning=" + isBackgroundThreadRunning.get());
        if (!isBackgroundThreadRunning.getAndSet(true)) {
            Log.d(TAG, "Starting background thread");
            executorService.submit(() -> {
                Log.d(TAG, "Background thread started.");
                try {
                    while (iterateOverKnownNodes() > 0) {
                        Log.d(TAG, "Finished traversal, sleeping.");
                        try {
                            Thread.sleep(INTERVAL_UPDATE_THREAD_SLEEP);
                            Log.d(TAG, "Woke up from sleep.");
                        } catch (Exception e) {
                            Log.e(TAG, "Error sleeping", e);
                        }
                    }
                    Log.d(TAG, "No more views to update, stopping background thread.");
                } catch (Exception e) {
                    Log.e(TAG, "Error in background thread", e);
                }
                isBackgroundThreadRunning.set(false);
            });
        }
    }


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
        Resources r = accessibilityService.getResources();
        float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                r.getDisplayMetrics()
        );
        return (int) px;
    }

    private int bottomLatchHeightPx() {
        float dip = 24f;
        Resources r = accessibilityService.getResources();
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

    private int getHeightAccountingForNavBar(AccessibilityNodeInfo source, int y,
                                             int initialHeight) {
        int px = navBarHeightPx();
        int screenHeight = getScreenHeight(source);


        if (isYoutubeNavBarVisible.get() && y + initialHeight > screenHeight - px) {
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

    private String getViewKey(AccessibilityNodeInfo source) {
        return "viewKey://" + source.getClassName() + "/" + source.getText() + "/" + source.getContentDescription();
    }

    private synchronized void traverseAccessibilityNodesForNodesToCover(
            AccessibilityNodeInfo source, Function<AccessibilityNodeInfo, Void> f) {
        if (source == null) {
            return;
        }
        CharSequence text = source.getText();
        CharSequence contentDescription = source.getContentDescription();
        Log.v(TAG + "-verbose", source.toString());

        if (text != null || contentDescription != null) {
            Log.d(TAG, "Text: " + text + ", ContentDescription: " + contentDescription);
        }

        if ((text != null && text.toString().equals("Shorts"))
                || (contentDescription != null && contentDescription.toString().equals("Shorts"))) {
            Log.d(TAG, "Shorts title found.");
            f.apply(source);
        }

        if (text == null && contentDescription != null && contentDescription.toString()
                .contains("play Short")) {
            Log.d(TAG, "Short link found.");
            f.apply(source);
        }

        for (int i = 0; i < source.getChildCount(); i++) {
            traverseAccessibilityNodesForNodesToCover(source.getChild(i), f);
        }
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
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

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
        return params;
    }

    private boolean updateViewCoveringNode(ViewAndNode viewAndNode) {
        AccessibilityWindowInfo window = viewAndNode.node.getWindow();
        View view = viewAndNode.view.orElse(null);
        AccessibilityNodeInfo node = viewAndNode.node;
        if (window == null) {
            Log.d(TAG, "Window is null, doing nothing");
            return false;
        }
        if (view == null) {
            Log.d(TAG, "View is null, doing nothing");
            return false;
        }
        Log.d(TAG, "window=" + window);
        Log.d(TAG, "node=" + node);

        WindowManager.LayoutParams priorParams = computeParamsForNode(node);
        boolean refreshSucceeded = node.refresh();
        if (refreshSucceeded) {
            WindowManager.LayoutParams newParams = computeParamsForNode(node);
            if (newParams.height > 0) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    animateViewYParam(view, priorParams, newParams);
                });
            } else {
                Log.d(TAG, " Skipping because of <0 height. params: " + newParams);
            }
        }
        return refreshSucceeded;
    }

    private void animateViewYParam(View view, WindowManager.LayoutParams priorParams,
                                   WindowManager.LayoutParams newParams) {
        if (priorParams.y != newParams.y) {
            ValueAnimator animator = ValueAnimator.ofInt(priorParams.y, newParams.y);
            animator.addUpdateListener(animation -> {
                WindowManager.LayoutParams animationFrameParams = new WindowManager.LayoutParams();
                animationFrameParams.copyFrom(newParams);
                animationFrameParams.y = (int) animation.getAnimatedValue();
                WindowManager windowManager = (WindowManager) accessibilityService.getSystemService(
                        WINDOW_SERVICE);
                synchronized (accessibilityService) {
                    if (view.isAttachedToWindow()) {
                        windowManager.updateViewLayout(view, animationFrameParams);
                    }
                }

            });
            animator.setDuration(DURATION_ANIMATE_COVER);
            animator.start();
        }
    }

    private void addViewCoveringNode(ViewAndNode viewAndNode) {
        View newView = new RectangleView(accessibilityService.getApplicationContext());
        viewAndNode.view = Optional.of(newView);
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isSourceFromNavBar(viewAndNode.node)) {
                Log.v(TAG, "Setting nav bar visible");
                isYoutubeNavBarVisible.set(true);
            }
            Log.d(TAG, "[post to main thread] Creating new view for node: " + getViewKey(
                    viewAndNode.node) + " " + viewAndNode.node);
            WindowManager windowManager = (WindowManager) accessibilityService.getSystemService(
                    WINDOW_SERVICE);
            WindowManager.LayoutParams params = computeParamsForNode(viewAndNode.node);
            windowManager.addView(newView, params);
        });
    }

    private void removeView(ViewAndNode viewAndNode) {
        new Handler(Looper.getMainLooper()).post(() -> {
            boolean sourceFromNavBar = isSourceFromNavBar(viewAndNode.node);

            if (viewAndNode.view.isPresent()) {
                WindowManager windowManager = (WindowManager) accessibilityService.getSystemService(
                        WINDOW_SERVICE);
                try {
                    windowManager.removeView(viewAndNode.view.get());
                } catch (Exception e) {
                    Log.e(TAG, "Error removing view", e);
                }
            }

            if (sourceFromNavBar) {
                Log.v(TAG, "Setting nav bar hidden");
                isYoutubeNavBarVisible.set(false);
            }
            viewKeyToViewMap.remove(getViewKey(viewAndNode.node));
        });
    }

    private synchronized int iterateOverKnownNodes() {
        AtomicInteger numViews = new AtomicInteger(0);
        viewKeyToViewMap.values().forEach(viewAndNode -> {
            if (viewAndNode.node == null) {
                return;
            }

            if (!prefs.isBlockShortsEnabled() && viewAndNode.view.isPresent()) {
                Log.d(TAG, "Block shorts disabled. Removing view for node: " + getViewKey(
                        viewAndNode.node));
                removeView(viewAndNode);
            } else if (viewAndNode.view.isPresent()) {
                Log.d(TAG, "Updating existing view for node: " + viewAndNode.node);
                boolean didFindForUpdate = updateViewCoveringNode(viewAndNode);
                if (!didFindForUpdate) {
                    removeView(viewAndNode);
                }
            } else {
                Log.d(TAG, "Creating new view for node: " + getViewKey(
                        viewAndNode.node) + " " + viewAndNode.node);
                addViewCoveringNode(viewAndNode);
            }
            numViews.incrementAndGet();
        });

        Log.d(TAG, "Num views: " + numViews.get() + " keysInMap: " + viewKeyToViewMap.keySet());
        return numViews.get();
    }

}
