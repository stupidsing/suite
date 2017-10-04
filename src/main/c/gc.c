#ifndef gcsource
#define gcsource

#include "heap.c"
#include "util.c"

#define FRESH__ 0
#define QUEUED_ 1
#define SCANNED 2

#define offsetof(type, member) __builtin_offsetof(type, member)

struct GcObject;

struct GcClass {
	int size;
	int *(*refoffsets) (struct GcObject*);
};

struct GcObject {
	int flag;
	struct GcObject *next;
	struct GcClass *class;
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

int *getrefoffsets(struct GcObject *gco) {
	struct GcClass *gcc = gco->class;
	return gcc ? gcc->refoffsets(gco) : norefoffsets;
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

void *gcalloc_(struct GcClass *gcc, int size) {
	if(watermark < nAllocs++) // pre-cautionary garbage collection
		markAndSweep();

	int n = 0;
	int size1 = gcosize + size;
	lastAllocated = 0;
	while(n++ < 3)
		if(!(lastAllocated = memalloc(size1))) markAndSweep(); // hungry garbage collection
		else break;

	struct GcObject *gco = lastAllocated;
	void *p = gcosize + (void*) gco;
	int *refoffsets = gcc->refoffsets(gco);
	while(*refoffsets) *(void**) (p + *refoffsets++) = 0;

	gco->class = gcc;
	gco->flag = SCANNED;
	gco->next = first;

	return first = p;
}

void *gcalloc(struct GcClass *gcc) {
	return gcalloc_(gcc, gcc->size);
}

void *gcallocleaf(int size) {
	return gcalloc_(0, size);
}

void gcsetroot(struct GcObject *r) {
	root = r;
}

void gcinit() {
	meminit();
	watermark = 256;
	first = 0;
	lastAllocated = 0;
}

void gcdeinit() {
	lastAllocated = 0;
	root = 0;
	markAndSweep();
	!first || err("some memory not garbage collected");
	memdeinit();
}

#endif
