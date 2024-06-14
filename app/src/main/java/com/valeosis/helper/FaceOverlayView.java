package com.valeosis.helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FaceOverlayView extends View {
    private final Paint paint;
    private final List<RectF> faceRects = new ArrayList<>();

    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(0xFFFF0000); // Red color
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f);
    }

    public void updateFaceRects(List<RectF> rects) {
        faceRects.clear();
        faceRects.addAll(rects);
        invalidate(); // Trigger onDraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (RectF rect : faceRects) {
            canvas.drawRect(rect, paint);
        }
    }
}


