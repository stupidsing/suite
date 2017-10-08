#ifndef heapsource
#define heapsource

#include "util.c"

#define heapMax (65536)

typedef struct Heap Heap;

struct Heap {
	int size;
	void *items[heapMax];
	int (*comparer) (void*, void*);
};

void heapnew(Heap *heap, int (*comparer) (void*, void*)) {
	heap->size = 0;
	heap->comparer = comparer;
}

void heapdelete(Heap *heap) {
}

void heapadd(Heap *heap, void *item) {
	int loc = heap->size++;
	int parentloc;
	while(0 < loc && heap->comparer(heap->items[parentloc = loc / 2], item) < 0) {
		heap->items[loc] = heap->items[parentloc];
		loc = parentloc;
	}
	heap->items[loc] = item;
}

void *heapremove(Heap *heap) {
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
