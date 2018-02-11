package com.reactiverobot.nudge.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class PrefsImpl implements Prefs {

    // TODO: Fix defaults for both sections.
    private static Set<String> getDefaultBlockedPackages() {
        Set<String> defaultPinnedPackages = new HashSet<>();
        defaultPinnedPackages.add("com.facebook.katana");
        defaultPinnedPackages.add( "com.instagram.android");
        return defaultPinnedPackages;
    }

    private final List<PinnedSubscriber> pinnedSubscribersGoodOptions = new ArrayList<>();
    private final List<CheckedSubscriber> checkedSubscribersGoodOptions = new ArrayList<>();

    private final List<PinnedSubscriber> pinnedSubscribersBadHabits = new ArrayList<>();
    private final List<CheckedSubscriber> checkedSubscribersBadHabits = new ArrayList<>();

    private static final Set<String> DEFAULT_BLOCKED_PACKAGES = getDefaultBlockedPackages();

    private static final String INDEXED_PACKAGES = "indexed_packages";

    private static final String PINNED_BAD_HABIT_PACKAGES = "pinned_bad_habit_packages";
    private static final String BLOCKED_PACKAGES = "blocked_packages";

    private static final String PINNED_GOOD_OPTION_PACKAGES = "pinned_good_option_packages";
    private static final String GOOD_OPTION_PACKAGES = "good_option_packages";

    private static final String CHECK_ACTIVE_ENABLED = "check_active_enabled";

    private final Context context;

    public static PrefsImpl from(Context context) {
        return new PrefsImpl(context);
    }

    private PrefsImpl(Context context) {
        this.context = context;
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

    public Set<String> getPinnedBadHabitPackages() {
        return getPackages(PINNED_BAD_HABIT_PACKAGES, DEFAULT_BLOCKED_PACKAGES);
    }

    public Set<String> getBadHabitPackages() {
        return getPackages(BLOCKED_PACKAGES, DEFAULT_BLOCKED_PACKAGES);
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

        checkedSubscribersBadHabits.stream()
                .forEach(subscriber -> subscriber.onCheckedUpdate());

        if (badHabit && !getPinnedBadHabitPackages().contains(packageName)) {
            updateStringSet(PINNED_BAD_HABIT_PACKAGES, getPinnedBadHabitPackages(), packageName, true);

            pinnedSubscribersBadHabits.stream()
                    .forEach(subscriber -> subscriber.onPinned(packageName, true));
        }
    }

    @Override
    public Set<String> getPinnedGoodOptionPackages() {
        return getPackages(PINNED_GOOD_OPTION_PACKAGES, new HashSet<>());
    }

    @Override
    public Set<String> getGoodOptionPackages() {
        return getPackages(GOOD_OPTION_PACKAGES, new HashSet<>());
    }

    @Override
    public void setPackageGoodOption(String packageName, boolean goodOption) {
        updateStringSet(BLOCKED_PACKAGES, getBadHabitPackages(), packageName, goodOption);

        checkedSubscribersGoodOptions.stream()
                .forEach(subscriber -> subscriber.onCheckedUpdate());

        if (goodOption && !getPinnedGoodOptionPackages().contains(packageName)) {
            updateStringSet(PINNED_GOOD_OPTION_PACKAGES, getPinnedBadHabitPackages(), packageName, true);

            pinnedSubscribersGoodOptions.stream()
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
    public void subscribeGoodOptions(PinnedSubscriber subscriber) {
        pinnedSubscribersGoodOptions.add(subscriber);
    }

    @Override
    public void subscribeBadHabits(PinnedSubscriber subscriber) {
        pinnedSubscribersBadHabits.add(subscriber);
    }

    @Override
    public void subscribeGoodOptions(CheckedSubscriber subscriber) {
        checkedSubscribersGoodOptions.add(subscriber);
    }

    @Override
    public void subscribeBadHabits(CheckedSubscriber subscriber) {
        checkedSubscribersBadHabits.add(subscriber);
    }
}
