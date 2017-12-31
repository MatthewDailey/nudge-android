package com.reactiverobot.nudge;

import android.app.KeyguardManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Date;
import java.util.List;

public class CheckActiveAppJobService extends JobService {

    private static final String TAG = CheckActiveAppJobService.class.getName();

    private static final long ONE_MIN_MILLIS = 60 * 1000;
    public static final String INSTAGRAM_PACKAGE = "com.instagram.android";
    public static final int JOB_ID = 1001;
    public static final int CHECK_INTERVAL_MILLIS = 5000;

    private String getForegroundAppPackageName() {
        try {
            UsageStatsManager usageStatsManager =
                    (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            ;
            Date date = new Date();

            List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    date.getTime() - ONE_MIN_MILLIS, date.getTime());

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

    public static void scheduleJob(Context context) {
        Prefs.from(context).setCheckActiveEnabled(true);

        JobScheduler jobService = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        JobInfo jobInfo = new JobInfo.Builder(
                JOB_ID,
                new ComponentName(context, CheckActiveAppJobService.class))
                .setMinimumLatency(CHECK_INTERVAL_MILLIS)
                .build();

        jobService.schedule(jobInfo);
    }

    public static void cancelJob(Context context) {
        Prefs.from(context).setCheckActiveEnabled(false);

        JobScheduler jobService = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        jobService.cancel(JOB_ID);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (isScreenLocked()) {
            Log.d(TAG, "Phone is locked");
        } else {
            String foregroundPackageName = getForegroundAppPackageName();
            Log.d(TAG, "Other foreground - " + foregroundPackageName);

            if (INSTAGRAM_PACKAGE.equals(foregroundPackageName)) {
                startActivity(new Intent(getApplicationContext(), SuggestChangeActivity.class));
            }
        }

        Prefs.from(this).setCheckActiveEnabled(false);

        CheckActiveAppJobService.scheduleJob(this);

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
