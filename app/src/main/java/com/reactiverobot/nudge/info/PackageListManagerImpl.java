package com.reactiverobot.nudge.info;

import android.content.pm.PackageManager;

import com.reactiverobot.nudge.PackageInfo;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PackageListManagerImpl implements
        PackageListManager,
        PackageInfoManager.Subscriber,
        Prefs.PinnedSubscriber {

    private List<PackageListHandler> subscribers = new ArrayList<>();

    private List<PackageInfo> pinnedPackages = new ArrayList<>();
    private List<PackageInfo> allPackages = new ArrayList<>();

    private final PackageManager packageManager;
    private final PackageInfoManager packageInfoManager;

    private final Supplier<Set<String>> pinnedPackagesSupplier;

    public PackageListManagerImpl(PackageManager packageManager,
                                  PackageInfoManager packageInfoManager,
                                  Supplier<Set<String>> pinnedPackagesSupplier) {
        this.packageManager = packageManager;
        this.packageInfoManager = packageInfoManager;
        this.pinnedPackagesSupplier = pinnedPackagesSupplier;
    }

    @Override
    public void initialize() {
        pinnedPackages = pinnedPackagesSupplier.get()
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

    @Override
    public void onPinned(String packageName, boolean pinned) {
        pinnedPackages.add(packageInfoManager.get(packageName));
        pinnedPackages.sort(ALPHABETIC);
        // TODO Handle un-pinned case.

        publishPackageList();
    }

}
