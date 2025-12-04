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
    
    // ✅ UPDATED: Royal Blue & Gold Theme
    private int[] mColors = {
            Color.parseColor("#2962FF"), // Royal Blue
            Color.parseColor("#FFC107"), // Gold
            Color.parseColor("#0D47A1"), // Dark Blue
            Color.parseColor("#2962FF"), // Royal Blue
            Color.parseColor("#FFC107"), // Gold
            Color.parseColor("#0D47A1")  // Dark Blue
    };
    
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
        mTextPaint.setTextSize(50f); // Slightly smaller for better fit
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setFakeBoldText(true);
        
        // Add Shadow so white text is visible on Gold slices
        mTextPaint.setShadowLayer(3f, 1f, 1f, Color.parseColor("#80000000"));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mWidth == 0) return;

        int radius = Math.min(mWidth, mHeight) / 2;
        int centerX = mWidth / 2;
        int centerY = mHeight / 2;
        
        // Add a small margin (padding) so it doesn't touch the edges
        int padding = 10;
        RectF range = new RectF(centerX - radius + padding, centerY - radius + padding, 
                                centerX + radius - padding, centerY + radius - padding);

        float sweepAngle = 360f / mTitles.length;

        for (int i = 0; i < mTitles.length; i++) {
            mPaint.setColor(mColors[i % mColors.length]);
            canvas.drawArc(range, mStartAngle + (i * sweepAngle), sweepAngle, true, mPaint);
            
            drawText(canvas, mStartAngle + (i * sweepAngle), sweepAngle, mTitles[i], radius - padding, centerX, centerY);
        }
    }

    private void drawText(Canvas canvas, float startAngle, float sweepAngle, String text, int radius, int centerX, int centerY) {
        float angle = (float) Math.toRadians(startAngle + sweepAngle / 2);
        
        // Position text at 75% of the radius
        float x = (float) (centerX + (radius * 0.75) * Math.cos(angle)); 
        float y = (float) (centerY + (radius * 0.75) * Math.sin(angle));

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
