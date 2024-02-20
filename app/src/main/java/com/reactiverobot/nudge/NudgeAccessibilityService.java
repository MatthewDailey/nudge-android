package com.reactiverobot.nudge;

import android.accessibilityservice.AccessibilityService;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
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
import com.reactiverobot.nudge.nicotine.NicotineApi;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.android.AndroidInjection;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.reactiverobot.nudge.prefs.PrefsImpl.TEMP_UNBLOCK_SEC;

@Singleton
public class NudgeAccessibilityService extends AccessibilityService {

    private final static int INTERVAL_UPDATE_THREAD_SLEEP = 30000;
    private final static int DURATION_ANIMATE_COVER = 100;

    private final static String TAG = NudgeAccessibilityService.class.getName() + "-foreground";
    private final static String BACKGROUND = NudgeAccessibilityService.class.getName() + "-background";

    @Inject
    Prefs prefs;
    @Inject
    ActivePackageChecker packageChecker;


    AtomicReference<String> lastEventPackage = new AtomicReference<>(null);
    AtomicBoolean isYoutubeNavBarVisible = new AtomicBoolean(false);
    AtomicReference<Map<String, View>> viewMapRef = new AtomicReference<>(new HashMap<>());

    ExecutorService executorService = Executors.newFixedThreadPool(1);

    private static NudgeAccessibilityService instance;
    public static NudgeAccessibilityService instance() {
        return instance;
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

    private boolean updateCoverViewForAccessibilityNode(String logTag, AccessibilityNodeInfo source, View view) {
        AccessibilityWindowInfo window = source.getWindow();
        Log.d(logTag, "window=" + window);
        Log.d(logTag, "source=" + source);
        if (window == null) {
            Log.d(logTag, "Window is null, doing nothing");
            return false;
        }
        if (view == null) {
            Log.d(logTag, "View is null, doing nothing");
            return false;
        }

        Handler handler = new Handler(Looper.getMainLooper());

        WindowManager.LayoutParams priorParams = computeParamsForNode(source);
        boolean refreshSucceeded = source.refresh();
        if (refreshSucceeded) {
            WindowManager.LayoutParams params = computeParamsForNode(source);
            if (params.height > 0) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (priorParams.y != params.y) {
                        ValueAnimator animator = ValueAnimator.ofInt(priorParams.y, params.y);
                        animator.addUpdateListener(animation -> {
                            WindowManager.LayoutParams animationFrameParams = new WindowManager.LayoutParams();
                            animationFrameParams.copyFrom(params);
                            animationFrameParams.y = (int) animation.getAnimatedValue();
                            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                            synchronized (NudgeAccessibilityService.this) {
                                if (view.isAttachedToWindow()) {
                                    windowManager.updateViewLayout(view, animationFrameParams);
                                }
                            }

                        });
                        animator.setDuration(DURATION_ANIMATE_COVER);
                        animator.start();
                    }
//                    WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
//                    windowManager.removeView(view);
//                    windowManager.addView(view, params);
                });

            } else {
                Log.d(logTag, " Skipping because of <0 height. params: " + params);
            }
            return true;
        } else {
            handler.post(() -> {
                Log.d(logTag, "[post to main thread] Removing view for node: " + getViewKey(source) + " " + source);
                WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                try {
                    windowManager.removeView(view);
                } catch (Exception e) {
                    Log.e(logTag, "Error removing view", e);
                }
            });
            return false;
        }
    }

    private static Set<String> KNOWN_SHORT_UI_STRINGS = new HashSet<>(Arrays.asList(
            "Subscribe", "Dislike", "Share", "Remix"
    ));
    private boolean isShortVideoDescription(AccessibilityNodeInfo source) {
        CharSequence text = source.getText();
        CharSequence contentDescription = source.getContentDescription();
        boolean sameTextAndDescription = text != null && contentDescription != null && text.toString().equals(contentDescription.toString());
        boolean isNotKnownUiString = text != null && !KNOWN_SHORT_UI_STRINGS.contains(text.toString());
        boolean isNotUserName = text != null && !text.toString().startsWith("@");
        boolean hasNoClassName = source.getClassName().toString().equals("");
        boolean isNotNumber = text != null && !text.toString().matches("/^\\d*\\.?\\d*(K|M)$");
        return sameTextAndDescription && isNotKnownUiString && isNotUserName && hasNoClassName && isNotNumber;
    }

    private static Set<String> KNOWN_SHORT_UI_ELEMENT_DESCRIPTIONS = new HashSet<>(Arrays.asList(
            "Dislike this video", "Share this video", "Remix", "See more videos using this sound"
    ));
    private Optional<String> isShortVideoEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        if (packageName == null || !packageName.toString().equals("com.google.android.youtube")) {
            return Optional.empty();
        }

        AtomicReference<String> result = new AtomicReference<>(null);
        Map<String, Boolean> contentDescriptions = new ConcurrentHashMap<>();
        traverseAllAccessibilityNodes(getRootInActiveWindow(), (accessibilityNodeInfo) -> {
            CharSequence text = accessibilityNodeInfo.getText();
            CharSequence contentDescription = accessibilityNodeInfo.getContentDescription();
            if (text == null && contentDescription != null) {
                contentDescriptions.put(contentDescription.toString(), true);
            }
            if (isShortVideoDescription(accessibilityNodeInfo)) {
                result.set(text.toString());
            }
            return null;
        });

        if (contentDescriptions.keySet().containsAll(KNOWN_SHORT_UI_ELEMENT_DESCRIPTIONS)) {
            return Optional.of(result.get());
        }
        return Optional.empty();
    }

    private synchronized void traverseAccessibilityNodesForNodesToCover(String logTag, AccessibilityNodeInfo source, Function<AccessibilityNodeInfo, Void> f) {
        if (source == null) {
            return;
        }
        CharSequence text = source.getText();
        CharSequence contentDescription = source.getContentDescription();
        Log.v(TAG + "-verbose", source.toString());

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
            traverseAccessibilityNodesForNodesToCover(logTag, source.getChild(i), f);
        }
    }

    private synchronized void traverseAllAccessibilityNodes(AccessibilityNodeInfo source, Function<AccessibilityNodeInfo, Void> f) {
        if (source == null) {
            return;
        }
        f.apply(source);
        for (int i = 0; i < source.getChildCount(); i++) {
            traverseAllAccessibilityNodes(source.getChild(i), f);
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
            if (viewAndNode.node == null) {
                return;
            }
            if (!prefs.isBlockShortsEnabled() && viewAndNode.view.isPresent()) {
                handler.post(() -> {
                    Log.d(logTag, "[post to main thread] Block shorts disabled. Removing view for node: " + getViewKey(viewAndNode.node));
                    WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                    try {
                        if (viewAndNode.view.get().isAttachedToWindow()) {
                            windowManager.removeView(viewAndNode.view.get());
                        }
                    } catch (Exception e) {
                        Log.e(logTag, "Error removing view", e);
                    }
                });
            } else if (viewAndNode.view.isPresent()) {
                Log.d(logTag, "Updating existing view for node: " + viewAndNode.node);
                boolean didFindForUpdate = updateCoverViewForAccessibilityNode(logTag, viewAndNode.node, viewAndNode.view.get());
                if (!didFindForUpdate) {
                    if (isSourceFromNavBar(viewAndNode.node)) {
                        Log.d(TAG + "-nav", "Setting nav bar hidden");
                        isYoutubeNavBarVisible.set(false);
                    }
                    viewKeyToViewMap.remove(getViewKey(viewAndNode.node));
                }
            } else {
                View newView = new RedRectangleView(getApplicationContext());
                viewAndNode.view = Optional.of(newView);
                handler.post(() -> {
                    if (isSourceFromNavBar(viewAndNode.node)) {
                        Log.d(TAG + "-nav", "Setting nav bar visible");
                        isYoutubeNavBarVisible.set(true);
                    }
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

        if (eventPackageName.equals("com.google.android.youtube") && prefs.isInterceptShortsEnabled()) {
            interceptShortIfNecessary(event);
        }

        if (eventPackageName.equals("com.google.android.youtube") && prefs.isBlockShortsEnabled()) {
            traverseAccessibilityNodesForNodesToCover(TAG, getRootInActiveWindow(), saveNodesToCover);

            Log.d(TAG, "Event for youtube backgroundThreadRunning=" + isBackgroundThreadRunning.get());
            if (!isBackgroundThreadRunning.getAndSet(true)) {
                Log.d(TAG, "Starting background thread");
                executorService.submit(() -> {
                    Log.d(BACKGROUND, "Background thread started.");
                    try {
                        while (iterateOverKnownNodes(BACKGROUND) > 0) {
                            Log.d(BACKGROUND, "Finished traversal, sleeping.");
                            try {
                                Thread.sleep(INTERVAL_UPDATE_THREAD_SLEEP);
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

    private final static int DEBOUNCE_MS = 500;
    AtomicLong allowedTimestampToClearShortVideoDescription = new AtomicLong(0);
    AtomicBoolean isCheckingShortDescriptions = new AtomicBoolean(false);
    AtomicReference<String> lastShortVideoDescription = new AtomicReference<>(null);

    private void updateLastShortVideoDescription(String description) {
        long currentTime = System.currentTimeMillis();

        long allowedTime = allowedTimestampToClearShortVideoDescription.getAndUpdate((allowedNextTime) -> {
            if (description == null) {
                return allowedNextTime;
            }

            // Bump up the time if the description exists.
            return currentTime + DEBOUNCE_MS;
        });

        if (currentTime < allowedTime) {
            Log.d(TAG, "Debouncing launching SuggestChangeActivity");
            return;
        }

        lastShortVideoDescription.set(description);
    }

    private void interceptShortIfNecessary(AccessibilityEvent event) {
        Optional<String> shortVideoDescription = isShortVideoEvent(event);
        // TODO: figure out how to properly handle non-short events that fall in between short events
        // TODO: ignore ad
        if (shortVideoDescription.isPresent()) {
            String thisDescription = shortVideoDescription.get();
            String priorDescription = lastShortVideoDescription.get();
            Log.d(TAG+ "-nico", "Found short video. thisDescription=" + thisDescription + " priorDescription=" + priorDescription);
            if (priorDescription != null
                    && !thisDescription.equals(priorDescription)
                    && !isCheckingShortDescriptions.getAndSet(true)) {
                areTwoShortsSimilar(thisDescription, priorDescription, (areSimilar) -> {
                    if (!areSimilar) {
                        Log.d(TAG + "-nico", "Blocking short video: " + shortVideoDescription.get());
                        Intent intent = new Intent(getApplicationContext(), InterceptShortActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplicationContext().startActivity(intent);
                        lastShortVideoDescription.set(null);
                        allowedTimestampToClearShortVideoDescription.set(0);
                    } else {
                        Log.d(TAG + "-nico", "Not blocking short video: " + shortVideoDescription.get());
                        lastShortVideoDescription.set(thisDescription);
                    }
                    isCheckingShortDescriptions.set(false);
                }, (error) -> {
                    Log.e(TAG + "-nico", "Error comparing shorts", error);
                    isCheckingShortDescriptions.set(false);
                });
            } else {
                updateLastShortVideoDescription(shortVideoDescription.get());
            }
            Log.d(TAG + "-nico", "Found short video: " + shortVideoDescription.get());
        } else {
            updateLastShortVideoDescription(null);
            Log.d(TAG + "-nico", "No short video found. " + event);
        }
    }

    @Override
    public void onServiceConnected() {
        AndroidInjection.inject(this);
        instance = this;
        Log.d(TAG, "Connecting service");
        super.onServiceConnected();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbinding service");
        instance = null;
        return super.onUnbind(intent);
    }

    public void goBack() {
        Log.d(TAG, "goBack");
        performGlobalAction(GLOBAL_ACTION_BACK);
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    private void areTwoShortsSimilar(String description1, String description2, Consumer<Boolean> onSuccess, Consumer<Throwable> onError) {
        final OkHttpClient okHttpClient = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).connectTimeout(60, TimeUnit.SECONDS).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(NicotineApi.COMPARE_SHORTS_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NicotineApi service = retrofit.create(NicotineApi.class);
        Call<NicotineApi.CompareShortsResponse> compareShortsResponseCall = service.compareYoutubeShortDescriptions(
                description1,
                description2);

        // get start time
        long startTime = System.currentTimeMillis();

        Log.d(TAG + "-nico", "Making request: " + description1 + " " + description2);
        compareShortsResponseCall.enqueue(new retrofit2.Callback<NicotineApi.CompareShortsResponse>() {
            @Override
            public void onResponse(Call<NicotineApi.CompareShortsResponse> call, retrofit2.Response<NicotineApi.CompareShortsResponse> response) {
                // get end time
                long endTime = System.currentTimeMillis();
                Log.d(TAG + "-nico", "[" + (endTime - startTime) + "ms] Got response: " + response.body());
                onSuccess.accept(response.body().similar);
            }

            @Override
            public void onFailure(Call<NicotineApi.CompareShortsResponse> call, Throwable t) {
                Log.e(TAG + "-nico", "Error: " + t.getMessage());
                onError.accept(t);
            }
        });
    }
}
