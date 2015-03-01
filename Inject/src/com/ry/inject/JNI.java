package com.ry.inject;

import java.io.File;
import com.ry.inject.util.FileUtils;
import com.ry.inject.util.ShellUtils;

import android.content.Context;

/**
 * @Title: JNI.java
 * @Description:
 * @author yangbing3@ucweb.com
 * @date 2014-9-26 2:29:06
 */
public class JNI {
	

    public static final String INJECT_NAME = "inject";
    
    public static final String HOOKER_SO_NAME = "libhooker.so";
 
 //   public static final String TARGET_PATH = "/data/data/com.ry.target/lib/libtarget.so";
    
	public void startHook(final Context context) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                 
            	 String basePath = context.getFilesDir().getAbsolutePath();
            	 
                 String injectPath = basePath + File.separator + INJECT_NAME;
                 FileUtils.copyAssetsFile(context, INJECT_NAME, injectPath);
                 
                 String hookerPath = basePath + File.separator + HOOKER_SO_NAME;
                 FileUtils.copyAssetsFile(context, HOOKER_SO_NAME, hookerPath);
                 
//                 String substratePath = basePath + File.separator + SUBSTRATE_SO_NAME;
//                 FileUtils.copyAssetsFile(context, SUBSTRATE_SO_NAME, substratePath);

                 try {
                	 String[] commands = new String[3];
                	
                	 commands[0] = "chmod 777 " + injectPath;
                	 commands[1] = "chmod 777 " + hookerPath;
//                	 commands[2] = "chmod 777 " + substratePath;
//                	 commands[3] = "chmod 777 " + TARGET_PATH;
                	 
                	 StringBuffer sb = new StringBuffer();
                	 sb.append("su -c");
                	 sb.append(" ").append(injectPath);//注入程序
                	 sb.append(" ").append("com.ry.target");//目标进程名称
                	 sb.append(" ").append(hookerPath);//注入代码so
                	 sb.append(" ").append("hook_entry");//注入代码入口函数
                	 sb.append(" ").append("hahaha");//注入代码入口函数参数
                	 
                	 commands[2] = sb.toString();
                	 System.out.println(commands[2]);
                	 ShellUtils.execCommand(commands, true);

				} catch (Exception e) {
					e.printStackTrace();
				}


            }
        }).start();

	}
}
