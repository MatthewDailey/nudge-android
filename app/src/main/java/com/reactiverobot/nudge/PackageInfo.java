package com.reactiverobot.nudge;


import android.graphics.drawable.Drawable;

public final class PackageInfo {
    final public String packageName;

    public String name;
    public String iconUrl;
    public Drawable iconDrawable;
    public boolean blocked;

    public PackageInfo(String name, String iconUrl, Drawable iconDrawable, String packageName, boolean blocked) {
        this.name = name;
        this.iconUrl = iconUrl;
        this.iconDrawable = iconDrawable;
        this.packageName = packageName;
        this.blocked = blocked;
    }
}
