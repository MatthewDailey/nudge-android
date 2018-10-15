package com.reactiverobot.nudge;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;

import com.reactiverobot.nudge.info.FullPackageListManager;
import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageListManagerSupplier;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class ChooseOnePackageActivity extends AppCompatActivity {

    @Inject
    Prefs prefs;
    @Inject
    PackageInfoManager packageInfoManager;

    private static final String TAG = MainActivity.class.getName();

    private PackageArrayAdapter packageArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_one_package);

        packageArrayAdapter = PackageArrayAdapter
                .builder(new FullPackageListManager.Supply(getPackageManager(), packageInfoManager), prefs)
                .onLoadPackagesComplete(() -> {
                    runOnUiThread(() -> findViewById(R.id.progressBar).setVisibility(View.GONE));
                })
                .onClick((packageType, packageInfo) -> {
                    prefs.setPackageSelected(packageType, packageInfo.packageName, true);
                    finish();
                })
                .attach(this, getPackageType());

        ListView listView = findViewById(R.id.list_view_choose_one_package);
        listView.setAdapter(packageArrayAdapter);

        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(getToolbarTitle());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @NonNull
    private PackageType getPackageType() {
        return PackageType.valueOf(getIntent().getStringExtra("packageType"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_choose_one_package, menu);

        packageArrayAdapter.withSearchView((SearchView) menu.findItem(R.id.search).getActionView());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_done_select_packages) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String getToolbarTitle() {
        switch (getPackageType()) {
            case BAD_HABIT:
                return "Select App to Block";
            case GOOD_OPTION:
                return "Select App to Suggest";
            default:
                return "Select Package";
        }
    }
}

