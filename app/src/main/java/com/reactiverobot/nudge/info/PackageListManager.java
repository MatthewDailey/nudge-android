package com.reactiverobot.nudge.info;


import com.reactiverobot.nudge.PackageInfo;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.List;

// Determines which elements are in the list.
public interface  PackageListManager extends Prefs.PinnedSubscriber {
    interface PackageListHandler {
        // if the actual list changes (aka new pinned package or query)
        void accept(List<PackageInfo> packageInfos);
    }

    void initialize();

    void subscribe(PackageListHandler handler);

    void setFilter(String query);
}
