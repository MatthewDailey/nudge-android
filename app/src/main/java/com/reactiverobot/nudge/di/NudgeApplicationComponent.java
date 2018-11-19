package com.reactiverobot.nudge.di;

import android.content.Context;

import com.reactiverobot.nudge.checker.PackageCheckerModule;
import com.reactiverobot.nudge.info.PackageModule;
import com.reactiverobot.nudge.prefs.PrefsModule;

import dagger.BindsInstance;
import dagger.Component;

@Component(modules = {
        AppModule.class,
        PrefsModule.class,
        PackageModule.class,
        PackageCheckerModule.class,
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
