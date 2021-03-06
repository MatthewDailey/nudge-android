package com.reactiverobot.nudge;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.reactiverobot.nudge.info.FullPackageListManager;
import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageListManagerSupplier;
import com.reactiverobot.nudge.info.PackageType;
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

        PackageArrayAdapter packageArrayAdapter = PackageArrayAdapter
                .builder(new FullPackageListManager.Supply(getPackageManager(), packageInfoManager), prefs)
                .searchView(findViewById(R.id.search_packages))
                .withCheckbox()
                .onLoadPackagesComplete(() -> {
                    runOnUiThread(() -> {
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                        findViewById(R.id.search_packages).setVisibility(View.VISIBLE);
                    });
                })
                .attach(this, getPackageType());

        ListView listView = findViewById(R.id.list_all_packages);
        listView.setAdapter(packageArrayAdapter);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
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
        getMenuInflater().inflate(R.menu.menu_select_packages, menu);
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
                return "Select Apps to Block";
            case GOOD_OPTION:
                return "Select Apps to Suggest";
            default:
                return "Select Packages";
        }
    }
}
