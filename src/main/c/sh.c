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
int linecount = 0;

void render0() {
	printf("\x1b[%dD\x1b[2K", linecount);
}

void render1(char *buffer, int length) {
	printf(prompt);
	for(int i = 0; i < length; i++) putchar(buffer[i]);
	linecount = strlen(prompt) + length;
}

void renderchar(int c) {
	putchar(c);
	linecount++;
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

#define resizebuffer (buffer = pos < size ? buffer : memrealloc_(buffer, (size <<= 1) * sizeof(char)))
#define rewrite(block) { render0(); block; render1(buffer, pos); }

char *searchtermios() {
	return "";
}

char *readlinetermios() {
	int pos, c, size, histpos = histsize;
	char *buffer = memalloc((size = buffersize) * sizeof(char));
	render1(buffer, pos = 0);

	while((c = getch()) != EOF && (pos || c != 4)) {
		int c1 = c == 27 && getch() == 91 ? getch() : 0;

		if(c == '\n') {
			putchar(c);
			resizebuffer[pos] = '\0';
			pos++;
			return addhistory(buffer);
		} else if(c < 27 || c1) {
			char *buffer0 = buffer;
			rewrite({
				histpos = c1 == 65 ? max(0, histpos - 1) // up
					: c1 == 66 ? min(histpos + 1, histsize) // down
					: histpos;
				char *history = c == 3 || c == 21 ? "" // ctrl-C, ctrl-U
					: c == 18 ? searchtermios() // ctrl-R
					: histpos < histsize ? histories[histpos]
					: buffer;
				strcpy(buffer = memalloc(size = (pos = strlen(history)) + 16), history);
			});
			memfree(buffer0);
		} else if(c == 127) {
			rewrite(pos = max(0, pos - 1));
		} else {
			renderchar(resizebuffer[pos] = c);
			pos++;
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
