package com.customview;

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.github.skykai.stickercamera.R;

/**
 * 照相机井字线
 * Created by sky on 2015/7/7.
 */
public class CameraGrid extends View {

    private int topBannerWidth = 0;
    private Paint mPaint;

    public CameraGrid(Context context) {
        this(context,null);
    }

    public CameraGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setAlpha(120);
        mPaint.setStrokeWidth(1f);
    }


    //Draw a word, draw two gray side, the middle of the square
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = this.getWidth();
        int height = this.getHeight();
        if (width < height) {

            topBannerWidth = height - width;
        }
        if (showGrid) {
            Bitmap originalBitmap= BitmapFactory.decodeResource(getResources(), R.drawable.face_outline1);

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                    originalBitmap, width/2, height/2, false);
            canvas.drawBitmap(resizedBitmap,(float) (width / 3.5),(float) (height / 3.5),mPaint);

           // canvas.drawLine(width / 3, 0, width / 3, height, mPaint);
          //  canvas.drawLine(width * 2 / 3, 0, width * 2 / 3, height, mPaint);
          //  canvas.drawLine(0, height / 3, width, height / 3, mPaint);
          //  canvas.drawLine(0, height * 2 / 3, width, height * 2 / 3, mPaint);
        }
    }

    private boolean showGrid = true;

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public int getTopWidth() {
        return topBannerWidth;
    }
}
