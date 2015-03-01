#include <android/log.h>

#ifndef __LOG_UTIL_H__
#define __LOG_UTIL_H__

#define LOG_TAG "INJECT"
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)

#endif
