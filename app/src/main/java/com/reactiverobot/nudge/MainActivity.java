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
import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageInfoManagerImpl;
import com.reactiverobot.nudge.job.CheckActiveAppJobScheduler;
import com.reactiverobot.nudge.prefs.Prefs;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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


    private PackageInfoManager packageInfoManager;

    PackageArrayAdapter badHabitPackageAdapter;


//    private void setupSearchBar() {
//        final SearchView searchBar = (SearchView) findViewById(R.id.search_bad_habits);
//        searchBar.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                searchBar.setIconified(false);
//            }
//        });
//
//        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                if (newText == null || newText.isEmpty()) {
//                    badHabitPackageAdapter.setFilter(Optional.<String>empty());
//                } else {
//                    badHabitPackageAdapter.setFilter(Optional.of(newText));
//                }
//
//                return false;
//            }
//        });
//
//        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//        searchBar.setSearchableInfo(
//                searchManager.getSearchableInfo(
//                        new ComponentName(
//                                "com.reactiverobot.nudge",
//                                SearchActivity.class.getCanonicalName())));
//    }


    @Override
    protected void onResume() {
        super.onResume();

        Switch enableServiceSwitch = (Switch) findViewById(R.id.switch_enable_service);
        enableServiceSwitch.setChecked(prefs.getCheckActiveEnabled());
//
//        final Set<String> pinnedPackages = prefs.getPinnedPackages();
//        final Set<String> blockedPackages = prefs.getBlockedPackages();
//
//        final Set<PackageInfo> allPackages = new HashSet<>();
//
//        allPackages.addAll(pinnedPackages.stream()
//            .map(new Function<String, PackageInfo>() {
//                @Override
//                public PackageInfo apply(String packageName) {
//                    return new PackageInfo(
//                            null,
//                            null,
//                            null,
//                            packageName,
//                            blockedPackages.contains(packageName));
//                }
//            }).collect(Collectors.<PackageInfo>toList()));
//
//        getPackageManager().getInstalledApplications(0)
//                .stream()
//                .forEach(new Consumer<ApplicationInfo>() {
//                    @Override
//                    public void accept(final ApplicationInfo applicationInfo) {
//                        if (!pinnedPackages.contains(applicationInfo.packageName)) {
//                            allPackages.add(new PackageInfo(
//                                    null,
//                                    null,
//                                    null,
//                                    applicationInfo.packageName,
//                                    blockedPackages.contains(applicationInfo.packageName)));
//                        }
//                    }
//                });

//        badHabitPackageAdapter.setPackageInfos(allPackages);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Set<String> pinnedPackages = prefs.getPinnedPackages();

        packageInfoManager = PackageInfoManagerImpl.builder(getPackageManager())
                .withPriorityPackages(pinnedPackages)
                .withAllPackages()
                .build(this);


//        setupSearchBar();

        setupTabsAndTitle();

//        final Set<String> blockedPackages = prefs.getBlockedPackages();
//        List<PackageInfo> pinnedPackageInfos = pinnedPackages.stream()
//                .map(new Function<String, PackageInfo>() {
//                    @Override
//                    public PackageInfo apply(String packageName) {
//                        return new PackageInfo(
//                                null,
//                                null,
//                                null,
//                                packageName,
//                                blockedPackages.contains(packageName));
//                    }
//                }).collect(Collectors.<PackageInfo>toList());

        badHabitPackageAdapter = new PackageArrayAdapter(this, packageInfoManager);
        badHabitPackageAdapter.addAll(pinnedPackages);

        packageInfoManager.subscribe(badHabitPackageAdapter);

        ListView badHabitsList = (ListView) findViewById(R.id.list_view_bad_habits);
        badHabitsList.setAdapter(badHabitPackageAdapter);

        Switch enableServiceSwitch = (Switch) findViewById(R.id.switch_enable_service);
        enableServiceSwitch.setOnCheckedChangeListener((compoundButton, isEnabled) -> {
            if (isEnabled) {
                jobScheduler.scheduleJob();
            } else {
                jobScheduler.cancelJob();
            }
        });

//        indexAllApps();
    }

    private void setupTabsAndTitle() {
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
    }
}
