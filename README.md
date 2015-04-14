#<center>Android进程注入</center>
##概述

我们平时所说的代码注入，主要静态和动态两种方式:<br>
静态注入，针对是可执行文件，比如修改ELF，DEX文件等，相关的辅助工具也很多，比如IDA、ApkTool等；<br>
动态注入，也可以叫进程注入,针对是进程，比如修改进程的寄存器、内存值等；<br>
动态跟静态最大的区别是，动态不需要改动源文件，但需要高权限（通常是root权限），而且所需的技术含量更高。
##基本思路
关键点在于让目标进加载自定义的动态库so，当so被加载后，so就可以加载其他模块、dex文件等，具体的注入过程大致如下：<br>
<br>
1)  attach上目标进程；<br>
2)  让目标进程的执行流程跳转到mmap函数来分配内存空间；<br>
3)  加载注入so；<br>
4)  最后让目标进程的执行流程跳转到注入的代码执行。<br>

后面会更详细地分析注入过程。

##示例演示
我们准备了2个程序：<br>
<img src="http://ww1.sinaimg.cn/mw690/7027af81gw1eqyaqcrgr3j20dw068t8y.jpg" width="200" width="220" />
<br>
Target作为目标程序，Inject注入程序。
###目标程序
<img src="http://ww2.sinaimg.cn/mw690/7027af81gw1eqyaqd1yp0j20io0c874t.jpg" width="200" />
<br>
在注入前数字每次增加1,点击启动inject启动注入程序。
####注入程序
<img src="http://ww2.sinaimg.cn/mw690/7027af81gw1eqyaqdeho6j20ig0aqq3b.jpg" width="200" />
<br>
点击inject浮窗按钮,开始执行注入，可以看到数字每次增加2。<br>
查看log日志:<br>
<img src="http://ww1.sinaimg.cn/mw690/7027af81gw1eqyavndby9j20qg02mgmd.jpg" width="400" />
<br>
<br>
##注入程序分析
###注入代码分析
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

这段代码就是我们想在目标进程里面执行的代码，代码很简单，做了2件事情:<br>
1.打印调用传参字符串;<br>
2.调用目标进程的set_step函数,让每次增加的数为2。
<br>
编译注入代码为动态库libhooker.so。

<br>

###注入过程详解

我们的inject代码必须运行在root进程,

	
	StringBuffer sb = new StringBuffer();
    sb.append("su -c");
    sb.append(" ").append(injectPath);//注入程序
    sb.append(" ").append("com.ry.target");//目标进程名称
    sb.append(" ").append(hookerPath);//注入代码so
    sb.append(" ").append("hook_entry");//注入代码入口函数
    sb.append(" ").append("hahaha");//注入代码入口函数参数
通过"su -c "启动一个root进程来执行。<br>
下面开始分析具体注入过程。



####一.attach到目标进程

	//1.attach
	if(ptrace_attach(target_pid) < 0) {
		LOGD("attach error");
		return -1;
	}
	
####二.获取目标进程寄存器，并复制一份保存，以便在注入完成后恢复目标进程

	struct pt_regs regs, original_regs;
	if (ptrace_getregs(target_pid, &regs) < 0) {
		LOGD("getregs error");
		return -1;
	}
	memcpy(&original_regs, &regs, sizeof(regs));
###三.取目标进程mmap函数地址

	void *target_mmap_addr = get_remote_func_address(target_pid, libc_path, (void *) mmap);
	LOGD("target mmap address: %x\n", target_mmap_addr);
get\_remote\_func\_address函数位于proccess_util.c中:<br>

	/**
    * 获取目标进程中函数地址
    * */
    void* get_remote_func_address(pid_t target_pid, const char* module_name,void* local_addr) {
	void* local_handle, *remote_handle;
	local_handle = get_lib_adress(-1, module_name);
	remote_handle = get_lib_adress(target_pid, module_name);
	
	/*目标进程函数地址= 目标进程lib库地址 + （本进程函数地址 -本进程lib库地址）*/
	void * ret_addr = (void *) ((uint32_t) remote_handle + (uint32_t) local_addr - (uint32_t) local_handle);
	return ret_addr;
    }	
为了在目标进程中调用mmap函数，需要得到mmap函数在目标进程中的地址。<br>
一个模块库里的函数地址等于模块库的装载地址加上一个偏移量，所以:<br>
目标进程函数地址= 目标进程lib库地址 + （本进程函数地址 -本进程lib库地址）<br>
mmap函数在/system/lib/libc.so库里面，所以(void*)mmap可以取得inject本身进程的mmap函数的地址，这样其实只要得到动态库的装载地址就能算出目标进程的mmap的地址。一种得到动态库装载地址的方法是分析Linux进程的/proc/pid/maps文件，这个文件包含了进程中所有mmap映射的地址。下面我们写一个获取动态库地址的函数，代码如下

	/**
    * 获取动态库装载地址
    * */
    void* get_lib_adress(pid_t pid, const char* module_name) {
	FILE *fp;
	long addr = 0;
	char *pch;
	char filename[32];
	char line[1024];

	if (pid < 0) {
		/* self process */
		snprintf(filename, sizeof(filename), "/proc/self/maps");
	} else {
		snprintf(filename, sizeof(filename), "/proc/%d/maps", pid);
	}

	fp = fopen(filename, "r");

	if (fp != NULL) {
		while (fgets(line, sizeof(line), fp)) {
			//在所有的映射行中寻找目标动态库所在的行
			if (strstr(line, module_name)) {
				pch = strtok(line, "-");
				addr = strtoul(pch, NULL, 16);

				if (addr == 0x8000)
					addr = 0;

				break;
			}
		}

		fclose(fp);
	}

	return (void *) addr;
    }

此函数的功能就是通过遍历/proc/pid/maps文件，来找到目的module_name的内存映射起始地址。
由于内存地址的表达方式是xxxxxxx-xxxxxxx的，所以会在后面使用strtok(line,"-")来分割字符串
如果pid = -1,表示获取本地进程的某个模块的地址，
否则就是pid进程的某个模块的地址。
	
####四.调用目标进程mmap函数分配一块内存

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
	
mmap函数原型：<br>

	void *mmap(void *addr, size_t length, int prot, int flags,
                  int fd, off_t offset);
                  
 准备好参数后，调用ptrace\_call\_wrapper函数:
 
 	  
 	int ptrace_call_wrapper(pid_t target_pid, const char * func_name, 	void * func_addr, long * parameters, int param_num, struct pt_regs * regs) {
    	LOGD("Calling [%s] in target process <%d> \n", func_name,target_pid);
    	if (ptrace_call(target_pid, (uint32_t)func_addr, parameters, param_num, regs) < 0) {
        return -1;
	}
	
    if (ptrace_getregs(target_pid, regs) < 0) {
        return -1;
	}
    return 0;
    }
总结一下ptrace\_call\_wrapper，它完成两个功能：<br>
一是调用ptrace\_call函数来执行指定函数；<br>
二是调用ptrace\_getregs函数获取所有寄存器的值。
<br>

下面来分析ptrace_call函数:

	int ptrace_call(pid_t pid, uint32_t addr, const long *params, uint32_t num_params, struct pt_regs* regs){
    uint32_t i;
    	//前面四个参数用寄存器传递
    	for (i = 0; i < num_params && i < 4; i ++) {
        regs->uregs[i] = params[i];
    	}

    	//后面参数放到栈里
    	if (i < num_params) {
        regs->ARM_sp -= (num_params - i) * sizeof(long) ;
        ptrace_writedata(pid, (void *)regs->ARM_sp, (uint8_t *)&params[i], (num_params - i) * sizeof(long));
    	}

    	//PC指向要执行的函数地址
    	regs->ARM_pc = addr;

    	if (regs->ARM_pc & 1) {
        	/* thumb */
        	regs->ARM_pc &= (~1u);
        	regs->ARM_cpsr |= CPSR_T_MASK;
    	} else {
        	/* arm */
        	regs->ARM_cpsr &= ~CPSR_T_MASK;
    	}
    	
	   //把返回地址设为0，这样目标进程执行完返回时会出现地址错误，这样目标进程将被挂起，控制权会回到调试进程手中
    	regs->ARM_lr = 0;

    	//设置目标进程的寄存器,让目标进程继续运行
    	if (ptrace_setregs(pid, regs) == -1 || ptrace_continue(pid) == -1) {
        	return -1;
    }
    	//等待目标进程结束
    	int stat = 0;
    	waitpid(pid, &stat, WUNTRACED);
    	while (stat != 0xb7f) {
        	if (ptrace_continue(pid) == -1) {
            	return -1;
        }
        waitpid(pid, &stat, WUNTRACED);
    }

    return 0;
	}
	

功能总结：<br>
1，将要执行的指令参数写入寄存器中，个数大于4的话，需要将剩余的指令通过ptrace\_writedata函数写入栈中；<br>
2,修改寄存器，PC指向要执行的函数，lr设置为0，调用ptrace\_setregs设置修改后的寄存器;<br>
3，使用ptrace\_continue函数运行目的进程;<br>
4，等待目标进程执行完毕;<br>

目标进程执行完成后，调用：

	uint8_t *target_mmap_base = ptrace_retval(&regs);
ptrace\_retval函数从寄存器里面取值，arm平台函数返回值是存放在ARM_r0的

	long ptrace_retval(struct pt_regs * regs) {
	return regs->ARM_r0;
    }

这一步完成后，我们得到了在目标进程开辟的一块内存地址。target\_mmap\_base指向这块地址的首地址。<br>

####五.调用目标进程dlopen函数加载注入so

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
	
dlopen函数原型:<br>

	void *dlopen(const char *filename, int flag);
	
这个函数的作用就是把一个so库加载到进程空间中。<br>

我们要调用这个函数，就和上一步调用mmap差不多，首先取得dlopen函数的地址，然后准备好参数，通过ptrace\_call\_wrapper调用执行。<br>

重点在准备参数这里，我们要先把so地址这个字符串写到目标进程里面去，写到哪里呢？就是上一步我们调用mmap得到的target\_mmap\_base。<br>


####六.调用dlsym取注入so库执行函数的地址

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
	
dlsym函数原型:<br>

    void *dlsym(void *handle, const char *symbol);	
这个函数可以得到so库中函数的地址。<br>
这一步和调用dlopen差不多，注意准备参数的时候要加上一段偏移量。<br>
####七.调用注入函数

	//写入函数需要的参数
	ptrace_writedata(target_pid, target_mmap_base + FUNCTION_PARAM_ADDR_OFFSET, param,strlen(param) + 1);
	parameters[0] = target_mmap_base + FUNCTION_PARAM_ADDR_OFFSET;

	if (ptrace_call_wrapper(target_pid, function_name, hook_func_addr,parameters, 1, &regs) < 0) {
		LOGD("call target %s error",function_name);
		return -1;
	}
	
一切准备就绪，这一步执行注入入口函数。<br>
####八.调用dlclose卸载注入so
	
	void *target_dlclose_addr = get_remote_func_address(target_pid, linker_path, (void *) dlclose);
	parameters[0] = target_so_handle;

	if (ptrace_call_wrapper(target_pid, "dlclose", target_dlclose_addr, parameters, 1,&regs) < -1) {
		LOGD("call target dlclose error");
		return -1;
	}
	
####九.恢复现场

	ptrace_setregs(target_pid, &original_regs);
####十.detach

	ptrace_detach(target_pid);


##参考资料
<http://blog.csdn.net/jinzhuojun/article/details/9900105>
<br>
<http://blog.csdn.net/u013234805/article/details/24796515>

























