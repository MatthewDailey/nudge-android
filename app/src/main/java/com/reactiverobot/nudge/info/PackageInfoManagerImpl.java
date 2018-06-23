package com.reactiverobot.nudge.info;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.reactiverobot.nudge.PackageInfo;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PackageInfoManagerImpl implements PackageInfoManager {

    private static final String TAG = PackageInfoManagerImpl.class.getName();

    private final List<Subscriber> subscribers = new ArrayList<>();

    private ConcurrentHashMap<String, PackageInfo> packageInfoMap;
    private final PackageManager packageManager;

    public PackageInfoManagerImpl(Context context) {
        this.packageManager = context.getPackageManager();
        this.packageInfoMap = new ConcurrentHashMap<>();
    }

    @Override
    public @NonNull PackageInfo get(String packageName) {
        return this.packageInfoMap.computeIfAbsent(packageName,
                (packageNameKey) -> {
                    PackageInfo packageInfo = new PackageInfo(packageNameKey);

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

    @Override
    public void subscribe(Subscriber subscriber) {
        this.subscribers.add(subscriber);
    }

    private void updateSubscribers() {
        for (Subscriber subscriber : subscribers) {
            subscriber.update();
        }
    }
}
