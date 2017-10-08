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

#include "mem.c"

char *substr(char *start, char *end) {
	char *result = memalloc(end - start + 1), *s = start, *d = result;
	while(s < end) *d++ = *s++;
	*d = 0;
	return result;
}

char *dupstr(char *str) {
	return substr(str, str + strlen(str));
}

#endif
