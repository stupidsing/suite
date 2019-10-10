// gcc -std=c99 -g src/main/c/encrypt.c -o target/encrypt
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void fail(char *msg) {
	fprintf(stderr, "%s", msg);
	exit(1);
}

int main(int argc, char **argv) {
	char *p = argv[1];
	int length = strlen(p);
	char input[1024];
	char output[1024];
	while(!feof(stdin)) {
		int size = fread(input, 1, 1024, stdin);
		if(0 <= size) {
			for(int i = 0; i < size; i++) output[i] = input[i] ^ p[i % length];
			if(fwrite(output, 1, size, stdout) != size) fail("output error");
		}
		else fail("input error");
	}
}
