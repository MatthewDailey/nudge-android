package com.reactiverobot.nudge;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.SearchManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
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
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    /**
     * This method exists to bootstrap the AndroidAppIndex indexes. Ideally, we would crawl
     * and not rely on clients to suggest data to index. However, this is enough for now.
     */
    private void indexAllApps() {
        final RequestQueue requestQueue = Volley.newRequestQueue(this);

        final Prefs prefs = Prefs.from(this);
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

        searchBar.setIconifiedByDefault(false);


        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchBar.setSearchableInfo(
                searchManager.getSearchableInfo(
                        new ComponentName(
                                "com.reactiverobot.nudge",
                                SearchResultsActivity.class.getCanonicalName())));

//        final RequestQueue requestQueue = Volley.newRequestQueue(this);
//
//        searchBar.setOnSearchClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                CharSequence query = searchBar.getQuery();
//                Log.d(TAG, "Hit search! " + query);
//
//                if (query != null && !query.toString().isEmpty()) {
//                    String url = "http://android-app-index.herokuapp.com/api/v1/search/" + query;
//
//                    JsonObjectRequest request = new JsonObjectRequest(url, null,
//                            new Response.Listener<JSONObject>() {
//                                @Override
//                                public void onResponse(JSONObject response) {
//                                    Log.d(TAG, "Search results: " + response);
//                                }
//                            },
//                            new Response.ErrorListener() {
//                                @Override
//                                public void onErrorResponse(VolleyError error) {
//                                    Log.e(TAG, "Failed to load package data.", error);
//                                }
//                            });
//
//                    requestQueue.add(request);
//                } else {
//                    Log.d(TAG, "Empty query.");
//                }
//            }
//        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        Switch enableServiceSwitch = (Switch) findViewById(R.id.switch_enable_service);
        enableServiceSwitch.setChecked(Prefs.from(this).getCheckActiveEnabled());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        final Set<String> blockedPackages = Prefs.from(this).getBlockedPackages();
        List<PackageInfo> pinnedPackageInfos = Prefs.from(this).getPinnedPackages().stream()
                .map(new Function<String, PackageInfo>() {
                    @Override
                    public PackageInfo apply(String packageName) {
                        return new PackageInfo(
                                null,
                                null,
                                packageName,
                                blockedPackages.contains(packageName));
                    }
                }).collect(Collectors.<PackageInfo>toList());

        PackageArrayAdapter badHabitPackageAdapter = new PackageArrayAdapter(this);
        badHabitPackageAdapter.addAll(pinnedPackageInfos);

        ListView badHabitsList = (ListView) findViewById(R.id.list_view_bad_habits);
        badHabitsList.setAdapter(badHabitPackageAdapter);

        Switch enableServiceSwitch = (Switch) findViewById(R.id.switch_enable_service);
        enableServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isEnabled) {
                if (isEnabled) {
                    CheckActiveAppJobService.scheduleJob(getApplicationContext());
                } else {
                    CheckActiveAppJobService.cancelJob(getApplicationContext());
                }
            }
        });


        indexAllApps();
    }
}
