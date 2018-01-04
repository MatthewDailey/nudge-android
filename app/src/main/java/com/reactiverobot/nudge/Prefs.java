package com.reactiverobot.nudge;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;


public class Prefs {

    private static Set<String> getDefaultBlockedPackages() {
        Set<String> defaultPinnedPackages = new HashSet<>();
        defaultPinnedPackages.add("com.facebook.katana");
        defaultPinnedPackages.add( "com.instagram.android");
        return defaultPinnedPackages;
    }

    private static final Set<String> DEFAULT_BLOCKED_PACKAGES = getDefaultBlockedPackages();

    private static final String INDEXED_PACKAGES = "indexed_packages";
    private static final String PINNED_PACKAGES = "pinned_packages";
    private static final String BLOCKED_PACKAGES = "blocked_packages";

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


    private Set<String> getPackages(String key, Set<String> defaultPackages) {
        Set<String> pinnedPackages = getPrefs().getStringSet(key, null);

        if (pinnedPackages == null) {
            pinnedPackages = defaultPackages;
            getPrefs().edit().putStringSet(key, defaultPackages).commit();
        }

        return new HashSet<>(pinnedPackages);
    }

    public Set<String> getPinnedPackages() {
        return getPackages(PINNED_PACKAGES, DEFAULT_BLOCKED_PACKAGES);
    }

    public Set<String> getBlockedPackages() {
        return getPackages(BLOCKED_PACKAGES, DEFAULT_BLOCKED_PACKAGES);
    }

    private void updateStringSet(String setKey, Set<String> originalSet, String toUpdate, boolean membership) {
        if (membership) {
            originalSet.add(toUpdate);
        } else {
            originalSet.remove(toUpdate);
        }

        getPrefs().edit().putStringSet(setKey, originalSet).commit();
    }

    synchronized public void setPackagePinned(String packageName, boolean pinned) {
        updateStringSet(PINNED_PACKAGES, getPinnedPackages(), packageName, pinned);

    }

    synchronized public void setPackageBlocked(String packageName, boolean blocked) {
        updateStringSet(BLOCKED_PACKAGES, getBlockedPackages(), packageName, blocked);

    }

    public Set<String> getIndexedPackages() {
        return new HashSet<>(getPrefs().getStringSet(INDEXED_PACKAGES, new HashSet<String>()));
    }

    synchronized public void setPackageIndexed(String packageName) {
        updateStringSet(INDEXED_PACKAGES, getIndexedPackages(), packageName, true);

    }
}
