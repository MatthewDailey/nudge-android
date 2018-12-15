package com.reactiverobot.nudge;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.reactiverobot.nudge.checker.ActivePackageChecker;
import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageListManagerSupplier;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class RedesignActivity extends AppCompatActivity {

    @Inject
    Prefs prefs;
    @Inject
    ActivePackageChecker packageChecker;
    @Inject
    PackageInfoManager packageInfoManager;
    @Inject
    PackageListManagerSupplier packageListManagerSupplier;

    private static final String TAG = RedesignActivity.class.getName();

    private PackageType currentFocusPackageType = PackageType.BAD_HABIT;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
    }

}
