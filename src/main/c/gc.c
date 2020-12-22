#ifndef gcsource
#define gcsource

#include "heap.c"
#include "mem.c"

#define FRESH__ 0
#define QUEUED_ 1
#define SCANNED 2
#define UNUSED_ 3

// #define offsetof(type, member) __builtin_offsetof(type, member)

typedef struct GcClass GcClass;
typedef struct GcObject GcObject;

struct GcClass {
	char *name;
	int size;
	int *(*refoffsets) (GcObject*);
};

struct GcObject {
	int flag;
	GcObject *next;
	GcClass *class;
};

struct {
	int gcosize;
	int watermark;
	int currentmark;

	GcObject *first;
	GcObject *root;
	GcObject *lastAllocated;
} gc_;

int norefoffsets[] = { 0 };

GcObject *toGcObject(void *p) { return (GcObject*) (p - gc_.gcosize); }
void *toObject(GcObject *gco) { return gc_.gcosize + (void*) gco; }

int compareaddresses(void *p0, void *p1) {
	return p0 != p1 ? (p0 < p1 ? -1 : 1) : 0;
}

int *getrefoffsets(GcObject *gco) {
	GcClass *gcc = gco->class;
	return gcc ? gcc->refoffsets(gco) : norefoffsets;
}

GcObject *markAndSweep() {
	GcObject *gco = gc_.first;

	// mark all as fresh
	for(gco = gc_.first; gco; gco = gco->next) gco->flag = FRESH__;

	// initialize heap and add root into it
	Heap heap;
	heapnew(&heap, &compareaddresses);

	if(gc_.root) heapadd(&heap, gc_.root);
	if(gc_.lastAllocated) heapadd(&heap, gc_.lastAllocated);

	// add child objects into heap
	while(gco = heapremove(&heap)) {
		void *object = toObject(gco);
		void *ref;
		GcObject *gco1;

		gco->flag = QUEUED_;

		for(int *refoffsets = getrefoffsets(gco); *refoffsets; refoffsets++)
			if((ref = *(void**) (object + *refoffsets++)) && (gco1 = toGcObject(ref))->flag == FRESH__) {
				gco1->flag = QUEUED_;
				heapadd(&heap, gco1);
			}

		gco->flag = SCANNED;
	}

	heapdelete(&heap);

	// mark fresh as unused
	int n = 0;

	for(gco = gc_.first; gco; gco = gco->next)
		if(gco->flag != FRESH__) n++;
		else gco->flag = UNUSED_;

	// evict unused objects
	GcObject **current = &gc_.first;

	while(gco = *current) {
		GcObject **next = &gco->next;
		if(gco->flag == UNUSED_) {
			*current = *next;
			memfree(gco);
		}
		else current = next;
	}

	gc_.watermark = 32 + n * 3 / 2;
	gc_.currentmark = n;
	return gc_.first;
}

void *gcalloc_(GcClass *gcc, int size) {
	if(gc_.watermark < gc_.currentmark++) // pre-cautionary garbage collection
		markAndSweep();

	int n = 0;
	int size1 = gc_.gcosize + size;
	gc_.lastAllocated = 0;
	while(n++ < 3)
		if(!(gc_.lastAllocated = memalloc(size1))) markAndSweep(); // hungry garbage collection
		else break;

	GcObject *gco = gc_.lastAllocated;
	gco->class = gcc;
	gco->flag = SCANNED;
	gco->next = gc_.first;

	void *p = toObject(gco);
	int *refoffsets = gcc->refoffsets(gco);
	while(*refoffsets) *(void**) (p + *refoffsets++) = 0;

	gc_.first = gco;
	return p;
}

void *gcalloc(GcClass *gcc) {
	return gcalloc_(gcc, gcc->size);
}

void *gcallocleaf(int size) {
	return gcalloc_(0, size);
}

void gcsetroot(GcObject *r) {
	gc_.root = r;
}

module(gc, {
	meminit();
	gc_.gcosize = sizeof(GcObject);
	gc_.watermark = 256;
	gc_.currentmark = 0;
	gc_.first = 0;
	gc_.lastAllocated = 0;
}, {
	gc_.lastAllocated = 0;
	gc_.root = 0;
	markAndSweep();
	gc!_.first || fatal("some memory not garbage collected");
	memdeinit();
})

#endif
