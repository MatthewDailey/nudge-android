package com.reactiverobot.nudge.checker;

import android.content.Context;

import com.reactiverobot.nudge.prefs.Prefs;

import dagger.Module;
import dagger.Provides;

@Module
public class PackageCheckerModule {
    @Provides
    ActivePackageChecker activePackageChecker(Context context, Prefs prefs) {
        return new ActivePackageCheckerImpl(context, prefs);
    }
}
