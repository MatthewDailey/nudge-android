package com.reactiverobot.nudge;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.reactiverobot.nudge.job.CheckActiveAppJobScheduler;
import com.reactiverobot.nudge.prefs.Prefs;

import org.json.JSONObject;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class MainActivity extends AppCompatActivity {

    @Inject
    Prefs prefs;
    @Inject
    CheckActiveAppJobScheduler jobScheduler;

    private static final String TAG = MainActivity.class.getName();

    PackageArrayAdapter badHabitPackageAdapter;

    /**
     * This method exists to bootstrap the AndroidAppIndex indexes. Ideally, we would crawl
     * and not rely on clients to suggest data to index. However, this is enough for now.
     */
    // TODO: do this async
    private void indexAllApps() {
        final RequestQueue requestQueue = Volley.newRequestQueue(this);

        final Set<String> indexedPackages = prefs.getIndexedPackages();

        List<ApplicationInfo> installedApplications = getPackageManager().getInstalledApplications(0);

        installedApplications.stream()
                .forEach(new Consumer<ApplicationInfo>() {
                    @Override
                    public void accept(final ApplicationInfo applicationInfo) {
                        if (!indexedPackages.contains(applicationInfo.packageName)) {
                            String url = "http://android-app-index.herokuapp.com/api/v1/update/" + applicationInfo.packageName;

                            JsonObjectRequest request = new JsonObjectRequest(url, new JSONObject(),
                                    new Response.Listener<JSONObject>() {
                                        @Override
                                        public void onResponse(JSONObject response) {
                                            prefs.setPackageIndexed(applicationInfo.packageName);
                                            Log.d(TAG, "Successfully indexed " + applicationInfo.packageName);
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
                });
    }

    private void setupSearchBar() {
        final SearchView searchBar = (SearchView) findViewById(R.id.search_bad_habits);
        searchBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBar.setIconified(false);
            }
        });

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchBar.setSearchableInfo(
                searchManager.getSearchableInfo(
                        new ComponentName(
                                "com.reactiverobot.nudge",
                                SearchActivity.class.getCanonicalName())));
    }


    @Override
    protected void onResume() {
        super.onResume();

        Switch enableServiceSwitch = (Switch) findViewById(R.id.switch_enable_service);
        enableServiceSwitch.setChecked(prefs.getCheckActiveEnabled());

        final Set<String> pinnedPackages = prefs.getPinnedPackages();

        final Set<String> blockedPackages = prefs.getBlockedPackages();

        badHabitPackageAdapter.addAll(pinnedPackages.stream()
            .map(new Function<String, PackageInfo>() {
                @Override
                public PackageInfo apply(String packageName) {
                    return new PackageInfo(
                            null,
                            null,
                            null,
                            packageName,
                            blockedPackages.contains(packageName));
                }
            }).collect(Collectors.<PackageInfo>toList()));

        getPackageManager().getInstalledApplications(0)
                .stream()
                .forEach(new Consumer<ApplicationInfo>() {
                    @Override
                    public void accept(final ApplicationInfo applicationInfo) {
                        if (!pinnedPackages.contains(applicationInfo.packageName)) {
                            badHabitPackageAdapter.add(new PackageInfo(
                                    null,
                                    null,
                                    null,
                                    applicationInfo.packageName,
                                    blockedPackages.contains(applicationInfo.packageName)));
                        }
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupSearchBar();

        TabHost host = (TabHost) findViewById(R.id.tabs_main);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Tab One");
        spec.setContent(R.id.tab1);
        spec.setIndicator("BAD HABITS");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("Tab Two");
        spec.setContent(R.id.tab2);
        spec.setIndicator("GOOD OPTIONS");
        host.addTab(spec);

        TextView titleView = (TextView) findViewById(R.id.title_text_view);
        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/Pacifico-Regular.ttf");
        titleView.setTypeface(typeFace);

        final Set<String> blockedPackages = prefs.getBlockedPackages();
        List<PackageInfo> pinnedPackageInfos = prefs.getPinnedPackages().stream()
                .map(new Function<String, PackageInfo>() {
                    @Override
                    public PackageInfo apply(String packageName) {
                        return new PackageInfo(
                                null,
                                null,
                                null,
                                packageName,
                                blockedPackages.contains(packageName));
                    }
                }).collect(Collectors.<PackageInfo>toList());

        badHabitPackageAdapter = new PackageArrayAdapter(this);

        ListView badHabitsList = (ListView) findViewById(R.id.list_view_bad_habits);
        badHabitsList.setAdapter(badHabitPackageAdapter);

        Switch enableServiceSwitch = (Switch) findViewById(R.id.switch_enable_service);
        enableServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isEnabled) {
                if (isEnabled) {
                    jobScheduler.scheduleJob();
                } else {
                    jobScheduler.cancelJob();
                }
            }
        });


        indexAllApps();
    }
}
