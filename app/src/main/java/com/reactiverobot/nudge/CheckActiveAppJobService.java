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

    public static final String TAG = CheckActiveAppJobService.class.getName();

    private String getLollipopFGAppPackageName(Context ctx) {

        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager) ctx.getSystemService(USAGE_STATS_SERVICE);
            long milliSecs = 60 * 1000;
            Date date = new Date();
            List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, date.getTime() - milliSecs, date.getTime());
            if (queryUsageStats.size() > 0) {
                Log.i(TAG, "queryUsageStats size: " + queryUsageStats.size());
            }
            long recentTime = 0;
            UsageStats recentStats = null;
            String recentPkg = "";
            for (int i = 0; i < queryUsageStats.size(); i++) {
                UsageStats stats = queryUsageStats.get(i);

                if (stats.getLastTimeStamp() > recentTime) {
                    recentTime = stats.getLastTimeStamp();
                    recentPkg = stats.getPackageName();


                    recentStats = stats;
                }
            }
            if ("com.instagram.android".equals(recentPkg)) {
                startActivity(new Intent(getApplicationContext(), SuggestChangeActivity.class));
            }

            return recentPkg;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return "";
    }

    private boolean isScreenLocked() {
        KeyguardManager myKM = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        return  myKM.inKeyguardRestrictedInputMode();
    }

    public static void scheduleJob(Context context) {
        Prefs.from(context).setCheckActiveEnabled(true);

        JobScheduler jobService = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        JobInfo jobInfo = new JobInfo.Builder(1001, new ComponentName(context, CheckActiveAppJobService.class))
                .setMinimumLatency(5000)
                .build();

        jobService.schedule(jobInfo);
    }

    public static void cancelJob(Context context) {
        Prefs.from(context).setCheckActiveEnabled(false);

        JobScheduler jobService = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        jobService.cancel(1001);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        if (isScreenLocked()) {
            Log.d(TAG, "Phone is locked");
        } else {
            Log.d(TAG, "Other foreground - " + getLollipopFGAppPackageName(this));
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
