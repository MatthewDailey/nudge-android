package com.reactiverobot.nudge.di.test;

import android.util.Log;


public class TestInterfaceImpl implements TestInterface {
    @Override
    public void coolMethod() {
        Log.d("TEST DI", "called coolMethod");
    }
}
