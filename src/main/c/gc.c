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
	struct Class *class;
	int flag;
	struct Object *next;
};

int watermark;
struct Object *first;
struct Object *root;
struct Object *lastAllocated;

int compareaddresses(void *p0, void *p1) {
	return p0 != p1 ? (p0 < p1 ? -1 : 1) : 0;
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
	while(object = (struct Object*) heapremove(&heap)) {
		object->flag = QUEUED_;
		int *refoffsets = object->class->refoffsets(object);
		for (int i = 0; refoffsets[i]; i++) {
			struct Object *child = (struct Object*) (refoffsets[i] + (char*) object);
			if(child && child->flag == FRESH__) {
				child->flag = QUEUED_;
				heapadd(&heap, child);
			}
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
	return first;
}

void *gcalloc(struct Class *class, int size) {
	if(nAllocs > watermark) {
		markAndSweep();
		watermark = nAllocs * 3 / 2;
	}

	struct Object *object = lastAllocated = memalloc(size);
	object->class = class;
	object->flag = SCANNED;
	object->next = first;

	first = object;
	return object;
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
