package com.reactiverobot.nudge;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.reactiverobot.nudge.checker.ActivePackageChecker;
import com.reactiverobot.nudge.nicotine.NicotineApi;
import com.reactiverobot.nudge.prefs.Prefs;
import com.reactiverobot.nudge.youtubeshorts.YoutubeAccessibilityEventListener;
import com.reactiverobot.nudge.youtubeshorts.YoutubeShortBlocker;
import com.reactiverobot.nudge.youtubeshorts.YoutubeShortInterceptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final static String TAG = NudgeAccessibilityService.class.getName() + "-foreground";
    @Inject
    Prefs prefs;
    @Inject
    ActivePackageChecker packageChecker;

    YoutubeAccessibilityEventListener youtubeShortBlocker;
    YoutubeAccessibilityEventListener youtubeShortInterceptor;


    AtomicReference<String> lastEventPackage = new AtomicReference<>(null);

    private static NudgeAccessibilityService instance;
    public static NudgeAccessibilityService instance() {
        return instance;
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

        if (eventPackageName.equals("com.google.android.youtube")) {
            youtubeShortInterceptor.onYoutubeEvent(event);
            youtubeShortBlocker.onYoutubeEvent(event);
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
        instance = this;
        youtubeShortBlocker = new YoutubeShortBlocker(this, prefs);
        youtubeShortInterceptor = new YoutubeShortInterceptor(this, prefs);
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

}
