#include <string.h>
#include <dlfcn.h>
 #include <sys/mman.h>
#include "process_util.h"
#include "ptrace_util.h"

#define FUNCTION_NAME_ADDR_OFFSET       0x100
#define FUNCTION_PARAM_ADDR_OFFSET      0x200

const char *libc_path = "/system/lib/libc.so";
const char *linker_path = "/system/bin/linker";

/**
 * 注入目标进程,执行注入代码
 * target_pid:目标进程ID
 * library_path：注入代码so路径
 * function_name:注入代码执行函数名
 * param：注入代码执行函数参数
 * param_size：注入代码执行函数参数大小
 *
 * */
int inject_remote_process(pid_t target_pid, const char *library_path,
		const char *function_name, const char *param, size_t param_size) {
	LOGD("start injecting process< %d > \n", target_pid);

	//1.attach上目标进程
	if(ptrace_attach(target_pid) < 0) {
		LOGD("attach error");
		return -1;
	}

	//2.获取目标进程寄存器，并复制一份保存，以便在注入完成后恢复目标进程
	struct pt_regs regs, original_regs;
	if (ptrace_getregs(target_pid, &regs) < 0) {
		LOGD("getregs error");
		return -1;
	}
	memcpy(&original_regs, &regs, sizeof(regs));

	//3.取目标进程mmap函数地址
	void *target_mmap_addr = get_remote_func_address(target_pid, libc_path, (void *) mmap);
	LOGD("target mmap address: %x\n", target_mmap_addr);

	//4.调用目标进程mmap函数分配一块内存
	long parameters[6];
	parameters[0] = 0;  // addr
	parameters[1] = 0x400; // size
	parameters[2] = PROT_READ | PROT_WRITE | PROT_EXEC;  // prot
	parameters[3] = MAP_ANONYMOUS | MAP_PRIVATE; // flags
	parameters[4] = 0; //fd
	parameters[5] = 0; //offset

	if (ptrace_call_wrapper(target_pid, "mmap", target_mmap_addr, parameters, 6, &regs) < 0) {
		LOGD("call target mmap error");
		return -1;
	}
	//得到mmap分配的内存地址
	uint8_t *target_mmap_base = ptrace_retval(&regs);
	LOGD("target_mmap_base: %x\n", target_mmap_base);

	//5.调用目标进程dlopen函数加载注入so

	// 函数原型：void *dlopen(const char *filename, int flag);

	//取目标进程dlopen函数地址
	void *target_dlopen_addr = get_remote_func_address(target_pid, linker_path, (void *) dlopen);
	LOGD("target dlopen address: %x\n", target_dlopen_addr);

	//把注入so地址写入目标进程
	ptrace_writedata(target_pid, target_mmap_base, library_path,strlen(library_path) + 1);

	//准备参数
	parameters[0] = target_mmap_base;
	parameters[1] = RTLD_NOW | RTLD_GLOBAL;
    //通过ptrace调用
	if (ptrace_call_wrapper(target_pid, "dlopen", target_dlopen_addr, parameters, 2,&regs) < 0){
		LOGD("call target dlopen error");
		return -1;
	}
	//取返回结果
	void * target_so_handle = ptrace_retval(&regs);

	//6.调用dlsym取注入so库执行函数的地址

	//函数原型：void *dlsym(void *handle, const char *symbol);

	//取目标进程dlsym函数的地址
	void *target_dlsym_addr = get_remote_func_address(target_pid, linker_path, (void *) dlsym);
	LOGD("target dlsym address: %x\n", target_dlsym_addr);
    //把函数名称字符串写进目标进程
	ptrace_writedata(target_pid, target_mmap_base + FUNCTION_NAME_ADDR_OFFSET,function_name, strlen(function_name) + 1);

	parameters[0] = target_so_handle;
	parameters[1] = target_mmap_base + FUNCTION_NAME_ADDR_OFFSET;

	if (ptrace_call_wrapper(target_pid, "dlsym", target_dlsym_addr, parameters, 2,&regs) < 0) {
		LOGD("call target dlsym error");
		return -1;
	}

	void * hook_func_addr = ptrace_retval(&regs);
	LOGD("target %s address: %x\n", function_name,target_dlsym_addr);
	//7.调用hook函数
	//写入函数需要的参数
	ptrace_writedata(target_pid, target_mmap_base + FUNCTION_PARAM_ADDR_OFFSET, param,strlen(param) + 1);
	parameters[0] = target_mmap_base + FUNCTION_PARAM_ADDR_OFFSET;

	if (ptrace_call_wrapper(target_pid, function_name, hook_func_addr,parameters, 1, &regs) < 0) {
		LOGD("call target %s error",function_name);
		return -1;
	}

	//8.调用dlclose卸载注入so
	//函数原型:int dlclose(void *handle);
	void *target_dlclose_addr = get_remote_func_address(target_pid, linker_path, (void *) dlclose);
	parameters[0] = target_so_handle;

	if (ptrace_call_wrapper(target_pid, "dlclose", target_dlclose_addr, parameters, 1,&regs) < -1) {
		LOGD("call target dlclose error");
		return -1;
	}
	//9.恢复现场
	ptrace_setregs(target_pid, &original_regs);
	//10.detach
	ptrace_detach(target_pid);

	return 0;
}


int main(int argc, char** argv) {

	pid_t target_pid;
	target_pid = find_pid_of(argv[1]);
	if (-1 == target_pid) {
		LOGD("Can't find the process\n");
		return -1;
	}

	inject_remote_process(target_pid, argv[2], argv[3], argv[4],strlen(argv[4]));

	return 0;
}
