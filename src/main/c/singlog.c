// gcc -std=c99 -g singlog.c -o singlog && ./singlog

#define _GNU_SOURCE

#include <ctype.h>

#include "util.c"

// adjustable parameters
const int genHashSize = 256;
const int handlerHashSize = 1024;
const int ruleHashSize = 262144;
const int trailSize = 65536;
const int bufferSize = 65536;

#define min(a, b) ((a) < (b) ? (a) : (b))
#define max(a, b) ((a) > (b) ? (a) : (b))

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
	" <= ", " < ", " >= ", " > ",
	" != ", " = ",
	" + ", " - ", " * ", " / ", " %% ",
	" ^ ",
	" ", "/", ":",
};
int *isLeftAssoc;
const int nOperators = sizeof(operators) / sizeof(operators[0]);

struct Node **atomHashes;
int nAtomHashes;
int atomHashSize; // must be power of 2

struct Node **intNodes;

char *ruleOp, *smcOp, *commaOp, *equalOp, *spaceOp;
struct Node *nilAtom, *failAtom, *undAtom, *cutAtom;

char *importingpath;

struct Hashtab handlerHashtab;
struct Hashtab ruleHashtab;

int enabletrace;
int tracedepth;

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

struct Node *newRefTo(struct Node *target) {
	struct Node *node = newNode(REF_);
	node->u.target = target;
	return node;
}

struct Node *newRef() {
	return newRefTo(0);
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
}

struct Node *newInternal(int type, struct Node *target, void *internal) {
	struct Node *node = newNode(type);
	node->u.target = ref(target);
	node->internal = internal;
	return node;
}

int getAtomHashPos0(char *start, char *end) {
	struct Node *atom;
	int i = hashstr(start, end);

	while(atom = atomHashes[i &= atomHashSize - 1]) {
		char *p0 = start, *p1 = atom->u.name;
		int match = 1;
		while(p0 < end && (match = match && *p0++ == *p1++));
		match = match && !*p1;
		if(!match) i++; else break;
	}

	return i;
}

int getAtomHashPos(char *name) {
	return getAtomHashPos0(name, name + strlen(name));
}

struct Node *getAtom0(char *start, char *end) {
	int i = getAtomHashPos0(start, end), j;

	if(!atomHashes[i]) {
		if(nAtomHashes >= atomHashSize * 3 / 4) { // rehash if looks full
			struct Node **atomHashes0 = atomHashes;
			int atomHashSize0 = atomHashSize;

			atomHashSize <<= 1;
			atomHashes = memalloczeroed(atomHashSize * sizeof(struct Node*));

			for(j = 0; j < atomHashSize0; j++) {
				struct Node *atom = atomHashes0[j];
				if(atom) atomHashes[getAtomHashPos(atom->u.name)] = atom;
			}

			memfree(atomHashes0);
			i = getAtomHashPos0(start, end);
		}

		nAtomHashes++;
		atomHashes[i] = ref(newAtom(start, end));
	}

	return atomHashes[i];
}

struct Node *getAtom(char *name) {
	return getAtom0(name, name + strlen(name));
}

struct Node *getInt(int value) {
	if(value >= -128 && value < 128) return intNodes[value + 128];
	else return newInt(value);
}

int findOperatorIndex(char *operator) {
	int i;
	for(i = 0; i < nOperators; i++)
		if(strcmp(operators[i], operator) == 0) return i;
}

int fromHexDigit(char c) {
	if(c > 'a') return c - 'a' + 10;
	else if(c > 'A') return c - 'A' + 10;
	else if(c > '0') return c - '0';
	else return -1; // wtf
}

char toHexDigit(int i) {
	return i < 10 ? i + '0' : i + 'A' - 10;
}

char *escape(char *s, int asString) {
	int length0 = strlen(s), length1 = length0;
	int i0 = 0, i1 = 0;
	char quote = asString ? '"' : 0;
	char *s1 = memalloc(length1 + 1);

	while(i0 < length0) {
		char out[4] = { 0, 0, 0, 0 };
		unsigned char c = s[i0++];

		int normalChar;
		if(asString) normalChar = c >= 32 && c < 128 && c != '"';
		else normalChar = c >= '0' && c <= '9'
				|| c >= 'A' && c <= 'Z'
				|| c >= 'a' && c <= 'z'
				|| c == '.' || c == '_' || c == '-' || c == '!' || c == '$';

		if(normalChar) out[0] = c;
		else {
			out[0] = '%';
			out[1] = toHexDigit(c >> 4);
			out[2] = toHexDigit(c & 0x0F);
			if(!quote) quote = '\'';
		}

		char *o = out;
		while(*o) {
			if(i1 >= length1) {
				length1 += max(length1 >> 1, 32);
				s1 = realloc(s1, length1 + 1);
			}

			s1[i1++] = *o++;
		}
	}

	s1[i1] = 0;
	memfree(s);

	if(quote) {
		char *s2 = memalloc(i1 + 3);
		s2[i1 + 2] = 0;
		s2[0] = s2[i1 + 1] = quote;
		while(--i1 >= 0) s2[i1 + 1] = s1[i1];
		memfree(s1);
		return s2;
	} else return s1;
}

char *unescape(char *s) {
	int length0 = strlen(s), length1 = length0;
	int i0 = 0, i1 = 0;
	char *s1 = memalloc(length1 + 1);

	while(i0 <= length0) {
		char c = s[i0++];

		if(c == '%' && i0 < length0) {
			int v0, v1;
			char d0 = s[i0];

			if(d0 == '%') {
				i0++;
				s1[i1++] = d0;
			} else if((v0 = fromHexDigit(d0)) >= 0
					&& (v1 = fromHexDigit(s[i0 + 1])) >= 0) {
				i0 += 2;
				s1[i1++] = (v0 << 4) + v1;
			}
		} else s1[i1++] = c;
	}

	s1[i1] = 0;
	memfree(s);
	return s1;
}

struct Node *parse0(char *start, char *end) {
	struct Node *node = 0;
	char *last = end - 1;
	int depth = 0, quote = 0, op;

	if(!node) {
		for(op = 0; op < nOperators; op++) {
			int assoc = isLeftAssoc[op];
			char *s = assoc ? end : start;
			char *t = end - s + start;

			while(s != t) {
				if(assoc) s--;

				int match = 1;
				char *operator = operators[op];
				char *p0 = s, *p1 = operator;
				while(*p1 && (match = match && p0 < end && *p0++ == *p1++));

				char c = *s;
				if(!quote) {
					depth += c == '(' || c == '[' || c == '{';
					depth -= c == ')' || c == ']' || c == '}';
				}

				int q = 0;
				if(c == '\'') q = 1;
				if(c == '"') q = 2;
				if(quote == 0 || quote == q) quote = q - quote;

				if(!depth && !quote && match) {
					while(s > start && isspace(s[-1])) s--; // trims space before/after
					while(p0 < end && isspace(*p0)) p0++;
					node = newTree(operator, parse0(start, s), parse0(p0, end));
					goto done;
				}

				if(!assoc) s++;
			}
		}
	}

	
	if(!node)
		if(*start == '(' && *last == ')'
				|| *start == '[' && *last == ']'
				|| *start == '{' && *last == '}')
			node = parse0(start + 1, last);

	if(!node && *start == *last)
		if(*start == '\'') node = getAtom0(start + 1, last);
		else if(*start == '"') node = newString(unescape(substr(start + 1, last)));

	if(!node && start != end) {
		int isInt = 1;
		char *s = start;
		if(*s == '-') s++;
		while(isInt && s < end) isInt = isInt && isdigit(*s++);

		if(isInt) {
			node = newInt(atoi(s = substr(start, end)));
			memfree(s);
		}
	}

	if(!node) node = getAtom0(start, end);

done:
	return node;
}

struct Node *parse(char *s0) {
	char *p0 = s0;
	char *s1 = memalloc(strlen(s0)), *p1 = s1;

retry:
	while(*p0) {
		if(p0[0] == '-') {
			if(p0[1] == '-') {
				while(*p0 && *p0++ != '\n');
				goto retry;
			} else if(p0[1] == '=') {
				p0 += 2;
				while(*p0 && *p0++ != '=' && *p0 && *p0++ != '-');
				goto retry;
			}
		}

		char c = *p0++;
		if(isspace(c)) c = ' ';
		*p1++ = c;
	}

	char *start = s1, *end = p1;
	while(start < end && isspace(*start)) start++;
	while(start < end && isspace(end[-1])) end--;

	struct Node *node = parse0(start, end);
	memfree(s1);
	return node;
}

char *dump(struct Node *node) {
	char buffer[256], *s1, *s2, *operator, *merged;
	int size;

	switch(node->type) {
	case REF_: case BT__:
		if(node->u.target)
			return dump(node->u.target);
		else {
			snprintf(buffer, 256, ".%lx", (long) (intptr_t) node);
			return dupstr(buffer);
		}
	case CUT_: return dupstr("<cut>");
	case INV_: return dupstr("<invoke>");
	case ATOM: return escape(dupstr(node->u.name), 0);
	case INT_: snprintf(buffer, 256, "%d", node->u.value); return dupstr(buffer);
	case STR_: return escape(dupstr(node->u.name), 1);
	case TREE:
		operator = node->u.tree->operator;
		s1 = dump(node->u.tree->left);
		s2 = dump(node->u.tree->right);
		size = strlen(operator) + strlen(s1) + strlen(s2) + 5;
		merged = memalloc(size);
		snprintf(merged, size, "(%s)%s(%s)", s1, operator, s2);
		memfree(s1);
		memfree(s2);
		return merged;
	default: err("Unknown node type"); return dupstr("<unknown>");
	}
}

struct Node *final(struct Node *node) {
	while(node->type == REF_ && node->u.target) node = node->u.target;
	return node;
}

int compare(struct Node *node0, struct Node *node1) {
	node0 = final(node0);
	node1 = final(node1);

	struct Tree *tree0, *tree1;
	int c = node0->type - node1->type;

	if(!c) {
		switch(node0->type) {
		case ATOM: case STR_: return strcmp(node0->u.name, node1->u.name);
		case INT_:
			if(node0->u.value == node1->u.value) return 0;
			else return node0->u.value > node1->u.value ? 1 : -1;
		case TREE:
			tree0 = node0->u.tree;
			tree1 = node1->u.tree;
			c = findOperatorIndex(tree0->operator) - findOperatorIndex(tree1->operator);
			if(!c) c = compare(tree0->left, tree1->left);
			if(!c) c = compare(tree0->right, tree1->right);
			return c;
		default: err("Unknown node type");
		}
	}

	return c;
}

struct Node *clone0(struct Node *node, struct Hashtab *hashtab) {
	struct Tree *tree;
	node = final(node);

	switch(node->type) {
	case REF_:
		if(node->u.target) return node->u.target;
		else {
			struct Node *ref = gethashtab(hashtab, node);
			if(!ref) puthashtab(hashtab, node, ref = newRef());
			return ref;
		}
	case TREE:
		tree = node->u.tree;
		return newTree(tree->operator
			, clone0(tree->left, hashtab)
			, clone0(tree->right, hashtab));
	default: return node;
	}
}

struct Node *clone(struct Node *node) {
	struct Hashtab hashtab;
	newhashtab(&hashtab, genHashSize);
	struct Node *cloned = clone0(node, &hashtab);
	deletehashtab(hashtab);
	return cloned;
}

struct Node *generalize0(struct Node *node, struct Hashtab *hashtab) {
	struct Tree *tree;
	struct Node *l, *r;
	char first;
	node = final(node);

	switch(node->type) {
	case ATOM:
		first = node->u.name[0];
		if(first == '.' || first == '!') {
			struct Node *ref = gethashtab(hashtab, node);
			if(!ref) puthashtab(hashtab, node, ref = newRef());
			return ref;
		} else if(node == undAtom) return newRef();
		else return node;
	case TREE:
		tree = node->u.tree;
		l= generalize0(tree->left, hashtab);
		r= generalize0(tree->right, hashtab);
		if(l != tree->left || r != tree->right) return newTree(tree->operator, l, r);
	default: return node;
	}
}

struct Node *generalizeWithCut(struct Node *node, struct Node *cut) {
	struct Hashtab hashtab;
	newhashtab(&hashtab, genHashSize);
	puthashtab(&hashtab, cutAtom, cut);
	struct Node *generalized = generalize0(node, &hashtab);
	deletehashtab(hashtab);
	return generalized;
}

struct Node *generalize(struct Node *node) {
	struct Hashtab hashtab;
	newhashtab(&hashtab, genHashSize);
	struct Node *generalized = generalize0(node, &hashtab);
	deletehashtab(hashtab);
	return generalized;
}

void bindref(struct Node *from, struct Node *to, struct Node ***ptrail) {
	*(*ptrail)++ = ref(from);
	from->u.target = ref(to);
}

void rollback(struct Node ***trail, struct Node **to) {
	while(*trail > to) {
		struct Node *ref = *--*trail;
		unref(ref->u.target);
		ref->u.target = 0;
		unref(ref);
	}
}

int bind(struct Node *node0, struct Node *node1, struct Node ***ptrail);

int bind0(struct Node *node0, struct Node *node1, struct Node ***ptrail) {
	node0 = final(node0);
	node1 = final(node1);

	if(node0 != node1) {
		int t0 = node0->type, t1 = node1->type;

		if(t0 == REF_ && t1 == REF_) {
			bindref(max(node0, node1), min(node0, node1), ptrail);
			return 1;
		} else if(t0 == REF_) {
			bindref(node0, node1, ptrail);
			return 1;
		} else if(t1 == REF_) {
			bindref(node1, node0, ptrail);
			return 1;
		} else if(t0 == t1) {
			struct Tree *tree0, *tree1;

			switch(t0) {
			case ATOM: return 0;
			case INT_: return node0->u.value == node1->u.value;
			case STR_: return strcmp(node0->u.name, node1->u.name) == 0;
			case TREE:
				tree0 = node0->u.tree;
				tree1 = node1->u.tree;
				return tree0->operator == tree1->operator
					&& bind(tree0->left, tree1->left, ptrail)
					&& bind(tree0->right, tree1->right, ptrail);
			default: err("Unknown node type");
			}
		} else return 0;
	} else return 1;
}

int bind(struct Node *node0, struct Node *node1, struct Node ***ptrail) {
	struct Node **ptrail0 = *ptrail;
	int result = bind0(node0, node1, ptrail);
	if(!result) rollback(ptrail, ptrail0);
	return result;
}

struct Node *prototype(struct Node *node) {
	struct Node *n;
	switch(node->type) {
	case REF_: return prototype(node->u.target);
	case TREE: return prototype(node->u.tree->left);
	default: return node;
	}
}

struct Node *expand(struct Node *query, struct Node *cut, struct Node *rules) {
	struct Node *expanded = failAtom;

	while(rules != nilAtom) { // a # b
		struct Tree *tree = rules->u.tree;
		struct Node *rule = ref(generalizeWithCut(tree->left, cut));
		rules = tree->right;

		tree = rule->u.tree; // head :- tail

		expanded = newTree(smcOp
			, newTree(commaOp
				, newTree(equalOp
					, query
					, tree->left
				)
				, tree->right
			), expanded
		);

		unref(rule);
	}

	return expanded;
}

void trace(char *type, struct Node *query) {
	if(enabletrace) {
		int i;
		char *d = dump(query);
		fprintf(stderr, "[%s] ", type);
		for(i = 0; i < tracedepth; i++) fprintf(stderr, "  ");
		fprintf(stderr, "%s\n", d);
		memfree(d);
	}
}

int prove0(struct Node *goal, struct Node ***ptrail) {
	struct Node *finally = newInternal(BT__, failAtom, *ptrail);

	// final result = goal && remaining || alternative
	goal = ref(goal);
	struct Node *remaining = ref(nilAtom), *alternative = ref(finally);
	struct Node *node, *left, *right, *proto, *rule;
	char *op;
	void *ptr;
	int i, result;

retry:
	switch(goal->type) {
	case BT__: rollback(ptrail, goal->internal);
	case REF_: assignref(&goal, goal->u.target); goto retry;
	case INV_: ptr = goal->internal; goto handle;
	case CUT_: assignref(&alternative, goal->u.target); goto okay;
	case ATOM:
		if(goal == nilAtom) goto okay;
		else if(goal == failAtom) goto fail;
		else if(strcmp(goal->u.name, "okay") == 0) goto okay;
		else goto invoke;
	case TREE:
		op = goal->u.tree->operator;
		left = goal->u.tree->left;
		right = goal->u.tree->right;

		if(op == commaOp) {
			struct Node *rem1 = remaining != nilAtom ? newTree(commaOp, right, remaining) : right;
			assignref(&remaining, rem1);
			assignref(&goal, left);
		} else if(op == smcOp) {
			struct Node *bt = newInternal(BT__, right, *ptrail); // facilitates backtracking
			struct Node *alt0 = newTree(commaOp, bt, remaining);
			struct Node *alt1 = alternative != failAtom ? newTree(smcOp, alt0, alternative) : alt0;
			assignref(&alternative, alt1);
			assignref(&goal, left);
		} else if(op == spaceOp) goto invoke;
		else if(op == equalOp && bind(left, right, ptrail)) goto okay;
		else goto fail;
		goto retry;
	default: err("Unknown node type");
	}

invoke:
	proto = prototype(goal);
	if(rule = gethashtab(&ruleHashtab, proto)) {
		if(enabletrace) {
			int handleexit(struct Node*, struct Node***, struct Node**, struct Node**);
			struct Node *after = newInternal(INV_, goal, &handleexit);
			assignref(&alternative, newTree(smcOp, after, alternative));
		}

		struct Node *cut = ref(newInternal(CUT_, alternative, 0));
		struct Node *expanded = expand(goal, cut, rule);

		if(enabletrace) {
			int handleentry(struct Node*, struct Node***, struct Node**, struct Node**);
			struct Node *before = newInternal(INV_, goal, &handleentry);
			expanded = newTree(commaOp, before, expanded);
		}

		assignref(&goal, expanded);
		unref(cut);
		goto retry;
	} else if(ptr = gethashtab(&handlerHashtab, proto)) {
handle: ;
		int (*handler)(struct Node*, struct Node***, struct Node**, struct Node**) = ptr;
		int result = (*handler)(goal, ptrail, &remaining, &alternative);
		if(enabletrace && goal->type != INV_) trace("CALL_", goal);
		if(result) goto okay; else goto fail;
	}

fail:
	if(alternative != failAtom) {
		assignref(&goal, alternative);
		assignref(&alternative, failAtom);
		goto retry;
	} else {
		result = 0;
		goto done;
	}

okay:
	if(remaining != nilAtom) {
		assignref(&goal, remaining);
		assignref(&remaining, nilAtom);
		goto retry;
	} else {
		result = 1;
		goto done;
	}

done:
	unref(alternative);
	unref(remaining);
	unref(goal);
	return result;
}

int prove(struct Node *goal) {
	int tracedepth0 = tracedepth;
	struct Node *trailStack[trailSize], **trail = trailStack;
	int result = prove0(goal, &trail);
	while(trail > trailStack) unref(*--trail);
	if(enabletrace) tracedepth = tracedepth0;
	return result;
}

void assert(struct Node *rule) {
	struct Node *proto = prototype(rule);
	int i = gethashtabpos(&ruleHashtab, proto);

	if(!ruleHashtab.keys[i]) {
		ruleHashtab.keys[i] = ref(proto);
		ruleHashtab.values[i] = ref(nilAtom);
	}

	// always in head :- tail form
	if(rule->type != TREE || rule->u.tree->operator != ruleOp) rule = newTree(ruleOp, rule, nilAtom);

	assignref((struct Node**) ruleHashtab.values + i, newTree(commaOp, rule, ruleHashtab.values[i]));
}

int nSolutions, lastResult = 0;
struct Hashtab genHashtab;

int cmpstr(const void *s0, const void *s1) {
	return strcmp(*(char**) s0, *(char**) s1);
}

int handleelaborate(struct Node *query, struct Node ***trail, struct Node **remaining) {
	const int arraySize = 32, bufferSize = 1024;

	int i, nEntries = 0;
	char *entries[arraySize];

	for(i = 0; i < genHashtab.size; i++) {
		struct Node *key = genHashtab.keys[i];

		if(key && key->u.name[0] == '.') {
			char *s = memalloc(bufferSize);
			char *sk = dump(key), *sv = dump(genHashtab.values[i]);
			snprintf(s, bufferSize, "%s = %s", sk, sv);
			memfree(sk);
			memfree(sv);
			entries[nEntries++] = s;

			if(nEntries >= arraySize) break;
		}
	}

	qsort(entries, nEntries, sizeof(char*), &cmpstr);

	for(i = 0; i < nEntries; i++) {
		if(i > 0) fputs(", ", stdout);
		fputs(entries[i], stdout);
		memfree(entries[i]);
	}

	fputs("\n", stdout);
	nSolutions++;
	return 0;
}

void process(char *buffer) {
	struct Node *node, *cut, *generalized;
	int yesno = buffer[0] == '?', elab = buffer[0] == '/';

	if(yesno || elab) {
		newhashtab(&genHashtab, genHashSize);
		puthashtab(&genHashtab, cutAtom, cut = ref(newInternal(CUT_, failAtom, 0)));

		node = ref(parse(buffer + 1));
		generalized = ref(generalize0(node, &genHashtab));

		if(elab) {
			struct Node *inv = newInternal(INV_, nilAtom, &handleelaborate);
			assignref(&generalized, newTree(commaOp, generalized, inv));
		}

		nSolutions = 0;
		lastResult = prove(generalized);
		if(elab) fprintf(stdout, "%d solution%s\n", nSolutions, nSolutions != 1 ? "s" : "");
		else fputs(lastResult ? "yes\n" : "no\n", stdout);

		deletehashtab(genHashtab);
		unref(generalized);
		unref(node);
		unref(cut);
	} else if(buffer[0]) assert(parse(buffer));
}

void import(FILE *in) {
	char buffer[bufferSize], *previous, *current = buffer, *last;

	while(!feof(in)) {
		if(in == stdin) fputs(buffer == current ?  "=> " : "   ", stdout);
		fgets(previous = current, buffer + bufferSize - current, in);

		while(*current) current++;

		last = current - 1;
		while(last >= previous && isspace(*last)) last--;

		if(last < previous || *last == '#') {
			if(last >= previous) *last = 0;
			process(buffer);
			current = buffer;
		}
	}
}

int importfile(char *filename) {
	char filename1[bufferSize], *importingpath0 = importingpath, *s;
	int appendpath = importingpath0 && filename[0] != '/';
	if(appendpath) snprintf(filename1, bufferSize, "%s/%s", importingpath0, filename);
	else snprintf(filename1, bufferSize, "%s", filename);

	s = filename1 + strlen(filename1);
	while(s > filename1 && *s != '/') s--;
	importingpath = substr(filename1, s);

	FILE *file = fopen(filename1, "r");
	if(file) {
		import(file);
		fclose(file);
	}

	memfree(importingpath);
	importingpath = importingpath0;
	return !!file;
}

int bindfree(struct Node *node0, struct Node *node1, struct Node ***ptrail) {
	ref(node0);
	ref(node1);
	int result = bind(node0, node1, ptrail);
	unref(node1);
	unref(node0);
	return result;
}

int eval(struct Node *expr) {
	if(expr->type == REF_) return eval(expr->u.target);
	else if(expr->type == INT_) return expr->u.value;
	else if(expr->type == TREE) {
		struct Tree *tree = expr->u.tree;
		if(strcmp(tree->operator, " + ") == 0) return eval(tree->left) + eval(tree->right);
		else if(strcmp(tree->operator, " - ") == 0) return eval(tree->left) - eval(tree->right);
		else if(strcmp(tree->operator, " * ") == 0) return eval(tree->left) * eval(tree->right);
		else if(strcmp(tree->operator, " / ") == 0) return eval(tree->left) / eval(tree->right);
		else if(strcmp(tree->operator, " ^ ") == 0) return eval(tree->left) ^ eval(tree->right);
	} else err("Cannot evaluate node type");
}

struct Node *getfirst(struct Node **node) {
	struct Node *param, *node1 = final(*node);
	if(node1->type == TREE && node1->u.tree->operator == spaceOp) {
		param = node1->u.tree->left;
		*node = node1->u.tree->right;
	} else {
		param = node1;
		*node = nilAtom;
	}
	return param;
}

void getparams(struct Node *node, int n, struct Node **params) {
	getfirst(&node);
	while(n--) *params++ = final(getfirst(&node));
}

void tbc(struct Node **palt, struct Node *cont, struct Node *rem
	, struct Node **trail) { // to be continued - extends alternative to allow backtracking
	struct Node *alt1 = newTree(smcOp, newTree(commaOp, cont, rem), *palt);
	assignref(palt, newInternal(BT__, alt1, trail));
}

int handlefound(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt);

int handleatomstr(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct Node *ps[2];
	getparams(query, 2, ps);
	struct Node *atom = ps[0], *str = ps[1];

	if(atom->type == ATOM) return bindfree(str, newString(dupstr(atom->u.name)), ptrail);
	else if(str->type == STR_) return bindfree(atom, getAtom(str->u.name), ptrail);
	else return 0;
}

int handleclone(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct Node *ps[2];
	getparams(query, 2, ps);
	return bindfree(clone(ps[0]), ps[1], ptrail);
}

int handledump(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	char *s = dump(query->u.tree->right);
	printf("%s", s);
	memfree(s);
	return 1;
}

int handleentry(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	trace("ENTRY", query->u.target);
	tracedepth++;
	return 1;
}

int handleeof(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	return feof(stdin);
}

int handleexit(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	tracedepth--;
	trace("EXIT_", query->u.target);
	return 0;
}

int handlefindall(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct Node *ps[3];
	getparams(query, 3, ps);
	struct Node *var = ps[0], *pred = ps[1], *list0 = ps[2], *list1 = ref(nilAtom);

	prove(newTree(commaOp, pred
		, newInternal(INV_, newInternal(INV_, var, &list1), &handlefound)));

	int result = bind(list0, list1, ptrail);
	unref(list1);
	return result;
}

int handlefound(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct Node *node = query->u.target;
	struct Node *sublist = newTree(commaOp, clone(node->u.target), nilAtom);
	assignref((struct Node**) node->internal, sublist);
	node->internal = &sublist->u.tree->right;
	return 0;
}

int handleimport(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	importfile(final(query->u.tree->right)->u.name);
}

int handleisatom(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	return final(query->u.tree->right)->type == ATOM;
}

int handleisint(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	return final(query->u.tree->right)->type == INT_;
}

int handleisref(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	return final(query->u.tree->right)->type == REF_;
}

int handleisstr(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	return final(query->u.tree->right)->type == STR_;
}

int handlelet(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct Node *ps[2];
	getparams(query, 2, ps);
	return bindfree(ps[0], getInt(eval(ps[1])), ptrail);
}

int handleletint(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct LetInt {
		struct Node *variable;
		int value, end, inc;
	} *letInt;

	if(query->type == INV_) {
		letInt = query->u.target->internal;
		letInt->value += letInt->inc;
	} else {
		struct Node *ps[4];
		getparams(query, 4, ps);

		letInt = memalloc(sizeof(struct LetInt));
		letInt->variable = ps[0];
		letInt->value = ps[1]->u.value;
		letInt->end = ps[2]->u.value;
		letInt->inc = ps[3]->u.value;

		query = newInternal(INV_, newInternal(INV_, query, letInt), &handleletint);
	}

	if(letInt->value < letInt->end) {
		tbc(palt, query, *prem, *ptrail);
		return bindfree(letInt->variable, getInt(letInt->value), ptrail);
	} else {
		memfree(letInt);
		return 0;
	}
}

int handleliststr(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct Node *ps[2];
	getparams(query, 2, ps);
	struct Node *list = ps[0], *str = ps[1], *n;
	int result;

	if(str->type == STR_) {
		char *name = str->u.name;
		struct Node *list1 = ref(nilAtom), **plist1 = &list1;

		while(*name) {
			char s[2] = { *name++, 0 };
			assignref(plist1, n = newTree(commaOp, getAtom(s), nilAtom));
			plist1 = &n->u.tree->right;
		}

		result = bind(list, list1, ptrail);
		unref(list1);
	} else {
		int size = 32, len = 0;
		char *buf = memalloc(size + 1);

		while(list != nilAtom) {
			char *s = list->u.tree->left->u.name;
			int len1 = strlen(s);
			while(len + len1 > size) buf = realloc(buf, (size <<= 1) + 1);

			strcpy(buf + len, s);
			len += len1;
			list = list->u.tree->right;
		}

		buf[len] = 0;
		result = bindfree(str, newString(buf), ptrail);
	}

	return result;
}

int handleonce(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	return prove(query->u.tree->right);
}

int handleord(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct Node *ps[2];
	getparams(query, 2, ps);
	return compare(ps[0], ps[1]) < 0;
}

int handlenl(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	printf("\n");
	return 1;
}

int handlenot(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct Node **trail0 = *ptrail;
	int result = prove0(query->u.tree->right, ptrail);
	if(result) rollback(ptrail, trail0); // rolls back anyway
	return !result;
}

int handleparse(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct Node *ps[2];
	getparams(query, 2, ps);
	if(ps[1]->type != REF_)
		return bindfree(ps[0], parse(ps[1]->u.name), ptrail);
	else return bindfree(newString(dump(ps[0])), ps[1], ptrail);
}

int handleread(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	size_t nBytes = 0;
	char *line = 0;

	nBytes = getline(&line, &nBytes, stdin);
	int result = bindfree(query->u.tree->right, newString(substr(line, line + nBytes)), ptrail);
	free(line);
	return result;
}

int handlereadchar(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	char c[] = { fgetc(stdin) };
	return bindfree(query->u.tree->right, newString(substr(c, c + 1)), ptrail);
}

int handlereadeach(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	if(!feof(stdin)) {
		struct Node **trail = *ptrail;
		int result = handleread(query, ptrail, prem, palt);
		if(result) tbc(palt, query, *prem, trail);
		return result;
	} else return 0;
}

int handlerule(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct Node *ps[2], *rules;
	getparams(query, 2, ps);
	rules = gethashtab(&ruleHashtab, ps[1]);
	return rules ? bindfree(ps[0], rules, ptrail) : 0;
}

int handlesystem(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct Node *ps[2];
	getparams(query, 2, ps);
	return bindfree(ps[0], getInt(system(ps[1]->u.name)), ptrail);
}

int handletrace(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	enabletrace = 1 - enabletrace;
	return 1;
}

int handletree(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	struct Node *ps[4], *node;
	getparams(query, 4, ps);
	node = ps[0];

	if(node->type == TREE) {
		struct Tree *tree = node->u.tree;
		char *operator = tree->operator;
		return bind(newAtom(operator, operator + strlen(operator)), ps[1], ptrail)
			&& bind(tree->left, ps[2], ptrail)
			&& bind(tree->right, ps[3], ptrail);
	} else if(node->type == REF_)
		return bind(ps[0], newTree(
			operators[findOperatorIndex(ps[1]->u.name)], ps[2], ps[3]), ptrail);
	else return 0;
}

int handlewrite(struct Node *query, struct Node ***ptrail, struct Node **prem, struct Node **palt) {
	printf("%s", query->u.tree->right->u.name);
	return 1;
}

void init() {
	int i;

	meminit();
	nAtomHashes = 0;
	atomHashSize = 256;
	atomHashes = memalloczeroed(atomHashSize * sizeof(struct Node*));

	intNodes = memalloczeroed(256 * sizeof(struct Node*));
	for(i = -128; i < 128; i++) intNodes[i + 128] = ref(newInt(i));

	isLeftAssoc = memalloc(nOperators * sizeof(int));

	for(i = 0; i < nOperators; i++) {
		char *operator = operators[i];

		if(strcmp(operator, " :- ") == 0) ruleOp = operator;
		else if(strcmp(operator, ";") == 0) smcOp = operator;
		else if(strcmp(operator, ",") == 0) commaOp = operator;
		else if(strcmp(operator, " = ") == 0) equalOp = operator;
		else if(strcmp(operator, " ") == 0) spaceOp = operator;

		isLeftAssoc[i] = strcmp(operator, " - ") == 0 || strcmp(operator, " / ") == 0;
	}

	nilAtom = ref(getAtom(""));
	failAtom = ref(getAtom("fail"));
	undAtom = ref(getAtom("_"));
	cutAtom = ref(getAtom("!"));

	importingpath = 0;

	newhashtab(&handlerHashtab, handlerHashSize);
	puthashtab(&handlerHashtab, ref(getAtom("atom.str")), &handleatomstr);
	puthashtab(&handlerHashtab, ref(getAtom("clone")), &handleclone);
	puthashtab(&handlerHashtab, ref(getAtom("dump")), &handledump);
	puthashtab(&handlerHashtab, ref(getAtom("findall")), &handlefindall);
	puthashtab(&handlerHashtab, ref(getAtom("import")), &handleimport);
	puthashtab(&handlerHashtab, ref(getAtom("is.atom")), &handleisatom);
	puthashtab(&handlerHashtab, ref(getAtom("is.int")), &handleisint);
	puthashtab(&handlerHashtab, ref(getAtom("is.ref")), &handleisref);
	puthashtab(&handlerHashtab, ref(getAtom("is.str")), &handleisstr);
	puthashtab(&handlerHashtab, ref(getAtom("let")), &handlelet);
	puthashtab(&handlerHashtab, ref(getAtom("let.int")), &handleletint);
	puthashtab(&handlerHashtab, ref(getAtom("list.str")), &handleliststr);
	puthashtab(&handlerHashtab, ref(getAtom("nl")), &handlenl);
	puthashtab(&handlerHashtab, ref(getAtom("not")), &handlenot);
	puthashtab(&handlerHashtab, ref(getAtom("once")), &handleonce);
	puthashtab(&handlerHashtab, ref(getAtom("ord")), &handleord);
	puthashtab(&handlerHashtab, ref(getAtom("parse")), &handleparse);
	puthashtab(&handlerHashtab, ref(getAtom("read")), &handleread);
	puthashtab(&handlerHashtab, ref(getAtom("readchar")), &handlereadchar);
	puthashtab(&handlerHashtab, ref(getAtom("readeach")), &handlereadeach);
	puthashtab(&handlerHashtab, ref(getAtom("rule")), &handlerule);
	puthashtab(&handlerHashtab, ref(getAtom("system")), &handlesystem);
	puthashtab(&handlerHashtab, ref(getAtom("trace")), &handletrace);
	puthashtab(&handlerHashtab, ref(getAtom("tree")), &handletree);
	puthashtab(&handlerHashtab, ref(getAtom("write")), &handlewrite);

	newhashtab(&ruleHashtab, ruleHashSize);
	enabletrace = tracedepth = 0;
}

void deinit() {
	int i;
	for(i = 0; i < ruleHashtab.size; i++) {
		struct Node *proto = ruleHashtab.keys[i], *rule = ruleHashtab.values[i];
		if(proto) unref(proto);
		if(rule) unref(rule);
	}

	deletehashtab(ruleHashtab);

	for(i = 0; i < handlerHashtab.size; i++) {
		struct Node *proto = handlerHashtab.keys[i];
		if(proto) unref(proto);
	}

	deletehashtab(handlerHashtab);

	unref(cutAtom);
	unref(undAtom);
	unref(failAtom);
	unref(nilAtom);
	memfree(isLeftAssoc);

	for(i = 0; i < 256; i++) unref(intNodes[i]);
	memfree(intNodes);

	for(i = 0; i < atomHashSize; i++) {
		struct Node *atom = atomHashes[i];
		if(atom) unref(atom);
	}

	memfree(atomHashes);
	nAtomHashes = 0;
	memdeinit();
}

#define test(t) (t) || err("test case failed");

int testmain() {
	struct Node *n;
	init();

	n = parse("1 + 2 * 3");
	char *s = dump(n);
	test(strcmp(s, "(1) + ((2) * (3))") == 0);
	memfree(s);
	unref(n);

	test(prove(parse("okay"))); // prove() will automatically ref()/unref()
	test(!prove(parse("fail")));
	test(prove(parse("okay, okay")));
	test(!prove(parse("okay, fail")));
	test(prove(parse("fail; okay")));
	test(!prove(parse("fail; fail")));
	test(prove(parse("a = a")));
	test(!prove(parse("a = b")));
	test(prove(parse("a b = a b")));

	test(prove(generalize(n = ref(parse("a = a"))))); unref(n);
	test(!prove(generalize(n = ref(parse("a = b"))))); unref(n);
	test(prove(generalize(n = ref(parse(".a = A, .a = A"))))); unref(n);
	test(!prove(generalize(n = ref(parse(".a = A, .a = B"))))); unref(n);
	test(prove(generalize(n = ref(parse(".a = A; .a = B"))))); unref(n);

	test(prove(parse("let 7 (1 + 2 * 3)")));
	test(prove(parse("let 2 (9 - 4 - 3)")));
	test(prove(generalize(n = ref(parse("list.str .l \"ab\", .l = (a, b,)"))))); unref(n);
	test(prove(generalize(n = ref(parse("list.str (a, b,) .s, .s = \"ab\""))))); unref(n);
	test(prove(parse("list.str (a, b, c,) \"abc\"")));
	test(prove(parse("ord 2 3")));
	test(!prove(parse("ord 3 3")));
	test(!prove(parse("ord 3 2")));

	assert(parse("bt :- fail"));
	assert(parse("bt"));
	assert(parse("cutokay :- !, okay"));
	assert(parse("cutfail :- !, fail"));
	assert(parse("cutfail :- okay"));
	assert(parse("elab 1"));
	assert(parse("elab 2"));
	assert(parse("elab 3"));
	assert(parse("query 0"));
	assert(parse("query2 .v :- query .v"));
	test(prove(generalize(n = ref(parse("query 0"))))); unref(n);
	test(!prove(generalize(n = ref(parse("query 1"))))); unref(n);
	test(prove(generalize(n = ref(parse("query2 0"))))); unref(n);
	test(!prove(generalize(n = ref(parse("query2 1"))))); unref(n);
	test(!prove(generalize(n = ref(parse("elab .v, fail"))))); unref(n);
	test(prove(generalize(n = ref(parse("findall .v (elab .v) .l, .l = (1, 2, 3,)"))))); unref(n);
	test(prove(generalize(n = ref(parse("bt"))))); unref(n);
	test(prove(generalize(n = ref(parse("cutokay"))))); unref(n);
	test(!prove(generalize(n = ref(parse("cutfail"))))); unref(n);

	deinit();
}

int main(int argc, char **argv) {
	int i;
	testmain();

	init();
	for(i = 1; i < argc; i++) importfile(argv[i]);
	import(stdin);
	deinit();

	return !lastResult;
}
