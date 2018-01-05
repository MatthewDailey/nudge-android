package com.reactiverobot.nudge.di;

import android.content.Context;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;

@Component(modules = { ActivityModule.class })
public interface NudgeApplicationComponent {
    void inject(NudgeApplication application);

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder context(Context context);

        NudgeApplicationComponent build();
    }
}
