package com.reactiverobot.nudge;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.api.Api;
import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.Set;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class InterceptShortActivity extends Activity {

    private final static String TAG = InterceptShortActivity.class.getName();


    @Inject
    PackageInfoManager packageInfoManager;
    @Inject
    Prefs prefs;
    @Inject
    NudgeAccessibilityService mServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intercept_short);
        this.setFinishOnTouchOutside(false);

        Button goBackButton = findViewById(R.id.button_back_out);
        goBackButton.setOnClickListener(v -> {
            finish();
            if (mServer != null) {
                Log.d(TAG, "Going back");
                mServer.goBack();
            } else {
                Log.e(TAG, "Unable to go back - mServer is null");
            }
        });

        Button keepWatchingButton = findViewById(R.id.button_keep_watching);
        keepWatchingButton.setOnClickListener(v -> finish());
    }
}
