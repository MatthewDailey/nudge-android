package com.reactiverobot.nudge.info;

import android.content.pm.PackageManager;

import com.reactiverobot.nudge.PackageInfo;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PackageListManagerImpl implements PackageListManager, PackageInfoManager.Subscriber {

    private List<PackageListHandler> subscribers = new ArrayList<>();

    private List<PackageInfo> pinnedPackages = new ArrayList<>();
    private List<PackageInfo> allPackages = new ArrayList<>();

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
        pinnedPackages = prefs.getPinnedPackages()
                .stream()
                .map(packageName -> packageInfoManager.get(packageName))
                .sorted(ALPHABETIC)
                .collect(Collectors.toList());

        allPackages = packageManager.getInstalledApplications(0)
                .stream()
                .map(applicationInfo -> packageInfoManager.get(applicationInfo.packageName))
                .sorted(ALPHABETIC)
                .collect(Collectors.toList());

        publishPackageList();
    }

    private void publishPackageList() {
        List<PackageInfo> packages = new ArrayList<>();
        packages.add(new PackageInfo("Pinned Apps", PackageInfo.Type.HEADING));
        packages.addAll(pinnedPackages);
        packages.add(new PackageInfo("All Apps", PackageInfo.Type.HEADING));
        packages.addAll(allPackages);

        // TODO: In-line this so it only does 1 pass.
        Set<String> blockedPackages = prefs.getBlockedPackages();
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

    private final Comparator<PackageInfo> ALPHABETIC = (o1, o2) -> {
        if (o1.name != null && o2.name != null) {
            return o1.name.compareTo(o2.name);
        }

        if (o1.name == null && o2.name == null) {
            return o1.packageName.compareTo(o2.packageName);
        }

        if (o1.name == null) {
            return -1;
        }

        if (o2.name == null) {
            return -1;
        }

        return 0;
    };
}
