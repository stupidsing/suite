#ifndef memsource
#define memsource

#include "util.c"

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

void memrealloc(void *p, int size) {
	void **p1 = (void**) p;
	if(*p1 = realloc(*p1, size)) return;
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

#endif
