package com.reactiverobot.nudge.info;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

import com.reactiverobot.nudge.PackageInfo;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PinnedAndFullPackageListManager implements PackageListManager {

    public static class Supply implements PackageListManagerSupplier {

        private final Context context;
        private final PackageInfoManager packageInfoManager;
        private final Prefs prefs;

        public Supply(Context context, PackageInfoManager packageInfoManager, Prefs prefs) {
            this.context = context;
            this.packageInfoManager = packageInfoManager;
            this.prefs = prefs;
        }

        @Override
        public PackageListManager get(PackageType packageType) {
            return new PinnedAndFullPackageListManager(
                    context.getPackageManager(),
                    packageInfoManager,
                    () -> prefs.getPinnedPackages(packageType));
        }
    }

    private List<PackageListHandler> subscribers = new ArrayList<>();

    private List<PackageInfo> pinnedPackages = new ArrayList<>();
    private List<PackageInfo> allPackages = new ArrayList<>();

    private String filter = null;

    private final PackageManager packageManager;
    private final PackageInfoManager packageInfoManager;

    private final Supplier<Set<String>> pinnedPackagesSupplier;

    public PinnedAndFullPackageListManager(PackageManager packageManager,
                                           PackageInfoManager packageInfoManager,
                                           Supplier<Set<String>> pinnedPackagesSupplier) {
        this.packageManager = packageManager;
        this.packageInfoManager = packageInfoManager;
        this.pinnedPackagesSupplier = pinnedPackagesSupplier;
    }

    @Override
    public void initialize(@Nullable Runnable onComplete) {
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


        if (onComplete != null) {
            onComplete.run();
        }

        publishPackageList();
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

    @Override
    public void onPinned(String packageName, boolean pinned) {
        pinnedPackages.add(packageInfoManager.get(packageName));
        pinnedPackages.sort(ALPHABETIC);
        // TODO Handle un-pinned case.

        publishPackageList();
    }

}
