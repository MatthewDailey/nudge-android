package com.reactiverobot.nudge.prefs;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import com.reactiverobot.nudge.info.PackageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class PrefsImpl implements Prefs {

    private static final String TAG = Prefs.class.getName();

    // TODO: Fix defaults for both sections.
    private static Set<String> getDefaultBlockedPackages() {
        Set<String> defaultPinnedPackages = new HashSet<>();
        defaultPinnedPackages.add("com.facebook.katana");
        defaultPinnedPackages.add( "com.instagram.android");
        return defaultPinnedPackages;
    }

    private final Map<PackageType, List<PinnedSubscriber>> packageTypeToPinnedSubscribers = new HashMap<>();

    private final Map<PackageType, List<CheckedSubscriber>> packageTypeToCheckedSubscriber = new HashMap<>();

    private static final String PINNED_PACKAGES_PREFIX = "pinned_packages_";
    private static final String SELECTED_PACKAGES_PREFIX = "selected_package_";
    private static final String CHECK_ACTIVE_ENABLED = "check_active_enabled";
    private static final String ONBOARDING_COMPLETE = "onboarding_complete";

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

    @Override
    public void setCheckActiveEnabled(boolean enabled) {
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
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        return am.isEnabled();
    }
}
