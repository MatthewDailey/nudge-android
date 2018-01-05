package com.reactiverobot.nudge.prefs;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class PrefsModule {

    @Provides Prefs prefs(Context context) {
        return PrefsImpl.from(context);
    }
}
