package com.reactiverobot.nudge.di;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.DispatchingAndroidInjector;

@Singleton
@Component(modules = { MainActivityModule.class })
public interface NudgeApplicationComponent {
    void inject(NudgeApplication application);
}
