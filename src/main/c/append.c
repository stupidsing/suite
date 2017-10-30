#ifndef appendsource
#define appendsource

#include "util.c"

#define buffercapacity 64

typedef struct Appender Appender;

struct Appender {
	char *buffer;
	int capacity;
	int length;
};

#define enlarge(app, length1) { while(app->capacity <= length1) memrealloc(&app->buffer, (app->capacity <<= 1) * sizeof(char)); }

void append(Appender *app, int c) {
	int length1 = app->length + 1;
	enlarge(app, length1);
	app->buffer[app->length] = c;
	app->length = length1;
}

void appendbuffer(Appender *app, void *b, int length) {
	int length1 = app->length + length;
	enlarge(app, length1);
	memcpy(app->buffer + app->length, b, length);
	app->length = length1;
}

void *appendpointer(Appender *app, void *p) {
	appendbuffer(app, &p, sizeof(p));
	return p;
}

void apback(Appender *app, int length) {
	app->length = max(0, app->length - length);
}

void apcopyfrom(Appender *app, char *buffer) {
	if(app->buffer != buffer) {
		int length1 = (app->length = strlen(buffer)) + 1;
		enlarge(app, length1);
		memcpy(app->buffer, buffer, length1);
	}
}

void apinit(Appender *app) {
	app->buffer = memalloc((app->capacity = buffercapacity) * sizeof(char));
	app->capacity = buffercapacity;
	app->length = 0;
}

void apdeinit(Appender *app) {
	memfree(app->buffer);
}

#endif
