package com.reactiverobot.nudge.di;

import android.content.Context;

import com.reactiverobot.nudge.ChooseOnePackageActivity;
import com.reactiverobot.nudge.InterceptShortActivity;
import com.reactiverobot.nudge.MainActivity;
import com.reactiverobot.nudge.NudgeAccessibilityService;
import com.reactiverobot.nudge.OnboardingActivity;
import com.reactiverobot.nudge.RedesignActivity;
import com.reactiverobot.nudge.SelectPackagesActivity;
import com.reactiverobot.nudge.SuggestChangeActivity;
import com.reactiverobot.nudge.prefs.Prefs;
import com.reactiverobot.nudge.prefs.PrefsImpl;
import com.reactiverobot.nudge.prefs.PrefsModule;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module
abstract class AppModule {

    @ContributesAndroidInjector(modules = { PrefsModule.class })
    abstract MainActivity contributeMainActivityInjector();

    @ContributesAndroidInjector(modules = { PrefsModule.class })
    abstract RedesignActivity contributeRedesignActivityInjector();

    @ContributesAndroidInjector(modules = { PrefsModule.class })
    abstract SuggestChangeActivity contributeSuggestChangeActivityInjector();

    @ContributesAndroidInjector(modules = { PrefsModule.class})
    abstract InterceptShortActivity contributeInterceptShortActivity();

    @ContributesAndroidInjector
    abstract OnboardingActivity contributesOnboardingActivity();

    @ContributesAndroidInjector
    abstract SelectPackagesActivity contributesSelectPackagesActivity();

    @ContributesAndroidInjector
    abstract ChooseOnePackageActivity contributeChooseOnePackageActivity();

    @ContributesAndroidInjector
    abstract NudgeAccessibilityService contributesNudgeAccessibilityService();

    @Provides static NudgeAccessibilityService nudgeAccessibilityService() {
        return NudgeAccessibilityService.instance();
    }
}
