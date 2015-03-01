package com.ry.inject.window;

import com.ry.inject.JNI;
import com.ry.inject.R;
import com.ry.inject.service.FloatWindowService;

import android.view.View;
import android.widget.Button;


/**
 * 操作悬浮窗
 * 
 * @author yangbing3@ucweb.com
 * */
public class DockWindow extends MoveableWindow implements View.OnClickListener {

    private Button mStartHookBtn;
//    private Button mStopHookBtn;
    private Button mExitBtn;

    private JNI jni=new JNI();


    public DockWindow(FloatWindowManager fwManager) {
        super(fwManager, R.layout.float_window);

    }

    @Override
    public void initView() {
        mStartHookBtn = (Button) findViewById(R.id.start_hook_btn);
        mStartHookBtn.setOnClickListener(this);

//        mStopHookBtn = (Button) findViewById(R.id.stop_hook_btn);
//        mStopHookBtn.setOnClickListener(this);
        
        mExitBtn = (Button) findViewById(R.id.exit_btn);
        mExitBtn.setOnClickListener(this);

    }

    @Override
    public void setLayoutParams(android.view.WindowManager.LayoutParams layoutParams) {
        setDefaultLayoutParams(layoutParams);
        // 设置显示位置
        layoutParams.x = 10;
        layoutParams.y = 0;
        layoutParams.flags = android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_hook_btn:
                jni.startHook(FloatWindowService.getContext());
                break;
//            case R.id.stop_hook_btn:
//                jni.stopHook();
//                break;
            case R.id.exit_btn:
                FloatWindowService.getContext().stopSelf();
                break;

            default:
                break;
        }

    }


}
