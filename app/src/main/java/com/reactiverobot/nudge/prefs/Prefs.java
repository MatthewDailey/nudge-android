package com.reactiverobot.nudge.prefs;

import java.util.Set;

public interface Prefs {

    void setCheckActiveEnabled(boolean enabled);

    boolean getCheckActiveEnabled();

    Set<String> getPinnedPackages();

    Set<String> getBlockedPackages();

    void setPackagePinned(String packageName, boolean pinned);

    void setPackageBlocked(String packageName, boolean blocked);

    Set<String> getIndexedPackages();

    void setPackageIndexed(String packageName);
}
