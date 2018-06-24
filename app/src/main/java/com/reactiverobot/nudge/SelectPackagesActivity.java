package com.reactiverobot.nudge;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
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

        PackageArrayAdapter.attach(this, getPackageType(), packageInfoManager, prefs, R.id.list_all_packages, R.id.search_packages);

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
                return "Select Bad Habits";
            case GOOD_OPTION:
                return "Select Better Options";
            default:
                return "Select Packages";
        }
    }
}
