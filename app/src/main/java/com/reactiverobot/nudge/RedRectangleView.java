package com.reactiverobot.nudge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class RedRectangleView extends View {
    private Paint paint;

    public RedRectangleView(Context context) {
        super(context);
        init();
    }

    private void init() {
        // Initialize a paint object with color red
        paint = new Paint();
        paint.setColor(Color.RED);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Fill the canvas with red color
        // Use getWidth() for the view's current width and getHeight() for the view's current height
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }
}
