package com.reactiverobot.nudge.info;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import com.reactiverobot.nudge.R;

public class BreathingActivity extends AppCompatActivity {

    public static final int SCALE_FACTOR = 10;

    private void afterAnimator(AnimatorSet animatorSet, Runnable after) {
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                after.run();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_breathing);

        View block = findViewById(R.id.block);

        AnimatorSet scaleOutAnimator = new AnimatorSet();
        scaleOutAnimator.playTogether(
                ObjectAnimator.ofFloat(block, "scaleX", SCALE_FACTOR),
                ObjectAnimator.ofFloat(block, "scaleY", SCALE_FACTOR));
        scaleOutAnimator.setDuration(4000);
        scaleOutAnimator.start();

        AnimatorSet scaleInAnimator = new AnimatorSet();
        scaleInAnimator.playTogether(
                ObjectAnimator.ofFloat(block, "scaleX", 1),
                ObjectAnimator.ofFloat(block, "scaleY", 1));
        scaleInAnimator.setDuration(4000);

        ObjectAnimator rotation = ObjectAnimator.ofFloat(block, "rotation", 0f, 90f);
        rotation.setDuration(2000);

        AnimatorSet outerRotation = new AnimatorSet();
        outerRotation.play(rotation);

        AnimatorSet innerRotation = new AnimatorSet();
        innerRotation.play(rotation);

        afterAnimator(scaleOutAnimator, () -> outerRotation.start());
        afterAnimator(outerRotation, () -> scaleInAnimator.start());
        afterAnimator(scaleInAnimator, () -> innerRotation.start());
        afterAnimator(innerRotation, () -> scaleOutAnimator.start());
    }
}
