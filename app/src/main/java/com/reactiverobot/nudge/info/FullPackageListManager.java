package com.reactiverobot.nudge.info;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.reactiverobot.nudge.PackageInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FullPackageListManager implements PackageListManager {

    private List<PackageListHandler> subscribers = new ArrayList<>();

    private List<PackageInfo> allPackages = new ArrayList<>();
    private String filter;

    private final PackageManager packageManager;
    private final PackageInfoManager packageInfoManager;

    public FullPackageListManager(PackageManager packageManager, PackageInfoManager packageInfoManager) {
        this.packageManager = packageManager;
        this.packageInfoManager = packageInfoManager;
    }

    @Override
    public void initialize(@Nullable Runnable onComplete) {
        AsyncTask.execute(() -> {
            int flags = PackageManager.GET_META_DATA |
                    PackageManager.GET_SHARED_LIBRARY_FILES |
                    PackageManager.MATCH_UNINSTALLED_PACKAGES;

            allPackages = packageManager.getInstalledApplications(flags)
                    .stream()
//                    .filter(applicationInfo -> (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1)
                    .map(applicationInfo -> packageInfoManager.get(applicationInfo.packageName))
                    .sorted(ALPHABETIC)
                    .collect(Collectors.toList());

            if (onComplete != null) {
                onComplete.run();
            }

            publishPackageList();
        });
    }

    private void sortPackages() {
        allPackages.sort(ALPHABETIC);
    }

    private void publishPackageList() {
        sortPackages();

        List<PackageInfo> packages = new ArrayList<>();
        packages.addAll(filter(allPackages));

        // TODO: Filter pinned packages of type.

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
        // Noop
    }

    public static class Supply implements PackageListManagerSupplier {

        private final PackageInfoManager packageInfoManager;
        private final PackageManager packageManager;

        public Supply(PackageManager packageManager, PackageInfoManager packageInfoManager) {
            this.packageInfoManager = packageInfoManager;
            this.packageManager = packageManager;
        }

        @Override
        public PackageListManager get(PackageType packageType) {
            return  new FullPackageListManager(
                    packageManager,
                    packageInfoManager);
        }
    }
}
