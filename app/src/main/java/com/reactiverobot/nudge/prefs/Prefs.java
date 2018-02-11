package com.reactiverobot.nudge.prefs;

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

    Set<String> getBadHabitPackages();

    void setPackageBadHabit(String packageName, boolean badHabit);

    // TODO: Combine these into a paramterized method call.
    Set<String> getPinnedPackages(PackageType packageType);

    Set<String> getGoodOptionPackages();

    void setPackageGoodOption(String packageName, boolean goodOption);

    Set<String> getIndexedPackages();

    void setPackageIndexed(String packageName);

    void addSubscriber(PinnedSubscriber subscriber, PackageType packageType);

    void addSubscriber(CheckedSubscriber subscriber, PackageType packageType);
}
