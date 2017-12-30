package com.reactiverobot.nudge;

import android.app.ActivityManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.List;
import java.util.function.Consumer;

public class CheckActiveAppJobService extends JobService {

    public static final String TAG = CheckActiveAppJobService.class.getName();

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();

        if (runningAppProcesses.size() > 0) {
            Log.d(TAG, "Active apps: " + runningAppProcesses.size());
            runningAppProcesses
                    .stream()
                    .forEach(new Consumer<ActivityManager.RunningAppProcessInfo>() {
                        @Override
                        public void accept(ActivityManager.RunningAppProcessInfo runningAppProcessInfo) {
                            Log.d(TAG, runningAppProcessInfo.processName);
                        }
                    });
        } else {
            Log.d(TAG, "Active app: none");
        }


        jobFinished(jobParameters, true);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
