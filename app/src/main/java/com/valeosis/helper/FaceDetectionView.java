package com.valeosis.helper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.util.AttributeSet;


public class FaceDetectionView extends View {


        private Paint paint;
        private RectF boundingBox;

        public FaceDetectionView(Context context) {
            super(context);
            init();
        }

        public FaceDetectionView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        private void init() {
            paint = new Paint();
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
        }

        public void setBoundingBox(RectF boundingBox) {
            this.boundingBox = boundingBox;
            invalidate(); // Redraw the view
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (boundingBox != null) {
                canvas.drawRect(boundingBox, paint);
            }
        }
    }