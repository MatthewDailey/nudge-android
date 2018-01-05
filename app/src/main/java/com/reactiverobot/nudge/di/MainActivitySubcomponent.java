package com.reactiverobot.nudge.di;

import com.reactiverobot.nudge.MainActivity;
import com.reactiverobot.nudge.di.test.TestModule;

import dagger.Subcomponent;
import dagger.android.AndroidInjector;

@Subcomponent(modules = TestModule.class)
public interface MainActivitySubcomponent extends AndroidInjector<MainActivity> {

    @Subcomponent.Builder
    public abstract class Builder extends AndroidInjector.Builder<MainActivity>{}
}
