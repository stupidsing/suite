#ifndef listsource
#define listsource

#include "mem.c"

typedef struct List List;

struct List {
	int capacity;
	int size;
	void **items;
};

#define listfor(list, item, apply) \
	for(int i##__LINE__ = 0; i##__LINE__ < list.size; i##__LINE__++) { void *item = list.items[i##__LINE__]; apply }

void listnew(List *list) {
	int capacity = 4;
	list->capacity = capacity;
	list->size = 0;
	list->items = memalloc(capacity * sizeof(void*));
}

void listdelete(List *list) {
	memfree(list->items);
}

void listadd(List *list, void *item) {
	int loc = list->size++;
	if(list->capacity <= loc) memrealloc(&list->items, (list->capacity <<= 1) * sizeof(void*));
	list->items[loc] = item;
}

#define listmap(list, item0, item1) { \
	for(int i##__LINE__ = 0; i##__LINE__ < list.size; i##__LINE__++) { \
		void **p = list.items + [i##__LINE__]; void *item0 = *p; *p = item1
	} \
}

#define listfilter(list, item, pred) { \
	for(int si##__LINE__ = 0, di##__LINE__ = 0; si##__LINE__ < list.size; si##__LINE__++) { \
		void *item = list.items[si##__LINE__]; int b = (pred); if(b) list.items[di##__LINE__++] = item; \
	} \
	list.size = di##__LINE__; \
}

#endif
