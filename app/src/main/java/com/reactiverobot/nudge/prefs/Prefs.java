package com.reactiverobot.nudge.prefs;

import java.util.Set;

public interface Prefs {

    interface PinnedSubscriber {
        void onPinned(String packageName, boolean pinned);
    }

    interface CheckedSubscriber {
        void onCheckedUpdate();
    }

    void setCheckActiveEnabled(boolean enabled);

    boolean getCheckActiveEnabled();

    Set<String> getPinnedBadHabitPackages();

    Set<String> getBadHabitPackages();

    void setPackageBadHabit(String packageName, boolean badHabit);

    // TODO: Combine these into a paramterized method call.
    Set<String> getPinnedGoodOptionPackages();

    Set<String> getGoodOptionPackages();

    void setPackageGoodOption(String packageName, boolean goodOption);

    Set<String> getIndexedPackages();

    void setPackageIndexed(String packageName);

    void subscribeGoodOptions(PinnedSubscriber subscriber);
    void subscribeBadHabits(PinnedSubscriber subscriber);

    void subscribeGoodOptions(CheckedSubscriber subscriber);
    void subscribeBadHabits(CheckedSubscriber subscriber);
}
