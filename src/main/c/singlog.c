// gcc -std=c99 -g src/main/c/singlog.c -o target/singlog && echo | target/singlog

#include "logic.c"

int testmain() {
	Node *n;
	singloginit();

	n = parse("1 + 2 * 3");
	char *s = dump(n);
	test(strcmp(s, "(1) + ((2) * (3))") == 0);
	memfree(s);
	unref(n);

	test(prove(parse("okay"))); // prove() will automatically ref()/unref()
	test(!prove(parse("fail")));
	test(prove(parse("okay, okay")));
	test(!prove(parse("okay, fail")));
	test(prove(parse("fail; okay")));
	test(!prove(parse("fail; fail")));
	test(prove(parse("a = a")));
	test(!prove(parse("a = b")));
	test(prove(parse("a b = a b")));

	test(prove(generalize(n = ref(parse("a = a"))))); unref(n);
	test(!prove(generalize(n = ref(parse("a = b"))))); unref(n);
	test(prove(generalize(n = ref(parse(".a = A, .a = A"))))); unref(n);
	test(!prove(generalize(n = ref(parse(".a = A, .a = B"))))); unref(n);
	test(prove(generalize(n = ref(parse(".a = A; .a = B"))))); unref(n);

	test(prove(parse("let 7 (1 + 2 * 3)")));
	test(prove(parse("let 2 (9 - 4 - 3)")));
	test(prove(generalize(n = ref(parse("list.str .l \"ab\", .l = (a, b,)"))))); unref(n);
	test(prove(generalize(n = ref(parse("list.str (a, b,) .s, .s = \"ab\""))))); unref(n);
	test(prove(parse("list.str (a, b, c,) \"abc\"")));
	test(prove(parse("ord 2 3")));
	test(!prove(parse("ord 3 3")));
	test(!prove(parse("ord 3 2")));

	assert(parse("bt :- fail"));
	assert(parse("bt"));
	assert(parse("cutokay :- !, okay"));
	assert(parse("cutfail :- !, fail"));
	assert(parse("cutfail :- okay"));
	assert(parse("elab 1"));
	assert(parse("elab 2"));
	assert(parse("elab 3"));
	assert(parse("query 0"));
	assert(parse("query2 .v :- query .v"));
	test(prove(generalize(n = ref(parse("query 0"))))); unref(n);
	test(!prove(generalize(n = ref(parse("query 1"))))); unref(n);
	test(prove(generalize(n = ref(parse("query2 0"))))); unref(n);
	test(!prove(generalize(n = ref(parse("query2 1"))))); unref(n);
	test(!prove(generalize(n = ref(parse("elab .v, fail"))))); unref(n);
	test(prove(generalize(n = ref(parse("findall .v (elab .v) .l, .l = (1, 2, 3,)"))))); unref(n);
	test(prove(generalize(n = ref(parse("bt"))))); unref(n);
	test(prove(generalize(n = ref(parse("cutokay"))))); unref(n);
	test(!prove(generalize(n = ref(parse("cutfail"))))); unref(n);

	singlogdeinit();
	return 0;
}

int main(int argc, char **argv) {
	int i;
	testmain();

	singloginit();
	for(i = 1; i < argc; i++) importfile(argv[i]);
	import(stdin);
	singlogdeinit();

	return !lastResult;
}
