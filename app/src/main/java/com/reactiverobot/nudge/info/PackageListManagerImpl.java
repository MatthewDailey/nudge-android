package com.reactiverobot.nudge.info;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.reactiverobot.nudge.PackageInfo;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PackageListManagerImpl implements PackageListManager, PackageInfoManager.Subscriber {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<PackageListHandler> subscribers = new ArrayList<>();

    private final PackageManager packageManager;
    private final PackageInfoManager packageInfoManager;
    private final Prefs prefs;

    public PackageListManagerImpl(PackageManager packageManager, Prefs prefs, PackageInfoManager packageInfoManager) {
        this.packageManager = packageManager;
        this.packageInfoManager = packageInfoManager;
        this.prefs = prefs;
    }

    @Override
    public void initialize() {
//        executor.submit(() -> {
           List<PackageInfo> packages = new ArrayList<>();

           prefs.getPinnedPackages()
                   .stream()
                   .forEach(packageName -> packages.add(packageInfoManager.get(packageName)));

           packageManager.getInstalledApplications(0)
                .stream()
                .forEach(applicationInfo ->
                        packages.add(packageInfoManager.get(applicationInfo.packageName)));

           for (PackageListHandler handler : subscribers) {
               handler.accept(packages);
           }
//        });
    }

    @Override
    public void subscribe(PackageListHandler handler) {
        subscribers.add(handler);
    }

    @Override
    public void update() {
        for (PackageListHandler handler : subscribers) {
            handler.update();
        }
    }
}
