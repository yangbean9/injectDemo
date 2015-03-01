package com.ry.inject;


import com.ry.inject.service.FloatWindowService;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * @Title: MainActivity.java
 * @Description:
 * @author yangbing3@ucweb.com
 * @date 2014-9-26 2:14:49
 */
public class MainActivity extends Activity  {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        startService(new Intent(this,FloatWindowService.class));
        this.finish();
    }
}
