package com.reactiverobot.nudge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.Set;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class SuggestChangeActivity extends Activity {

    @Inject
    PackageInfoManager packageInfoManager;
    @Inject
    Prefs prefs;

    class SuggestedAppAdapter extends ArrayAdapter<String> {

        private final Activity activity;

        public SuggestedAppAdapter(@NonNull Activity context) {
            super(context, R.layout.button_launch_suggestion);
            activity = context;
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
                    openSuggestionButton.setText("Take a Breathe");
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
        Intent i;

        if (packageName.equals("com.reactiverobot.nudge")) {
            i = new Intent(this, BreatheActivity.class);
        } else {
            i = getPackageManager().getLaunchIntentForPackage(packageName);
        }

        startActivity(i);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_suggest_change);

        Set<String> pinnedPackages = prefs.getSelectedPackages(PackageType.GOOD_OPTION);

        ListView suggestedAppsView = findViewById(R.id.list_suggested_apps);

        SuggestedAppAdapter suggestedAppAdapter = new SuggestedAppAdapter(this);
        suggestedAppAdapter.addAll(pinnedPackages);

        suggestedAppAdapter.add("com.reactiverobot.nudge");

        suggestedAppsView.setAdapter(suggestedAppAdapter);
    }
}
