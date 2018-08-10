package com.reactiverobot.nudge.info;

import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionManager;
import android.view.View;

import com.reactiverobot.nudge.R;

public class BreathingActivity extends AppCompatActivity {

    int currentLayout = R.layout.activity_breathing_1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_breathing_1);

        final ConstraintLayout constraintLayout = findViewById(R.id.constraint_layout_breathing);
        constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentLayout = currentLayout == R.layout.activity_breathing_1 ?
                        R.layout.activity_breathing_2 : R.layout.activity_breathing_1;
                changeToScene(currentLayout);
            }
        });
    }

    private void changeToScene(int activity_resource) {
        final ConstraintLayout constraintLayout = findViewById(R.id.constraint_layout_breathing);

        final ConstraintSet constraintSet3 = new ConstraintSet();
        constraintSet3.clone(getApplicationContext(), activity_resource);

        TransitionManager.beginDelayedTransition(constraintLayout);
        constraintSet3.applyTo(constraintLayout);
    }
}
