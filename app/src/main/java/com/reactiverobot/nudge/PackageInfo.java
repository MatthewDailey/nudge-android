package com.reactiverobot.nudge;


import android.graphics.drawable.Drawable;

import com.reactiverobot.nudge.info.PackageType;

import java.util.HashMap;
import java.util.Map;

public final class PackageInfo {
    public enum Type {
        HEADING,
        PACKAGE,
    }

    public final String packageName;
    public final Type type;
    private final Map<PackageType, Boolean> selectedTypes = new HashMap<>();

    @Override
    public String toString() {
        return "PackageInfo{" +
                "packageName='" + packageName + '\'' +
                ", type=" + type +
                ", selectedTypes=" + selectedTypes +
                ", name='" + name + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", iconDrawable=" + iconDrawable +
                '}';
    }

    public String name;
    public String iconUrl;
    public Drawable iconDrawable;

    public boolean isSelected(PackageType packageType) {
        return selectedTypes.getOrDefault(packageType, false);
    }

    public void setSelected(PackageType packageType, boolean isSelected) {
        selectedTypes.put(packageType, isSelected);
    }

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
