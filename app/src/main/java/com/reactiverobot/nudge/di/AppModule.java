package com.reactiverobot.nudge.di;

import com.reactiverobot.nudge.MainActivity;
import com.reactiverobot.nudge.job.CheckActiveAppJobService;
import com.reactiverobot.nudge.prefs.PrefsModule;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
abstract class AppModule {

    @ContributesAndroidInjector(modules = { PrefsModule.class })
    abstract MainActivity contributeMainActivityInjector();

    @ContributesAndroidInjector
    abstract CheckActiveAppJobService contributeCheckActiveAppJobService();

}
