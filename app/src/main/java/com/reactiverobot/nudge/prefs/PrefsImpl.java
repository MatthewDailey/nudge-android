package com.reactiverobot.nudge.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import com.reactiverobot.nudge.info.PackageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class PrefsImpl implements Prefs {

    // TODO: Fix defaults for both sections.
    private static Set<String> getDefaultBlockedPackages() {
        Set<String> defaultPinnedPackages = new HashSet<>();
        defaultPinnedPackages.add("com.facebook.katana");
        defaultPinnedPackages.add( "com.instagram.android");
        return defaultPinnedPackages;
    }

    private final Map<PackageType, List<PinnedSubscriber>> packageTypeToPinnedSubscribers = new HashMap<>();

    private final Map<PackageType, List<CheckedSubscriber>> packageTypeToCheckedSubscriber = new HashMap<>();

    private static final String INDEXED_PACKAGES = "indexed_packages";

    private static final String PINNED_PACKAGES_PREFIX = "pinned_packages_";

    private static final String BLOCKED_PACKAGES = "blocked_packages";
    private static final String GOOD_OPTION_PACKAGES = "good_option_packages";

    private static final String CHECK_ACTIVE_ENABLED = "check_active_enabled";

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

    public void setCheckActiveEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(CHECK_ACTIVE_ENABLED, enabled).commit();
    }

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

    public Set<String> getBadHabitPackages() {
        return getPackages(BLOCKED_PACKAGES, new HashSet<>());
    }

    private void updateStringSet(String setKey, Set<String> originalSet, String toUpdate, boolean membership) {
        if (membership) {
            originalSet.add(toUpdate);
        } else {
            originalSet.remove(toUpdate);
        }

        getPrefs().edit().putStringSet(setKey, originalSet).commit();
    }

    synchronized public void setPackageBadHabit(String packageName, boolean badHabit) {
        updateStringSet(BLOCKED_PACKAGES, getBadHabitPackages(), packageName, badHabit);

        packageTypeToCheckedSubscriber.get(PackageType.BAD_HABIT).stream()
                .forEach(subscriber -> subscriber.onCheckedUpdate());

        if (badHabit && !getPinnedPackages(PackageType.BAD_HABIT).contains(packageName)) {
            updateStringSet(PINNED_PACKAGES_PREFIX + PackageType.BAD_HABIT.name(), getPinnedPackages(PackageType.BAD_HABIT), packageName, true);

            packageTypeToPinnedSubscribers.get(PackageType.BAD_HABIT).stream()
                    .forEach(subscriber -> subscriber.onPinned(packageName, true));
        }
    }

    @Override
    public Set<String> getPinnedPackages(PackageType packageType) {
        return getPackages(PINNED_PACKAGES_PREFIX + packageType.name(), new HashSet<>());
    }

    @Override
    public Set<String> getGoodOptionPackages() {
        return getPackages(GOOD_OPTION_PACKAGES, new HashSet<>());
    }

    @Override
    public void setPackageGoodOption(String packageName, boolean goodOption) {
        updateStringSet(BLOCKED_PACKAGES, getBadHabitPackages(), packageName, goodOption);

        packageTypeToCheckedSubscriber.get(PackageType.GOOD_OPTION).stream()
                .forEach(subscriber -> subscriber.onCheckedUpdate());

        if (goodOption && !getPinnedPackages(PackageType.GOOD_OPTION).contains(packageName)) {
            updateStringSet(PINNED_PACKAGES_PREFIX + PackageType.GOOD_OPTION.name(), getPinnedPackages(PackageType.GOOD_OPTION), packageName, true);

            packageTypeToPinnedSubscribers.get(PackageType.GOOD_OPTION).stream()
                    .forEach(subscriber -> subscriber.onPinned(packageName, true));
        }
    }

    public Set<String> getIndexedPackages() {
        return new HashSet<>(getPrefs().getStringSet(INDEXED_PACKAGES, new HashSet<String>()));
    }

    synchronized public void setPackageIndexed(String packageName) {
        updateStringSet(INDEXED_PACKAGES, getIndexedPackages(), packageName, true);

    }

    @Override
    public void addSubscriber(PinnedSubscriber subscriber, PackageType packageType) {
        packageTypeToPinnedSubscribers.get(packageType).add(subscriber);
    }

    @Override
    public void addSubscriber(CheckedSubscriber subscriber, PackageType packageType) {
        packageTypeToCheckedSubscriber.get(packageType).add(subscriber);
    }
}
