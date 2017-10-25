// gcc -std=c99 -g src/main/c/gctest.c -o target/gctest && target/gctest

#include "cons.c"

int main(int argc, char **argv) {
	gcinit();
	Cons *t0 = newCons(0, 0, 0);
	Cons *t1 = newCons(0, 0, 0);
	Cons *t2 = newCons(0, t0, t1);
	Cons *t3 = newCons(0, 0, t2);
	gcdeinit();
}
