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

TestObject *newTest(int type, TestObject *left, TestObject *right) {
	TestObject *test = gcalloc(&testClass);
	test->type = type;
	test->left = left;
	test->right = right;
}

int main(int argc, char **argv) {
	gcinit();
	TestObject *t0 = newTest(0, 0, 0);
	TestObject *t1 = newTest(0, 0, 0);
	TestObject *t2 = newTest(0, t0, t1);
	TestObject *t3 = newTest(0, 0, t2);
	gcdeinit();
}
