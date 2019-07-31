// gcc -std=c99 -g src/main/c/decb.c -o target/decb
#include <stdio.h>
#include <stdlib.h>

void fail(char *msg) {
	fprintf(stderr, msg);
	exit(1);
}

int v(char c) {
	return 'A' < c ? c - 'A' + 10 : c - '0';
}

int main(int argc, char **argv) {
	char input[4096];
	char output[1024];
	while(!feof(stdin)) {
		int count = fread(input, 3, 1024, stdin);
		if(0 <= count) {
			int size = 3 * count;
			int o = 0;
			for(int i = 0; i < size; i += 3)
				output[o++] = (v(input[i]) << 4) + v(input[i + 1]);
			if(fwrite(output, 1, o, stdout) != o) fail("output error");
		}
		else fail("input error");
	}
}
