package com.reactiverobot.nudge;

import android.content.Context;
import android.content.SharedPreferences;


public class Prefs {

    private static final String CHECK_ACTIVE_ENABLED = "check_active_enabled";

    private final Context context;

    public static Prefs from(Context context) {
        return new Prefs(context);
    }

    private Prefs(Context context) {
        this.context = context;
    }

    private SharedPreferences getPrefs() {
         return this.context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE);
    }

    public void setCheckActiveEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(CHECK_ACTIVE_ENABLED, enabled).commit();
    }

    public boolean getCheckActiveEnabled() {
        return getPrefs().getBoolean(CHECK_ACTIVE_ENABLED, false);
    }
}
