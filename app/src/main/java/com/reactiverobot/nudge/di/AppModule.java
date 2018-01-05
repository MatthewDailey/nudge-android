package com.reactiverobot.nudge.di;

import com.reactiverobot.nudge.MainActivity;
import com.reactiverobot.nudge.SearchActivity;
import com.reactiverobot.nudge.di.test.TestModule;
import com.reactiverobot.nudge.job.CheckActiveAppJobService;
import com.reactiverobot.nudge.prefs.PrefsModule;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
abstract class AppModule {

    @ContributesAndroidInjector(modules = { PrefsModule.class })
    abstract MainActivity contributeMainActivityInjector();

    @ContributesAndroidInjector(modules = { PrefsModule.class })
    abstract SearchActivity contributeSearchActivityInjector();

    @ContributesAndroidInjector
    abstract CheckActiveAppJobService contributeCheckActiveAppJobService();

}
