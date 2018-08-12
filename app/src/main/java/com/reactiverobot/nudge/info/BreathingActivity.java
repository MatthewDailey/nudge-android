package com.reactiverobot.nudge.info;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import com.reactiverobot.nudge.R;

public class BreathingActivity extends AppCompatActivity {

    public static final int SCALE_FACTOR = 10;
    int currentLayout = R.layout.activity_breathing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_breathing);

        View block = findViewById(R.id.block);

        ScaleAnimation scaleOut = new ScaleAnimation(1, SCALE_FACTOR, 1, SCALE_FACTOR, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleOut.setDuration(4000);

        ScaleAnimation scaleIn = new ScaleAnimation(SCALE_FACTOR, 1, SCALE_FACTOR, 1,  Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleIn.setDuration(4000);

        scaleOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                block.startAnimation(scaleIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) { }
        });

        scaleIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                block.startAnimation(scaleOut);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        block.startAnimation(scaleOut);
    }
}
