package com.reactiverobot.nudge.info;


import android.support.annotation.Nullable;

import com.reactiverobot.nudge.PackageInfo;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.Comparator;
import java.util.List;

// Determines which elements are in the list.
public interface  PackageListManager extends Prefs.PinnedSubscriber {
    interface PackageListHandler {
        // if the actual list changes (aka new pinned package or query)
        void accept(List<PackageInfo> packageInfos);
    }

    void initialize(@Nullable Runnable onComplete);

    void subscribe(PackageListHandler handler);

    void setFilter(String query);


    Comparator<PackageInfo> ALPHABETIC = (o1, o2) -> {
        if (o1.name != null && o2.name != null) {
            return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
        }

        if (o1.name == null && o2.name == null) {
            return o1.packageName.toLowerCase().compareTo(o2.packageName.toLowerCase());
        }

        if (o1.name == null) {
            return -1;
        }

        return 1;
    };
}
