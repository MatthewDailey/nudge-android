package com.reactiverobot.nudge;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Switch;

import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageListManagerImpl;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.job.CheckActiveAppJobScheduler;
import com.reactiverobot.nudge.prefs.Prefs;

import javax.inject.Inject;

import dagger.android.AndroidInjection;


public class SelectPackagesActivity extends AppCompatActivity {

    @Inject
    Prefs prefs;
    @Inject
    PackageInfoManager packageInfoManager;

    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_packages);

        setupListPackageList(PackageType.BAD_HABIT, R.id.list_all_packages, R.id.search_packages);
    }

    private void setupListPackageList(PackageType packageType, int listViewId, int searchViewId) {
        PackageArrayAdapter packageAdapter = new PackageArrayAdapter(
                this,
                new PackageArrayAdapter.CheckHandler() {
                    @Override
                    public void accept(PackageInfo packageInfo, boolean isChecked) {
                        packageInfo.setSelected(packageType, isChecked);
                        prefs.setPackageSelected(packageType, packageInfo.packageName, isChecked);
                    }

                    @Override
                    public boolean isChecked(PackageInfo packageInfo) {
                        return packageInfo.isSelected(packageType);
                    }
                });

        ListView badHabitsList = findViewById(listViewId);
        badHabitsList.setAdapter(packageAdapter);

        PackageListManagerImpl packageListManager = new PackageListManagerImpl(
                getPackageManager(),
                packageInfoManager,
                () -> prefs.getPinnedPackages(packageType));
        packageListManager.subscribe(packageAdapter);
        packageListManager.initialize();

        prefs.addSubscriber(packageListManager, packageType);
        prefs.addSubscriber(packageAdapter, packageType);

        SearchView searchView = findViewById(searchViewId);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newQuery) {
                packageListManager.setFilter(newQuery);
                return true;
            }
        });
    }
}