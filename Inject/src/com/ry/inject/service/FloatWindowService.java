package com.ry.inject.service;

import com.ry.inject.MainActivity;
import com.ry.inject.R;
import com.ry.inject.window.FloatWindowManager;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


/**
 * 服务，管理悬浮窗
 * 
 * @author yangbing3@ucweb.com
 * */
public class FloatWindowService extends Service {
	
   
    
    private static boolean isStart=false;

    private FloatWindowManager mFloatWindowManager;

    private static FloatWindowService sContext;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
    	
        super.onCreate();
        sContext = this;
        mFloatWindowManager = new FloatWindowManager(this);

        
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	
        mFloatWindowManager.showDockWindow();
        notification();
        isStart=true;
        return START_NOT_STICKY;
        
    }

    public static FloatWindowService getContext() {
        return sContext;
    }
    
    public static boolean isStart() {
		return isStart;
	}

	/**
     * 将服务startForeground
     * */
    @SuppressWarnings("deprecation")
    private void notification() {
        Notification notification = new Notification(R.drawable.ic_launcher, "Inject", System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "Inject", "Inject运行中", pendingIntent);
        startForeground(1234456, notification);
    }

    @Override
    public void onDestroy() {
        if (mFloatWindowManager != null) {
            mFloatWindowManager.destory();
            mFloatWindowManager = null;
        }
        isStart=false;
        super.onDestroy();
    }
}
