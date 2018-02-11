package com.reactiverobot.nudge.info;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.reactiverobot.nudge.PackageInfo;
import com.reactiverobot.nudge.prefs.Prefs;
import com.reactiverobot.nudge.prefs.PrefsImpl;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PackageInfoManagerImpl implements PackageInfoManager {

    private static final String TAG = PackageInfoManagerImpl.class.getName();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Subscriber> subscribers = new ArrayList<>();

    private ConcurrentHashMap<String, PackageInfo> packageInfoMap;
    private final RequestQueue requestQueue;

    private final Context context;
    private final Prefs prefs;
    private final PackageManager packageManager;

    public PackageInfoManagerImpl(Context context, Prefs prefs) {
        this.context = context;
        this.prefs = prefs;
        this.packageManager = context.getPackageManager();

        this.packageInfoMap = new ConcurrentHashMap<>();
        this.requestQueue = Volley.newRequestQueue(context);
    }

    private void updatePackageInfo(final PackageInfo packageInfo) {
        if (prefs.getSelectedPackages(PackageType.BAD_HABIT).contains(packageInfo.packageName)) {
            packageInfo.badHabit = true;
        }

        if (prefs.getSelectedPackages(PackageType.GOOD_OPTION).contains(packageInfo.packageName)) {
            packageInfo.goodOption = true;
        }

        try {
            Drawable appIcon = packageManager.getApplicationIcon(packageInfo.packageName);
            String appName = packageManager.getApplicationInfo(packageInfo.packageName, 0)
                    .loadLabel(packageManager).toString();

            packageInfo.name = appName;
            packageInfo.iconDrawable = appIcon;

            updateSubscribers();
        } catch (PackageManager.NameNotFoundException e) {
            String url = "http://android-app-index.herokuapp.com/api/v1/get/" + packageInfo.packageName;

            JsonObjectRequest request = new JsonObjectRequest(url, null,
                    response -> {
                        Log.d(TAG, response.toString());
                        try {
                            packageInfo.name = response.getString("name");
                            packageInfo.iconUrl = response.getString("icon_url");
                            updateSubscribers();
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    },
                    error -> {
                        Log.e(TAG, "Failed to load package data.", error);
                    });

            requestQueue.add(request);
        }
    }

    /**
     * This method exists to bootstrap the AndroidAppIndex indexes. Ideally, we would crawl
     * and not rely on clients to suggest data to index. However, this is enough for now.
     */
    // TODO: Find the right place to do this. Probably anonther service that does just this one thing.
    private void indexAllApps() {
        final Set<String> indexedPackages = PrefsImpl.from(context).getIndexedPackages();

        List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(0);

        installedApplications.stream()
                .forEach(applicationInfo -> {
                    if (!indexedPackages.contains(applicationInfo.packageName)) {
                        String url = "http://android-app-index.herokuapp.com/api/v1/update/" + applicationInfo.packageName;

                        JsonObjectRequest request = new JsonObjectRequest(url, new JSONObject(),
                                response -> {
                                    PrefsImpl.from(context).setPackageIndexed(applicationInfo.packageName);
                                    Log.d(TAG, "Successfully indexed " + applicationInfo.packageName);
                                }, error -> Log.e(TAG, "Failed to load package data.", error));

                        requestQueue.add(request);
                    }
                });
    }


    @Override
    public @NonNull PackageInfo get(String packageName) {
        return this.packageInfoMap.computeIfAbsent(packageName,
                (packageNameKey) -> {
                    PackageInfo packageInfo = new PackageInfo(packageNameKey);
                    executor.submit(() -> updatePackageInfo(packageInfo));
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
