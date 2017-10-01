#ifndef utilsource
#define utilsource

#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define min(a, b) ((a) < (b) ? (a) : (b))
#define max(a, b) ((a) < (b) ? (b) : (a))

int msg(char *t, int line, char *m) {
	fprintf(stderr, "[%s] %s in line %d\n", t, m, line);
	return 1;
}

int fmsg(char *t, int line, char *m) {
	msg(t, line, m);
	exit(1);
}

#define info(m) msg("INFO", __LINE__, (m))
#define err(m) msg("ERROR", __LINE__, (m))
#define fatal(m) fmsg("FATAL", __LINE__, (m));

int nAllocs;

void meminit() {
	nAllocs = 0;
}

void memdeinit() {
	!nAllocs || err("some memory not freed");
}

void *memalloc_(int size) {
	void *p = malloc(size);
	if(p) return p;
	else err("out of memory");
}

void *memrealloc_(void *p, int size) {
	if(p = realloc(p, size)) return p;
	else err("out of memory");
}

void *memalloc(int size) {
	nAllocs++;
	return memalloc_(size);
}

void *memalloczeroed(int size) {
	void *p = memalloc(size);
	memset(p, 0, size);
	return p;
}

void memfree(void *p) {
	free(p);
	nAllocs--;
}

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
