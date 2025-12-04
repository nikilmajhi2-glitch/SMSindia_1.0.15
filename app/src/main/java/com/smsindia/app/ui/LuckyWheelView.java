package com.smsindia.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class LuckyWheelView extends View {

    private int mWidth;
    private int mHeight;
    private Paint mPaint;
    private Paint mTextPaint;
    private String[] mTitles = {"₹0.6", "₹0.8", "₹10", "₹0", "₹100", "₹0.6"};
    private int[] mColors = {
            Color.parseColor("#FF5252"), // Red
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#607D8B"), // Grey
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#2196F3")  // Blue
    };
    
    // We start angle at 270 (top)
    private float mStartAngle = 0;

    public LuckyWheelView(Context context) {
        this(context, null);
    }

    public LuckyWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(60f); // Adjust size as needed
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setFakeBoldText(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mWidth == 0) return;

        int radius = Math.min(mWidth, mHeight) / 2;
        int centerX = mWidth / 2;
        int centerY = mHeight / 2;
        
        RectF range = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        float sweepAngle = 360f / mTitles.length;

        for (int i = 0; i < mTitles.length; i++) {
            mPaint.setColor(mColors[i % mColors.length]);
            canvas.drawArc(range, mStartAngle + (i * sweepAngle), sweepAngle, true, mPaint);
            
            // Draw Text
            drawText(canvas, mStartAngle + (i * sweepAngle), sweepAngle, mTitles[i], radius, centerX, centerY);
        }
    }

    private void drawText(Canvas canvas, float startAngle, float sweepAngle, String text, int radius, int centerX, int centerY) {
        float angle = (float) Math.toRadians(startAngle + sweepAngle / 2);
        float x = (float) (centerX + (radius * 0.70) * Math.cos(angle)); // 0.70 to center text inside slice
        float y = (float) (centerY + (radius * 0.70) * Math.sin(angle));

        // Center text vertically
        float textHeight = mTextPaint.descent() - mTextPaint.ascent();
        float textOffset = (textHeight / 2) - mTextPaint.descent();

        canvas.drawText(text, x, y + textOffset, mTextPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
    }
}
