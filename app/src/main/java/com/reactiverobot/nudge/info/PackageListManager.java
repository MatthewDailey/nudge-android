package com.reactiverobot.nudge.info;


import com.reactiverobot.nudge.PackageInfo;

import java.util.List;

public interface PackageListManager {
    interface PackageListHandler {
        // if the actual list changes (aka new pinned package or query)
        void accept(List<PackageInfo> packageInfos);
        // if contents changes (aka something pinned does from selected to un-selected)
        void update();
    }

    void pinned(String packageName, boolean isPinned);
    void selected(String packageName, boolean isSelected);

    void query(String query);

    void subscribe(PackageListHandler handler);
}
