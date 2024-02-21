package com.reactiverobot.nudge.youtubeshorts;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.reactiverobot.nudge.InterceptShortActivity;
import com.reactiverobot.nudge.nicotine.NicotineApi;
import com.reactiverobot.nudge.prefs.Prefs;

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

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class YoutubeShortInterceptor implements YoutubeAccessibilityEventListener {

    private static final String TAG = YoutubeShortInterceptor.class.getName();
    private final static int DEBOUNCE_MS = 500;
    private final static Set<String> KNOWN_SHORT_UI_STRINGS = new HashSet<>(Arrays.asList(
            "Subscribe", "Dislike", "Share", "Remix"
    ));
    private final static Set<String> KNOWN_SHORT_UI_ELEMENT_DESCRIPTIONS = new HashSet<>(Arrays.asList(
            "Dislike this video", "Share this video", "Remix", "See more videos using this sound"
    ));
    private final AccessibilityService accessibilityService;
    private final Prefs prefs;

    private final AtomicLong allowedTimestampToClearShortVideoDescription = new AtomicLong(0);
    private final AtomicBoolean isCheckingShortDescriptions = new AtomicBoolean(false);
    private final AtomicReference<String> lastShortVideoDescription = new AtomicReference<>(null);

    public YoutubeShortInterceptor(AccessibilityService accessibilityService, Prefs prefs) {
        this.accessibilityService = accessibilityService;
        this.prefs = prefs;
    }

    @Override
    public void onYoutubeEvent(AccessibilityEvent event) {
        if (!prefs.isInterceptShortsEnabled()) {
            return;
        }

        interceptShortIfNecessary(event);
    }

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
                        Intent intent = new Intent(accessibilityService.getApplicationContext(), InterceptShortActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        accessibilityService.getApplicationContext().startActivity(intent);
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

    private Optional<String> isShortVideoEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        if (packageName == null || !packageName.toString().equals("com.google.android.youtube")) {
            return Optional.empty();
        }

        AtomicReference<String> result = new AtomicReference<>(null);
        Map<String, Boolean> contentDescriptions = new ConcurrentHashMap<>();
        traverseAllAccessibilityNodes(accessibilityService.getRootInActiveWindow(), (accessibilityNodeInfo) -> {
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

    private synchronized void traverseAllAccessibilityNodes(AccessibilityNodeInfo source, Function<AccessibilityNodeInfo, Void> f) {
        if (source == null) {
            return;
        }
        f.apply(source);
        for (int i = 0; i < source.getChildCount(); i++) {
            traverseAllAccessibilityNodes(source.getChild(i), f);
        }
    }

    private void areTwoShortsSimilar(String description1,
                                     String description2,
                                     Consumer<Boolean> onSuccess,
                                     Consumer<Throwable> onError) {
        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

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
            public void onResponse(Call<NicotineApi.CompareShortsResponse> call,
                                   retrofit2.Response<NicotineApi.CompareShortsResponse> response) {
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
