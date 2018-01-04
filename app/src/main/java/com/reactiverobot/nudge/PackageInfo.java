package com.reactiverobot.nudge;


public final class PackageInfo {
    public String name;
    public String iconUrl;
    public String packageName;
    public boolean blocked;

    public PackageInfo(String name, String iconUrl, String packageName, boolean blocked) {
        this.name = name;
        this.iconUrl = iconUrl;
        this.packageName = packageName;
        this.blocked = blocked;
    }
}
