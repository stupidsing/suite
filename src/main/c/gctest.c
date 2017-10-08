// gcc -std=c99 -g src/main/c/gctest.c -o target/gctest

#define _GNU_SOURCE

#include <ctype.h>

#include "gc.c"

typedef struct TestObject TestObject;

struct TestObject {
	int type;
	TestObject *left;
	TestObject *right;
};

int testRefOffsets[] = { offsetof(TestObject, left), offsetof(TestObject, right), 0 };

int *testRefOffsetsFunction(GcObject *object) { return testRefOffsets; }

GcClass testClass = {
	.size = sizeof(TestObject),
	.refoffsets = &testRefOffsetsFunction,
};

int main(int argc, char **argv) {
	gcinit();
	gcalloc(&testClass);
	gcdeinit();
}
