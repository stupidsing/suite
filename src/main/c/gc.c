#ifndef gcsource
#define gcsource

#include "util.c"

#define FRESH__ 0
#define QUEUED_ 1
#define SCANNED 2

struct GcObject;

struct Class {
	int *(*refoffsets) (struct GcObject*);
};

struct GcObject {
	int flag;
	struct GcObject *next;
	struct Class *class;
};

int gcosize = sizeof(struct GcObject);

int watermark;
struct GcObject *first;
struct GcObject *root;
struct GcObject *lastAllocated;

int norefoffsets[] = { 0 };

int compareaddresses(void *p0, void *p1) {
	return p0 != p1 ? (p0 < p1 ? -1 : 1) : 0;
}

int *getrefoffsets(struct GcObject *object) {
	struct Class *class = object->class;
	return class ? class->refoffsets(object) : norefoffsets;
}

struct GcObject *markAndSweep() {
	struct GcObject *gco = first;

	// mark all as fresh
	while(gco) {
		gco->flag = FRESH__;
		gco = gco->next;
	}

	// initialize heap and add root into it
	struct Heap heap;
	heapnew(&heap, &compareaddresses);

	if(root) heapadd(&heap, root);
	if(lastAllocated) heapadd(&heap, lastAllocated);

	// add child objects into heap
	while(gco = heapremove(&heap)) {
		gco->flag = QUEUED_;
		int *refoffsets = getrefoffsets(gco);
		while(*refoffsets) {
			struct GcObject *child = *(void**) (gcosize + *refoffsets + (void*) gco) - gcosize;
			if(child && child->flag == FRESH__) {
				child->flag = QUEUED_;
				heapadd(&heap, child);
			}
			refoffsets++;
		}
		gco->flag = SCANNED;
	}

	heapdelete(&heap);

	// evict orphan objects
	struct GcObject **prev = &first;
	while(gco) {
		struct GcObject *next = gco->next;
		if(gco->flag == FRESH__) {
			*prev = next;
			memfree(gco);
		} else prev = &gco->next;
		gco = next;
	}

	watermark = 32 + nAllocs * 3 / 2;
	nAllocs = 0;
	return first;
}

void *gcalloc(struct Class *class, int size) {
	if(nAllocs++ > watermark) // Pre-cautionary garbage collection
		markAndSweep();

	int n = 0;
	int size1 = gcosize + size;
	lastAllocated = 0;
	while(n++ < 3)
		if(!(lastAllocated = memalloc(size1))) markAndSweep(); // Space-hunger garbage collection
		else break;

	struct GcObject *gco = lastAllocated;
	gco->class = class;
	gco->flag = SCANNED;
	gco->next = first;

	return gcosize + (void*) (first = gco);
}

void *gcallocleaf(int size) {
	return gcalloc(0, size);
}

void gcsetroot(struct GcObject *r) {
	root = r;
}

void gcinit() {
	watermark = 256;
	first = 0;
	lastAllocated = 0;
}

void gcdeinit() {
	lastAllocated = 0;
	root = 0;
	markAndSweep();
	!first || err("some memory not garbage collected");
}

#endif
