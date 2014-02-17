#include "util.c"

#define FRESH__ 0
#define QUEUED_ 1
#define SCANNED 2

struct Class {
	int *(*refoffsets) ();
};

struct Object {
	struct Class *class;
	int flag;
	struct Object *next;
};

int compareaddresses(void *p0, void *p1) {
	return p0 != p1 ? (p0 < p1 ? -1 : 1) : 0;
}

struct Object *markAndSweep(struct Object *first, struct Object *root) {
	struct Object *object;

	object = first;
	while(object) {
		object->flag = FRESH__;
		object = object->next;
	}

	struct Heap heap;
	heapcreate(&heap, &compareaddresses);

	object->flag = QUEUED_;
	heapadd(&heap, object);

	while(object = (struct Object*) heapremove(&heap)) {
		int *refoffsets = object->class->refoffsets();
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

	struct Object **prev = &first;
	while(object) {
		if(object->flag == FRESH__) (*prev) = object->next;
		else prev = &object->next;
		object = object->next;
	}
	return first;
}

int main() {
}
