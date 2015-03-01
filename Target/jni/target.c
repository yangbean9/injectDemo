#include <android/log.h>
#include <stdlib.h>
#include <jni.h>



#define  LOG_TAG "TARGET"
#define  LOGD(fmt, args...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG, fmt, ##args)

int step = 1;

void set_step(int new_step) {
	step = new_step;
}

jint Java_com_ry_target_JNI_getNumber(JNIEnv* env, jobject thiz,jint number) {
	int res = number + step;
	LOGD("target >>> step = %d,res = %d",step,res);
	return number + step;
}

void Java_com_ry_target_JNI_init(JNIEnv* env, jobject thiz) {
	LOGD("target >>> init");
}

void Java_com_ry_target_JNI_destroy(JNIEnv* env, jobject thiz) {
	LOGD("target >>> destroy");
	step = 1;

}
