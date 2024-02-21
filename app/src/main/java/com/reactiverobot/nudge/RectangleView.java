package com.reactiverobot.nudge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

public class RectangleView extends View {
    private Paint paint;

    public RectangleView(Context context) {
        super(context);
        init();

        // Set the view's onTouchListener to a new instance of HapticTouchListener
        setOnTouchListener(new HapticTouchListener());
    }

    private void init() {
        // Initialize a paint object with color red
        paint = new Paint();
        paint.setColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Fill the canvas with red color
        // Use getWidth() for the view's current width and getHeight() for the view's current height
        RectF r = new RectF(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(r, 0 , 0, paint);
    }

    class HapticTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    break;
                case MotionEvent.ACTION_UP:
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE);
                    break;
            }
            return true;
        }
    }

}
