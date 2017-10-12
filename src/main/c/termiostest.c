// gcc -std=c99 -g src/main/c/termiostest.c -o target/termiostest && target/termiostest

#include <stdio.h>
#include <termios.h>

#include "termios.c"

int main() {
	printf("getche()");
	char c0 = getche();
	printf("\nYou typed: %c\n", c0);
	printf("getch()");
	char c1 = getch();
	printf("\nYou typed: %c\n", c1);
	return 0;
}
