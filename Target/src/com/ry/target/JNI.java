package com.ry.target;

/**
 * @Title: JNI.java
 * @Description:
 * @author yangbing3@ucweb.com
 * @date 2014-9-26 2:29:06
 */
public class JNI {
    static {
        System.loadLibrary("target");
    }

    public native void init();
    
    public native int getNumber(int number);
        
    public native void destroy();


}
