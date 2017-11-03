#ifndef iosource
#define iosource

#include "node.c"

Node **atomHashes;
int nAtomHashes;
int atomHashSize; // must be power of 2

Node **intNodes;

int getAtomHashPos_(char *start, char *end) {
	Node *atom;
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
	return getAtomHashPos_(name, name + strlen(name));
}

Node *getAtom_(char *start, char *end) {
	int i = getAtomHashPos_(start, end), j;

	if(!atomHashes[i]) {
		if(atomHashSize * 3 / 4 <= nAtomHashes) { // rehash if looks full
			Node **atomHashes0 = atomHashes;
			int atomHashSize0 = atomHashSize;

			atomHashSize <<= 1;
			atomHashes = memalloczeroed(atomHashSize * sizeof(Node*));

			for(j = 0; j < atomHashSize0; j++) {
				Node *atom = atomHashes0[j];
				if(atom) atomHashes[getAtomHashPos(atom->u.name)] = atom;
			}

			memfree(atomHashes0);
			i = getAtomHashPos_(start, end);
		}

		nAtomHashes++;
		atomHashes[i] = ref(newAtom(start, end));
	}

	return atomHashes[i];
}

Node *getAtom(char *name) {
	return getAtom_(name, name + strlen(name));
}

Node *getInt(int value) {
	if(-128 <= value && value < 128) return intNodes[value + 128];
	else return newInt(value);
}

int findOperatorIndex(char *operator) {
	int i;
	for(i = 0; i < nOperators; i++)
		if(strcmp(operators[i], operator) == 0) return i;
}

int fromHexDigit(char c) {
	if('a' < c) return c - 'a' + 10;
	else if('A' < c) return c - 'A' + 10;
	else if('0' < c) return c - '0';
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
		if(asString) normalChar = 32 <= c && c < 128 && c != '"';
		else normalChar = '0' <= c && c <= '9'
				|| 'A' <= c && c <= 'Z'
				|| 'a' <= c && c <= 'z'
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
			if(length1 <= i1) {
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
		while(0 <= --i1) s2[i1 + 1] = s1[i1];
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
			} else if(0 <= (v0 = fromHexDigit(d0))
					&& 0 <= (v1 = fromHexDigit(s[i0 + 1]))) {
				i0 += 2;
				s1[i1++] = (v0 << 4) + v1;
			}
		} else s1[i1++] = c;
	}

	s1[i1] = 0;
	memfree(s);
	return s1;
}

Node *parse_(char *start, char *end) {
	Node *node = 0;
	char *last = end - 1;
	int depth = 0, quote = 0, op;

	if(!node)
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
					while(start < s && isspace(s[-1])) s--; // trims space before/after
					while(p0 < end && isspace(*p0)) p0++;
					node = newTree(operator, parse_(start, s), parse_(p0, end));
					goto done;
				}

				if(!assoc) s++;
			}
		}

	if(!node)
		if(*start == '(' && *last == ')'
				|| *start == '[' && *last == ']'
				|| *start == '{' && *last == '}')
			node = parse_(start + 1, last);

	if(!node && *start == *last)
		if(*start == '\'') node = getAtom_(start + 1, last);
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

	if(!node) node = getAtom_(start, end);

done:
	return node;
}

Node *parse(char *s0) {
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

	Node *node = parse_(start, end);
	memfree(s1);
	return node;
}

char *dump(Node *node) {
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

#endif
