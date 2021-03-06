#ifndef utilsource
#define utilsource

#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define min(a, b) ((a) < (b) ? (a) : (b))
#define max(a, b) ((a) < (b) ? (b) : (a))

int msg(char *t, char *fn, int line, char *m) {
	fprintf(stderr, "[%s] %s in %s:%d\n", t, m, fn, line);
	return 1;
}

int fmsg(char *t, char *fn, int line, char *m) {
	msg(t, fn, line, m);
	exit(1);
}

#define info(m) msg("INFO", __FILE__, __LINE__, (m))
#define err(m) msg("ERROR", __FILE__, __LINE__, (m))
#define fatal(m) fmsg("FATAL", __FILE__, __LINE__, (m))
#define test(t) (t) || err("test case failed");

#define module(m, c, d) int m##count = 0; \
int m##initonce() { c } \
int m##deinitonce() { d } \
void m##init() { m##count++ || m##initonce(); } \
void m##deinit() { --m##count || m##deinitonce(); }

typedef struct Closure Closure;

struct Closure {
	void *frame;
	void *(*apply) (void*);
};

void *invoke(Closure closure) { (*closure.apply)(closure.frame); }

#endif
