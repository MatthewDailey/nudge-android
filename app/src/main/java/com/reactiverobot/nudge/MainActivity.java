package com.reactiverobot.nudge;

import android.app.ActivityManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UsageStatsManager usageStatsManager = (UsageStatsManager) getApplication()
                .getSystemService(Context.USAGE_STATS_SERVICE);

        long timeNow = System.currentTimeMillis();
        long timeYesterday = timeNow - (24 * 60 * 60 * 1000);

        UsageEvents usageStats = usageStatsManager.queryEvents(timeNow, timeYesterday);

        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean appInactive = usageStatsManager.isAppInactive("com.reactiverobot.nudge");

            Log.d(TAG, "isInactive : " + appInactive);
        }

        Log.d(TAG, "Usage stats" + usageStats);
    }
}
