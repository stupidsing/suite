#ifndef utilsource
#define utilsource

#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define min(a, b) ((a) < (b) ? (a) : (b))
#define max(a, b) ((a) < (b) ? (b) : (a))

#define info(m) msg("INFO", __LINE__, (m))
#define err(m) msg("ERROR", __LINE__, (m))
#define fatal(m) fmsg("FATAL", __LINE__, (m));

int msg(char *t, int line, char *m) {
	fprintf(stderr, "[%s] %s in line %d\n", t, m, line);
	return 1;
}

int fmsg(char *t, int line, char *m) {
	msg(t, line, m);
	exit(1);
}

int nAllocs;

void meminit() {
	nAllocs = 0;
}

void memdeinit() {
	!nAllocs || err("some memory not freed");
}

void *memalloc(int size) {
	nAllocs++;
	return malloc(size);
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

struct Hashtab {
	int size;
	void **keys, **values;
};

int hashptr(void *node) {
	int hash = (intptr_t) node;
	hash += hash >> 3; // randomize least significant bits
	return hash;
}

int hashstr(char *start, char *end) {
	char *s = start;
	int hash = 1;
	while(s < end) hash = 31 * hash + 5 + *s++;
	return hash;
}

void htnew(struct Hashtab *hashtab, int size) {
	hashtab->size = size;
	hashtab->keys = memalloczeroed(hashtab->size * sizeof(void*));
	hashtab->values = memalloczeroed(hashtab->size * sizeof(void*));
}

void htdelete(struct Hashtab hashtab) {
	memfree(hashtab.values);
	memfree(hashtab.keys);
}

int htgetpos(struct Hashtab *hashtab, void *key) {
	void **keys = hashtab->keys, *k;
	int size = hashtab->size, i = hashptr(key);
	while((k = keys[i %= size]) && k != key) i++;
	return i;
}

void *htget(struct Hashtab *hashtab, void *key) {
	return hashtab->values[htgetpos(hashtab, key)];
}

void htput(struct Hashtab *hashtab, void *key, void *value) {
	int i = htgetpos(hashtab, key);
	hashtab->keys[i] = key;
	hashtab->values[i] = value;
}

#define heapMax (65536)

struct Heap {
	int size;
	void *items[heapMax];
	int (*comparer) (void*, void*);
};

void heapnew(struct Heap *heap, int (*comparer) (void*, void*)) {
	heap->size = 0;
	heap->comparer = comparer;
}

void heapdelete(struct Heap *heap) {
}

void heapadd(struct Heap *heap, void *item) {
	int loc = heap->size++;
	int parentloc;
	while(0 < loc && heap->comparer(heap->items[parentloc = loc / 2], item) < 0) {
		heap->items[loc] = heap->items[parentloc];
		loc = parentloc;
	}
	heap->items[loc] = item;
}

void *heapremove(struct Heap *heap) {
	if(0 < heap->size) {
		void *taken = heap->items[0];
		heap->items[0] = heap->items[--heap->size];
		int loc = 0, loc0, loc1;
		while(1) {
			void *item0 = heap->items[loc0 = 2 * loc + 1];
			void *item1 = heap->items[loc1 = loc0 + 1];
			if(!item0 && !item1) break;

			if(heap->comparer(item0, item1) < 0) {
				heap->items[loc] = item0;
				loc = loc0;
			} else {
				heap->items[loc] = item1;
				loc = loc1;
			}
		}
		return taken;
	} else return 0;
}

#endif
