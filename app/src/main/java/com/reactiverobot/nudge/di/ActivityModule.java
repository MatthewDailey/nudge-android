package com.reactiverobot.nudge.di;

import com.reactiverobot.nudge.MainActivity;
import com.reactiverobot.nudge.SearchActivity;
import com.reactiverobot.nudge.di.test.TestModule;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
abstract class ActivityModule {
    @ContributesAndroidInjector(modules = { TestModule.class })
    abstract MainActivity contributeMainActivityInjector();

    @ContributesAndroidInjector(modules = { TestModule.class })
    abstract SearchActivity contributeSearchActivityInjector();
}
