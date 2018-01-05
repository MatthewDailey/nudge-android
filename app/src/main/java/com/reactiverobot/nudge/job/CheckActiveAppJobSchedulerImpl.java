package com.reactiverobot.nudge.job;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

import com.reactiverobot.nudge.prefs.PrefsImpl;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

public class CheckActiveAppJobSchedulerImpl implements CheckActiveAppJobScheduler {

    private final Context context;

    public static final int JOB_ID = 1001;
    public static final int CHECK_INTERVAL_MILLIS = 5000;

    public CheckActiveAppJobSchedulerImpl(Context context) {
        this.context = context;
    }

    @Override
    public void scheduleJob() {
        PrefsImpl.from(context).setCheckActiveEnabled(true);

        JobScheduler jobService = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        JobInfo jobInfo = new JobInfo.Builder(
                JOB_ID,
                new ComponentName(context, CheckActiveAppJobService.class))
                .setMinimumLatency(CHECK_INTERVAL_MILLIS)
                .build();

        jobService.schedule(jobInfo);
    }

    @Override
    public void cancelJob() {
        PrefsImpl.from(context).setCheckActiveEnabled(false);

        JobScheduler jobService = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        jobService.cancel(JOB_ID);
    }
}
