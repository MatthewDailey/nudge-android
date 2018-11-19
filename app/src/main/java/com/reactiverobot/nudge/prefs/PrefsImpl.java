package com.reactiverobot.nudge.prefs;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.reactiverobot.nudge.NudgeAccessibilityService;
import com.reactiverobot.nudge.info.PackageType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class PrefsImpl implements Prefs {

    public static final int TEMP_UNBLOCK_SEC = 60;

    private static final String TAG = Prefs.class.getName();

    private final Map<PackageType, List<PinnedSubscriber>> packageTypeToPinnedSubscribers = new HashMap<>();
    private final Map<PackageType, List<CheckedSubscriber>> packageTypeToCheckedSubscriber = new HashMap<>();

    private static final String PINNED_PACKAGES_PREFIX = "pinned_packages_";
    private static final String SELECTED_PACKAGES_PREFIX = "selected_package_";
    private static final String CHECK_ACTIVE_ENABLED = "check_active_enabled";
    private static final String ONBOARDING_COMPLETE = "onboarding_complete";
    private static final String HAS_RATED_APP = "has_rated_app";

    private final Context context;

    public static PrefsImpl from(Context context) {
        return new PrefsImpl(context);
    }

    private PrefsImpl(Context context) {
        this.context = context;

        for (PackageType packageType : PackageType.values()) {
            packageTypeToPinnedSubscribers.put(packageType, new ArrayList<>());
            packageTypeToCheckedSubscriber.put(packageType, new ArrayList<>());
        }
    }

    private SharedPreferences getPrefs() {
         return this.context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE);
    }

    private SharedPreferences getTemporaryUnblockPrefs() {
        return this.context.getSharedPreferences("nudge_temp_unblock", Context.MODE_PRIVATE);
    }

    @Override
    public void setCheckActiveEnabled(boolean enabled) {
        Log.d(TAG, "Setting checkActiveEnabled : " + enabled);
        getPrefs().edit().putBoolean(CHECK_ACTIVE_ENABLED, enabled).commit();
    }

    @Override
    public boolean getCheckActiveEnabled() {
        return getPrefs().getBoolean(CHECK_ACTIVE_ENABLED, false);
    }

    private Set<String> getPackages(String key, Set<String> defaultPackages) {
        Set<String> pinnedPackages = getPrefs().getStringSet(key, null);

        if (pinnedPackages == null) {
            pinnedPackages = defaultPackages;
            getPrefs().edit().putStringSet(key, defaultPackages).commit();
        }

        return new HashSet<>(pinnedPackages);
    }

    private void updateStringSet(String setKey, Set<String> originalSet, String toUpdate, boolean membership) {
        if (membership) {
            originalSet.add(toUpdate);
        } else {
            originalSet.remove(toUpdate);
        }

        FirebaseAnalytics.getInstance(context).setUserProperty(setKey, String.valueOf(originalSet.size()));

        getPrefs().edit().putStringSet(setKey, originalSet).commit();
    }

    @Override
    synchronized public void setPackageSelected(PackageType packageType, String packageName, boolean selected) {
        updateStringSet(SELECTED_PACKAGES_PREFIX + packageType.name(), getSelectedPackages(packageType), packageName, selected);

        packageTypeToCheckedSubscriber.get(packageType).stream()
                .forEach(subscriber -> subscriber.onCheckedUpdate());

        if (selected && !getPinnedPackages(packageType).contains(packageName)) {
            updateStringSet(PINNED_PACKAGES_PREFIX + packageType.name(), getPinnedPackages(packageType), packageName, true);

            packageTypeToPinnedSubscribers.get(packageType).stream()
                    .forEach(subscriber -> subscriber.onPinned(packageName, true));
        }
    }

    @Override
    synchronized public void unpinPackage(PackageType packageType, String packageName) {
        updateStringSet(SELECTED_PACKAGES_PREFIX + packageType.name(), getSelectedPackages(packageType), packageName, false);
        updateStringSet(PINNED_PACKAGES_PREFIX + packageType.name(), getSelectedPackages(packageType), packageName, false);

        packageTypeToPinnedSubscribers.get(packageType).stream()
                .forEach(subscriber -> subscriber.onPinned(packageName, false));
    }

    @Override
    public Set<String> getPinnedPackages(PackageType packageType) {
        return getPackages(PINNED_PACKAGES_PREFIX + packageType.name(), new HashSet<>());
    }

    @Override
    public Set<String> getSelectedPackages(PackageType packageType) {
        return getPackages(SELECTED_PACKAGES_PREFIX + packageType.name(), new HashSet<>());
    }

    @Override
    public void addSubscriber(PinnedSubscriber subscriber, PackageType packageType) {
        packageTypeToPinnedSubscribers.get(packageType).add(subscriber);
    }

    @Override
    public boolean hasCompletedOnboarding() {
        return getPrefs().getBoolean(ONBOARDING_COMPLETE, false);
    }

    @Override
    public void completeOnboarding() {
        getPrefs().edit().putBoolean(ONBOARDING_COMPLETE, true).commit();
    }

    @Override
    public boolean isAccessibilityAccessGranted() {
        final AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        final String nudgeServiceName = "com.reactiverobot.nudge/." + NudgeAccessibilityService.class.getSimpleName();
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .stream()
                .map((service) -> {
                    Log.d("DEVUG", service.getId());
                    return nudgeServiceName.equals(service.getId());
                })
                .reduce(false, (a, b) -> a || b);
    }

    @Override
    public boolean hasRatedApp() {
        return getPrefs().getBoolean(HAS_RATED_APP, false);
    }

    @Override
    public void setHasRatedApp(boolean hasRatedApp) {
        getPrefs().edit().putBoolean(HAS_RATED_APP, hasRatedApp).commit();
    }

    @Override
    public void openPlayStore() {
        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
        }
    }

    @Override
    public boolean isPackageBlocked(String packageName) {
        Set<String> blockedPackages = getSelectedPackages(PackageType.BAD_HABIT);

        long unblockedUntil = getTemporaryUnblockPrefs().getLong(packageName, 0);

        Date currentTime = Calendar.getInstance().getTime();
        if (unblockedUntil > currentTime.getTime()) {
            return false;
        }

        return blockedPackages.contains(packageName);
    }

    @Override
    public boolean isTemporarilyUnblocked(String packageName) {
        Set<String> blockedPackages = getSelectedPackages(PackageType.BAD_HABIT);

        long unblockedUntil = getTemporaryUnblockPrefs().getLong(packageName, 0);

        Date currentTime = Calendar.getInstance().getTime();

        return blockedPackages.contains(packageName) && unblockedUntil > currentTime.getTime();
    }

    @Override
    public void setTemporarilyUnblocked(String packageName, boolean isUnblocked) {
        if (isUnblocked) {
            Date currentTime = Calendar.getInstance().getTime();
            getTemporaryUnblockPrefs().edit().putLong(packageName, currentTime.getTime() + (TEMP_UNBLOCK_SEC * 1000)).commit();
        } else {
            getTemporaryUnblockPrefs().edit().remove(packageName).commit();
        }
    }

}
