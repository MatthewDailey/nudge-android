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

        final ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.onboarding_contraint_layout);

        findViewById(R.id.button_get_started).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ConstraintSet constraintSet2 = new ConstraintSet();
                constraintSet2.clone(getApplication(), R.layout.activity_onboarding_2);

                TransitionManager.beginDelayedTransition(constraintLayout);
                constraintSet2.applyTo(constraintLayout);
            }
        });

        findViewById(R.id.button_open_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ConstraintSet constraintSet3 = new ConstraintSet();
                constraintSet3.clone(getApplicationContext(), R.layout.activity_onboarding_3);

                TransitionManager.beginDelayedTransition(constraintLayout);
                constraintSet3.applyTo(constraintLayout);
            }
        });

        findViewById(R.id.button_bad_habits).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ConstraintSet constraintSet4 = new ConstraintSet();
                constraintSet4.clone(getApplicationContext(), R.layout.activity_onboarding_4);

                TransitionManager.beginDelayedTransition(constraintLayout);
                constraintSet4.applyTo(constraintLayout);
            }
        });

        findViewById(R.id.button_better_options).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ConstraintSet constraintSet5 = new ConstraintSet();
                constraintSet5.clone(getApplicationContext(), R.layout.activity_onboarding_5);

                TransitionManager.beginDelayedTransition(constraintLayout);
                constraintSet5.applyTo(constraintLayout);
            }
        });
    }


}
