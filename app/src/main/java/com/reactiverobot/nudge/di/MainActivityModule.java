package com.reactiverobot.nudge.di;

import android.app.Activity;

import com.reactiverobot.nudge.MainActivity;

import dagger.Binds;
import dagger.Module;
import dagger.android.ActivityKey;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;

@Module(subcomponents = MainActivitySubcomponent.class)
abstract class MainActivityModule  {

    @Binds
    @IntoMap
    @ActivityKey(MainActivity.class)
    abstract AndroidInjector.Factory<? extends Activity>
        bindMainActivityInjectorFactory(MainActivitySubcomponent.Builder builder);

}
