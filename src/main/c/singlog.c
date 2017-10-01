// gcc -std=c99 -g src/main/c/singlog.c -o target/singlog && target/singlog

#define _GNU_SOURCE

#include <ctype.h>

#include "hashtab.c"
#include "io.c"

const int genHashSize = 256;
const int handlerHashSize = 1024;
const int ruleHashSize = 262144;
const int trailSize = 65536;
const int bufferSize = 65536;

struct Node *nilAtom, *failAtom, *undAtom, *cutAtom;

char *importingpath;

struct Hashtab handlerHashtab;
struct Hashtab ruleHashtab;

int enabletrace;
int tracedepth;

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
			else return node0->u.value < node1->u.value ? -1 : 1;
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

struct Node *clone_(struct Node *node, struct Hashtab *hashtab) {
	struct Tree *tree;
	node = final(node);

	switch(node->type) {
	case REF_:
		if(node->u.target) return node->u.target;
		else {
			struct Node *ref = htget(hashtab, node);
			if(!ref) htput(hashtab, node, ref = newRef());
			return ref;
		}
	case TREE:
		tree = node->u.tree;
		return newTree(tree->operator
			, clone_(tree->left, hashtab)
			, clone_(tree->right, hashtab));
	default: return node;
	}
}

struct Node *clone(struct Node *node) {
	struct Hashtab hashtab;
	htnew(&hashtab, genHashSize);
	struct Node *cloned = clone_(node, &hashtab);
	htdelete(hashtab);
	return cloned;
}

struct Node *generalize_(struct Node *node, struct Hashtab *hashtab) {
	struct Tree *tree;
	struct Node *l, *r;
	char first;
	node = final(node);

	switch(node->type) {
	case ATOM:
		first = node->u.name[0];
		if(first == '.' || first == '!') {
			struct Node *ref = htget(hashtab, node);
			if(!ref) htput(hashtab, node, ref = newRef());
			return ref;
		} else if(node == undAtom) return newRef();
		else return node;
	case TREE:
		tree = node->u.tree;
		l = generalize_(tree->left, hashtab);
		r = generalize_(tree->right, hashtab);
		if(l != tree->left || r != tree->right) return newTree(tree->operator, l, r);
	default: return node;
	}
}

struct Node *generalizeWithCut(struct Node *node, struct Node *cut) {
	struct Hashtab hashtab;
	htnew(&hashtab, genHashSize);
	htput(&hashtab, cutAtom, cut);
	struct Node *generalized = generalize_(node, &hashtab);
	htdelete(hashtab);
	return generalized;
}

struct Node *generalize(struct Node *node) {
	struct Hashtab hashtab;
	htnew(&hashtab, genHashSize);
	struct Node *generalized = generalize_(node, &hashtab);
	htdelete(hashtab);
	return generalized;
}

void bindref(struct Node *from, struct Node *to, struct Node ***ptrail) {
	*(*ptrail)++ = ref(from);
	from->u.target = ref(to);
}

void rollback(struct Node ***trail, struct Node **to) {
	while(to < *trail) {
		struct Node *ref = *--*trail;
		unref(ref->u.target);
		ref->u.target = 0;
		unref(ref);
	}
}

int bind(struct Node *node0, struct Node *node1, struct Node ***ptrail);

int bind_(struct Node *node0, struct Node *node1, struct Node ***ptrail) {
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
	int result = bind_(node0, node1, ptrail);
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

int prove_(struct Node *goal, struct Node ***ptrail) {
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
	if(rule = htget(&ruleHashtab, proto)) {
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
	} else if(ptr = htget(&handlerHashtab, proto)) {
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
	int result = prove_(goal, &trail);
	while(trailStack < trail) unref(*--trail);
	if(enabletrace) tracedepth = tracedepth0;
	return result;
}

void assert(struct Node *rule) {
	struct Node *proto = prototype(rule);
	int i = htgetpos(&ruleHashtab, proto);

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

			if(arraySize <= nEntries) break;
		}
	}

	qsort(entries, nEntries, sizeof(char*), &cmpstr);

	for(i = 0; i < nEntries; i++) {
		if(0 < i) fputs(", ", stdout);
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
		htnew(&genHashtab, genHashSize);
		htput(&genHashtab, cutAtom, cut = ref(newInternal(CUT_, failAtom, 0)));

		node = ref(parse(buffer + 1));
		generalized = ref(generalize_(node, &genHashtab));

		if(elab) {
			struct Node *inv = newInternal(INV_, nilAtom, &handleelaborate);
			assignref(&generalized, newTree(commaOp, generalized, inv));
		}

		nSolutions = 0;
		lastResult = prove(generalized);
		if(elab) fprintf(stdout, "%d solution%s\n", nSolutions, nSolutions != 1 ? "s" : "");
		else fputs(lastResult ? "yes\n" : "no\n", stdout);

		htdelete(genHashtab);
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
		while(previous <= last && isspace(*last)) last--;

		if(last < previous || *last == '#') {
			if(previous <= last) *last = 0;
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
	while(filename1 < s && *s != '/') s--;
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
	return importfile(final(query->u.tree->right)->u.name);
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
			while(size < len + len1) buf = realloc(buf, (size <<= 1) + 1);

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
	int result = prove_(query->u.tree->right, ptrail);
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
	rules = htget(&ruleHashtab, ps[1]);
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

	gcinit();
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

	htnew(&handlerHashtab, handlerHashSize);
	htput(&handlerHashtab, ref(getAtom("atom.str")), &handleatomstr);
	htput(&handlerHashtab, ref(getAtom("clone")), &handleclone);
	htput(&handlerHashtab, ref(getAtom("dump")), &handledump);
	htput(&handlerHashtab, ref(getAtom("findall")), &handlefindall);
	htput(&handlerHashtab, ref(getAtom("import")), &handleimport);
	htput(&handlerHashtab, ref(getAtom("is.atom")), &handleisatom);
	htput(&handlerHashtab, ref(getAtom("is.int")), &handleisint);
	htput(&handlerHashtab, ref(getAtom("is.ref")), &handleisref);
	htput(&handlerHashtab, ref(getAtom("is.str")), &handleisstr);
	htput(&handlerHashtab, ref(getAtom("let")), &handlelet);
	htput(&handlerHashtab, ref(getAtom("let.int")), &handleletint);
	htput(&handlerHashtab, ref(getAtom("list.str")), &handleliststr);
	htput(&handlerHashtab, ref(getAtom("nl")), &handlenl);
	htput(&handlerHashtab, ref(getAtom("not")), &handlenot);
	htput(&handlerHashtab, ref(getAtom("once")), &handleonce);
	htput(&handlerHashtab, ref(getAtom("ord")), &handleord);
	htput(&handlerHashtab, ref(getAtom("parse")), &handleparse);
	htput(&handlerHashtab, ref(getAtom("read")), &handleread);
	htput(&handlerHashtab, ref(getAtom("readchar")), &handlereadchar);
	htput(&handlerHashtab, ref(getAtom("readeach")), &handlereadeach);
	htput(&handlerHashtab, ref(getAtom("rule")), &handlerule);
	htput(&handlerHashtab, ref(getAtom("system")), &handlesystem);
	htput(&handlerHashtab, ref(getAtom("trace")), &handletrace);
	htput(&handlerHashtab, ref(getAtom("tree")), &handletree);
	htput(&handlerHashtab, ref(getAtom("write")), &handlewrite);

	htnew(&ruleHashtab, ruleHashSize);
	enabletrace = tracedepth = 0;
}

void deinit() {
	int i;
	for(i = 0; i < ruleHashtab.size; i++) {
		struct Node *proto = ruleHashtab.keys[i], *rule = ruleHashtab.values[i];
		if(proto) unref(proto);
		if(rule) unref(rule);
	}

	htdelete(ruleHashtab);

	for(i = 0; i < handlerHashtab.size; i++) {
		struct Node *proto = handlerHashtab.keys[i];
		if(proto) unref(proto);
	}

	htdelete(handlerHashtab);

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
	gcdeinit();
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
	return 0;
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
