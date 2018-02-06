package com.reactiverobot.nudge.info;

import com.reactiverobot.nudge.PackageInfo;

// Singleton map of package name -> packge info, the package info will be updated
public interface PackageInfoManager {

    interface Subscriber {
        void update();
    }

    PackageInfo get(String packageName);

    void subscribe(Subscriber subscriber);
}