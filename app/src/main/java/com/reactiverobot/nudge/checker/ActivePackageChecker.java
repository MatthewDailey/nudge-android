package com.reactiverobot.nudge.checker;

public interface ActivePackageChecker {
    void launchSuggestActivityIfBlocked(String packageName);
    String getCurrentActivePackage();
}
