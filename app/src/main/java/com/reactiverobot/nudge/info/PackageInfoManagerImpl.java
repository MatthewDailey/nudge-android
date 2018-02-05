package com.reactiverobot.nudge.info;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.reactiverobot.nudge.PackageInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PackageInfoManagerImpl implements PackageInfoManager {

    private static final String TAG = PackageInfoManagerImpl.class.getName();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Subscriber> subscribers = new ArrayList<>();

    private Map<String, PackageInfo> packageInfoMap;
    private final RequestQueue requestQueue;

    private final PackageManager packageManager;
    private final List<String> priorityPackages;
    private final boolean loadAllPackages;

    private PackageInfoManagerImpl(
            Context context,
            PackageManager packageManager,
            List<String> priorityPackages,
            boolean loadAllPackages) {
        this.packageManager = packageManager;
        this.loadAllPackages = loadAllPackages;
        this.priorityPackages = priorityPackages;

        this.packageInfoMap = new HashMap<>();
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public static Builder builder(PackageManager packageManager) {
        return new Builder(packageManager);
    }

    static class Builder {
        private final PackageManager packageManager;

        private List<String> priorityPackages = new ArrayList<>();
        private boolean loadAllPackages = false;

        public Builder(PackageManager manager) {
            this.packageManager = manager;
        }

        public Builder withPriorityPackages(List<String> priorityPackages) {
            this.priorityPackages = priorityPackages;
            return this;
        }

        public Builder withAllPackages() {
            this.loadAllPackages = true;
            return this;
        }

        public PackageInfoManager build(Context context) {
            PackageInfoManagerImpl manager = new PackageInfoManagerImpl(
                    context, packageManager, priorityPackages, loadAllPackages);

            manager.initializeAsync();

            return manager;
        }
    }

    public void initializeAsync() {
        if (!this.priorityPackages.isEmpty()) {
            executor.submit(() -> this.priorityPackages.forEach(packageName -> {
                PackageInfo packageInfo = new PackageInfo(packageName);
                packageInfoMap.put(packageName, packageInfo);
                updatePackageInfo(packageInfo);
            }));
        }

        if (this.loadAllPackages) {
            executor.submit(() ->
                    packageManager.getInstalledApplications(0).stream()
                            .forEach((ApplicationInfo applicationInfo) ->
                                    packageInfoMap.put(applicationInfo.packageName, new PackageInfo(
                                            applicationInfo.loadLabel(packageManager).toString(),
                                            null,
                                            applicationInfo.loadIcon(packageManager),
                                            applicationInfo.packageName,
                                            false))));
        }

    }

    private void updatePackageInfo(final PackageInfo packageInfo) {
        try {
            Drawable appIcon = packageManager.getApplicationIcon(packageInfo.packageName);
            String appName = packageManager.getApplicationInfo(packageInfo.packageName, 0)
                    .loadLabel(packageManager).toString();

            packageInfo.name = appName;
            packageInfo.iconDrawable = appIcon;

            update();
        } catch (PackageManager.NameNotFoundException e) {
            String url = "http://android-app-index.herokuapp.com/api/v1/get/" + packageInfo.packageName;

            JsonObjectRequest request = new JsonObjectRequest(url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, response.toString());
                            try {
                                packageInfo.name = response.getString("name");
                                packageInfo.iconUrl = response.getString("icon_url");
                                update();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Failed to load package data.", error);
                }
            });

            requestQueue.add(request);
        }
    }


    @Override
    public @Nullable PackageInfo get(String packageName) {
        return this.packageInfoMap.get(packageName);
    }

    @Override
    public void subscribe(Subscriber subscriber) {
        this.subscribers.add(subscriber);
    }

    private void update() {
        for (Subscriber subscriber : subscribers) {
            subscriber.update();
        }
    }
}
