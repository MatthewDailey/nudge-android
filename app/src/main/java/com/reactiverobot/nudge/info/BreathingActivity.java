package com.reactiverobot.nudge.info;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.reactiverobot.nudge.R;

public class BreathingActivity extends AppCompatActivity {

    public static final int SCALE_FACTOR = 10;

    private static final int BREATHE_DURATION_MS = 4000;
    public static final int HOLD_DURATION_MS = BREATHE_DURATION_MS / 2;
    private static final int TEXT_FADE_DURATION_MS = 500;

    private void afterAnimator(AnimatorSet animatorSet, Runnable after) {
        animatorSet.removeAllListeners();
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                after.run();
            }
        });
    }

    private void setNextText(String newText, int completeFadeDuration) {
        TextView breatheInstructions = findViewById(R.id.text_breathe_instruction);

        AnimatorSet hideTextAnimation = new AnimatorSet();
        hideTextAnimation.play(ObjectAnimator.ofFloat(breatheInstructions, "alpha", 0));
        hideTextAnimation.setDuration(TEXT_FADE_DURATION_MS);
        hideTextAnimation.setStartDelay(completeFadeDuration - TEXT_FADE_DURATION_MS);

        AnimatorSet showTextAnimation = new AnimatorSet();
        showTextAnimation.play(ObjectAnimator.ofFloat(breatheInstructions, "alpha", 1));
        showTextAnimation.setDuration(TEXT_FADE_DURATION_MS);

        afterAnimator(hideTextAnimation, () -> {
            breatheInstructions.setText(newText);
            showTextAnimation.start();
        });
        hideTextAnimation.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_breathing);

        View block = findViewById(R.id.block);
        TextView breatheInstructions = findViewById(R.id.text_breathe_instruction);

        AnimatorSet scaleOutAnimator = new AnimatorSet();
        scaleOutAnimator.playTogether(
                ObjectAnimator.ofFloat(block, "scaleX", SCALE_FACTOR),
                ObjectAnimator.ofFloat(block, "scaleY", SCALE_FACTOR));
        scaleOutAnimator.setDuration(BREATHE_DURATION_MS);

        AnimatorSet scaleInAnimator = new AnimatorSet();
        scaleInAnimator.playTogether(
                ObjectAnimator.ofFloat(block, "scaleX", 1),
                ObjectAnimator.ofFloat(block, "scaleY", 1));
        scaleInAnimator.setDuration(BREATHE_DURATION_MS);

        ObjectAnimator rotation = ObjectAnimator.ofFloat(block, "rotation", 0f, 90f);
        rotation.setDuration(HOLD_DURATION_MS);

        AnimatorSet outerRotation = new AnimatorSet();
        outerRotation.play(rotation);

        AnimatorSet innerRotation = new AnimatorSet();
        innerRotation.play(rotation);

        afterAnimator(scaleOutAnimator, () -> {
            outerRotation.start();
            setNextText("OUT", BREATHE_DURATION_MS / 2);
        });
        afterAnimator(outerRotation, () -> {
            scaleInAnimator.start();
            setNextText("HOLD", BREATHE_DURATION_MS);
        });
        afterAnimator(scaleInAnimator, () -> {
            innerRotation.start();
            setNextText("IN", BREATHE_DURATION_MS / 2);
        });
        afterAnimator(innerRotation, () -> {
            scaleOutAnimator.start();
            setNextText("HOLD", BREATHE_DURATION_MS);
        });

        scaleOutAnimator.start();
        setNextText("HOLD", BREATHE_DURATION_MS);
    }
}
