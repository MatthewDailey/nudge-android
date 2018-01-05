package com.reactiverobot.nudge.di;

import android.content.Context;

import com.reactiverobot.nudge.job.CheckActiveAppModule;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;

@Component(modules = {
        ActivityModule.class,
        CheckActiveAppModule.class
})
public interface NudgeApplicationComponent {
    void inject(NudgeApplication application);

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder context(Context context);

        NudgeApplicationComponent build();
    }
}
