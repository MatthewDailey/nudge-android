package com.reactiverobot.nudge.info;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.reactiverobot.nudge.PackageInfo;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.concurrent.ConcurrentHashMap;

public class PackageInfoManagerImpl implements PackageInfoManager {

    private static final String TAG = PackageInfoManagerImpl.class.getName();

    private ConcurrentHashMap<String, PackageInfo> packageInfoMap;
    private final PackageManager packageManager;
    private final Prefs prefs;

    public PackageInfoManagerImpl(Context context, Prefs prefs) {
        this.packageManager = context.getPackageManager();
        this.prefs = prefs;
        this.packageInfoMap = new ConcurrentHashMap<>();
    }

    @Override
    public @NonNull
    PackageInfo get(String packageName) {
        return this.packageInfoMap.computeIfAbsent(packageName,
                (packageNameKey) -> {
                    PackageInfo packageInfo = new PackageInfo(packageNameKey);

                    if (prefs.getSelectedPackages(PackageType.BAD_HABIT).contains(packageInfo.packageName)) {
                        packageInfo.setSelected(PackageType.BAD_HABIT, true);
                    }

                    if (prefs.getSelectedPackages(PackageType.GOOD_OPTION).contains(packageInfo.packageName)) {
                        packageInfo.setSelected(PackageType.GOOD_OPTION, true);
                    }

                    try {
                        Drawable appIcon = packageManager.getApplicationIcon(packageInfo.packageName);
                        String appName = packageManager.getApplicationInfo(packageInfo.packageName, 0)
                                .loadLabel(packageManager).toString();

                        packageInfo.name = appName;
                        packageInfo.iconDrawable = appIcon;
                    } catch (PackageManager.NameNotFoundException e) {
                        // Do nothing
                    }

                    return packageInfo;
                });
    }

}
