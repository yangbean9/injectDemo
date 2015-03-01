package com.ry.inject.window;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

/**
 * 悬浮窗视图基类
 * 
 * @author yangbing3@ucweb.com
 * */
public abstract class BaseWindow extends LinearLayout {

    protected boolean mIsShow;
    protected Context mContext;
    protected FloatWindowManager mFloatWindowManager;

    protected android.view.WindowManager.LayoutParams mLayoutParams;

    /**
     * 构造函数
     * 
     * @param fwManager
     *            :窗口管理
     * @param layoutId
     *            :布局资源id
     * 
     * */
    public BaseWindow(FloatWindowManager fwManager, int layoutId) {
        super(fwManager.getContext());
        mContext = fwManager.getContext();
        mFloatWindowManager = fwManager;
        LayoutInflater.from(fwManager.getContext()).inflate(layoutId, this);
        initView();
        mLayoutParams = new android.view.WindowManager.LayoutParams();
        setLayoutParams(mLayoutParams);
    }

    public android.view.WindowManager.LayoutParams getLayoutParams() {
        return mLayoutParams;
    }

    public boolean isShow() {
        return mIsShow;
    }

    public void setShow(boolean isShow) {
        this.mIsShow = isShow;
    }

    protected void setDefaultLayoutParams(android.view.WindowManager.LayoutParams layoutParams) {
        layoutParams.type = android.view.WindowManager.LayoutParams.TYPE_PHONE;
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.width = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
    }

    public abstract void initView();

    public abstract void setLayoutParams(android.view.WindowManager.LayoutParams layoutParams);

}
