// gcc -std=c99 -g src/main/c/encb.c -o target/encb
#include <stdio.h>
#include <stdlib.h>

void fail(char *msg) {
	fprintf(stderr, msg);
	exit(1);
}

int main(int argc, char **argv) {
	char input[1024];
	char output[4096];
	while(!feof(stdin)) {
		int size = fread(input, 1, 1024, stdin);
		if(0 <= size) {
			int o = 0;
			for(int i = 0; i < size; i++) {
				char *hex = "0123456789ABCDEF";
				char b = input[i];
				output[o++] = hex[(b >> 4) & 15];
				output[o++] = hex[(b >> 0) & 15];
				output[o++] = '_';
			}
			if(fwrite(output, 1, o, stdout) != o) fail("output error");
		}
		else fail("input error");
	}
}
