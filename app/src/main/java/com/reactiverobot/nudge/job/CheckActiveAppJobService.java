package com.reactiverobot.nudge.job;

import android.app.KeyguardManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.reactiverobot.nudge.SuggestChangeActivity;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.PrefsImpl;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class CheckActiveAppJobService extends JobService {

    @Inject
    CheckActiveAppJobScheduler jobScheduler;

    private static final String TAG = CheckActiveAppJobService.class.getName();

    private static final long ONE_MIN_MILLIS = 60 * 1000;

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();
    }

    private String getForegroundAppPackageName() {
        try {
            UsageStatsManager usageStatsManager =
                    (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);

            Date date = new Date();

            List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    date.getTime() - ONE_MIN_MILLIS, date.getTime());
            Log.d("check", queryUsageStats.toString());

            return computeMostRecentPackageFromStats(queryUsageStats);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return "";
    }

    private String computeMostRecentPackageFromStats(List<UsageStats> queryUsageStats) {
        long recentTime = 0;
        String recentPkg = "";

        for (UsageStats stats : queryUsageStats) {
            if (stats.getLastTimeStamp() > recentTime) {
                recentTime = stats.getLastTimeStamp();
                recentPkg = stats.getPackageName();
            }
        }

        return recentPkg;
    }

    private boolean isScreenLocked() {
        KeyguardManager myKM = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        return  myKM.inKeyguardRestrictedInputMode();
    }


    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (isScreenLocked()) {
            Log.d(TAG, "Phone is locked");
        } else {
            String foregroundPackageName = getForegroundAppPackageName();
            Log.d(TAG, "Other foreground - " + foregroundPackageName);


            Set<String> blockedPackages = PrefsImpl.from(this).getSelectedPackages(PackageType.BAD_HABIT);
            if (blockedPackages.contains(foregroundPackageName)) {
                startActivity(new Intent(getApplicationContext(), SuggestChangeActivity.class));
            }
        }

        // This is how we expired the active check. It should be re-set true when scheduling a job.
        PrefsImpl.from(this).setCheckActiveEnabled(false);
        jobScheduler.scheduleJob();

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
