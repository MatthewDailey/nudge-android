package com.reactiverobot.nudge.di;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = { ActivityModule.class })
public interface NudgeApplicationComponent {
    void inject(NudgeApplication application);
}
