#ifndef conssource
#define conssource

#include <ctype.h>

#include "gc.c"

typedef struct Cons Cons;

struct Cons {
	int type;
	Cons *left;
	Cons *right;
};

int consRefOffsets[] = { offsetof(Cons, left), offsetof(Cons, right), 0 };

int *consRefOffsetsFunction(GcObject *object) { return consRefOffsets; }

GcClass consClass = {
	.name = "Cons",
	.size = sizeof(Cons),
	.refoffsets = &consRefOffsetsFunction,
};

Cons *newCons(int type, Cons *left, Cons *right) {
	Cons *cons = gcalloc(&consClass);
	cons->type = type;
	cons->left = left;
	cons->right = right;
}

#endif
