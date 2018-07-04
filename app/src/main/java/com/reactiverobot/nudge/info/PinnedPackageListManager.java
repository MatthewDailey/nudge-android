package com.reactiverobot.nudge.info;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.reactiverobot.nudge.PackageInfo;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PinnedPackageListManager implements PackageListManager {

    public static class Supply implements PackageListManagerSupplier {

        private final Prefs prefs;
        private final PackageInfoManager packageInfoManager;

        public Supply(PackageInfoManager packageInfoManager, Prefs prefs) {
            this.prefs = prefs;
            this.packageInfoManager = packageInfoManager;
        }

        @Override
        public PackageListManager get(PackageType packageType) {
            return  new PinnedPackageListManager(
                    packageInfoManager,
                    () -> prefs.getPinnedPackages(packageType));
        }
    }

    private final List<PackageListHandler> subscribers = new ArrayList<>();

    private List<PackageInfo> pinnedPackages = new ArrayList<>();

    private final PackageInfoManager packageInfoManager;
    private final Supplier<Set<String>> pinnedPackagesSupplier;

    public PinnedPackageListManager(PackageInfoManager packageInfoManager, Supplier<Set<String>> pinnedPackagesSupplier) {
        this.packageInfoManager = packageInfoManager;
        this.pinnedPackagesSupplier = pinnedPackagesSupplier;
    }

    @Override
    public void initialize(@Nullable Runnable onComplete) {
        AsyncTask.execute(() -> {
            pinnedPackages = pinnedPackagesSupplier.get()
                    .stream()
                    .map(packageName -> packageInfoManager.get(packageName))
                    .sorted(ALPHABETIC)
                    .collect(Collectors.toList());

            if (onComplete != null) {
                onComplete.run();
            }

            publishPackageList();
        });
    }

    @Override
    public void subscribe(PackageListHandler handler) {
        subscribers.add(handler);
    }

    @Override
    public void setFilter(String query) {
        // NOOP
    }

    private void sortPackages() {
        pinnedPackages.sort(ALPHABETIC);
    }

    private void publishPackageList() {
        sortPackages();

        for (PackageListHandler handler : subscribers) {
            handler.accept(pinnedPackages);
        }
    }

    @Override
    public void onPinned(String packageName, boolean pinned) {
        pinnedPackages.add(packageInfoManager.get(packageName));
        pinnedPackages.sort(ALPHABETIC);
        // TODO Handle un-pinned case.

        publishPackageList();
    }
}
