package com.ry.inject.window;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;

/**
 * 悬浮窗视图管理
 * 
 * @author yangbing3@ucweb.com
 * */
public class FloatWindowManager {

    private Context mContext;
    protected WindowManager mWindowManager;
    private BaseWindow mDockWindow;

    public FloatWindowManager(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public Context getContext() {
        return mContext;
    }

    public WindowManager getWindowManager() {
        return mWindowManager;
    }

    public void showDockWindow() {
    	
        if(mDockWindow==null){
            mDockWindow = new DockWindow(this);
        }
        
        if (!mDockWindow.isShow()) {
			
        	addWindow(mDockWindow, mDockWindow.getLayoutParams());
            mDockWindow.setShow(true);
        	
		}
    }
          
    public void addWindow(View view, android.view.WindowManager.LayoutParams layoutParams) {
        mWindowManager.addView(view, layoutParams);
    }

    public void hideDockWindow() {
        if (mDockWindow != null&&mDockWindow.isShow()) {
            mWindowManager.removeView(mDockWindow);
            mDockWindow.setShow(false);
        }
    }
    
    public void destory() {
        if (mDockWindow != null) {
            if(mDockWindow.isShow()){
                mWindowManager.removeView(mDockWindow);
            }
            mDockWindow = null;
        }
    }
}
