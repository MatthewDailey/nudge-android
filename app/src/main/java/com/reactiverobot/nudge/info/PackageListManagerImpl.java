package com.reactiverobot.nudge.info;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

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
        Prefs.PinnedSubscriber {

    private List<PackageListHandler> subscribers = new ArrayList<>();

    private List<PackageInfo> pinnedPackages = new ArrayList<>();
    private List<PackageInfo> allPackages = new ArrayList<>();

    private String filter = null;

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
        AsyncTask.execute(() -> {
            pinnedPackages = pinnedPackagesSupplier.get()
                    .stream()
                    .map(packageName -> packageInfoManager.get(packageName))
                    .sorted(ALPHABETIC)
                    .collect(Collectors.toList());

            int flags = PackageManager.GET_META_DATA |
                    PackageManager.GET_SHARED_LIBRARY_FILES |
                    PackageManager.MATCH_UNINSTALLED_PACKAGES;

            allPackages = packageManager.getInstalledApplications(flags)
                    .stream()
                    .filter(applicationInfo -> (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1)
                    .map(applicationInfo -> packageInfoManager.get(applicationInfo.packageName))
                    .sorted(ALPHABETIC)
                    .collect(Collectors.toList());

            publishPackageList();
        });
    }

    private void sortPackages() {
        pinnedPackages.sort(ALPHABETIC);
        allPackages.sort(ALPHABETIC);
    }

    private void publishPackageList() {
        sortPackages();

        List<PackageInfo> packages = new ArrayList<>();
        packages.add(new PackageInfo("Pinned Apps", PackageInfo.Type.HEADING));
        packages.addAll(filter(pinnedPackages));
        packages.add(new PackageInfo("All Apps", PackageInfo.Type.HEADING));
        packages.addAll(filter(allPackages));

        for (PackageListHandler handler : subscribers) {
            handler.accept(packages);
        }
    }

    private List<PackageInfo> filter(List<PackageInfo> packages) {
        if (filter != null) {
            return packages.stream()
                    .filter(packageInfo -> packageInfo.name != null && packageInfo.name.toLowerCase().contains(filter))
                    .collect(Collectors.toList());
        } else {
            return packages;
        }
    }

    @Override
    public void subscribe(PackageListHandler handler) {
        subscribers.add(handler);
    }

    @Override
    public void setFilter(String query) {
        if (query.isEmpty()) {
            this.filter = null;
        } else {
            this.filter = query.toLowerCase();
        }
        publishPackageList();
    }

    private final Comparator<PackageInfo> ALPHABETIC = (o1, o2) -> {
        if (o1.name != null && o2.name != null) {
            return o1.name.toLowerCase().compareTo(o2.name.toLowerCase());
        }

        if (o1.name == null && o2.name == null) {
            return o1.packageName.toLowerCase().compareTo(o2.packageName.toLowerCase());
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
