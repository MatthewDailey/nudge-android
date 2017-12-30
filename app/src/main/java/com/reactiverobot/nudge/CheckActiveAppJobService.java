package com.reactiverobot.nudge;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

public class CheckActiveAppJobService extends JobService {

    public static final String TAG = CheckActiveAppJobService.class.getName();

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "hi from job");

        jobFinished(jobParameters, true);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
