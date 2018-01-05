package com.reactiverobot.nudge.di.test;

import dagger.Module;
import dagger.Provides;

@Module
public class TestModule {

    @Provides static TestInterface provideTestInterface() {
        return new TestInterfaceImpl();
    }
}
