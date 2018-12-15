package com.reactiverobot.nudge;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.reactiverobot.nudge.checker.ActivePackageChecker;
import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageListManagerSupplier;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class RedesignActivity extends AppCompatActivity {

    @Inject
    Prefs prefs;
    @Inject
    ActivePackageChecker packageChecker;
    @Inject
    PackageInfoManager packageInfoManager;
    @Inject
    PackageListManagerSupplier packageListManagerSupplier;

    private static final String TAG = RedesignActivity.class.getName();

    private void showOpenBrieflyDialog(PackageType packageType, PackageInfo packageInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("This will open and unblock " + packageInfo.name + " for 60 seconds.")
                .setTitle("Open '" + packageInfo.name + "'?");

        builder.setPositiveButton("Open",
                (dialog, id) -> {
                    Log.d("handler", "opening temp");
                    prefs.setTemporarilyUnblocked(packageInfo.packageName, true);
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageInfo.packageName);
                    startActivity(launchIntent);
                });
        builder.setNegativeButton("Cancel", (dialog, id) -> {
            // User cancelled the dialog
        });

        builder.create().show();
    }

    private void showUnpinDialog(PackageType packageType, PackageInfo packageInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("This package will no longer appear in this list. " +
                "You can always add it back with the '+' button.")
                .setTitle("Remove '" + packageInfo.name + "'?");

        builder.setPositiveButton("Remove",
                (dialog, id) -> prefs.unpinPackage(packageType, packageInfo.packageName));
        builder.setNegativeButton("Cancel", (dialog, id) -> {
            // User cancelled the dialog
        });

        builder.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        PackageArrayAdapter.Builder builder = PackageArrayAdapter
                .builder(packageListManagerSupplier, prefs)
                .onLongPress((packageType, packageInfo) -> showUnpinDialog(packageType, packageInfo))
                .onShowBriefly((packageType, packageInfo) -> showOpenBrieflyDialog(packageType, packageInfo))
                .withCheckbox();

        setupGroup(findViewById(R.id.group_blocked), "BLOCKED", builder.attach(this, PackageType.BAD_HABIT));
        setupGroup(findViewById(R.id.group_suggested), "SUGGESTED", builder.attach(this, PackageType.GOOD_OPTION));
    }

    private void setupGroup(View groupView, String title, PackageArrayAdapter adapter) {
        ListView blockedPackagesList = groupView.findViewById(R.id.list_packages);
        blockedPackagesList.setAdapter(adapter);

        TextView sectionTitle = groupView.findViewById(R.id.section_title);
        sectionTitle.setText(title);
    }

}
