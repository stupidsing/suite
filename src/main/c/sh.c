// gcc -std=c99 -g src/main/c/sh.c -o target/sh && echo | target/sh
// reference: https://github.com/brenns10/lsh

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#include "mem.c"
#include "termios.c"

#define buffersize 64
#define delimiters " \t\r\n\a"
#define histsize 256

char *readlinestdin() {
	int pos = 0, c, size;
	char *buffer = memalloc((size = buffersize) * sizeof(char));

	while((c = getchar()) != EOF) {
		if(size <= pos) memrealloc(&buffer, (size <<= 1) * sizeof(char));

		if(c == '\n') {
			buffer[pos++] = '\0';
			return buffer;
		} else
			buffer[pos++] = c;
	}

	memfree(buffer);
	return 0;
}

char *prompt = "> ";

void render0(int length) {
	printf("\x1b[%dD\x1b[2K", length + strlen(prompt));
}

void render1(char *buffer, int length) {
	printf(prompt);
	for(int i = 0; i < length; i++) putchar(buffer[i]);
}

char *histories[histsize];

char *addhistory(char *buffer) {
	int isAdd = 1;
	for(int i = 0; i < histsize; i++) isAdd &= strcmp(buffer, histories[i]) != 0;

	if(isAdd) {
		memfree(histories[0]);
		int i1, histsize1 = histsize - 1;
		for(int i = 0; i < histsize1; i = i1) histories[i] = histories[i1 = i + 1];
		char *h = histories[histsize1] = memalloc(strlen(buffer));
		strcpy(h, buffer);
	}

	return buffer;
}

char *searchtermios() {
	int pos, c, size;
	char *buffer = memalloc((size = buffersize) * sizeof(char));

	while((c = getch()) != EOF) {
	}
}

#define resizebuffer (buffer = pos < size ? buffer : memrealloc_(buffer, (size <<= 1) * sizeof(char)))
#define rewrite(block) { render0(pos0); block; render1(buffer, pos); }

char *readlinetermios() {
	int pos, c, size, histpos = histsize;
	char *buffer = memalloc((size = buffersize) * sizeof(char));
	render1(buffer, pos = 0);

	while((c = getch()) != EOF && (pos || c != 4)) {
		int pos0 = pos, c1 = c == 27 && getch() == 91 ? getch() : 0;

		if(c == '\n') {
			putchar(c);
			pos++;
			resizebuffer[pos0] = '\0';
			return addhistory(buffer);
		} else if(c < 27 || c1) {
			char *buffer0 = buffer;
			render0(pos0);
			histpos = c1 == 65 ? max(0, histpos - 1) // up
				: c1 == 66 ? min(histpos + 1, histsize) // down
				: histpos;
			char *history = c == 3 || c == 21 ? "" // ctrl-C, ctrl-U
				: c == 18 ? searchtermios() // ctrl-R
				: histpos < histsize ? histories[histpos]
				: buffer;
			strcpy(buffer = memalloc(size = (pos = strlen(history)) + 16), history);
			render1(buffer, pos);
			memfree(buffer0);
		} else if(c == 127) {
			render0(pos0);
			render1(buffer, --pos);
		} else {
			pos++;
			putchar(resizebuffer[pos0] = c);
		}
	}

	memfree(buffer);
	return 0;
}

char *readline() {
	return termiosavailable() ? readlinetermios() : readlinestdin();
}

char **splitline(char *line) {
	int size = buffersize;
	char **tokens = memalloc(size * sizeof(char*));
	int pos = 0;
	char *source = line;

	while(tokens[pos++] = strtok(source, delimiters)) {
		source = 0;
		if(size <= pos) memrealloc(&tokens, (size <<= 1) * sizeof(char*));
	}

	return tokens;
}

int launch(char **args) {
	pid_t pid = fork();

	if(pid == 0) // child process
		if(execvp(args[0], args) == -1) perror("execvp()");
		else exit(1);
	else if(0 <= pid) { // parent process
		int status;
		do {
			waitpid(pid, &status, WUNTRACED);
		} while(!WIFEXITED(status) && !WIFSIGNALED(status));
		return WEXITSTATUS(status) == 0;
	}
	else perror("fork()");

	return 1;
}

int execute(char **args) {
	return args[0] ? launch(args) : 1;
}

void loop() {
	int status = 1;
	char *line;

	while(status && (line = readline())) {
		char **args = splitline(line);
		status = execute(args);
		memfree(args);
		memfree(line);
	}
}

module(sh, {
	meminit();
	for(int i = 0; i < histsize; i++) *(histories[i] = memalloc(1)) = 0;
}, {
	for(int i = 0; i < histsize; i++) memfree(histories[i]);
	memdeinit();
})

int main(int argc, char **argv) {
	shinit();
	loop();
	shdeinit();
	return 0;
}
