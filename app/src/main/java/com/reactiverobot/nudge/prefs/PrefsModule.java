package com.reactiverobot.nudge.prefs;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class PrefsModule {

    private static Prefs prefs = null;

    @Provides Prefs prefs(Context context) {
        if (prefs == null) {
            prefs = PrefsImpl.from(context);
        }
        return prefs;
    }
}
