package com.reactiverobot.nudge.di;

import com.reactiverobot.nudge.MainActivity;
import com.reactiverobot.nudge.di.test.TestModule;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.ContributesAndroidInjector;
import dagger.android.DispatchingAndroidInjector;

@Singleton
@Component(modules = { MainActivityModule.class })
public interface NudgeApplicationComponent {
    void inject(NudgeApplication application);

}
