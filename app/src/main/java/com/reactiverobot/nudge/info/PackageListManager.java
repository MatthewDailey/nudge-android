package com.reactiverobot.nudge.info;


import com.reactiverobot.nudge.PackageInfo;

import java.util.List;

// Determines which elements are in the list.
public interface PackageListManager {
    interface PackageListHandler {
        // if the actual list changes (aka ne\w pinned package or query)
        void accept(List<PackageInfo> packageInfos);
        // if contents changes (aka something pinned does from selected to un-selected)
        void update();
    }

    void initialize();

    void subscribe(PackageListHandler handler);
}
