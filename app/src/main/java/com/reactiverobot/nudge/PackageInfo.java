package com.reactiverobot.nudge;


public final class PackageInfo {
    public final String name;
    public final String iconUrl;
    public final String packageName;
    public final boolean blocked;

    public PackageInfo(String name, String iconUrl, String packageName, boolean blocked) {
        this.name = name;
        this.iconUrl = iconUrl;
        this.packageName = packageName;
        this.blocked = blocked;
    }
}
