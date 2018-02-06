package com.reactiverobot.nudge.prefs;

import java.util.Set;

public interface Prefs {

    interface Subscriber {
        void onBadHabitPinned(String packageName, boolean pinned);
        // TODO onGoodOptionPinned
    }

    void setCheckActiveEnabled(boolean enabled);

    boolean getCheckActiveEnabled();

    Set<String> getPinnedBadHabitPackages();

    Set<String> getBadHabitPackages();

    void setPackageBadHabit(String packageName, boolean badHabit);

    // TODO setPackageGoodOption

    Set<String> getIndexedPackages();

    void setPackageIndexed(String packageName);

    void subscribe(Subscriber subscriber);
}
