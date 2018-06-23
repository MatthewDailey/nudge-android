package com.reactiverobot.nudge;

import android.app.Activity;
import android.content.Context;
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

    class SuggestedAppAdapter extends ArrayAdapter<String>  {

        public SuggestedAppAdapter(@NonNull Context context) {
            super(context, R.layout.button_launch_suggestion);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final String suggestedApp = getItem(position);

            LayoutInflater layoutInflater = LayoutInflater.from(getContext());

            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.button_launch_suggestion, null);
            }

            PackageInfo packageInfo = packageInfoManager.get(suggestedApp);

            if (packageInfo != null) {
                Button openSuggestionButton = convertView.findViewById(R.id.button_open_suggestion);
                openSuggestionButton.setText(packageInfo.name);
                openSuggestionButton.setCompoundDrawables(packageInfo.iconDrawable, null, null, null);
            }

            return convertView;
        }
    }

    private void launchApplicationAndClose(String packageName) {
        Intent i = getPackageManager().getLaunchIntentForPackage(packageName);
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

        suggestedAppsView.setAdapter(suggestedAppAdapter);
    }
}
