// gcc -std=c99 -g src/main/c/gctest.c -o target/gctest

#define _GNU_SOURCE

#include <ctype.h>

#include "gc.c"

struct TestObject {
	int type;
	struct TestObject *left;
	struct TestObject *right;
};

int testRefOffsets[] = { offsetof(struct TestObject, left), offsetof(struct TestObject, right), 0 };

int *testRefOffsetsFunction(struct GcObject *object) { return testRefOffsets; }

struct GcClass testClass = {
	.size = sizeof(struct TestObject),
	.refoffsets = &testRefOffsetsFunction,
};

int main(int argc, char **argv) {
	gcinit();
	gcalloc(&testClass);
	gcdeinit();
}
