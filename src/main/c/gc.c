#define FRESH__ 0
#define QUEUED_ 1
#define SCANNED 2

#define gcMaxPointers (65536)

struct Heap {
	int size;
	void *items[gcMaxPointers];
	int (*comparer) (void*, void*);
};

void heapcreate(struct Heap *heap, int (*comparer) (void*, void*)) {
	heap->size = 0;
	heap->comparer = comparer;
}

void heapdelete(struct Heap *heap) {
}

void heapadd(struct Heap *heap, void *item) {
	int loc = heap->size++;
	int uploc;
	while (loc > 0 && heap->comparer(heap->items[uploc = loc / 2], item) < 0) {
		heap->items[loc] = heap->items[uploc];
		loc = uploc;
	}
	heap->items[loc] = item;
}

void *heapremove(struct Heap *heap) {
	if (heap->size > 0) {
		void *taken = heap->items[0];
		heap->items[0] = heap->items[--heap->size];
		int loc = 0, loc0, loc1;
		while (1) {
			void *item0 = heap->items[loc0 = 2 * loc];
			void *item1 = heap->items[loc1 = loc0 + 1];
			if (!item0 && !item1) break;

			if (heap->comparer(item0, item1) < 0) {
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
	while (object) {
		object->flag = FRESH__;
		object = object->next;
	}

	struct Heap heap;
	heapcreate(&heap, &compareaddresses);

	object->flag = QUEUED_;
	heapadd(&heap, object);

	while (object = (struct Object*) heapremove(&heap)) {
		int *refoffsets = object->class->refoffsets();
		for (int i = 0; refoffsets[i]; i++) {
			struct Object *child = (struct Object*) (refoffsets[i] + (char*) object);
			if (child && child->flag == FRESH__) {
				child->flag = QUEUED_;
				heapadd(&heap, child);
			}
		}
		object->flag = SCANNED;
	}

	heapdelete(&heap);

	struct Object **prev = &first;
	while (object) {
		if (object->flag == FRESH__) (*prev) = object->next;
		else prev = &object->next;
		object = object->next;
	}
	return first;
}

int main() {
}
