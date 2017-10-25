// gcc -std=c99 -g src/main/c/termiostest.c -o target/termiostest && target/termiostest

#include <stdio.h>
#include <termios.h>

#include "termios.c"

int main() {
	printf("getche()");
	printf("\nYou typed: %c\n", getche());
	printf("getch()");
	printf("\nYou typed: %c\n", getch());
	return 0;
}
