package com.reactiverobot.nudge;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import com.reactiverobot.nudge.info.PackageListManagerSupplier;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class ChooseOnePackageActivity extends AppCompatActivity {

    @Inject
    Prefs prefs;
    @Inject
    PackageListManagerSupplier packageListManagerSupplier;

    private static final String TAG = MainActivity.class.getName();

    private PackageArrayAdapter packageArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_one_package);

        packageArrayAdapter = PackageArrayAdapter.builder(packageListManagerSupplier, prefs)
            .attach(this, R.id.list_view_choose_one_package, getPackageType());

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
                return "Select Bad Habit";
            case GOOD_OPTION:
                return "Select Better Option";
            default:
                return "Select Package";
        }
    }
}

