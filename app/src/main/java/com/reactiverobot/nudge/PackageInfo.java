package com.reactiverobot.nudge;


import android.graphics.drawable.Drawable;

public final class PackageInfo {
    final public String packageName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PackageInfo that = (PackageInfo) o;

        return packageName.equals(that.packageName);
    }

    @Override
    public int hashCode() {
        return packageName.hashCode();
    }

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
