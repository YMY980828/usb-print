package com.example.gw.usbprint.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.SweepGradient;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

public class FaceView extends View {
    public FaceView(Context context) {
        super(context);
        init();
    }

    public FaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FaceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public FaceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
    private int color = Color.parseColor("#1b315e");
    Paint paint;
    Paint boder;
    Paint rotatePaint;
    Path path;
    PathMeasure pathMeasure;
    private void init(){
        paint =new Paint(Paint.ANTI_ALIAS_FLAG);
        boder =new Paint(Paint.ANTI_ALIAS_FLAG);
        rotatePaint =new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        rotatePaint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(10);
        rotatePaint.setStrokeWidth(10);
        boder.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

    }
    float ratio=0;
    float start = 0;
    float stop = 0;
    Path mDistPath;


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        SweepGradient sweepGradient = new SweepGradient(getWidth()/2,getHeight()/2,Color.RED,Color.BLUE);
        rotatePaint.setShader(sweepGradient);
        mDistPath = new Path();
        path = new Path();

        path.addCircle(getWidth()/2,getHeight()*0.5f,Math.min(getWidth()/2,getHeight())*0.8f,Path.Direction.CW);
        pathMeasure = new PathMeasure();
        pathMeasure.setPath(path,true);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0,1);
        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        valueAnimator.setDuration(3000);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ratio = (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                ratio=0;
            }
        });
        valueAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(color);
        float length = pathMeasure.getLength();
        float stop =length*ratio;
        float start = (float)(stop-(0.5-Math.abs(ratio-0.5))*length);
        mDistPath.reset();
        pathMeasure.getSegment(start,stop,mDistPath,true);
        canvas.drawCircle(getWidth()/2,getHeight()*0.5f,Math.min(getWidth()/2,getHeight())*0.8f,paint);
        canvas.drawPath(mDistPath,rotatePaint);
        canvas.drawCircle(getWidth()/2,getHeight()*0.5f,Math.min(getWidth()/2,getHeight())*0.8f,boder);

    }
}
