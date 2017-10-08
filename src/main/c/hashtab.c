#ifndef hashtabsource
#define hashtabsource

#include "mem.c"

typedef struct Hashtab Hashtab;

struct Hashtab {
	int size;
	void **keys, **values;
};

int hashptr(void *node) {
	int hash = (intptr_t) node;
	hash += hash >> 3; // randomize least significant bits
	return hash;
}

int hashstr(char *start, char *end) {
	char *s = start;
	int hash = 1;
	while(s < end) hash = 31 * hash + 5 + *s++;
	return hash;
}

void htnew(Hashtab *hashtab, int size) {
	hashtab->size = size;
	hashtab->keys = memalloczeroed(hashtab->size * sizeof(void*));
	hashtab->values = memalloczeroed(hashtab->size * sizeof(void*));
}

void htdelete(Hashtab hashtab) {
	memfree(hashtab.values);
	memfree(hashtab.keys);
}

int htgetpos(Hashtab *hashtab, void *key) {
	void **keys = hashtab->keys, *k;
	int size = hashtab->size, i = hashptr(key);
	while((k = keys[i %= size]) && k != key) i++;
	return i;
}

void *htget(Hashtab *hashtab, void *key) {
	return hashtab->values[htgetpos(hashtab, key)];
}

void htput(Hashtab *hashtab, void *key, void *value) {
	int i = htgetpos(hashtab, key);
	hashtab->keys[i] = key;
	hashtab->values[i] = value;
}

#endif
