package com.reactiverobot.nudge.di.test;

import javax.inject.Inject;


public class TestClass {

    @Inject
    TestInterface api;

    @Inject
    public TestClass() {}

    public void run() {
        api.coolMethod();
    }
}
