// cat src/main/js/parse.js | node src/main/js/parse.js
// parsing a javascript subset and inference variable types.
// able to parse myself.

// [a, b,] is an array.
// [a, b] is a tuple.

let assumeAny = Object.assign;

let assumeList = v => {
	let list = assumeAny(v);
	let first = list[0];
	return list;
};

let assumeObject = v => {
	let object = assumeAny(v);
	let { id } = object;
	return object;
};

let fake = Object.assign;

let ascii = s => s.charCodeAt(0);
let error = message => { throw new Error(message); };
let stringify = json => JSON.stringify(json, undefined, '  ');

let cons = (head, tail) => [head, ...tail,];
let head = list => list[0];
let isEmpty = list => list.length === 0;
let isNotEmpty = list => 0 < list.length;
let nil = [];
let tail = list => list.slice(1, undefined);

let getp = (m, k) => fake(m)[k !== '' && fake(k)];
let setp = (m, k, v) => { fake(m)[k !== '' && fake(k)] = v; return v; };

let contains;
contains = (es, e) => isNotEmpty(es) && (head(es) === e || contains(tail(es), e));

let fold;
fold = (init, es, op) => isNotEmpty(es) ? fold(op(init, head(es)), tail(es), op) : init;

let dump = v => {
	let dump_;
	dump_ = (vs, v) => false ? ''
		: contains(vs, v) ?
			'<recurse>'
		: v.id !== undefined ? function() {
			let join = Object
				.entries(v)
				.filter(([k, v_]) => k !== 'id')
				.map(([k, v_]) => `${k}:${dump_(cons(v, vs), v_)}`)
				.join(' ');
			return `${v.id}(${join})`;
		}()
		: v.toString();
	return dump_(nil, v);
};

let isAll = pred => s => {
	let isAll_;
	isAll_ = i => i < s.length ? pred(s.charCodeAt(i)) && isAll_(i + 1) : true;
	return isAll_(0);
};

let isIdentifier_ = isAll(ch => false
	|| ascii('0') <= ch && ch <= ascii('9')
	|| ascii('A') <= ch && ch <= ascii('Z')
	|| ch === ascii('_')
	|| ascii('a') <= ch && ch <= ascii('z'));

let isIdentifier = s => 0 < s.length && isIdentifier_(s);

let isQuote = ch => ch === ascii("'") || ch === ascii('"') || ch === ascii('`');

let quoteBracket = (qb, ch) => {
	let qb0 = head(qb);

	return false ? nil
	: ch === ascii('{') && qb0 === ascii('`') ? cons(ch, qb)
	: ch === ascii('}') && qb0 === ascii('`') ? cons(ch, qb)
	: isQuote(qb0) ? (qb0 === ch ? tail(qb) : qb)
	: isQuote(ch) ? cons(ch, qb)
	: ch === ascii('(') ? (qb0 === ascii(')') ? tail(qb) : cons(ch, qb))
	: ch === ascii(')') ? (qb0 === ascii('(') ? tail(qb) : cons(ch, qb))
	: ch === ascii('[') ? (qb0 === ascii(']') ? tail(qb) : cons(ch, qb))
	: ch === ascii(']') ? (qb0 === ascii('[') ? tail(qb) : cons(ch, qb))
	: ch === ascii('{') ? (qb0 === ascii('}') ? tail(qb) : cons(ch, qb))
	: ch === ascii('}') ? (qb0 === ascii('{') ? tail(qb) : cons(ch, qb))
	: qb;
};

let splitl = (s, sep) => {
	let i = 0;
	let j;
	let qb = nil;
	let qb1;

	while (function() {
		j = i + sep.length;
		return j <= s.length && function() {
			let ch = s.charCodeAt(i);
			qb1 = quoteBracket(qb, ch);
			return isNotEmpty(qb) || s.slice(i, j) !== sep || i === 0;
		}();
	}()) (function() {
		i = i + 1;
		qb = qb1;
		return true;
	}());

	return j <= s.length ? [s.slice(0, i), s.slice(j, undefined)] : [s, undefined];
};

let splitr = (s, sep) => {
	let i;
	let j = s.length;
	let qb = nil;
	let qb1;

	while (function() {
		i = j - sep.length;
		return 0 <= i && function() {
			let ch = s.charCodeAt(j - 1);
			qb1 = quoteBracket(qb, ch);
			return isNotEmpty(qb1) || s.slice(i, j) !== sep || i === 0;
		}();
	}()) (function() {
		j = j - 1;
		qb = qb1;
		return true;
	}());

	return 0 <= i ? [s.slice(0, i), s.slice(j, undefined)] : [undefined, s];
};

let keepsplitl = (s, sep, apply) => {
	let keepsplitl_;
	keepsplitl_ = input => input !== '' ? function() {
		let [left, right] = splitl(input, sep);
		return cons(apply(left), keepsplitl_(right));
	}() : nil;
	return keepsplitl_(s);
};

let parseAssocLeft_ = (id, op, parseValue) => {
	let parseAssocLeft__;
	parseAssocLeft__ = program_ => {
		let program = program_.trim();
		let [left, right] = splitr(program, op);
		let rhs = parseValue(right);
		return left === undefined ? rhs : { id, lhs: parseAssocLeft__(left), rhs };
	};
	return parseAssocLeft__;
};

let parseAssocRight = (id, op, parseValue) => {
	let parseAssocRight_;
	parseAssocRight_ = program_ => {
		let program = program_.trim();
		let [left, right] = splitl(program, op);
		let lhs = parseValue(left);
		return right === undefined ? lhs : { id, lhs, rhs: parseAssocRight_(right) };
	};
	return parseAssocRight_;
};

let parsePrefix = (id, op, parseValue) => {
	let parsePrefix_;
	parsePrefix_ = program_ => {
		let program = program_.trim();
		return !program.startsWith(op)
			? parseValue(program)
			: { id, expr: parsePrefix_(program.slice(op.length, undefined)) };
	};
	return parsePrefix_;
};

let parseNumber = program => {
	let parseNumber_;
	parseNumber_ = i => 0 <= i ? function() {
		let ch = program.charCodeAt(i);
		return ascii('0') <= ch && ch <= ascii('9')
			? parseNumber_(i - 1) * 10 + ch - ascii('0')
			: error(`invalid number ${program}`);
	}() : 0;
	return parseNumber_(program.length - 1);
};

let parseApplyBlockFieldIndex;

let parseBackquote;

parseBackquote = program => {
	let index = program.indexOf('${', 0);

	return 0 <= index ? function() {
		let remains = program.slice(index + 2, undefined);
		let [expr_, right] = splitl(remains, '}');

		let exprToString = {
			id: 'apply',
			arg: { id: 'never' },
			expr: { id: 'dot', field: '.toString', expr: parseApplyBlockFieldIndex(expr_) },
		};

		return {
			id: 'add',
			lhs: { id: 'string', value: program.slice(0, index) },
			rhs: { id: 'add', lhs: exprToString, rhs: parseBackquote(right) },
		};
	}() : { id: 'string', value: program };
};

let parseConstant = program => {
	let first = program.charCodeAt(0);

	return false ? {}
	: ascii('0') <= first && first <= ascii('9') ? { id: 'number', value: program, i: parseNumber(program) }
	: program.startsWith("'") && program.endsWith("'") ? { id: 'string', value: program.slice(1, -1) }
	: program.startsWith('"') && program.endsWith('"') ? { id: 'string', value: program.slice(1, -1) }
	: program.startsWith('`') && program.endsWith('`') ? parseBackquote(program.slice(1, -1))
	: program === 'false' ? { id: 'boolean', value: 'false' }
	: program === 'new Error' ? { id: 'new-error' }
	: program === 'new Map' ? { id: 'new-map' }
	: program === 'new Promise' ? { id: 'new-promise' }
	: program === 'nil' ? { id: 'nil' }
	: program === 'true' ? { id: 'boolean', value: 'true' }
	: program === 'undefined' ? { id: 'undefined' }
	: isIdentifier(program) ? { id: 'var', value: program }
	: error(`cannot parse "${program}"`);
};

let parseArray = (program, parse) => {
	let parseArray_;
	parseArray_ = program_ => {
		let program = program_.trim();

		return program !== '' ? function() {
			let [head, tail_] = splitl(program, ',');
			let tail = tail_.trim();
			return {
				id: 'cons',
				head: parse(head),
				tail: tail.startsWith('...') && tail.endsWith(',') ? parse(tail.slice(3, -1)) : parseArray_(tail)
			};
		}()
		: { id: 'nil' };
	};
	return parseArray_(program);
};

let parseTuple = (program, parse) => ({
	id: 'tuple',
	values: keepsplitl(program + ',', ',', parse),
});

let parseArrayTuple = (program_, parse) => {
	let program = program_.slice(1, -1).trim();
	return (program === '' || program.endsWith(',') ? parseArray : parseTuple)(program, parse);
};

let parseStructInner = (program, parse) => {
	let appendTrailingComma = s => s + (s === '' || s.endsWith(',') ? '' : ',');

	return {
		id: 'struct',
		kvs: keepsplitl(appendTrailingComma(program), ',', kv => {
			let [key_, value_] = splitl(kv, ':');
			let field = parseConstant(key_.trim()).value;
			let value = value_ !== undefined ? parse(value_) : { id: 'var', value: field };
			return { key: '.' + field, value };
		}),
	};
};

let parseStruct = (program, parse) => parseStructInner(program.slice(1, -1).trim(), parse);

let parseProgram;

let parseValue;

parseValue = program_ => {
	let program = program_.trim();

	return false ? {}
	: program.startsWith('try {') && program.endsWith('}') ? function() {
		let [try_, catch_] = splitl(program.slice(4, undefined), 'catch (e)');
		return {
			id: 'try',
			expr: parseProgram(try_),
			catch_: { id: 'lambda', bind: { id: 'var', value: 'e' }, expr: parseProgram(catch_) }
		};
	}()
	: program.startsWith('typeof ') ?
		{ id: 'typeof', expr: parseValue(program.slice(7, undefined)) }
	: program.startsWith('(') && program.endsWith(')') ?
		parseProgram(program.slice(1, -1))
	: program.startsWith('[') && program.endsWith(']') ?
		parseArrayTuple(program, parseProgram)
	: program.startsWith('{') && program.endsWith('}') ? function() {
		let block = program.slice(1, -1).trim();
		return block.endsWith(';') ? parseProgram(block) : parseStructInner(block, parseProgram);
	}()
	: parseConstant(program);
};

let parseLvalue = program_ => {
	let program = program_.trim();
	let [expr, field] = splitr(program, '.');

	return false ? {}
	: expr !== undefined && isIdentifier(field) ?
		{ id: 'dot', field: '.' + field, expr: parseApplyBlockFieldIndex(expr) }
	: program.endsWith(']') ? function() {
		let [expr, index_] = splitr(program, '[');
		let index = index_.slice(0, -1);
		return expr === undefined ? parseValue(program)
		: index === '0' ? {
			id: 'element',
			expr: parseProgram(expr),
			index,
		}
		: {
			id: 'index',
			expr: parseProgram(expr),
			index: parseProgram(index),
		};
	}()
	: parseValue(program);
};

parseApplyBlockFieldIndex = program_ => {
	let program = program_.trim();

	return false ? {}
	: program.startsWith('function() {') && program.endsWith('}()') ?
		parseProgram(program.slice(12, -3).trim())
	: program.endsWith('()') ? {
		id: 'apply',
		expr: parseProgram(program.slice(0, -2)),
		arg: { id: 'never' },
	}
	: program.endsWith(')') ? function() {
		let [expr, paramStr_] = splitr(program, '(');
		let paramStr = paramStr_.slice(0, -1).trim();
		return expr !== undefined ? {
			id: 'apply',
			expr: parseProgram(expr),
			arg: parseProgram(paramStr),
		} : parseValue(program);
	}()
	: parseLvalue(program);
};

let parseIf = [parseApplyBlockFieldIndex,]
	.map(p => parsePrefix('await', 'await ', p))
	.map(p => parseAssocLeft_('div', '/', p))
	.map(p => parseAssocRight('mul', '*', p))
	.map(p => parsePrefix('neg', '-', p))
	.map(p => parseAssocLeft_('sub', '-', p))
	.map(p => parsePrefix('pos', '+', p))
	.map(p => parseAssocRight('add', '+', p))
	.map(p => parseAssocRight('lt_', '<', p))
	.map(p => parseAssocRight('le_', '<=', p))
	.map(p => parsePrefix('not', '!', p))
	.map(p => parseAssocRight('ne_', '!==', p))
	.map(p => parseAssocRight('eq_', '===', p))
	.map(p => parseAssocRight('and', '&&', p))
	.map(p => parseAssocRight('or_', '||', p))
	.map(p => parseAssocLeft_('app', '|>', p))
	.map(p => program => {
		let [if_, thenElse] = splitl(program, '?');

		return thenElse === undefined ? p(if_) : function() {
			let [then, else_] = splitl(thenElse, ':');

			return {
				id: 'if',
				if_: parseProgram(if_),
				then: parseProgram(then),
				else_: parseProgram(else_),
			};
		}();
	})
	[0];

let parsePair = (program, parse) => {
	let parsePair_;
	parsePair_ = program => {
		let [left, right] = splitl(program, ',');
		let lhs = parse(left.trim());
		return right === undefined ? lhs : { id: 'pair', lhs, rhs: parsePair_(right) };
	};
	return parsePair_(program);
};

let parseBind;

parseBind = program_ => {
	let program = program_.trim();

	return false ? {}
	: program === '()' ?
		{ id: 'never' }
	: program.startsWith('(') && program.endsWith(')') ?
		parseBind(program.slice(1, -1))
	: program.startsWith('[') && program.endsWith(']') ?
		parseArrayTuple(program, parseBind)
	: program.startsWith('{') && program.endsWith('}') ?
		parseStruct(program, parseBind)
	:
		parsePair(program, parseConstant);
};

let parseLambda = program => {
	let [left, right] = splitl(program, '=>');

	return right === undefined ?
		parseIf(left)
	: left.startsWith('async ') ? {
		id: 'lambda-async',
		bind: parseBind(left.slice(6, undefined)),
		expr: parseProgram(right.trim()),
	}
	: {
		id: 'lambda',
		bind: parseBind(left),
		expr: parseProgram(right.trim()),
	};
};

let dummyCount = 0;

let newDummy = () => {
	dummyCount = dummyCount + 1;
	return `dummy${dummyCount}`;
};

parseProgram = program => {
	let [statement_, expr_] = splitl(program, ';');

	return expr_ === undefined ? parsePair(statement_, parseLambda) : function() {
		let statement = statement_.trim();
		let expr = expr_.trim();

		return false ? {}
		: statement.startsWith('let ') ? function() {
			let [var_, value] = splitl(statement.slice(4, undefined), '=');
			let v = var_.trim();

			return value !== undefined ? {
				id: 'let',
				bind: parseBind(var_),
				value: parseProgram(value),
				expr: parseProgram(expr),
			}
			: isIdentifier(var_) ? {
				id: 'alloc',
				v,
				expr: parseProgram(expr),
			}
			: error(`cannot parse let variable "${v}"`);
		}()
		: statement.startsWith('return ') && expr === '' ?
			parseProgram(statement.slice(7, undefined))
		: statement.startsWith('throw ') && expr === '' ?
			{ id: 'throw', expr: parseProgram(statement.slice(6, undefined)) }
		: statement.startsWith('while ') ? function() {
			let [cond, loop] = splitl(statement.slice(6, undefined), ' ');
			return {
				id: 'while',
				cond: parseProgram(cond),
				loop: parseProgram(loop),
				expr: parseProgram(expr),
			};
		}()
		: function() {
			let [lhs, rhs] = splitl(statement, '=');

			return rhs !== undefined
			? {
				id: 'assign',
				var_: parseLvalue(lhs),
				value: parseProgram(rhs),
				expr: parseProgram(expr),
			}
			: {
				id: 'let',
				bind: { id: 'var', value: newDummy() },
				value: parseProgram(lhs),
				expr: parseProgram(expr),
			};
		}();
	}();
};

let format;

format = ast => {
	let id = ast.id;

	let f = false ? (({}) => '')
	: id === 'add' ? (({ lhs, rhs }) => `${format(lhs)} + ${format(rhs)}`)
	: id === 'alloc' ? (({ v, expr }) => error('FIXME'))
	: id === 'and' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'app' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'apply' ? (({ arg, expr }) => error('FIXME'))
	: id === 'array' ? (({ values }) => error('FIXME'))
	: id === 'assign' ? (({ var_, value, expr }) => error('FIXME'))
	: id === 'await' ? (({ expr }) => error('FIXME'))
	: id === 'boolean' ? (({ value }) => error('FIXME'))
	: id === 'cons' ? (({ head, tail }) => error('FIXME'))
	: id === 'div' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'dot' ? (({ field, expr }) => error('FIXME'))
	: id === 'element' ? (({ index, expr }) => error('FIXME'))
	: id === 'eq_' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'if' ? (({ if_, then, else_ }) => error('FIXME'))
	: id === 'index' ? (({ index, expr }) => error('FIXME'))
	: id === 'lambda-async' ? (({ bind, expr }) => error('FIXME'))
	: id === 'lambda' ? (({ bind, expr }) => error('FIXME'))
	: id === 'le_' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'let' ? (({ bind, value, expr }) => error('FIXME'))
	: id === 'lt_' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'mul' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'ne_' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'neg' ? (({ expr }) => error('FIXME'))
	: id === 'never' ? (({}) => error('FIXME'))
	: id === 'new-error' ? (({}) => error('FIXME'))
	: id === 'new-map' ? (({}) => error('FIXME'))
	: id === 'new-promise' ? (({}) => error('FIXME'))
	: id === 'nil' ? (({}) => error('FIXME'))
	: id === 'not' ? (({ expr }) => error('FIXME'))
	: id === 'number' ? ((i) => `${i}`)
	: id === 'or_' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'pair' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'pos' ? (({ expr }) => error('FIXME'))
	: id === 'string' ? (({ value }) => `'${value}'`)
	: id === 'struct' ? (({ kvs }) => error('FIXME'))
	: id === 'sub' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'throw' ? (({ expr }) => error('FIXME'))
	: id === 'try' ? (({ expr, catch_ }) => error('FIXME'))
	: id === 'tuple' ? (({ values }) => error('FIXME'))
	: id === 'typeof' ? (({ expr }) => error('FIXME'))
	: id === 'undefined' ? (({}) => `${id}`)
	: id === 'var' ? (({ value }) => error('FIXME'))
	: id === 'while' ? (({ cond, loop, expr }) => error('FIXME'))
	: error(`cannot format ${id}`);

	return `(${f(ast)})`;
};

let refs = new Map();
let refCount;

refCount = 0;

let finalRef;

finalRef = v => {
	let ref = v.ref;
	return ref !== undefined && refs.get(ref) !== v ? finalRef(refs.get(ref)) : v;
};

let setRef = (ref, target) => {
	refs.set(ref, target);
	return true;
};

let newRef = () => {
	refCount = refCount + 1;
	let ref = { ref: refCount };
	refs.set(refCount, ref);
	return ref;
};

let dumpRef = v => {
	let dumpRef_;
	dumpRef_ = (vs, v) => {
		let { ref } = v;
		let listv = assumeList(v);
		return false ? ''
		: contains(vs, v) ?
			'<recurse>'
		: ref !== undefined ?
			(refs.get(ref) !== v ? dumpRef_(cons(v, vs), refs.get(ref)) : `_${ref}`)
		: typeof v === 'object' ? (false ? ''
			: isEmpty(listv) ?
				''
			: isNotEmpty(listv) ?
				`${dumpRef_(vs, head(listv))}:${dumpRef_(vs, assumeObject(tail(listv)))}`
			: function() {
				let id = v.id;
				let join = Object
					.entries(v)
					.filter(([k, v_]) => k !== 'id')
					.map(([k, v_]) => `${k}:${dumpRef_(cons(v, vs), v_)}`)
					.join(' ');
				return id !== undefined ? `${id}(${join})` : `{${join}}`;
			}()
		)
		: typeof v === 'string' ?
			v.toString()
		: JSON.stringify(v, undefined, undefined);
	};
	return dumpRef_(nil, v);
};

let tryBind;

tryBind = (a, b) => function() {
	let lista = assumeList(a);
	let listb = assumeList(b);
	let refa = a.ref;
	let refb = b.ref;

	return false ? false
	: a === b ?
		true
	: refa !== undefined ? function() {
		let olda = refs.get(refa);
		let finalb = finalRef(b);
		return setRef(refa, finalb) && tryBind(olda, finalb) || !setRef(refa, olda);
	}()
	: refb !== undefined ? function() {
		let oldb = refs.get(refb);
		let finala = finalRef(a);
		return setRef(refb, finala) && tryBind(finala, oldb) || !setRef(refb, oldb);
	}()
	: typeof a === 'object' && typeof b === 'object'
		&& (lista.length !== undefined
		? lista.length === listb.length && function() {
			let tryBindList;
			tryBindList = index => index === lista.length || tryBind(lista[index], listb[index]) && tryBindList(index + 1);
			return tryBindList(0);
		}()
		: true
			&& Object.keys(a).reduce((r, k) => {
				let b_k = getp(b, k);
				let s = b_k !== undefined || b.completed !== true && function() { b_k = newRef(); setp(b, k, b_k); return true; }();
				return r && s && tryBind(getp(a, k), b_k);
			}, true)
			&& Object.keys(b).reduce((r, k) => {
				let a_k = getp(a, k);
				let s = a_k !== undefined || a.completed !== true && function() { a_k = newRef(); setp(a, k, a_k); return true; }();
				return r && s && tryBind(a_k, getp(b, k));
			}, true)
		);
}();

let doBind_ = (msg, a, b) => tryBind(a, b) || error(`in ${msg()}:\ncannot bind types between\nfr: ${dumpRef(a)}\nto: ${dumpRef(b)}`);
let doBind = (ast, a, b) => doBind_(() => dump(ast), a, b);

let cloneRef = v => {
	let fromTos = new Map();
	let cloneRef_;

	cloneRef_ = v => {
		let { ref } = v;
		let vlist = assumeList(v);
		return false ? {}
		: ref !== undefined
			? (fromTos.has(ref) ? fromTos.get(ref) : function() {
				let v1 = newRef();
				fromTos.set(ref, v1);
				return doBind_(() => 'clone reference', v1, cloneRef_(refs.get(ref))) && v1;
			}())
		: typeof v === 'object'
			? (vlist.length !== undefined
				? assumeObject(vlist.map(cloneRef_))
				: Object.fromEntries(Object.entries(v).map(([k, v_]) => [k, cloneRef_(v_)]))
			)
		:
			v;
	};

	return cloneRef_(v);
};

let lookup = (vts, v) => {
	let lookup_;
	lookup_ = vts => isNotEmpty(vts) ? function() {
		let [v_, t] = head(vts);
		return v_ === v ? t : lookup_(tail(vts));
	}() : error(`undefined variable ${v}`);
	return lookup_(vts);
};

let defineBindTypes;

defineBindTypes = (vts, ast) => false ? vts
	: ast.id === 'array' ? fold(vts, ast.values, defineBindTypes)
	: ast.id === 'never' ? vts
	: ast.id === 'nil' ? vts
	: ast.id === 'pair' ? defineBindTypes(defineBindTypes(vts, ast.lhs), ast.rhs)
	: ast.id === 'struct' ? fold(vts, ast.kvs, (vts_, kv) => defineBindTypes(vts_, kv.value))
	: ast.id === 'tuple' ? fold(vts, ast.values, defineBindTypes)
	: ast.id === 'var' ? cons([ast.value, newRef()], vts)
	: error(`cannot destructure ${dump(ast)}`);

let typeArrayOf = type => ({ id: 'array', of: type });
let typeBoolean = ({ id: 'boolean' });
let typeError = ({ id: 'error' });
let typeLambdaOf = (in_, out) => ({ id: 'lambda', generic: true, in_, out });
let typeLambdaOfFixed = (in_, out) => ({ id: 'lambda', in_, out });
let typeNever = { id: 'never' };
let typeNumber = ({ id: 'number' });
let typePairOf = (lhs, rhs) => ({ id: 'pair', lhs, rhs });
let typePromiseOf = out => ({ id: 'promise', out });
let typeString = typeArrayOf({ id: 'char' });
let typeStructOf = kvs => ({ id: 'struct', kvs });
let typeStructOfCompleted = kvs => { setp(kvs, 'completed', true); return ({ id: 'struct', kvs }); };
let typeTupleOf = types => ({ id: 'tuple', types });
let typeVoid = typeStructOfCompleted({});

let typeMapOf = (tk, tv) => typeStructOfCompleted({
	'.get': typeLambdaOfFixed(tk, tv),
	'.has': typeLambdaOfFixed(tk, typeBoolean),
	'.set': typeLambdaOfFixed(typePairOf(tk, tv), typeNever),
});

let inferDot = (ast, field, ts) => {
	return false ? {}
	: field === '.charCodeAt' ?
		doBind(ast, ts, typeString) && typeLambdaOf(typeNumber, typeNumber)
	: field === '.endsWith' ?
		doBind(ast, ts, typeString) && typeLambdaOf(typeString, typeBoolean)
	: field === '.filter' ? function() {
		let ti = newRef();
		return doBind(ast, ts, typeArrayOf(ti)) && typeLambdaOf(typeLambdaOf(ti, typeBoolean), typeArrayOf(ti));
	}()
	: field === '.indexOf' ?
		doBind(ast, ts, typeString) && typeLambdaOf(typePairOf(typeString, typeNumber), typeNumber)
	: field === '.join' ?
		doBind(ast, ts, typeArrayOf(typeString)) && typeLambdaOf(typeString, typeString)
	: field === '.length' ?
		doBind(ast, ts, typeArrayOf(newRef())) && typeNumber
	: field === '.map' ? function() {
		let ti = newRef();
		let to = newRef();
		return doBind(ast, ts, typeArrayOf(ti)) && typeLambdaOf(typeLambdaOf(ti, to), typeArrayOf(to));
	}()
	: field === '.reduce' ? function() {
		let te = newRef();
		let tr = newRef();
		let treducer = typeLambdaOf(typePairOf(tr, te), tr);
		return doBind(ast, ts, typeArrayOf(te))
			&& typeLambdaOf(typePairOf(treducer, tr), tr);
	}()
	: field === '.slice' ? function() {
		let te = newRef();
		let tl = typeArrayOf(te);
		return doBind(ast, ts, tl) && typeLambdaOf(typePairOf(typeNumber, typeNumber), tl);
	}()
	: field === '.startsWith' ?
		doBind(ast, ts, typeString) && typeLambdaOf(typeString, typeBoolean)
	: field === '.then' ? function() {
		let ti = newRef();
		let to = newRef();
		return doBind(ast, ts, typePromiseOf(ti)) && typeLambdaOf(ti, typePromiseOf(to));
	}()
	: field === '.toString' ?
		doBind(ast, ts, newRef()) && typeLambdaOf(typeNever, typeString)
	: field === '.trim' ?
		doBind(ast, ts, typeString) && typeLambdaOf(typeNever, typeString)
	: function() {
		let kvs = {};
		let tr = setp(kvs, field, newRef());
		let to = typeStructOf(kvs);
		return doBind(ast, ts, to) && function() {
			let t = finalRef(tr);
			return t.generic !== true ? t : cloneRef(t);
		}();
	}();
};

let inferType;

inferType = (vts, isAsync, ast) => {
	let id = ast.id;

	let infer = ast_ => inferType(vts, isAsync, ast_);

	let inferCmpOp = ({ lhs, rhs }) => function() {
		let t = newRef();
		return true
			&& doBind(ast, infer(lhs), t)
			&& doBind(ast, infer(rhs), t)
			&& (tryBind(t, typeNumber) || tryBind(t, typeString) || error(`cannot compare values with type ${t}`))
			&& typeBoolean;
	}();

	let inferEqOp = ({ lhs, rhs }) => true
		&& doBind(ast, infer(lhs), infer(rhs))
		&& typeBoolean;

	let inferLogicalOp = ({ lhs, rhs }) => true
		&& doBind(ast, infer(lhs), typeBoolean)
		&& infer(rhs);

	let inferMathOp = ({ lhs, rhs }) => true
		&& doBind(ast, infer(lhs), typeNumber)
		&& doBind(ast, infer(rhs), typeNumber)
		&& typeNumber;

	let f = false ? (({}) => {})
	: id === 'add' ? (({ lhs, rhs }) => {
		let t = newRef();
		return true
			&& doBind(ast, infer(lhs), t)
			&& doBind(ast, infer(rhs), t)
			&& (tryBind(t, typeNumber) || tryBind(t, typeString) || error(`cannot add values with type ${dumpRef(t)}`))
			&& t;
	})
	: id === 'alloc' ? (({ v, expr }) =>
		inferType(cons([v, newRef()], vts), isAsync, expr)
	)
	: id === 'and' ?
		inferLogicalOp
	: id === 'app' ? (({ lhs, rhs }) => {
		let te = infer(lhs);
		let tp = infer(rhs);
		let tr = newRef();
		return doBind(ast, te, typeLambdaOf(tp, tr)) && tr;
	})
	: id === 'apply' ? (({ arg, expr }) => {
		let te = infer(expr);
		let tp = infer(arg);
		let tr = newRef();
		return doBind(ast, te, typeLambdaOf(tp, tr)) && tr;
	})
	: id === 'array' ? (({ values }) => {
		let te = newRef();
		return fold(true, values, (b, value) => b && doBind(ast, infer(value), te)) && typeArrayOf(te);
	})
	: id === 'assign' ? (({ var_, value, expr }) => function() {
		try {
			let tvar = infer(var_);
			let tvalue = infer(value);
			return doBind({ id: 'assign', var_, value }, tvar, tvalue);
		} catch (e) {
			e.message = `in assignment clause of ${dump(var_)}\n${e.message}`;
			throw e;
		}
	}() && infer(expr))
	: id === 'await' ? (({ expr }) => {
		let t = newRef();
		return isAsync ? doBind(ast, infer(expr), typePromiseOf(t)) && t : error(`await not inside async`);
	})
	: id === 'boolean' ? (({}) =>
		typeBoolean
	)
	: id === 'cons' ? (({ head, tail }) => {
		let tl = typeArrayOf(infer(head));
		return doBind(ast, infer(tail), tl) && tl;
	})
	: id === 'div' ?
		inferMathOp
	: id === 'dot' ? (({ field, expr }) =>
		inferDot(ast, field, infer(expr))
	)
	: id === 'element' ? (({ index, expr }) => {
		let te = newRef();
		let tl = typeArrayOf(te);
		return doBind(ast, infer(expr), tl) && (index === '0' ? te : {});
	})
	: id === 'eq_' ?
		inferEqOp
	: id === 'if' ? (({ if_, then, else_ }) => {
		let tt = function() {
			try {
				return infer(then);
			} catch (e) {
				e.message = `in then clause of ${dump(if_)}\n${e.message}`;
				throw e;
			}
		}();

		let te = infer(else_);
		return doBind(ast, infer(if_), typeBoolean) && doBind(ast, tt, te) && tt;
	})
	: id === 'index' ? (({ index, expr }) => {
		let t = newRef();
		return true
			&& doBind(ast, infer(index), typeNumber)
			&& doBind(ast, infer(expr), typeArrayOf(t))
			&& t;
	})
	: id === 'lambda' ? (({ bind, expr }) => {
		let vts1 = defineBindTypes(vts, bind);
		let tb = inferType(vts1, false, bind);
		let te = inferType(vts1, false, expr);
		return typeLambdaOf(tb, te);
	})
	: id === 'lambda-async' ? (({ bind, expr }) => {
		let vts1 = defineBindTypes(vts, bind);
		let tb = inferType(vts1, false, bind);
		let te = inferType(vts1, true, expr);
		return typeLambdaOf(tb, typePromiseOf(te));
	})
	: id === 'le_' ?
		inferCmpOp
	: id === 'let' ? (({ bind, value, expr }) => {
		let vts1 = defineBindTypes(vts, bind);
		return function() {
			try {
				let tb = inferType(vts1, false, bind);
				let tv = infer(value);
				return doBind({ id: 'let', bind, value }, tb, tv);
			} catch (e) {
				e.message = `in value clause of ${dump(bind)}\n${e.message}`;
				throw e;
			}
		}() && inferType(vts1, isAsync, expr);
	})
	: id === 'lt_' ?
		inferCmpOp
	: id === 'mul' ?
		inferMathOp
	: id === 'ne_' ?
		inferEqOp
	: id === 'neg' ? (({ expr }) =>
		doBind(ast, infer(expr), typeNumber) && typeNumber
	)
	: id === 'never' ? (({}) =>
		typeNever
	)
	: id === 'new-error' ? (({}) =>
		typeLambdaOf(typeString, typeError)
	)
	: id === 'new-map' ? (({}) =>
		typeLambdaOf(typeNever, typeMapOf(newRef(), newRef()))
	)
	: id === 'new-promise' ? (({}) => {
		let tr = newRef();
		let tres = typeLambdaOf(tr, typeNever);
		let trej = typeLambdaOf(typeError, typeNever);
		return typeLambdaOf(typeLambdaOf(typePairOf(tres, trej), typeVoid), typePromiseOf(tr));
	})
	: id === 'nil' ? (({}) =>
		typeArrayOf(newRef())
	)
	: id === 'not' ? (({ expr }) =>
		doBind(ast, infer(expr), typeBoolean) && typeBoolean
	)
	: id === 'number' ? (({}) =>
		typeNumber
	)
	: id === 'or_' ?
		inferLogicalOp
	: id === 'pair' ? (({ lhs, rhs }) =>
		typePairOf(infer(lhs), infer(rhs))
	)
	: id === 'pos' ? (({ expr }) =>
		doBind(ast, infer(expr), typeNumber) && typeNumber
	)
	: id === 'string' ? (({}) =>
		typeString
	)
	: id === 'struct' ? (({ kvs }) => {
		let inferKvs;
		inferKvs = kvs => 0 < kvs.length ? function() {
			let { key, value } = head(kvs);
			let type = inferKvs(tail(kvs));
			setp(type, key, function() {
				try {
					return infer(value);
				} catch (e) {
					e.message = `in field ${key}\n${e.message}`;
					throw e;
				}
			}());
			return type;
		}() : {};
		return typeStructOf(inferKvs(kvs));
	})
	: id === 'sub' ?
		inferMathOp
	: id === 'throw' ? (({}) =>
		newRef()
	)
	: id === 'try' ? (({ expr, catch_ }) =>
		doBind(ast, infer(catch_), newRef()) && infer(expr)
	)
	: id === 'tuple' ? (({ values }) => {
		let inferValues;
		inferValues = vs => isNotEmpty(vs) ? cons(infer(head(vs)), inferValues(tail(vs))) : nil;
		return typeTupleOf(inferValues(values));
	})
	: id === 'typeof' ? (({}) =>
		typeString
	)
	: id === 'undefined' ? (({}) =>
		newRef()
	)
	: id === 'var' ? (({ value }) => {
		let t = finalRef(lookup(vts, value));
		return t.generic !== true ? t : cloneRef(t);
	})
	: id === 'while' ? (({ cond, loop, expr }) => {
		doBind(ast, infer(cond), typeBoolean);
		doBind(ast, infer(loop), newRef());
		return infer(expr);
	})
	: (({}) =>
		error(`cannot infer type for ${id}`)
	);

	return f(ast);
};

let typeConsole = typeStructOfCompleted({
	'.error': typeLambdaOf(newRef(), typeNever),
	'.log': typeLambdaOf(newRef(), typeNever),
});

let typeJSON = typeStructOfCompleted({
	'.stringify': typeLambdaOf(typePairOf(newRef(), typePairOf(newRef(), newRef())), typeString),
});

let typeObject = typeStructOfCompleted({
	'.assign': typeLambdaOf(newRef(), newRef()),
	'.entries': typeLambdaOf(typeStructOf({}), typeArrayOf(typeTupleOf(cons(typeString, cons(newRef(), nil))))),
	'.fromEntries': typeLambdaOf(typeArrayOf(typeTupleOf(cons(typeString, cons(newRef(), nil)))), typeStructOf({})),
	'.keys': typeLambdaOf(typeStructOf({}), typeArrayOf(typeString)),
});

let typePromise = typeStructOfCompleted({
	'.reject': typeLambdaOf(typeError, typePromiseOf(newRef())),
	'.resolve': function() { let t = newRef(); return typeLambdaOf(t, typePromiseOf(t)); }(),
});

let typeRequire = typeLambdaOf(typeString, newRef());

let predefinedTypes = Object
	.entries({
		JSON: typeJSON,
		Object: typeObject,
		console: typeConsole,
		require: typeRequire,
	})
	.reduce((l, vt) => cons(vt, l), nil);

let generate;

generate = ast => {
	let id = ast.id;

	let f = false ? (({}) => error('FIXME'))
	: id === 'add' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'alloc' ? (({ v, expr }) => error('FIXME'))
	: id === 'and' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'app' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'apply' ? (({ arg, expr }) => error('FIXME'))
	: id === 'array' ? (({ values }) => error('FIXME'))
	: id === 'assign' ? (({ var_, value, expr }) => error('FIXME'))
	: id === 'await' ? (({ expr }) => error('FIXME'))
	: id === 'boolean' ? (({ value }) => error('FIXME'))
	: id === 'cons' ? (({ head, tail }) => error('FIXME'))
	: id === 'div' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'dot' ? (({ field, expr }) => error('FIXME'))
	: id === 'element' ? (({ index, expr }) => error('FIXME'))
	: id === 'eq_' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'if' ? (({ if_, then, else_ }) => error('FIXME'))
	: id === 'index' ? (({ index, expr }) => error('FIXME'))
	: id === 'lambda-async' ? (({ bind, expr }) => error('FIXME'))
	: id === 'lambda' ? (({ bind, expr }) => error('FIXME'))
	: id === 'le_' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'let' ? (({ bind, value, expr }) => error('FIXME'))
	: id === 'lt_' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'mul' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'ne_' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'neg' ? (({ expr }) => error('FIXME'))
	: id === 'never' ? (({}) => error('FIXME'))
	: id === 'new-error' ? (({}) => error('FIXME'))
	: id === 'new-map' ? (({}) => error('FIXME'))
	: id === 'new-promise' ? (({}) => error('FIXME'))
	: id === 'nil' ? (({}) => error('FIXME'))
	: id === 'not' ? (({ expr }) => error('FIXME'))
	: id === 'number' ? (({ value, i }) => error('FIXME'))
	: id === 'or_' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'pair' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'pos' ? (({ expr }) => error('FIXME'))
	: id === 'string' ? (({ value }) => error('FIXME'))
	: id === 'struct' ? (({ kvs }) => error('FIXME'))
	: id === 'sub' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'throw' ? (({ expr }) => error('FIXME'))
	: id === 'try' ? (({ expr, catch_ }) => error('FIXME'))
	: id === 'tuple' ? (({ values }) => error('FIXME'))
	: id === 'typeof' ? (({ expr }) => error('FIXME'))
	: id === 'undefined' ? (({}) => error('FIXME'))
	: id === 'var' ? (({ value }) => error('FIXME'))
	: id === 'while' ? (({ cond, loop, expr }) => error('FIXME'))
	: error(`cannot generate for ${id}`);

	return f(ast);
};

let rewrite = r => ast => {
	let rewrite_;
	rewrite_ = ast0 => ast0.id === undefined ? ast0 : function() {
		let ast1 = r(ast0.id)(ast0);
		return ast1 === undefined
			? Object.fromEntries(Object.entries(ast0).map(([k, v]) => [k, rewrite_(v)]))
			: ast1;
	}();
	return rewrite_(ast);
};

let process_ = program => {
	let ast = parseProgram(program);

	let type = inferType(predefinedTypes, false, ast);

	return { ast, type };
};

let process = program_ => {
	let program = program_;
	let pos0;
	let posx;
	while (function() {
		pos0 = program.indexOf('\/\/ ', 0);
		posx = 0 <= pos0 ? program.indexOf('\n', pos0) : -1;
		return 0 <= posx;
	}()) (function() {
		program = program.slice(0, pos0) + program.slice(posx, undefined);
		return true;
	}());
	return process_(program);
};

let actual = stringify(process(`
	let parse = ast => ast;
	console.log(parse(require('fs').readFileSync(0, 'utf8')))
`).ast);

let expect = stringify({
	id: 'let',
	bind: { id: 'var', value: 'parse' },
	value: {
		id: 'lambda',
		bind: { id: 'var', value: 'ast' },
		expr: { id: 'var', value: 'ast' },
	},
	expr: {
		id: 'apply',
		expr: {
			id: 'dot',
			field: '.log',
			expr: { id: 'var', value: 'console' },
		},
		arg: {
			id: 'apply',
			expr: { id: 'var', value: 'parse' },
			arg: {
				id: 'apply',
				expr: {
					id: 'dot',
					field: '.readFileSync',
					expr: {
						id: 'apply',
						expr: { id: 'var', value: 'require' },
						arg: { id: 'string', value: 'fs' },
					},
				},
				arg: {
					id: 'pair',
					lhs: { id: 'number', value: '0', i: 0 },
					rhs: { id: 'string', value: 'utf8' },
				},
			},
		},
	},
});

return actual === expect
? function() {
	try {
		let { ast, type } = process(require('fs').readFileSync(0, 'utf8'));
		console.log(`ast :: ${stringify(ast)}`);
		console.log(`type :: ${dumpRef(type)}`);
		return true;
	} catch (e) { return console.error(e); }
}() : error(`
test case failed,
actual = ${actual}
expect = ${expect}`);
