package com.reactiverobot.nudge;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.Set;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class SuggestChangeActivity extends Activity {

    private final static String TAG = SuggestChangeActivity.class.getName();

    public final static String EXTRA_APP_BEING_BLOCKED = "extra_app_being_blocked";

    @Inject
    PackageInfoManager packageInfoManager;
    @Inject
    Prefs prefs;

    class SuggestedAppAdapter extends ArrayAdapter<String> {

        public SuggestedAppAdapter(@NonNull Activity context) {
            super(context, R.layout.button_launch_suggestion);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final String packageName = getItem(position);

            LayoutInflater layoutInflater = LayoutInflater.from(getContext());

            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.button_launch_suggestion, null);
            }

            PackageInfo packageInfo = packageInfoManager.get(packageName);

            if (packageInfo != null) {
                Button openSuggestionButton = convertView.findViewById(R.id.button_open_suggestion);

                if (packageName.equals("com.reactiverobot.nudge")) {
                    openSuggestionButton.setText("Take a Breath");
                } else if (packageName.equals("com.reactiverobot.nudge.playstore")) {
                    openSuggestionButton.setText("Rate Nudge 😍");
                } else {
                    openSuggestionButton.setText(packageInfo.name);
                }

                if (packageInfo.iconDrawable != null) {
                    packageInfo.iconDrawable.setBounds(0, 0, 100, 100);
                    openSuggestionButton.setCompoundDrawables(packageInfo.iconDrawable, null, null, null);
                }

                openSuggestionButton.setOnClickListener(v -> launchApplicationAndClose(packageName));
            }

            return convertView;
        }

    }

    private void launchApplicationAndClose(String packageName) {
        if (packageName.equals("com.reactiverobot.nudge.playstore")) {
            prefs.openPlayStore();
            prefs.setHasRatedApp(true);
            finish();
            return;
        }

        Intent i;

        if (packageName.equals("com.reactiverobot.nudge")) {
            i = new Intent(this, BreatheActivity.class);
        } else {
            i = getPackageManager().getLaunchIntentForPackage(packageName);
        }

        if (i != null) {
            startActivity(i);
            finish();
        } else {
            Toast.makeText(
                    this,
                    "Unable to launch app " + packageName + ", app is not responding.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_suggest_change);

        Set<String> pinnedPackages = prefs.getSelectedPackages(PackageType.GOOD_OPTION);

        ListView suggestedAppsView = findViewById(R.id.list_suggested_apps);

        SuggestedAppAdapter suggestedAppAdapter = new SuggestedAppAdapter(this);
        suggestedAppAdapter.addAll(pinnedPackages);

        suggestedAppAdapter.add("com.reactiverobot.nudge");
        if (!prefs.hasRatedApp()) {
            suggestedAppAdapter.add("com.reactiverobot.nudge.playstore");
        }

        suggestedAppsView.setAdapter(suggestedAppAdapter);

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View closeView =  layoutInflater.inflate(R.layout.button_exit_suggestions, null);
        Button close = closeView.findViewById(R.id.button_exit_suggestions);
        close.setOnClickListener(view -> {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            finish();
        });
        suggestedAppsView.addFooterView(closeView);
    }
}
