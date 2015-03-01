package com.ry.target;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * @Title: MainActivity.java
 * @Description:
 * @author yangbing3@ucweb.com
 * @date 2014-9-26 2:14:49
 */
public class MainActivity extends Activity implements OnClickListener {

    private JNI jni = new JNI();
    private int mNumber;
    private TextView mNumberTv;
    private boolean mRunning;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            mNumber=msg.what;
            mNumberTv.setText(mNumber+"");
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        mNumberTv = (TextView) findViewById(R.id.number_tv);
        jni.init();
        mRunning=true;
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (mRunning) {
                    int number = jni.getNumber(mNumber);
                    mHandler.sendEmptyMessage(number);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }

            }
        }).start();
        
        findViewById(R.id.hook_btn).setOnClickListener(this);
        
        
        
    }
    @Override
    protected void onDestroy() {
        mRunning=false;
        jni.destroy();
        super.onDestroy();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.hook_btn:
            
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);     
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ComponentName cn = new ComponentName("com.ry.inject", "com.ry.inject.MainActivity");           
                intent.setComponent(cn);
                startActivity(intent);
                break;

            default:
                break;
        }
        
    }


}
