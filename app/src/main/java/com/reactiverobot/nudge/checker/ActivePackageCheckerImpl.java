package com.reactiverobot.nudge.checker;


import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.reactiverobot.nudge.SuggestChangeActivity;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ActivePackageCheckerImpl implements ActivePackageChecker {

    private final static int DEBOUNCE_MS = 500;
    private static final String TAG = ActivePackageCheckerImpl.class.getName();

    private final AtomicLong nextAllowedLaunch = new AtomicLong(0);
    private final Context context;
    private final Prefs prefs;

    public ActivePackageCheckerImpl(Context context, Prefs prefs) {
        this.context = context;
        this.prefs = prefs;
    }

    @Override
    public void launchSuggestActivityIfBlocked(String packageName) {
        if (packageName.equals("com.reactiverobot.nudge")) {
            Log.d(TAG, "Saw self, doing nothing." );
            return;
        }

        if (!prefs.getCheckActiveEnabled()) {
            Log.d(TAG, "Check is not enabled.");
            return;
        }

        if (prefs.isPackageBlocked(packageName)) {
            launchSuggestChangeActivity(packageName);
        }
    }

    @Override
    public String getCurrentActivePackage() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService( Context.ACTIVITY_SERVICE );
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for(ActivityManager.RunningAppProcessInfo appProcess : appProcesses){
            if(appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND){
                Log.i("Foreground App", appProcess.processName);
                return appProcess.processName;
            }
        }
        return null;
    }

    private synchronized void launchSuggestChangeActivity(String eventPackageName) {
        long currentTime = System.currentTimeMillis();

        long allowedTime = nextAllowedLaunch.getAndUpdate((allowedNextTime) -> {
            if (currentTime < allowedNextTime) {
                return allowedNextTime;
            }

            return currentTime + DEBOUNCE_MS;
        });

        if (currentTime < allowedTime) {
            Log.d(TAG, "Debouncing launching SuggestChangeActivity");
            return;
        }

        Log.d(TAG, "Launching SuggestChangeActivity");

        Intent intent = new Intent(context, SuggestChangeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(SuggestChangeActivity.EXTRA_APP_BEING_BLOCKED, eventPackageName);
        context.startActivity(intent);
    }
}
