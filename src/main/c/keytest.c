// gcc -std=c99 -g src/main/c/keytest.c -o target/keytest && target/keytest

#include <stdio.h>
#include <termios.h>

#include "termios.c"

int main() {
	while(termiosavailable()) printf("\nYou typed: %d\n", getche());
	return 0;
}
