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

int readchar() {
	return termiosavailable() ? getche() : getchar();
}

char *readline() {
	int size = buffersize;
	char *buffer = memalloc(size * sizeof(char));
	int pos = 0;
	int c;

	while((c = readchar()) != EOF && (pos || c != 4)) {
		if(c == '\n') {
			buffer[pos] = '\0';
			return buffer;
		} else
			buffer[pos] = c;

		if(size <= ++pos) memrealloc(&buffer, (size <<= 1) * sizeof(char));
	}

	memfree(buffer);
	return 0;
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

int prompt() {
	printf("> ");
}

void loop() {
	int status = 1;
	char *line;

	while(status && prompt() && (line = readline())) {
		char **args = splitline(line);
		status = execute(args);
		memfree(args);
		memfree(line);
	}
}

int main(int argc, char **argv) {
	meminit();
	loop();
	memdeinit();
	return 0;
}
