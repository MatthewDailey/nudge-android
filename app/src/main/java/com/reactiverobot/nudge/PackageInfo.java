package com.reactiverobot.nudge;


import android.graphics.drawable.Drawable;

public final class PackageInfo {
    public enum Type {
        HEADING,
        PACKAGE,
    }

    final public String packageName;
    final public Type type;

    public String name;
    public String iconUrl;
    public Drawable iconDrawable;
    public boolean badHabit;
    public boolean goodOption;

    public PackageInfo(String packageName) {
        this.packageName = packageName;
        this.type = Type.PACKAGE;
    }

    public PackageInfo(String packageName, Type type) {
        this.packageName = packageName;
        this.type = type;
    }

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
}
