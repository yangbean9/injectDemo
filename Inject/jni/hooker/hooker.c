#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>
#include <sys/mman.h>
#include <elf.h>
#include <fcntl.h>
#include <dlfcn.h>

#define LOG_TAG "HOOK"
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)

const char *target_path = "/data/data/com.ry.target/lib/libtarget.so";

void (*_set_step)(int);

int hook_entry(char * a) {
	LOGD("Hook success, pid = %d\n", getpid());
	LOGD("Hello %s\n", a);

	void *handle = dlopen(target_path, 2);
	if (handle == NULL) {
		LOGD("open target so error!\n");
		return -1;
	}

	void *symbol = dlsym(handle, "set_step");
	if (symbol == NULL) {
		LOGD("get set_step error!\n");
		return -1;
	}
	_set_step = symbol;
	LOGD("_set_step addr :%x\n", _set_step);
	_set_step(2);

	return 0;
}

