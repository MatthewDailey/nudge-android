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

    Set<String> getPinnedPackages(PackageType packageType);

    Set<String> getSelectedPackages(PackageType packageType);

    void setPackageSelected(PackageType packageType, String packageName, boolean isSelected);

    void addSubscriber(PinnedSubscriber subscriber, PackageType packageType);

    void addSubscriber(CheckedSubscriber subscriber, PackageType packageType);

    boolean hasCompletedOnboarding();

    void completeOnboarding();

    boolean isUsageAccessGranted();
}
