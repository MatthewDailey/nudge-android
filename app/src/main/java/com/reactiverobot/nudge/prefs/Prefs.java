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

    boolean isBlockShortsEnabled();

    void setBlockShortsEnabled(boolean enabled);

    boolean isInterceptShortsEnabled();

    void setInterceptShortsEnabled(boolean enabled);

    Set<String> getPinnedPackages(PackageType packageType);

    Set<String> getSelectedPackages(PackageType packageType);

    void setPackageSelected(PackageType packageType, String packageName, boolean isSelected);

    void unpinPackage(PackageType packageType, String packageName);

    void addSubscriber(PinnedSubscriber subscriber, PackageType packageType);

    boolean hasCompletedOnboarding();

    void completeOnboarding();

    boolean isAccessibilityAccessGranted();

    boolean hasRatedApp();

    void setHasRatedApp(boolean hasRatedApp);

    void openPlayStore();

    boolean isPackageBlocked(String packageName);

    boolean isTemporarilyUnblocked(String packageName);

    void setTemporarilyUnblocked(String packageName, boolean isUnblocked);
}
