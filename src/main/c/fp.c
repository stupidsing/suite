#ifndef fpsource
#define fpsource

typedef struct Closure Closure;

struct Closure {
	void *frame;
	void *(*apply) (void*);
};

void *invoke(Closure closure) { (*closure.apply)(closure.frame); }

#endif
