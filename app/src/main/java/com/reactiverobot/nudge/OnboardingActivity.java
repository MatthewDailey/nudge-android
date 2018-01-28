package com.reactiverobot.nudge;

import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class OnboardingActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_onboarding_1);

        final View imageView = findViewById(R.id.title_and_fish);

        final ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.onboarding_contraint_layout);

        final ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        final ConstraintSet constraintSet2 = new ConstraintSet();
        constraintSet2.clone(this, R.layout.activity_onboarding_2);

        final AtomicBoolean changed = new AtomicBoolean(false);

        findViewById(R.id.button_get_started).setOnClickListener(new View.OnClickListener() {
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
