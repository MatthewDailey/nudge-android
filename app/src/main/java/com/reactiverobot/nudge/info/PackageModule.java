package com.reactiverobot.nudge.info;

import android.content.Context;

import com.reactiverobot.nudge.prefs.Prefs;

import dagger.Module;
import dagger.Provides;


@Module
public class PackageModule {
    @Provides
    PackageInfoManager packageInfoManager(Context context, Prefs prefs) {
        return new PackageInfoManagerImpl(context, prefs);
    }
}
