package com.reactiverobot.nudge;

import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class CheckActiveAppJobService extends JobService {

    public static final String TAG = CheckActiveAppJobService.class.getName();

    private static String getLollipopFGAppPackageName(Context ctx) {

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
                    recentTime = stats.getLastTimeUsed();
                    recentPkg = stats.getPackageName();
                    recentStats = stats;
                }
            }
            return recentPkg;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return "";
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Other foreground - " + getLollipopFGAppPackageName(this));


        // Schedule next one.
        JobScheduler jobService = (JobScheduler) this.getSystemService(JOB_SCHEDULER_SERVICE);

        JobInfo jobInfo = new JobInfo.Builder(1001, new ComponentName(this, CheckActiveAppJobService.class))
                .setMinimumLatency(5000)
                .build();

        jobService.schedule(jobInfo);

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
