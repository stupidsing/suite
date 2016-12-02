#include "gc.c"

#define REF_ (0)
#define ATOM (1)
#define INT_ (2)
#define STR_ (3)
#define TREE (4)

#define BT__ (256) // only appears in alternative during prove
#define CUT_ (257)
#define INV_ (258) // used by findall etc

struct Node {
	int refcount;
	int type;
	void *internal;
	union {
		struct Node *target;
		char *name;
		int value;
		struct Tree *tree;
	} u;
};

struct Tree {
	char *operator;
	struct Node *left, *right;
};

char *operators[] = {
	"#",
	" :- ",
	" | ", " ? ",
	";", ",",
	" <= ", " < ",
	" != ", " = ",
	" + ", " - ", " * ", " / ", " %% ",
	" ^ ",
	" ", "/", ":",
};
int *isLeftAssoc;
const int nOperators = sizeof(operators) / sizeof(operators[0]);

char *ruleOp, *smcOp, *commaOp, *equalOp, *spaceOp;

void unref(struct Node *node);

struct Node *newNode(int type) {
	struct Node *node = memalloc(sizeof(struct Node));
	node->refcount = 0;
	node->type = type;
	return node;
}

void deleteNode(struct Node *node) {
	struct Node *n;
	struct Tree *tree;

	switch(node->type) {
	case REF_: case BT__: case CUT_: case INV_: if(n = node->u.target) unref(n); break;
	case ATOM: case STR_: memfree(node->u.name);
	case INT_: break;
	case TREE:
		tree = node->u.tree;
		unref(tree->left);
		unref(tree->right);
		memfree(tree);
		break;
	default: err("Unknown node type");
	}

	memfree(node);
}

struct Node *ref(struct Node *node) {
	node->refcount++;
	return node;
}

void unref(struct Node *node) {
	if(--node->refcount <= 0) deleteNode(node);
}

void assignref(struct Node **pto, struct Node *from) {
	ref(from);
	unref(*pto);
	*pto = from;
}

struct Node *newRef() {
	struct Node *node = newNode(REF_);
	node->u.target = 0;
	return node;
}

struct Node *newAtom(char *start, char *end) {
	struct Node *node = newNode(ATOM);
	node->u.name = substr(start, end);
	return node;
}

struct Node *newInt(int value) {
	struct Node *node = newNode(INT_);
	node->u.value = value;
	return node;
}

struct Node *newString(char *name) {
	struct Node *node = newNode(STR_);
	node->u.name = name;
	return node;
}

struct Node *newTree(char *operator, struct Node *left, struct Node *right) {
	struct Tree *tree = memalloc(sizeof(struct Tree));
	tree->operator = operator;
	tree->left = ref(left);
	tree->right = ref(right);

	struct Node *node = newNode(TREE);
	node->u.tree = tree;
	return node;
}

struct Node *newInternal(int type, struct Node *target, void *internal) {
	struct Node *node = newNode(type);
	node->u.target = ref(target);
	node->internal = internal;
	return node;
}
