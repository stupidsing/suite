#ifndef memsource
#define memsource

#include "util.c"

int nAllocs;

void *memalloc_(int size) {
	void *p = malloc(size);
	if(p) return p;
	else err("out of memory");
}

void *memrealloc_(void *p, int size) {
	if(p = realloc(p, size)) return p;
	else err("out of memory");
}

void memrealloc(void *p, int size) {
	void **p1 = (void**) p;
	*p1 = memrealloc_(*p1, size);
}

void *memalloc(int size) {
	nAllocs++;
	return memalloc_(size);
}

void *memalloczeroed(int size) {
	void *p = memalloc(size);
	return memset(p, 0, size);
}

void *memallocstrdup(char *source) {
	int length = strlen(source);
	char *p = memalloc(length);
	return memcpy(p, source, length);
}

void memfree(void *p) {
	free(p);
	nAllocs--;
}

module(mem, {
	nAllocs = 0;
}, {
	!nAllocs || err("some memory not freed");
})

#endif
