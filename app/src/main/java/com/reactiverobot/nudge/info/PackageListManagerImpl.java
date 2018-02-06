package com.reactiverobot.nudge.info;

import android.content.pm.PackageManager;

import com.reactiverobot.nudge.PackageInfo;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PackageListManagerImpl implements PackageListManager, PackageInfoManager.Subscriber {

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
        List<PackageInfo> packages = new ArrayList<>();

        Set<String> blockedPackages = prefs.getBlockedPackages();

        packages.add(new PackageInfo("Pinned Apps", PackageInfo.Type.HEADING));

        prefs.getPinnedPackages()
                .stream()
                .forEach(packageName -> packages.add(packageInfoManager.get(packageName)));

        packages.add(new PackageInfo("All Apps", PackageInfo.Type.HEADING));

        packageManager.getInstalledApplications(0)
                .stream()
                .forEach(applicationInfo ->
                        packages.add(packageInfoManager.get(applicationInfo.packageName)));

        // TODO: In-line this so it only does 1 pass.
        packages.stream().forEach(packageInfo ->
                packageInfo.blocked = blockedPackages.contains(packageInfo.packageName));

        for (PackageListHandler handler : subscribers) {
            handler.accept(packages);
        }
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
