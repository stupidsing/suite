#ifndef gcsource
#define gcsource

#include "heap.c"
#include "util.c"

#define FRESH__ 0
#define QUEUED_ 1
#define SCANNED 2

#define offsetof(type, member) __builtin_offsetof(type, member)

typedef struct GcClass GcClass;
typedef struct GcObject GcObject;

struct GcClass {
	int size;
	int *(*refoffsets) (GcObject*);
};

struct GcObject {
	int flag;
	GcObject *next;
	GcClass *class;
};

int gcosize = sizeof(GcObject);

int watermark;
int currentmark;

GcObject *first;
GcObject *root;
GcObject *lastAllocated;

int norefoffsets[] = { 0 };

int compareaddresses(void *p0, void *p1) {
	return p0 != p1 ? (p0 < p1 ? -1 : 1) : 0;
}

int *getrefoffsets(GcObject *gco) {
	GcClass *gcc = gco->class;
	return gcc ? gcc->refoffsets(gco) : norefoffsets;
}

GcObject *markAndSweep() {
	GcObject *gco = first;

	// mark all as fresh
	while(gco) {
		gco->flag = FRESH__;
		gco = gco->next;
	}

	// initialize heap and add root into it
	Heap heap;
	heapnew(&heap, &compareaddresses);

	if(root) heapadd(&heap, root);
	if(lastAllocated) heapadd(&heap, lastAllocated);

	// add child objects into heap
	while(gco = heapremove(&heap)) {
		gco->flag = QUEUED_;
		int *refoffsets = getrefoffsets(gco);
		while(*refoffsets) {
			void *ref = *(void**) (gcosize + *refoffsets + (void*) gco);
			GcObject *child;
			if(ref && (child = ref - gcosize)->flag == FRESH__) {
				child->flag = QUEUED_;
				heapadd(&heap, child);
			}
			refoffsets++;
		}
		gco->flag = SCANNED;
	}

	heapdelete(&heap);

	// evict orphan objects
	GcObject **current = &first;
	currentmark = 0;

	while(gco = *current) {
		GcObject **next = &gco->next;
		if(gco->flag == FRESH__) {
			*current = *next;
			memfree(-gcosize + (void*) gco);
		} else {
			current = next;
			currentmark++;
		}
	}

	watermark = 32 + currentmark * 3 / 2;
	currentmark = 0;
	return first;
}

void *gcalloc_(GcClass *gcc, int size) {
	if(watermark < currentmark++) // pre-cautionary garbage collection
		markAndSweep();

	int n = 0;
	int size1 = gcosize + size;
	lastAllocated = 0;
	while(n++ < 3)
		if(!(lastAllocated = memalloc(size1))) markAndSweep(); // hungry garbage collection
		else break;

	GcObject *gco = lastAllocated;
	void *p = gcosize + (void*) gco;
	int *refoffsets = gcc->refoffsets(gco);
	while(*refoffsets) *(void**) (p + *refoffsets++) = 0;

	gco->class = gcc;
	gco->flag = SCANNED;
	gco->next = first;

	return first = p;
}

void *gcalloc(GcClass *gcc) {
	return gcalloc_(gcc, gcc->size);
}

void *gcallocleaf(int size) {
	return gcalloc_(0, size);
}

void gcsetroot(GcObject *r) {
	root = r;
}

void gcinit() {
	meminit();
	watermark = 256;
	currentmark = 0;
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
