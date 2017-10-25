#ifndef termiossource
#define termiossource

#include <stdio.h>
#include <termios.h>

#include "util.c"

static struct termios termios0, termios1;

int termiosavailable() {
	return tcgetattr(0, &termios0) == 0;
}

void termioscfg(int echo) {
	if(tcgetattr(0, &termios0)) err("tcgetattr()");
	termios1 = termios0;
	termios1.c_lflag &= ~ICANON; // disable buffered I/O
	termios1.c_lflag &= echo ? ECHO : ~ECHO; // set echo mode
	if(tcsetattr(0, TCSANOW, &termios1)) err("tcsetattr()");
}

void termiosdecfg() {
	if(tcsetattr(0, TCSANOW, &termios0)) err("tcsetattr()");
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

char getche() {
	return getch_(1);
}

#endif
