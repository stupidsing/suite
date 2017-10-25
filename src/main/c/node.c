#include "gc.c"

#define REF_ (0)
#define ATOM (1)
#define INT_ (2)
#define STR_ (3)
#define TREE (4)

#define BT__ (256) // only appears in alternative during prove
#define CUT_ (257)
#define INV_ (258) // used by findall etc

char *substr(char *start, char *end) {
	char *result = memalloc(end - start + 1), *s = start, *d = result;
	while(s < end) *d++ = *s++;
	*d = 0;
	return result;
}

char *dupstr(char *str) {
	return substr(str, str + strlen(str));
}

typedef struct Node Node;
typedef struct Tree Tree;

struct Node {
	int refcount;
	int type;
	void *internal;
	union {
		Node *target;
		char *name;
		int value;
		Tree *tree;
	} u;
};

struct Tree {
	char *operator;
	Node *left, *right;
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

void unref(Node *node);

Node *newNode(int type) {
	Node *node = memalloc(sizeof(Node));
	node->refcount = 0;
	node->type = type;
	return node;
}

void deleteNode(Node *node) {
	Node *n;
	Tree *tree;

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

Node *ref(Node *node) {
	node->refcount++;
	return node;
}

void unref(Node *node) {
	if(--node->refcount <= 0) deleteNode(node);
}

void assignref(Node **pto, Node *from) {
	ref(from);
	unref(*pto);
	*pto = from;
}

Node *newRef() {
	Node *node = newNode(REF_);
	node->u.target = 0;
	return node;
}

Node *newAtom(char *start, char *end) {
	Node *node = newNode(ATOM);
	node->u.name = substr(start, end);
	return node;
}

Node *newInt(int value) {
	Node *node = newNode(INT_);
	node->u.value = value;
	return node;
}

Node *newString(char *name) {
	Node *node = newNode(STR_);
	node->u.name = name;
	return node;
}

Node *newTree(char *operator, Node *left, Node *right) {
	Tree *tree = memalloc(sizeof(Tree));
	tree->operator = operator;
	tree->left = ref(left);
	tree->right = ref(right);

	Node *node = newNode(TREE);
	node->u.tree = tree;
	return node;
}

Node *newInternal(int type, Node *target, void *internal) {
	Node *node = newNode(type);
	node->u.target = ref(target);
	node->internal = internal;
	return node;
}
