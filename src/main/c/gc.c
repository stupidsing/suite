#ifndef gcsource
#define gcsource

#include "util.c"

#define FRESH__ 0
#define QUEUED_ 1
#define SCANNED 2

struct Object;

struct Class {
	int *(*refoffsets) (struct Object*);
};

struct Object {
	int flag;
	struct Object *next;
	struct Class *class;
};

int watermark;
struct Object *first;
struct Object *root;
struct Object *lastAllocated;

int compareaddresses(void *p0, void *p1) {
	return p0 != p1 ? (p0 < p1 ? -1 : 1) : 0;
}

int *getrefoffsets(struct Object *object) {
	return object->class->refoffsets(object);
}

struct Object *markAndSweep() {
	struct Object *object = first;

	// mark all as fresh
	while(object) {
		object->flag = FRESH__;
		object = object->next;
	}

	// initialize heap and add root into it
	struct Heap heap;
	heapnew(&heap, &compareaddresses);

	if(root) heapadd(&heap, root);
	if(lastAllocated) heapadd(&heap, lastAllocated);

	// add child objects into heap
	while(object = heapremove(&heap)) {
		object->flag = QUEUED_;
		int *refoffsets = getrefoffsets(object);
		while(*refoffsets) {
			struct Object *child = *refoffsets + (void*) object;
			if(child && child->flag == FRESH__) {
				child->flag = QUEUED_;
				heapadd(&heap, child);
			}
			refoffsets++;
		}
		object->flag = SCANNED;
	}

	heapdelete(&heap);

	// evict orphan objects
	struct Object **prev = &first;
	while(object) {
		struct Object *next = object->next;
		if(object->flag == FRESH__) {
			(*prev) = next;
			memfree(object);
		} else prev = &object->next;
		object = next;
	}

	watermark = 32 + nAllocs * 3 / 2;
	nAllocs = 0;
	return first;
}

void *gcalloc(struct Class *class, int size) {
	if(nAllocs++ > watermark) // Pre-cautionary garbage collection
		markAndSweep();

	int n = 0;
	lastAllocated = 0;
	while(n++ < 3)
		if(!(lastAllocated = memalloc(size))) markAndSweep(); // Space-hunger garbage collection
		else break;

	struct Object *object = lastAllocated;
	object->class = class;
	object->flag = SCANNED;
	object->next = first;

	return first = object;
}

void gcsetroot(struct Object *r) {
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
