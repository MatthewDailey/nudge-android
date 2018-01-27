package com.reactiverobot.nudge;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class OnboardingActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_onboarding);

        final View imageView = findViewById(R.id.imageView);

        final ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.onboarding_contraint_layout);

        final ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        final ConstraintSet constraintSet2 = new ConstraintSet();
        constraintSet2.clone(this, R.layout.activity_onboarding_alt);

        AutoTransition autoTransition = new AutoTransition();
        autoTransition.setDuration(1000);

        final AtomicBoolean changed = new AtomicBoolean(false);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransitionManager.beginDelayedTransition(constraintLayout);
                if (changed.get()) {
                    constraintSet.applyTo(constraintLayout);
                } else {
                    constraintSet2.applyTo(constraintLayout);
                }
                changed.set(!changed.get());
            }
        });
    }


}
