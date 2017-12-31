package com.reactiverobot.nudge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class SuggestChangeActivity extends Activity {

    private void launchApplicationAndClose(String packageName) {
        Intent i = getPackageManager().getLaunchIntentForPackage(packageName);
        startActivity(i);

        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggest_change);

        findViewById(R.id.button_open_option_1)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        launchApplicationAndClose("com.amazon.kindle");
                    }
                });

        findViewById(R.id.button_open_option_2)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        launchApplicationAndClose("io.spire.android");
                    }
                });
    }
}
