package com.reactiverobot.nudge.di.test;

import dagger.Component;

@Component(modules = TestModule.class)
public interface TestComponent {
    TestClass testClass();
}
