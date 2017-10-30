// gcc -std=c99 -g src/main/c/sh.c -o target/sh && echo | target/sh
// reference: https://github.com/brenns10/lsh

#define _GNU_SOURCE

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#include "mem.c"
#include "append.c"
#include "termios.c"

#define delimiters " \t\r\n\a"
#define histsize 256

char *readlinestdin() {
	int c;
	Appender app;
	apinit(&app);

	while((c = getchar()) != EOF)
		if(c == '\n') {
			append(&app, 0);
			return app.buffer;
		}
		else append(&app, c);

	apdeinit(&app);
	return 0;
}

char *prompt = "> ";
char *promptsearch = "? ";
int linecount = 0;

void render0() {
	printf("\x1b[%dD\x1b[2K", linecount);
}

void render1(char *prompt, char *buffer, int length) {
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

#define rewrite(block) { render0(); block; render1(prompt, app.buffer, app.length); }

char *searchtermios(int *confirmed) {
	Appender app;
	apinit(&app);
	render1(promptsearch, "", 0);
	int c;
	char *history = "", *history_;

	while((c = getch()) != EOF) {
		int c1 = c == 27 && getch() == 91 ? getch() : 0;

		if(c == '\n') {
			*confirmed = 1;
			break;
		}
		else if(c == 127) apback(&app, 1);
		else if(c < 27 || c1) {
			*confirmed = 0;
			break;
		}
		else append(&app, c);

		char buffer[buffersize];
		strncpy(buffer, app.buffer, min(buffersize, app.length));
		buffer[buffersize - 1] = 0;
		history = "";

		for(int i = histsize - 1; 0 <= i; i--)
			if(strcasestr(history_ = histories[i], buffer)) {
				history = history_;
				break;
			}

		render0();
		render1(promptsearch, history, strlen(history));
	}

	render0();
	apdeinit(&app);
	return history;
}

char *readlinetermios() {
	Appender app;
	apinit(&app);
	render1(prompt, app.buffer, app.length);
	int c, histpos = histsize;

	while((c = getch()) != EOF && (app.length || c != 4)) {
		int c1 = c == 27 && getch() == 91 ? getch() : 0;

		if(c == '\n')
			goto issue;
		else if(c == 18) { // ctrl-R
			int confirmed;
			rewrite(apcopyfrom(&app, searchtermios(&confirmed)));
			if(confirmed) goto issue;
		}
		else if(c < 27 || c1) {
			rewrite({
				histpos = c1 == 65 ? max(0, histpos - 1) // up
					: c1 == 66 ? min(histpos + 1, histsize) // down
					: histpos;
				char *history = c == 3 || c == 21 ? "" // ctrl-C, ctrl-U
					: histpos < histsize ? histories[histpos]
					: app.buffer;
				apcopyfrom(&app, history);
			});
		}
		else if(c == 127) {
			rewrite(apback(&app, 1));
		}
		else {
			renderchar(c);
			append(&app, c);
		}
	}

	apdeinit(&app);
	return 0;
issue:
	putchar('\n');
	append(&app, 0);
	return addhistory(app.buffer);
}

char *readline() {
	return termiosavailable() ? readlinetermios() : readlinestdin();
}

char **splitline(char *line) {
	Appender app;
	apinit(&app);
	char *source = line;
	while(appendpointer(&app, strtok(source, delimiters))) source = 0;
	return (char**) app.buffer;
}

int launch(char **args) {
	pid_t pid = fork();

	if(pid == 0) { // child process
		if(execvp(args[0], args) == -1) perror("execvp()");
		exit(1);
	}
	else if(0 <= pid) { // parent process
		int status;
		do {
			waitpid(pid, &status, WUNTRACED);
		} while(!WIFEXITED(status) && !WIFSIGNALED(status));
		return WEXITSTATUS(status);
	}
	else perror("fork()");

	return 1;
}

int execute(char **args) {
	return args[0] ? launch(args) : 0;
}

int loop() {
	int rc = 0;
	char *line;

	while(line = readline()) {
		char **args = splitline(line);
		rc = execute(args);
		memfree(args);
		memfree(line);
	}

	return rc;
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
	int rc = loop();
	shdeinit();
	return rc;
}
