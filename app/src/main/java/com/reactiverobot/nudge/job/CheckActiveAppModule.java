package com.reactiverobot.nudge.job;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class CheckActiveAppModule {
    @Provides
    CheckActiveAppJobScheduler checkActiveAppJobScheduler(Context context) {
        return new CheckActiveAppJobSchedulerImpl(context);
    }
}
