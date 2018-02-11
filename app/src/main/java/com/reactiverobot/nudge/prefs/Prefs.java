package com.reactiverobot.nudge.prefs;

import com.reactiverobot.nudge.PackageInfo;
import com.reactiverobot.nudge.info.PackageType;

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

    void addSubscriber(PinnedSubscriber subscriber, PackageType packageType);

    void subscribeGoodOptions(CheckedSubscriber subscriber);
    void subscribeBadHabits(CheckedSubscriber subscriber);
}
