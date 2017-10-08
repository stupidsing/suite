// gcc -std=c99 -g src/main/c/termios.c -o target/termios && target/termios

#ifndef termiossource
#define termiossource

#include <stdio.h>
#include <termios.h>

#include "util.c"

static struct termios termios0, termios1;

void termioscfg(int echo) {
	tcgetattr(0, &termios0);
	termios1 = termios0;
	termios1.c_lflag &= ~ICANON; // disable buffered I/O
	termios1.c_lflag &= echo ? ECHO : ~ECHO; // set echo mode
	tcsetattr(0, TCSANOW, &termios1);
}

void termiosdecfg() {
	tcsetattr(0, TCSANOW, &termios0);
}

char getch_(int echo) {
	termioscfg(echo);
	char ch = getchar();
	termiosdecfg();
	return ch;
}

char getch() {
	return getch_(0);
}

char getche(void) {
	return getch_(1);
}

int main(void) {
	char c;
	printf("(getche example) please type a letter: ");
	c = getche();
	printf("\nYou typed: %c\n", c);
	printf("(getch example) please type a letter...");
	c = getch();
	printf("\nYou typed: %c\n", c);
	return 0;
} 

#endif
