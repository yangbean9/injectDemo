package com.ry.inject.window;

import java.lang.reflect.Field;

import android.view.MotionEvent;
/**
 * 可以移动的悬浮窗
 * 
 * @author yangbing3@ucweb.com
 * */
public abstract class MoveableWindow extends BaseWindow {
    /**
     * 记录系统状态栏的高度
     */
    private static int statusBarHeight;
    /**
     * 记录当前手指位置在屏幕上的横坐标值
     */
    private float xInScreen;

    /**
     * 记录当前手指位置在屏幕上的纵坐标值
     */
    private float yInScreen;

    /**
     * 记录手指按下时在屏幕上的横坐标的值
     */
    private float xDownInScreen;

    /**
     * 记录手指按下时在屏幕上的纵坐标的值
     */
    private float yDownInScreen;

    /**
     * 记录手指按下时在悬浮窗的View上的横坐标的值
     */
    private float xInView;
    /**
     * 记录手指按下时在悬浮窗的View上的纵坐标的值
     */
    private float yInView;
    public MoveableWindow(FloatWindowManager fwManager, int layoutId) {
        super(fwManager, layoutId);
        
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            // 手指按下时记录必要数据,纵坐标的值都需要减去状态栏高度
            xInView = event.getX();
            yInView = event.getY();
            xDownInScreen = event.getRawX();
            yDownInScreen = event.getRawY() - getStatusBarHeight();
            xInScreen = event.getRawX();
            yInScreen = event.getRawY() - getStatusBarHeight();
            break;
        case MotionEvent.ACTION_MOVE:
            xInScreen = event.getRawX();
            yInScreen = event.getRawY() - getStatusBarHeight();
            // 手指移动的时候更新悬浮窗的位置
            updateViewPosition();
            break;
        case MotionEvent.ACTION_UP:
            // 手指移动距离小，认为是单击事件，下发给子视图处理，移动距离大，干掉不下发
            return Math.abs(xDownInScreen - xInScreen) < 10 && Math.abs(yDownInScreen - yInScreen) < 10 ? false : true;
        default:
            break;
        }
        return false;
    }
    /**
     * 更新悬浮窗在屏幕中的位置
     */
    private void updateViewPosition() {
        mLayoutParams.x = (int) (xInScreen - xInView);
        mLayoutParams.y = (int) (yInScreen - yInView);
        mFloatWindowManager.getWindowManager().updateViewLayout(this, mLayoutParams);
    }
    /**
     * 获取状态栏的高度
     * 
     */
    private int getStatusBarHeight() {
        if (statusBarHeight == 0) {
            try {
                Class<?> c = Class.forName("com.android.internal.R$dimen");
                Object o = c.newInstance();
                Field field = c.getField("status_bar_height");
                int x = (Integer) field.get(o);
                statusBarHeight = getResources().getDimensionPixelSize(x);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return statusBarHeight;
    }
}
