// gcc -std=c99 -g src/main/c/jitdemo.c -o target/jitdemo && target/jitdemo

#define _ISOC11_SOURCE
#define _POSIX_C_SOURCE 199309L

#include <stdlib.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/mman.h>

#include "util.c"

void *ptr;

void ouch(int sig, siginfo_t *info, void *dummy) {
	int pageSize = sysconf(_SC_PAGE_SIZE);
	printf("OUCH! - I got signal %d on %x\n", sig, info->si_addr);
	mprotect(ptr, pageSize, PROT_READ | PROT_WRITE) && fatal("mprotect()");
}

int main() {
	int pageSize = sysconf(_SC_PAGE_SIZE);
	void *code = aligned_alloc(pageSize, pageSize);
	((char*) code)[0] = 0xC3; // near RET
	mprotect(code, pageSize, PROT_READ | PROT_EXEC) && fatal("mprotect()");
	((void(*)()) code)();
	free(code);

	struct sigaction act;
	sigemptyset(&act.sa_mask);
	act.sa_sigaction = ouch;
	act.sa_flags = SA_SIGINFO;

	sigaction(SIGSEGV, &act, 0) && fatal("sigaction()");

	ptr = aligned_alloc(pageSize, pageSize);
	mprotect(ptr, pageSize, PROT_READ) && fatal("mprotect()");

	printf("Hello World!\n");
	sleep(1);
	*((char*) ptr) = 0;
	printf("Another Hello World\n");

	free(ptr);
}
