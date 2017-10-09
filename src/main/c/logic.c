#ifndef logicsource
#define logicsource

#include <ctype.h>

#include "hashtab.c"
#include "io.c"

const int genHashSize = 256;
const int handlerHashSize = 1024;
const int ruleHashSize = 262144;
const int trailSize = 65536;
const int bufferSize = 65536;

Node *nilAtom, *failAtom, *undAtom, *cutAtom;

char *importingpath;

Hashtab handlerHashtab;
Hashtab ruleHashtab;

int enabletrace;
int tracedepth;

Node *final(Node *node) {
	while(node->type == REF_ && node->u.target) node = node->u.target;
	return node;
}

int compare(Node *node0, Node *node1) {
	node0 = final(node0);
	node1 = final(node1);

	Tree *tree0, *tree1;
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

Node *clone_(Node *node, Hashtab *hashtab) {
	Tree *tree;
	node = final(node);

	switch(node->type) {
	case REF_:
		if(node->u.target) return node->u.target;
		else {
			Node *ref = htget(hashtab, node);
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

Node *clone(Node *node) {
	Hashtab hashtab;
	htnew(&hashtab, genHashSize);
	Node *cloned = clone_(node, &hashtab);
	htdelete(hashtab);
	return cloned;
}

Node *generalize_(Node *node, Hashtab *hashtab) {
	Tree *tree;
	Node *l, *r;
	char first;
	node = final(node);

	switch(node->type) {
	case ATOM:
		first = node->u.name[0];
		if(first == '.' || first == '!') {
			Node *ref = htget(hashtab, node);
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

Node *generalizeWithCut(Node *node, Node *cut) {
	Hashtab hashtab;
	htnew(&hashtab, genHashSize);
	htput(&hashtab, cutAtom, cut);
	Node *generalized = generalize_(node, &hashtab);
	htdelete(hashtab);
	return generalized;
}

Node *generalize(Node *node) {
	Hashtab hashtab;
	htnew(&hashtab, genHashSize);
	Node *generalized = generalize_(node, &hashtab);
	htdelete(hashtab);
	return generalized;
}

void bindref(Node *from, Node *to, Node ***ptrail) {
	*(*ptrail)++ = ref(from);
	from->u.target = ref(to);
}

void rollback(Node ***trail, Node **to) {
	while(to < *trail) {
		Node *ref = *--*trail;
		unref(ref->u.target);
		ref->u.target = 0;
		unref(ref);
	}
}

int bind(Node *node0, Node *node1, Node ***ptrail);

int bind_(Node *node0, Node *node1, Node ***ptrail) {
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
			Tree *tree0, *tree1;

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

int bind(Node *node0, Node *node1, Node ***ptrail) {
	Node **ptrail0 = *ptrail;
	int result = bind_(node0, node1, ptrail);
	if(!result) rollback(ptrail, ptrail0);
	return result;
}

Node *prototype(Node *node) {
	Node *n;
	switch(node->type) {
	case REF_: return prototype(node->u.target);
	case TREE: return prototype(node->u.tree->left);
	default: return node;
	}
}

Node *expand(Node *query, Node *cut, Node *rules) {
	Node *expanded = failAtom;

	while(rules != nilAtom) { // a # b
		Tree *tree = rules->u.tree;
		Node *rule = ref(generalizeWithCut(tree->left, cut));
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

void trace(char *type, Node *query) {
	if(enabletrace) {
		int i;
		char *d = dump(query);
		fprintf(stderr, "[%s] ", type);
		for(i = 0; i < tracedepth; i++) fprintf(stderr, "  ");
		fprintf(stderr, "%s\n", d);
		memfree(d);
	}
}

int prove_(Node *goal, Node ***ptrail) {
	Node *finally = newInternal(BT__, failAtom, *ptrail);

	// final result = goal && remaining || alternative
	goal = ref(goal);
	Node *remaining = ref(nilAtom), *alternative = ref(finally);
	Node *node, *left, *right, *proto, *rule;
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
			Node *rem1 = remaining != nilAtom ? newTree(commaOp, right, remaining) : right;
			assignref(&remaining, rem1);
			assignref(&goal, left);
		} else if(op == smcOp) {
			Node *bt = newInternal(BT__, right, *ptrail); // facilitates backtracking
			Node *alt0 = newTree(commaOp, bt, remaining);
			Node *alt1 = alternative != failAtom ? newTree(smcOp, alt0, alternative) : alt0;
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
			int handleexit(Node*, Node***, Node**, Node**);
			Node *after = newInternal(INV_, goal, &handleexit);
			assignref(&alternative, newTree(smcOp, after, alternative));
		}

		Node *cut = ref(newInternal(CUT_, alternative, 0));
		Node *expanded = expand(goal, cut, rule);

		if(enabletrace) {
			int handleentry(Node*, Node***, Node**, Node**);
			Node *before = newInternal(INV_, goal, &handleentry);
			expanded = newTree(commaOp, before, expanded);
		}

		assignref(&goal, expanded);
		unref(cut);
		goto retry;
	} else if(ptr = htget(&handlerHashtab, proto)) {
handle: ;
		int (*handler)(Node*, Node***, Node**, Node**) = ptr;
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

int prove(Node *goal) {
	int tracedepth0 = tracedepth;
	Node *trailStack[trailSize], **trail = trailStack;
	int result = prove_(goal, &trail);
	while(trailStack < trail) unref(*--trail);
	if(enabletrace) tracedepth = tracedepth0;
	return result;
}

void assert(Node *rule) {
	Node *proto = prototype(rule);
	int i = htgetpos(&ruleHashtab, proto);

	if(!ruleHashtab.keys[i]) {
		ruleHashtab.keys[i] = ref(proto);
		ruleHashtab.values[i] = ref(nilAtom);
	}

	// always in head :- tail form
	if(rule->type != TREE || rule->u.tree->operator != ruleOp) rule = newTree(ruleOp, rule, nilAtom);

	assignref((Node**) ruleHashtab.values + i, newTree(commaOp, rule, ruleHashtab.values[i]));
}

int nSolutions, lastResult = 1;
Hashtab genHashtab;

int cmpstr(const void *s0, const void *s1) {
	return strcmp(*(char**) s0, *(char**) s1);
}

int handleelaborate(Node *query, Node ***trail, Node **remaining) {
	const int arraySize = 32, bufferSize = 1024;

	int i, nEntries = 0;
	char *entries[arraySize];

	for(i = 0; i < genHashtab.size; i++) {
		Node *key = genHashtab.keys[i];

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
	Node *node, *cut, *generalized;
	int yesno = buffer[0] == '?', elab = buffer[0] == '/';

	if(yesno || elab) {
		htnew(&genHashtab, genHashSize);
		htput(&genHashtab, cutAtom, cut = ref(newInternal(CUT_, failAtom, 0)));

		node = ref(parse(buffer + 1));
		generalized = ref(generalize_(node, &genHashtab));

		if(elab) {
			Node *inv = newInternal(INV_, nilAtom, &handleelaborate);
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

int bindfree(Node *node0, Node *node1, Node ***ptrail) {
	ref(node0);
	ref(node1);
	int result = bind(node0, node1, ptrail);
	unref(node1);
	unref(node0);
	return result;
}

int eval(Node *expr) {
	if(expr->type == REF_) return eval(expr->u.target);
	else if(expr->type == INT_) return expr->u.value;
	else if(expr->type == TREE) {
		Tree *tree = expr->u.tree;
		if(strcmp(tree->operator, " + ") == 0) return eval(tree->left) + eval(tree->right);
		else if(strcmp(tree->operator, " - ") == 0) return eval(tree->left) - eval(tree->right);
		else if(strcmp(tree->operator, " * ") == 0) return eval(tree->left) * eval(tree->right);
		else if(strcmp(tree->operator, " / ") == 0) return eval(tree->left) / eval(tree->right);
		else if(strcmp(tree->operator, " ^ ") == 0) return eval(tree->left) ^ eval(tree->right);
	} else err("Cannot evaluate node type");
}

Node *getfirst(Node **node) {
	Node *param, *node1 = final(*node);
	if(node1->type == TREE && node1->u.tree->operator == spaceOp) {
		param = node1->u.tree->left;
		*node = node1->u.tree->right;
	} else {
		param = node1;
		*node = nilAtom;
	}
	return param;
}

void getparams(Node *node, int n, Node **params) {
	getfirst(&node);
	while(n--) *params++ = final(getfirst(&node));
}

void tbc(Node **palt, Node *cont, Node *rem
	, Node **trail) { // to be continued - extends alternative to allow backtracking
	Node *alt1 = newTree(smcOp, newTree(commaOp, cont, rem), *palt);
	assignref(palt, newInternal(BT__, alt1, trail));
}

int handlefound(Node *query, Node ***ptrail, Node **prem, Node **palt);

int handleatomstr(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	Node *ps[2];
	getparams(query, 2, ps);
	Node *atom = ps[0], *str = ps[1];

	if(atom->type == ATOM) return bindfree(str, newString(dupstr(atom->u.name)), ptrail);
	else if(str->type == STR_) return bindfree(atom, getAtom(str->u.name), ptrail);
	else return 0;
}

int handleclone(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	Node *ps[2];
	getparams(query, 2, ps);
	return bindfree(clone(ps[0]), ps[1], ptrail);
}

int handledump(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	char *s = dump(query->u.tree->right);
	printf("%s", s);
	memfree(s);
	return 1;
}

int handleentry(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	trace("ENTRY", query->u.target);
	tracedepth++;
	return 1;
}

int handleeof(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	return feof(stdin);
}

int handleexit(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	tracedepth--;
	trace("EXIT_", query->u.target);
	return 0;
}

int handlefindall(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	Node *ps[3];
	getparams(query, 3, ps);
	Node *var = ps[0], *pred = ps[1], *list0 = ps[2], *list1 = ref(nilAtom);

	prove(newTree(commaOp, pred
		, newInternal(INV_, newInternal(INV_, var, &list1), &handlefound)));

	int result = bind(list0, list1, ptrail);
	unref(list1);
	return result;
}

int handlefound(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	Node *node = query->u.target;
	Node *sublist = newTree(commaOp, clone(node->u.target), nilAtom);
	assignref((Node**) node->internal, sublist);
	node->internal = &sublist->u.tree->right;
	return 0;
}

int handleimport(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	return importfile(final(query->u.tree->right)->u.name);
}

int handleisatom(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	return final(query->u.tree->right)->type == ATOM;
}

int handleisint(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	return final(query->u.tree->right)->type == INT_;
}

int handleisref(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	return final(query->u.tree->right)->type == REF_;
}

int handleisstr(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	return final(query->u.tree->right)->type == STR_;
}

int handlelet(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	Node *ps[2];
	getparams(query, 2, ps);
	return bindfree(ps[0], getInt(eval(ps[1])), ptrail);
}

int handleletint(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	typedef struct LetInt LetInt;
	struct LetInt {
		Node *variable;
		int value, end, inc;
	} *letInt;

	if(query->type == INV_) {
		letInt = query->u.target->internal;
		letInt->value += letInt->inc;
	} else {
		Node *ps[4];
		getparams(query, 4, ps);

		letInt = memalloc(sizeof(LetInt));
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

int handleliststr(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	Node *ps[2];
	getparams(query, 2, ps);
	Node *list = ps[0], *str = ps[1], *n;
	int result;

	if(str->type == STR_) {
		char *name = str->u.name;
		Node *list1 = ref(nilAtom), **plist1 = &list1;

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

int handleonce(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	return prove(query->u.tree->right);
}

int handleord(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	Node *ps[2];
	getparams(query, 2, ps);
	return compare(ps[0], ps[1]) < 0;
}

int handlenl(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	printf("\n");
	return 1;
}

int handlenot(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	Node **trail0 = *ptrail;
	int result = prove_(query->u.tree->right, ptrail);
	if(result) rollback(ptrail, trail0); // rolls back anyway
	return !result;
}

int handleparse(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	Node *ps[2];
	getparams(query, 2, ps);
	if(ps[1]->type != REF_)
		return bindfree(ps[0], parse(ps[1]->u.name), ptrail);
	else return bindfree(newString(dump(ps[0])), ps[1], ptrail);
}

int handleread(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	size_t nBytes = 0;
	char *line = 0;

	nBytes = getline(&line, &nBytes, stdin);
	int result = bindfree(query->u.tree->right, newString(substr(line, line + nBytes)), ptrail);
	free(line);
	return result;
}

int handlereadchar(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	char c[] = { fgetc(stdin) };
	return bindfree(query->u.tree->right, newString(substr(c, c + 1)), ptrail);
}

int handlereadeach(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	if(!feof(stdin)) {
		Node **trail = *ptrail;
		int result = handleread(query, ptrail, prem, palt);
		if(result) tbc(palt, query, *prem, trail);
		return result;
	} else return 0;
}

int handlerule(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	Node *ps[2], *rules;
	getparams(query, 2, ps);
	rules = htget(&ruleHashtab, ps[1]);
	return rules ? bindfree(ps[0], rules, ptrail) : 0;
}

int handlesystem(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	Node *ps[2];
	getparams(query, 2, ps);
	return bindfree(ps[0], getInt(system(ps[1]->u.name)), ptrail);
}

int handletrace(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	enabletrace = 1 - enabletrace;
	return 1;
}

int handletree(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	Node *ps[4], *node;
	getparams(query, 4, ps);
	node = ps[0];

	if(node->type == TREE) {
		Tree *tree = node->u.tree;
		char *operator = tree->operator;
		return bind(newAtom(operator, operator + strlen(operator)), ps[1], ptrail)
			&& bind(tree->left, ps[2], ptrail)
			&& bind(tree->right, ps[3], ptrail);
	} else if(node->type == REF_)
		return bind(ps[0], newTree(
			operators[findOperatorIndex(ps[1]->u.name)], ps[2], ps[3]), ptrail);
	else return 0;
}

int handlewrite(Node *query, Node ***ptrail, Node **prem, Node **palt) {
	printf("%s", query->u.tree->right->u.name);
	return 1;
}

void singloginit_() {
	int i;

	gcinit();
	nAtomHashes = 0;
	atomHashSize = 256;
	atomHashes = memalloczeroed(atomHashSize * sizeof(Node*));

	intNodes = memalloczeroed(256 * sizeof(Node*));
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

void singlogdeinit_() {
	for(int i = 0; i < ruleHashtab.size; i++) {
		Node *proto = ruleHashtab.keys[i], *rule = ruleHashtab.values[i];
		if(proto) unref(proto);
		if(rule) unref(rule);
	}

	htdelete(ruleHashtab);

	for(int i = 0; i < handlerHashtab.size; i++) {
		Node *proto = handlerHashtab.keys[i];
		if(proto) unref(proto);
	}

	htdelete(handlerHashtab);

	unref(cutAtom);
	unref(undAtom);
	unref(failAtom);
	unref(nilAtom);
	memfree(isLeftAssoc);

	for(int i = 0; i < 256; i++) unref(intNodes[i]);
	memfree(intNodes);

	for(int i = 0; i < atomHashSize; i++) {
		Node *atom = atomHashes[i];
		if(atom) unref(atom);
	}

	memfree(atomHashes);
	nAtomHashes = 0;
	gcdeinit();
}

module(singlog, { singloginit_(); }, { singlogdeinit_(); })

#endif
