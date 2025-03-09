// (cd src/main/js && cat parse.js | node parse.js)
// parse a javascript subset and inference variable types.
// able to parse myself.

// a, b is a pair.
// [a, b,] is an array.
// [a, b] is a tuple.

let __id = x => x;
let assumeAny = __id;

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

let fake = __id;

let ascii = s => s.charCodeAt(0);
let stringify = json => JSON.stringify(json, undefined, '  ');

let error = message => { throw new Error(message); };
let get0 = tuple => { let [a, b] = tuple; return a; };
let get1 = tuple => { let [a, b] = tuple; return b; };
let seti = (m, k, v) => { (0 <= k); fake(m)[fake(k)] = v; return v; };
let getp = (m, k) => { (k !== ''); return fake(m)[fake(k)]; };
let setp = (m, k, v) => { (k !== ''); fake(m)[fake(k)] = v; return v; };

let ll = function() {
	let empty = () => undefined;

	let cons = (head, tail) => [head, tail];
	let head = get0;
	let isEmpty = list => list === empty();
	let isNotEmpty = list => list !== empty();
	let tail = get1;

	let contains;
	contains = (es, e) => isNotEmpty(es) ? function() {
		let [head, tail] = es;
		return e !== head ? contains(tail, e) : true;
	}() : false;

	let find;
	find = (es, op) => isNotEmpty(es) ? function() {
		let [head, tail] = es;
		return op(head) ? head : find(tail, op);
	}() : undefined;

	let foldl;
	foldl = (init, es, op) => isNotEmpty(es) ? foldl(op(init, head(es)), tail(es), op) : init;

	let foldr;
	foldr = (init, es, op) => isNotEmpty(es) ? op(foldr(init, tail(es), op), head(es)) : init;

	let findk = (kvs, k) => {
		let kv = find(kvs, ([k_, v]) => k_ === k);
		return kv !== undefined ? get1(kv) : error(`variable ${k} not found`);
	};

	let len;
	len = es => isNotEmpty(es) ? 1 + len(tail(es)) : 0;

	let map_;
	map_ = (es, op) => isNotEmpty(es) ? cons(op(head(es)), map_(tail(es), op)) : empty();

	return { cons, contains, empty, find, findk, foldl, foldr, head, isEmpty, isNotEmpty, len, map_, tail, };
}();

let vec = function() {
	let empty = [];

	let cons = (head, tail) => [head, ...tail,];

	let contains = (es, e) => {
		let b = false;
		for (let i = 0; i < es.length; i = i + 1) {
			b = b || es[i] === e;
		};
		return b;
	};

	let find = (es, op) => {
		let r = undefined;
		for (let i = 0; r === undefined && i < es.length; i = i + 1) {
			let e = es[i];
			r = op(e) ? e : undefined;
		};
		return r;
	};

	let foldl = (init, es, op) => {
		let r = init;
		for (let i = 0; i < es.length; i = i + 1) {
			r = op(r, es[i]);
		};
		return r;
	};

	let foldr = (init, es, op) => {
		let r = init;
		for (let i = es.length - 1; 0 <= i; i = i - 1) {
			r = op(r, es[i]);
		};
		return r;
	};

	return { cons, contains, empty, find, foldl, foldr, };
}();

let argv = process.argv.slice(2, undefined);

let env = JSON.parse(JSON.stringify(process.env, undefined, undefined));

while (argv[0] !== undefined && argv[0].startsWith('--')) {
	setp(env, argv[0].slice(2, undefined), 'Y');
	argv = argv.slice(1, undefined);
};

let arg = argv[0];

let gen = i => {
	let array = [];
	while (0 < i) {
		i = i - 1;
		array.push(i);
	};
	return array;
};

let dummyCount = 0;

let newDummy = () => {
	dummyCount = dummyCount + 1;
	return `d${dummyCount}`;
};

let _add = (lhs, rhs) => ({ id: 'add', lhs, rhs });
let _alloc = (vn, expr) => ({ id: 'alloc', vn, expr });
let _app = (lhs, rhs) => ({ id: 'app', lhs, rhs });
let _assign = (bind, value, expr) => ({ id: 'assign', bind, value, expr });
let _bool = b => ({ id: 'bool', b });
let _cons = (lhs, rhs) => ({ id: 'cons', lhs, rhs });
let _deref = expr => ({ id: 'deref', expr });
let _dot = (expr, field) => ({ id: 'dot', expr, field });
let _eq = (lhs, rhs) => ({ id: 'eq_', lhs, rhs });
let _error = { id: 'new', clazz: 'Error' };
let _fmt = expr => ({ id: 'fmt', expr });
let _frame = (fs, ps, vn) => ({ id: 'frame', fs, ps, vn });
let _if = (if_, then, else_) => ({ id: 'if', if_, then, else_ });
let _index = (lhs, rhs) => ({ id: 'index', lhs, rhs });
let _lambda = (bind, expr) => ({ id: 'lambda', bind, expr });
let _lambdaAsync = (bind, expr) => ({ id: 'lambda-async', bind, expr });
let _lambdaCapture = (capture, bindCapture, bind, expr) => ({ id: 'lambda-capture', capture, bindCapture, bind, expr });
let _let = (bind, value, expr) => ({ id: 'let', bind, value, expr });
let _nil = { id: 'nil' };
let _not = expr => ({ id: 'not', expr });
let _num = i => ({ id: 'num', i });
let _pair = (lhs, rhs) => ({ id: 'pair', lhs, rhs });
let _pget = (expr, i) => ({ id: 'pget', expr, i });
let _ref = expr => ({ id: 'ref', expr });
let _segment = opcodes => ({ id: 'segment', opcodes });
let _str = v => ({ id: 'str', v });
let _struct = kvs => ({ id: 'struct', kvs });
let _tget = (expr, i) => ({ id: 'tget', expr, i });
let _throw = expr => ({ id: 'throw', expr });
let _try = (lhs, rhs) => ({ id: 'try', lhs, rhs });
let _tuple = values => ({ id: 'tuple', values });
let _typeof = expr => ({ id: 'typeof', expr });
let _undefined = { id: 'undefined' };
let _var = vn => ({ id: 'var', vn });
let _void = { id: 'struct', completed: true, kvs: [] };
let _while = (cond, loop, expr) => ({ id: 'while', cond, loop, expr });

let parserModule = () => {
	let isAll = pred => s => {
		let isAll_;
		isAll_ = i => i === s.length || pred(s.charCodeAt(i)) && isAll_(i + 1);
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
		let qb0 = ll.isNotEmpty(qb) ? ll.head(qb) : undefined;

		return false ? undefined
		: ch === ascii('{') && qb0 === ascii('`') ? ll.cons(ch, qb)
		: ch === ascii('}') && qb0 === ascii('`') ? ll.cons(ch, qb)
		: isQuote(qb0) ? (qb0 === ch ? ll.tail(qb) : qb)
		: isQuote(ch) ? ll.cons(ch, qb)
		: ch === ascii('(') ? (qb0 === ascii(')') ? ll.tail(qb) : ll.cons(ch, qb))
		: ch === ascii(')') ? (qb0 === ascii('(') ? ll.tail(qb) : ll.cons(ch, qb))
		: ch === ascii('[') ? (qb0 === ascii(']') ? ll.tail(qb) : ll.cons(ch, qb))
		: ch === ascii(']') ? (qb0 === ascii('[') ? ll.tail(qb) : ll.cons(ch, qb))
		: ch === ascii('{') ? (qb0 === ascii('}') ? ll.tail(qb) : ll.cons(ch, qb))
		: ch === ascii('}') ? (qb0 === ascii('{') ? ll.tail(qb) : ll.cons(ch, qb))
		: qb;
	};

	let splitl = (s, sep) => {
		let i = 0;
		let j;
		let qb = ll.empty();
		let qb1;

		while (function() {
			j = i + sep.length;
			return j <= s.length && function() {
				let ch = s.charCodeAt(i);
				qb1 = quoteBracket(qb, ch);
				return ll.isNotEmpty(qb) || s.slice(i, j) !== sep || i === 0;
			}();
		}()) {
			i = i + 1;
			qb = qb1;
		};

		return j <= s.length ? [s.slice(0, i), s.slice(j, undefined)] : [s, undefined];
	};

	let splitr = (s, sep) => {
		let i;
		let j = s.length;
		let qb = ll.empty();
		let qb1;

		while (function() {
			i = j - sep.length;
			return 0 <= i && function() {
				let ch = s.charCodeAt(j - 1);
				qb1 = quoteBracket(qb, ch);
				return ll.isNotEmpty(qb1) || s.slice(i, j) !== sep || i === 0;
			}();
		}()) {
			j = j - 1;
			qb = qb1;
		};

		return 0 <= i ? [s.slice(0, i), s.slice(j, undefined)] : [undefined, s];
	};

	let keepsplitl = (s, sep, apply) => {
		let keepsplitl_;
		keepsplitl_ = input => input !== '' ? function() {
			let [left, right] = splitl(input, sep);
			return vec.cons(apply(left), keepsplitl_(right));
		}() : vec.empty;
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

	let parseAssocLeftReverse = (id, op, parseValue) => {
		let parseAssocLeftReverse_;
		parseAssocLeftReverse_ = program_ => {
			let program = program_.trim();
			let [left, right] = splitr(program, op);
			let lhs = parseValue(right);
			return left === undefined ? lhs : { id, lhs, rhs: parseAssocLeftReverse_(left) };
		};
		return parseAssocLeftReverse_;
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

			return _add(
				_str(program.slice(0, index)),
				_add(_fmt(parseApplyBlockFieldIndex(expr_)), parseBackquote(right)));
		}() : _str(program);
	};

	let parseConstant = program => {
		let first = program.charCodeAt(0);

		return false ? undefined
		: ascii('0') <= first && first <= ascii('9') ? _num(parseNumber(program))
		: program.startsWith("'") && program.endsWith("'") ? _str(program.slice(1, -1))
		: program.startsWith('"') && program.endsWith('"') ? _str(program.slice(1, -1))
		: program.startsWith('`') && program.endsWith('`') ? parseBackquote(program.slice(1, -1))
		: program === 'false' ? _bool(false)
		: program === 'new Error' ? { id: 'new', clazz: 'Error' }
		: program === 'new Map' ? { id: 'new', clazz: 'Map' }
		: program === 'new Promise' ? { id: 'new', clazz: 'Promise' }
		: program === 'nil' ? _nil
		: program === 'true' ? _bool(true)
		: program === 'undefined' ? _undefined
		: isIdentifier(program) ? _var(program)
		: error(`cannot parse "${program}"`);
	};

	let parseArray = (program, parse) => {
		let parseArray_;
		parseArray_ = program_ => {
			let program = program_.trim();

			return program !== '' ? function() {
				let [head, tail_] = splitl(program, ',');
				return head.startsWith('...')
					? function() {
						let head_ = parse(head.slice(3, head.length));
						return tail_ !== '' ? _app(_dot(head_, 'concat'), parseArray_(tail_)) : head_;
					}()
					: _cons(parse(head), parseArray_(tail_));
			}()
			: _nil;
		};
		return parseArray_(program);
	};

	let parseTuple = (program, parse) => _tuple(keepsplitl(program + ',', ',', parse));

	let parseArrayTuple = (program_, parse) => {
		let program = program_.slice(1, -1).trim();
		return (program === '' || program.endsWith(',') ? parseArray : parseTuple)(program, parse);
	};

	let parseStructInner = (program, parse) => {
		let appendTrailingComma = s => s + (s === '' || s.endsWith(',') ? '' : ',');

		return _struct(keepsplitl(appendTrailingComma(program), ',', kv => {
			let [key_, value_] = splitl(kv, ':');
			let keyc = parseConstant(key_.trim());
			return keyc.id === 'var' ? function() {
				let key = keyc.vn;
				let value = value_ !== undefined ? parse(value_) : _var(key);
				return { key, value };
			}() : error(`cannot parse struct member ${kv}`);
		}));
	};

	let parseStruct = (program, parse) => parseStructInner(program.slice(1, -1).trim(), parse);

	let parse;

	let parseValue;

	parseValue = program_ => {
		let program = program_.trim();

		return false ? undefined
		: program.startsWith('try {') && program.endsWith('}') ? function() {
			let [try_, catch_] = splitl(program.slice(4, undefined), 'catch (e)');
			return _try(parse(try_), _lambda(_var('e'), parse(catch_)));
		}()
		: program.startsWith('(') && program.endsWith(')') ?
			parse(program.slice(1, -1))
		: program.startsWith('[') && program.endsWith(']') ?
			parseArrayTuple(program, parse)
		: program.startsWith('{') && program.endsWith('}') ? function() {
			let block = program.slice(1, -1).trim();
			return block.endsWith(';') ? parse(block) : parseStructInner(block, parse);
		}()
		:
			parseConstant(program);
	};

	let parseLvalue = program_ => {
		let program = program_.trim();
		let [expr, field] = splitr(program, '.');

		return false ? undefined
		: expr !== undefined && isIdentifier(field) ?
			_dot(parseApplyBlockFieldIndex(expr), field)
		: program.endsWith(']') ? function() {
			let [expr, index_] = splitr(program, '[');
			let index = index_.slice(0, -1);
			return expr === undefined ? parseValue(program) : _index(parse(expr), parse(index));
		}()
		:
			parseValue(program);
	};

	parseApplyBlockFieldIndex = program_ => {
		let program = program_.trim();

		return false ? undefined
		: program.startsWith('function() {') && program.endsWith('}()') ?
			parse(program.slice(12, -3).trim())
		: program.startsWith('typeof ') ?
			_typeof(parseValue(program.slice(7, undefined)))
		: program.endsWith('()') ?
			_app(parse(program.slice(0, -2)), _undefined)
		: program.endsWith(')') ? function() {
			let [expr, paramStr_] = splitr(program, '(');
			let paramStr = paramStr_.slice(0, -1).trim();
			return expr !== undefined ? _app(parse(expr), parse(paramStr)) : parseValue(program);
		}()
		: parseLvalue(program);
	};

	let parseIf = [parseApplyBlockFieldIndex,]
	.map(p => parsePrefix('await', 'await ', p))
	.map(p => parseAssocLeft_('mod', '%', p))
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
	.map(p => parseAssocRight('coal', '??', p))
	.map(p => parseAssocLeftReverse('app', '|>', p))
	.map(p => program => {
		let [if_, thenElse] = splitl(program, '?');

		return false ? undefined
		: thenElse === undefined ? p(if_)
		: thenElse.startsWith('?') ? p(program)
		: function() {
			let [then, else_] = splitl(thenElse, ':');

			return _if(parse(if_), parse(then), parse(else_));
		}();
	})
	[0];

	let parsePair = (program, parse) => {
		let parsePair_;
		parsePair_ = program => {
			let [left, right] = splitl(program, ',');
			let lhs = parse(left.trim());
			return right === undefined ? lhs : _pair(lhs, parsePair_(right));
		};
		return parsePair_(program);
	};

	let parseBind;

	parseBind = program_ => {
		let program = program_.trim();

		return false ? undefined
		: program === '()' ?
			_void
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
		: left.startsWith('async ') ?
			_lambdaAsync(parseBind(left.slice(6, undefined)), parse(right.trim()))
		:
			_lambda(parseBind(left), parse(right.trim()));
	};

	parse = program => {
		let [statement_, expr_] = splitl(program, ';');

		return expr_ === undefined ? parsePair(statement_, parseLambda) : function() {
			let statement = statement_.trim();
			let expr = expr_.trim();
			let parseExpr = expr !== '' ? parse(expr) : _undefined;

			return false ? undefined
			: statement.startsWith('for (') && statement.endsWith('}') ? function() {
				let [conds, loop] = splitl(statement.slice(5, statement.length - 1), ') {');
				let [init, conds1] = splitl(conds, ';');
				let [cond, inc] = splitl(conds1, ';');
				return parse(`${init}; while (${cond}) { ${loop} ${inc}; }; ${expr}`);
			}()
			: statement.startsWith('let ') ? function() {
				let [vn, value] = splitl(statement.slice(4, undefined), '=');
				let v = vn.trim();

				return false ? undefined
				: value !== undefined ? _let(parseBind(vn), parse(value), parseExpr)
				: isIdentifier(v) ? _alloc(v, parseExpr)
				: error(`cannot parse let variable "${v}"`);
			}()
			: statement.startsWith('return ') && expr === '' ?
				parse(statement.slice(7, undefined))
			: statement.startsWith('throw ') && expr === '' ?
				_throw(parse(statement.slice(6, undefined)))
			: statement.startsWith('while (') && statement.endsWith('}') ? function() {
				let [cond, loop] = splitl(statement.slice(7, statement.length - 1), ') {');
				return _while(parse(cond), parse(loop), parseExpr);
			}()
			: function() {
				let [lhs, rhs] = splitl(statement, '=');

				return rhs !== undefined
					? _assign(parseLvalue(lhs), parse(rhs), parseExpr)
					: _let(_var(newDummy()), parse(lhs), parseExpr);
			}();
		}();
	};

	return { parse };
};

let rewrite = (rf, ast) => {
	let { id } = ast;

	let f = false ? undefined
	: id === 'add' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'alloc' ? (({ vn, expr }) => ({ id, vn, expr: rf(expr) }))
	: id === 'and' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'app' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'assign' ? (({ bind, value, expr }) => ({ id, bind: rf(bind), value: rf(value), expr: rf(expr) }))
	: id === 'await' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'bool' ? (({ b }) => ast)
	: id === 'coal' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'cons' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'deref' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'div' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'dot' ? (({ expr, field }) => ({ id, expr: rf(expr), field }))
	: id === 'eq_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'fmt' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'frame' ? (({ fs, ps, vn }) => ast)
	: id === 'if' ? (({ if_, then, else_ }) => ({ id, if_: rf(if_), then: rf(then), else_: rf(else_) }))
	: id === 'index' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'lambda' ? (({ bind, expr }) => ({ id, bind: rf(bind), expr: rf(expr) }))
	: id === 'lambda-async' ? (({ bind, expr }) => ({ id, bind: rf(bind), expr: rf(expr) }))
	: id === 'lambda-capture' ? (({ capture, bindCapture, bind, expr }) =>
		({ id, capture: rf(capture), bindCapture: rf(bindCapture), bind: rf(bind), expr: rf(expr) })
	)
	: id === 'le_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'let' ? (({ bind, value, expr }) => ({ id, bind: rf(bind), value: rf(value), expr: rf(expr) }))
	: id === 'lt_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'mod' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'mul' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'ne_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'neg' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'new' ? (({ clazz }) => ast)
	: id === 'nil' ? (({}) => ast)
	: id === 'not' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'num' ? (({ i }) => ast)
	: id === 'or_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'pair' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'pget' ? (({ expr, i }) => ({ id, expr: rf(expr), i }))
	: id === 'pos' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'ref' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'segment' ? (({ opcodes }) => ast)
	: id === 'str' ? (({ v }) => ast)
	: id === 'struct' ? (({ kvs }) => ({ id, kvs: kvs.map(({ key, value }) => ({ key, value: rf(value) })) }))
	: id === 'sub' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'tget' ? (({ expr, i }) => ({ id, expr: rf(expr), i }))
	: id === 'throw' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'try' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'tuple' ? (({ values }) => ({ id, values: values.map(rf) }))
	: id === 'typeof' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'undefined' ? (({}) => ast)
	: id === 'var' ? (({ vn }) => ast)
	: id === 'while' ? (({ cond, loop, expr }) => ({ id, cond: rf(cond), loop: rf(loop), expr: rf(expr) }))
	: error(`cannot rewrite for ${id}`);

	return f(ast);
};

let formatModule = () => {
	let precs = function() {
		let precOrder = [
			['bool', 'nil', 'num', 'str', 'struct', 'tuple', 'undefined', 'var',],
			['new',],
			['cons', 'struct', 'tuple',],
			['typeof',],
			['fmt',],
			['app',],
			['await',],
			['mod',],
			['div',],
			['mul',],
			['neg',],
			['sub',],
			['pos',],
			['add',],
			['lt_',],
			['le_',],
			['not',],
			['ne_',],
			['eq_',],
			['and',],
			['or',],
			['coal',],
			['app',],
			['if',],
			['lambda', 'lambda-async', 'lambda-capture',],
			['pair',],
			['let',],
			['throw',],
			['while',],
			['assign',],
		];

		let precs = {};
		for (let i = 0; i < precOrder.length; i = i + 1) {
			precOrder[i].map(id => setp(precs, id, i));
		};
		return precs;
	}();

	let formatBlock;
	let format_;
	let format = ast => format_(9999, ast);

	formatBlock = ast => {
		let { id } = ast;

		let f = false ? undefined
		: id === 'alloc' ? (({ vn, expr }) => `let ${vn}; ${formatBlock(expr)}`)
		: id === 'assign' ? (({ bind, value, expr }) => `${format(bind)} = ${format(value)}; ${formatBlock(expr)}`)
		: id === 'let' ? (({ bind, value, expr }) => `let ${format(bind)} = ${format(value)}; ${formatBlock(expr)}`)
		: id === 'throw' ? (({ expr }) => `throw ${format(expr)}`)
		: id === 'try' ? (({ lhs, rhs }) => `try { ${formatBlock(lhs)}; } catch (e) { ${formatBlock(rhs)}; }`)
		: id === 'while' ? (({ cond, loop, expr }) => `while (${format(cond)}) { ${formatBlock(loop)}; } ${formatBlock(expr)}`)
		: (({}) => `return ${format(ast)}`);

		return f(ast);
	};

	format_ = (priority, ast) => {
		let { id } = ast;
		let priority_ = getp(precs, id) ?? 0;

		let fm = ast => format_(priority_ - 1, ast);
		let fmt = ast => format_(priority_, ast);

		let f = false ? undefined
		: id === 'add' ? (({ lhs, rhs }) => `${fm(lhs)} + ${fmt(rhs)}`)
		: id === 'and' ? (({ lhs, rhs }) => `${fm(lhs)} && ${fmt(rhs)}`)
		: id === 'app' ? (({ lhs, rhs }) => `${fmt(lhs)}(${format(rhs)})`)
		: id === 'await' ? (({ expr }) => `await ${fmt(expr)}`)
		: id === 'bool' ? (({ b }) => b)
		: id === 'coal' ? (({ lhs, rhs }) => `${fm(lhs)} ?? ${fmt(rhs)}`)
		: id === 'cons' ? (({ lhs, rhs }) => {
			let s = fmt(lhs);
			for (let r = rhs; r.id === 'cons'; r = r.rhs) {
				s = s + `, ${fmt(r.lhs)}`;
			};
			return r.id !== 'nil' ? `[${s}, ...${fmt(r)}]` : `[${s},]`;
		})
		: id === 'deref' ? (({ expr }) => `*${fmt(expr)}`)
		: id === 'div' ? (({ lhs, rhs }) => `${fmt(lhs)} / ${fm(rhs)}`)
		: id === 'dot' ? (({ expr, field }) => `${fmt(expr)}.${field}`)
		: id === 'eq_' ? (({ lhs, rhs }) => `${fmt(lhs)} === ${fmt(rhs)}`)
		: id === 'fmt' ? (({ expr }) => `fmt(${fmt(expr)})`)
		: id === 'frame' ? (({ fs, ps, vn }) => `frame[${fs}][${ps}]`)
		: id === 'if' ? (({ if_, then, else_ }) => `${fm(if_)} ? ${fmt(then)} : ${fmt(else_)}`)
		: id === 'index' ? (({ lhs, rhs }) => `${fmt(lhs)}[${format(rhs)}]`)
		: id === 'lambda' ? (({ bind, expr }) => `${fmt(bind)} => ${fmt(expr)}`)
		: id === 'lambda-async' ? (({ bind, expr }) => `async ${fmt(bind)} => ${fmt(expr)}`)
		: id === 'lambda-capture' ? (({ capture, bindCapture, bind, expr }) =>
			capture !== undefined && 0 < Object.keys(capture.kvs).length
				? `|${fmt(capture)}| ${fmt(bind)} => |${fmt(bindCapture)}| ${fmt(expr)}`
				: `${fmt(bind)} => ${fmt(expr)}`
		)
		: id === 'le_' ? (({ lhs, rhs }) => `${fm(lhs)} <= ${fmt(rhs)}`)
		: id === 'lt_' ? (({ lhs, rhs }) => `${fm(lhs)} < ${fmt(rhs)}`)
		: id === 'mod' ? (({ lhs, rhs }) => `${fmt(lhs)} % ${fm(rhs)}`)
		: id === 'mul' ? (({ lhs, rhs }) => `${fm(lhs)} * ${fmt(rhs)}`)
		: id === 'ne_' ? (({ lhs, rhs }) => `${fm(lhs)} !== ${fmt(rhs)}`)
		: id === 'neg' ? (({ expr }) => `- ${fmt(expr)}`)
		: id === 'new' ? (({ clazz }) => `new ${clazz}`)
		: id === 'nil' ? (({}) => '[]')
		: id === 'not' ? (({ expr }) => `! ${fmt(expr)}`)
		: id === 'num' ? (({ i }) => `${i}`)
		: id === 'or_' ? (({ lhs, rhs }) => `${fm(lhs)} || ${fmt(rhs)}`)
		: id === 'pair' ? (({ lhs, rhs }) => `${fm(lhs)}, ${fmt(rhs)}`)
		: id === 'pget' ? (({ expr, i }) => `${fmt(expr)}[${i}]`)
		: id === 'pos' ? (({ expr }) => `+ ${fmt(expr)}`)
		: id === 'ref' ? (({ expr }) => `&${fmt(expr)}`)
		: id === 'segment' ? (({ opcodes }) => `<<${stringify(opcodes)}>>`)
		: id === 'str' ? (({ v }) => `'${v}'`)
		: id === 'struct' ? (({ kvs }) => {
			let s = kvs
				.map(({ key, value }) => value.id === 'var' && value.vn === key ? key : `${key}: ${fmt(value)}`)
				.join(', ');
			return s !== '' ? `{ ${s} }` : '{}';
		})
		: id === 'sub' ? (({ lhs, rhs }) => `${fmt(lhs)} - ${fm(rhs)}`)
		: id === 'tget' ? (({ expr, i }) => `${fmt(expr)}[${i}]`)
		: id === 'tuple' ? (({ values }) => `[${values.map(fm).join(', ')}]`)
		: id === 'typeof' ? (({ expr }) => `typeof ${fmt(expr)}`)
		: id === 'undefined' ? (({}) => `${id}`)
		: id === 'var' ? (({ vn }) => vn)
		: (({}) => `function() { ${formatBlock(ast)}; }()`);

		return priority_ <= priority ? f(ast) : `(${f(ast)})`;
	};

	return { format };
};

let format = formatModule().format;

let typesModule = () => {
	let refs = new Map();
	let refCount;

	refCount = 0;

	let finalRef;

	finalRef = v => {
		let ref = v.ref;
		return ref !== undefined && refs.get(ref) !== v ? finalRef(refs.get(ref)) : v;
	};

	let setRef = (ref, target) => refs.set(ref, target);

	let newRef = () => {
		refCount = refCount + 1;
		let ref = { ref: refCount };
		refs.set(refCount, ref);
		return ref;
	};

	let dump = v => {
		let dump_;
		dump_ = (vs, v) => {
			let { ref } = v;
			let vs_ = ll.cons(v, vs);
			let listv = assumeAny(v);
			return false ? undefined
			: 8 <= ll.len(vs) ?
				'...'
			: ll.contains(vs, v) ?
				'<recurse>'
			: typeof ref === 'number' ? function() {
				let v_ = refs.get(ref);
				return v_ !== v ? dump_(vs_, v_) : `_${ref}`;
			}()
			: typeof v === 'object' ? (false ? undefined
				: ll.isEmpty(listv) ?
					''
				: v.t === undefined && ll.isNotEmpty(listv) && assumeList(listv).length === 2 ?
					`${dump_(vs_, ll.head(listv))}:${dump_(vs_, assumeObject(ll.tail(listv)))}`
				: function() {
					let t = v.t;
					let join = Object
						.entries(v)
						.filter(([k, v_]) => k !== 't')
						.map(([k, v_]) => `${k}:${dump_(vs_, v_)}`)
						.join(' ');
					return t !== undefined ? (join !== '' ? `${t}(${join})` : t) : `{${join}}`;
				}()
			)
			: typeof v === 'string' ?
				v.toString()
			:
				JSON.stringify(v, undefined, undefined);
		};
		return dump_(ll.empty(), v);
	};

	let tryBind;

	tryBind = (p, a, b) => {
		let lista = assumeList(a);
		let listb = assumeList(b);
		let refa = a.ref;
		let refb = b.ref;

		return false ? undefined
		: a === b ?
			undefined
		: typeof refa === 'number' ? function() {
			let olda = refs.get(refa);
			let finalb = finalRef(b);
			setRef(refa, finalb);
			let r = tryBind(p, olda, finalb);
			(r === undefined ? undefined : setRef(refa, olda));
			return r;
		}()
		: typeof refb === 'number' ? function() {
			let oldb = refs.get(refb);
			let finala = finalRef(a);
			setRef(refb, finala);
			let r = tryBind(p, finala, oldb);
			(r === undefined ? undefined : setRef(refb, oldb));
			return r;
		}()
		: typeof a !== 'object' ? p
		: typeof b !== 'object' ? p
		: lista.length !== undefined ? (
			lista.length === listb.length ? function() {
				let tryBindList;
				tryBindList = i => i < lista.length ? function() {
					let r = tryBind(`${p}[${i}]`, lista[i], listb[i]);
					return r ?? tryBindList(i + 1);
				}() : undefined;
				return tryBindList(0);
			}() : p
		)
		: function() {
			let tba = Object.keys(a).reduce((r, k) => r ?? function() {
				let b_k = getp(b, k);
				let s = b_k !== undefined || b.completed !== true && function() {
					b_k = newRef();
					setp(b, k, b_k);
					return true;
				}() ? undefined : p;
				return s ?? tryBind(`${p}.${k}`, getp(a, k), b_k);
			}(), undefined);

			let tbb = Object.keys(b).reduce((r, k) => r ?? function() {
				let a_k = getp(a, k);
				let s = a_k !== undefined || a.completed !== true && function() {
					a_k = newRef();
					setp(a, k, a_k);
					return true;
				}() ? undefined : p;
				return s ?? tryBind(`${p}.${k}`, a_k, getp(b, k));
			}(), undefined);

			return tba ?? tbb;
		}();
	};

	let doBind_ = (msg, a, b) => {
		let r = tryBind('', a, b);
		(r === undefined || error(`in ${msg()}:\ncannot bind types between\nfr: ${dump(a)}\nto: ${dump(b)}\nerror: ${r}`));
		return true;
	};

	let doBind = (ast, a, b) => doBind_(() => format(ast), a, b);

	let cloneRef = v => {
		let fromTos = new Map();
		let cloneRef_;

		cloneRef_ = v => {
			let { ref } = v;
			let vlist = assumeList(v);
			return false ? undefined
			: typeof ref === 'number'
				? (fromTos.has(ref) ? fromTos.get(ref) : function() {
					let v1 = newRef();
					fromTos.set(ref, v1);
					doBind_(() => 'clone reference', v1, cloneRef_(refs.get(ref)));
					return v1;
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

	let bindTypes;

	bindTypes = (vts, ast) => false ? undefined
		: ast.id === 'nil' ? vts
		: ast.id === 'pair' ? bindTypes(bindTypes(vts, ast.lhs), ast.rhs)
		: ast.id === 'struct' ? vec.foldl(vts, ast.kvs, (vts_, kv) => bindTypes(vts_, kv.value))
		: ast.id === 'tuple' ? vec.foldl(vts, ast.values, bindTypes)
		: ast.id === 'var' ? ll.cons([ast.vn, newRef()], vts)
		: error(`bindTypes(): cannot destructure ${format(ast)}`);

	let tyArrayOf = type => ({ t: 'array', of: type });
	let tyBoolean = ({ t: 'bool' });
	let tyError = ({ t: 'error' });
	let tyLambdaOf = (in_, out) => ({ t: 'lambda', generic: true, in_, out });
	let tyLambdaOfFixed = (in_, out) => ({ t: 'lambda', in_, out });
	let tyNumber = ({ t: 'num' });
	let tyPairOf = (lhs, rhs) => ({ t: 'pair', lhs, rhs });
	let tyPromiseOf = out => ({ t: 'promise', out });
	let tyRefOf = expr => ({ t: 'ref', expr });
	let tyString = tyArrayOf({ t: 'char' });
	let tyStructOf = kvs => ({ t: 'struct', kvs });
	let tyStructOfCompleted = kvs => ({ t: 'struct', completed: true, kvs });
	let tyTupleOf = types => ({ t: 'tuple', types });
	let tyVoid = tyStructOfCompleted({});

	let tyMapOf = (tk, tv) => tyStructOfCompleted({
		get: tyLambdaOfFixed(tk, tv),
		has: tyLambdaOfFixed(tk, tyBoolean),
		set: tyLambdaOfFixed(tyPairOf(tk, tv), tyVoid),
	});

	let inferDot = (ast, ts, field) => {
		return false ? undefined
		: field === 'apply' ?
			tyLambdaOf(newRef(), newRef())
		: field === 'charCodeAt' ? function() {
			doBind(ast, ts, tyString);
			return tyLambdaOf(tyNumber, tyNumber);
		}()
		: field === 'concat' ? function() {
			let ta = tyArrayOf(newRef());
			doBind(ast, ts, ta);
			return tyLambdaOf(ta, ta);
		}()
		: field === 'endsWith' ? function() {
			doBind(ast, ts, tyString);
			return tyLambdaOf(tyString, tyBoolean);
		}()
		: field === 'filter' ? function() {
			let ti = newRef();
			doBind(ast, ts, tyArrayOf(ti));
			return tyLambdaOf(tyLambdaOf(ti, tyBoolean), tyArrayOf(ti));
		}()
		: field === 'flatMap' ? function() {
			let ti = newRef();
			let to = newRef();
			doBind(ast, ts, tyArrayOf(ti));
			return tyLambdaOf(tyLambdaOf(ti, tyArrayOf(to)), tyArrayOf(to));
		}()
		: field === 'includes' ? function() {
			let te = newRef();
			doBind(ast, ts, tyArrayOf(te));
			return tyLambdaOf(te, tyBoolean);
		}()
		: field === 'indexOf' ? function() {
			doBind(ast, ts, tyString);
			return tyLambdaOf(tyPairOf(tyString, tyNumber), tyNumber);
		}()
		: field === 'join' ? function() {
			doBind(ast, ts, tyArrayOf(tyString));
			return tyLambdaOf(tyString, tyString);
		}()
		: field === 'length' ? function() {
			doBind(ast, ts, tyArrayOf(newRef()));
			return tyNumber;
		}()
		: field === 'map' ? function() {
			let ti = newRef();
			let to = newRef();
			doBind(ast, ts, tyArrayOf(ti));
			return tyLambdaOf(tyLambdaOf(ti, to), tyArrayOf(to));
		}()
		: field === 'pop' ? function() {
			let te = newRef();
			doBind(ast, ts, tyArrayOf(te));
			return tyLambdaOf(tyVoid, te);
		}()
		: field === 'push' ? function() {
			let te = newRef();
			doBind(ast, ts, tyArrayOf(te));
			return tyLambdaOf(te, tyVoid);
		}()
		: field === 'reduce' ? function() {
			let te = newRef();
			let tr = newRef();
			let treducer = tyLambdaOf(tyPairOf(tr, te), tr);
			doBind(ast, ts, tyArrayOf(te));
			return tyLambdaOf(tyPairOf(treducer, tr), tr);
		}()
		: field === 'slice' ? function() {
			let te = newRef();
			let tl = tyArrayOf(te);
			doBind(ast, ts, tl);
			return tyLambdaOf(tyPairOf(tyNumber, tyNumber), tl);
		}()
		: field === 'startsWith' ? function() {
			doBind(ast, ts, tyString);
			return tyLambdaOf(tyString, tyBoolean);
		}()
		: field === 'then' ? function() {
			let ti = newRef();
			let to = newRef();
			doBind(ast, ts, tyPromiseOf(ti));
			return tyLambdaOf(ti, tyPromiseOf(to));
		}()
		: field === 'toReversed' ? function() {
			let tl = tyArrayOf(newRef());
			doBind(ast, ts, tl);
			return tyLambdaOf(tyVoid, tl);
		}()
		: field === 'toString' ? function() {
			doBind(ast, ts, newRef());
			return tyLambdaOf(tyVoid, tyString);
		}()
		: field === 'trim' ? function() {
			doBind(ast, ts, tyString);
			return tyLambdaOf(tyVoid, tyString);
		}()
		: function() {
			let kvs = {};
			let tr = setp(kvs, field, newRef());
			let to = tyStructOf(kvs);
			doBind(ast, ts, to);
			return function() {
				let t = finalRef(tr);
				return t.generic !== true ? t : cloneRef(t);
			}();
		}();
	};

	let inferType;

	inferType = (vts, isAsync, ast) => {
		let { id } = ast;

		let infer = ast_ => inferType(vts, isAsync, ast_);

		let inferCmpOp = ({ lhs, rhs }) => {
			let t = newRef();
			doBind(ast, infer(lhs), t);
			doBind(ast, infer(rhs), t);
			(false
				|| tryBind('', t, tyNumber) === undefined
				|| tryBind('', t, tyString) === undefined
				|| error(`cannot compare values with type ${t}`));
			return tyBoolean;
		};

		let inferEqOp = ({ lhs, rhs }) => {
			doBind(ast, infer(lhs), infer(rhs));
			return tyBoolean;
		};

		let inferLogicalOp = ({ lhs, rhs }) => {
			doBind(ast, infer(lhs), tyBoolean);
			doBind(ast, infer(rhs), tyBoolean);
			return tyBoolean;
		};

		let inferMathOp = ({ lhs, rhs }) => {
			doBind(ast, infer(lhs), tyNumber);
			doBind(ast, infer(rhs), tyNumber);
			return tyNumber;
		};

		let f = false ? undefined
		: id === 'add' ? (({ lhs, rhs }) => {
			let t = newRef();
			doBind(ast, infer(lhs), t);
			doBind(ast, infer(rhs), t);
			(false
				|| tryBind('', t, tyNumber) === undefined
				|| tryBind('', t, tyString) === undefined
				|| error(`cannot add values with type ${dump(t)}`));
			return t;
		})
		: id === 'alloc' ? (({ vn, expr }) =>
			inferType(ll.cons([vn, newRef()], vts), isAsync, expr)
		)
		: id === 'and' ?
			inferLogicalOp
		: id === 'app' ? (({ lhs, rhs }) => {
			let te = infer(lhs);
			let tp = infer(rhs);
			let tr = newRef();
			doBind(ast, te, tyLambdaOf(tp, tr));
			return tr;
		})
		: id === 'assign' ? (({ bind, value, expr }) => function() {
			let tbind = infer(bind);
			let tvalue;
			try {
				tvalue = infer(value);
			} catch (e) {
				e.message = `in assignment clause of ${format(bind)}\n${e.message}`;
				throw e;
			};
			doBind(_assign(bind, value, _undefined), tbind, tvalue);
			return infer(expr);
		}())
		: id === 'await' ? (({ expr }) => {
			let t = newRef();
			return isAsync ? function() {
				doBind(ast, infer(expr), tyPromiseOf(t));
				return t;
			}() : error(`await not inside async`);
		})
		: id === 'bool' ? (({}) =>
			tyBoolean
		)
		: id === 'coal' ? (({ lhs, rhs }) => {
			let tl = infer(lhs);
			let tr = infer(rhs);
			doBind(ast, tl, tr);
			return tr;
		})
		: id === 'cons' ? (({ lhs, rhs }) => {
			let tl = tyArrayOf(infer(lhs));
			doBind(ast, infer(rhs), tl);
			return tl;
		})
		: id === 'deref' ?(({ expr }) => {
			let t = newRef();
			doBind(ast, infer(expr), tyRefOf(t));
			return t;
		})
		: id === 'div' ?
			inferMathOp
		: id === 'dot' ? (({ expr, field }) =>
			inferDot(ast, infer(expr), field)
		)
		: id === 'eq_' ?
			inferEqOp
		: id === 'fmt' ? (() =>
			tyString
		)
		: id === 'frame' ?
			error('BAD')
		: id === 'if' ? (({ if_, then, else_ }) => {
			let tt = function() {
				try {
					return infer(then);
				} catch (e) {
					e.message = `in then clause of ${format(if_)}\n${e.message}`;
					throw e;
				}
			}();

			let te = infer(else_);
			doBind(ast, infer(if_), tyBoolean);
			doBind(ast, tt, te);
			return tt;
		})
		: id === 'index' ? (({ lhs, rhs }) => {
			let t = newRef();
			doBind(ast, infer(rhs), tyNumber);
			doBind(ast, infer(lhs), tyArrayOf(t));
			return t;
		})
		: id === 'lambda' ? (({ bind, expr }) => {
			let vts1 = bindTypes(vts, bind);
			let tb = inferType(vts1, false, bind);
			let te = inferType(vts1, false, expr);
			return tyLambdaOf(tb, te);
		})
		: id === 'lambda-async' ? (({ bind, expr }) => {
			let vts1 = bindTypes(vts, bind);
			let tb = inferType(vts1, false, bind);
			let te = inferType(vts1, true, expr);
			return tyLambdaOf(tb, tyPromiseOf(te));
		})
		: id === 'le_' ?
			inferCmpOp
		: id === 'let' ? (({ bind, value, expr }) => {
			let vts1 = bindTypes(vts, bind);
			return function() {
				let tb = inferType(vts1, false, bind);
				let tv;
				try {
					tv = bind.id !== 'var' || !bind.vn.startsWith('__')
						? infer(value)
						: tyLambdaOf(newRef(), newRef());
				} catch (e) {
					e.message = `in value clause of ${format(bind)}\n${e.message}`;
					throw e;
				};
				doBind(_let(bind, value, undefined), tb, tv);
				return inferType(vts1, isAsync, expr);
			}();
		})
		: id === 'lt_' ?
			inferCmpOp
		: id === 'mod' ?
			inferMathOp
		: id === 'mul' ?
			inferMathOp
		: id === 'ne_' ?
			inferEqOp
		: id === 'neg' ? (({ expr }) => {
			doBind(ast, infer(expr), tyNumber);
			return tyNumber;
		})
		: id === 'new' ? (({ clazz }) =>
			false ? undefined
			: clazz === 'Error' ? tyLambdaOf(tyString, tyError)
			: clazz === 'Map' ? tyLambdaOf(tyVoid, tyMapOf(newRef(), newRef()))
			: clazz === 'Promise' ? function() {
				let tr = newRef();
				let tres = tyLambdaOf(tr, tyVoid);
				let trej = tyLambdaOf(tyError, tyVoid);
				return tyLambdaOf(tyLambdaOf(tyPairOf(tres, trej), tyVoid), tyPromiseOf(tr));
			}()
			: error(`unknown class ${clazz}`)
		)
		: id === 'nil' ? (({}) =>
			tyArrayOf(newRef())
		)
		: id === 'not' ? (({ expr }) => {
			doBind(ast, infer(expr), tyBoolean);
			return tyBoolean;
		})
		: id === 'num' ? (({}) =>
			tyNumber
		)
		: id === 'or_' ?
			inferLogicalOp
		: id === 'pair' ? (({ lhs, rhs }) =>
			tyPairOf(infer(lhs), infer(rhs))
		)
		: id === 'pget' ? (({ expr, i }) => {
			let tl = newRef();
			let tr = newRef();
			doBind(ast, infer(expr), tyPairOf(tl, tr));
			return i === 0 ? tl : tr;
		})
		: id === 'pos' ? (({ expr }) => {
			doBind(ast, infer(expr), tyNumber);
			return tyNumber;
		})
		: id === 'ref' ? (({ expr }) =>
			tyRefOf(infer(expr))
		)
		: id === 'segment' ? (({ opcodes }) =>
			newRef()
		)
		: id === 'str' ? (({}) =>
			tyString
		)
		: id === 'struct' ? (({ kvs }) => {
			return tyStructOf(vec.foldl({}, kvs, (type, kv) => {
				let { key, value } = kv;
				setp(type, key, function() {
					try {
						return infer(value);
					} catch (e) {
						e.message = `in field ${key}\n${e.message}`;
						throw e;
					}
				}());
				return type;
			}));
		})
		: id === 'sub' ?
			inferMathOp
		: id === 'tget' ? (({ expr, i }) => {
			let ts = vec.empty;
			while (0 < i) {
				ts = vec.cons(newRef(), ts);
				i = i - 1;
			};
			let ti = newRef();
			doBind(ast, infer(expr), tyTupleOf(vec.cons(ti, ts)));
			return ti;
		})
		: id === 'throw' ? (({}) =>
			newRef()
		)
		: id === 'try' ? (({ lhs, rhs }) => {
			doBind(ast, infer(rhs), newRef());
			return infer(lhs);
		})
		: id === 'tuple' ? (({ values }) =>
			tyTupleOf(vec.foldr(vec.empty, values, (tuple, value) => vec.cons(infer(value), tuple)))
		)
		: id === 'typeof' ? (({}) =>
			tyString
		)
		: id === 'undefined' ? (({}) =>
			newRef()
		)
		: id === 'var' ? (({ vn }) => {
			let t = finalRef(ll.findk(vts, vn));
			return t.generic !== true ? t : cloneRef(t);
		})
		: id === 'while' ? (({ cond, loop, expr }) => {
			doBind(ast, infer(cond), tyBoolean);
			doBind(ast, infer(loop), newRef());
			return infer(expr);
		})
		: (({}) =>
			error(`cannot infer type for ${id}`)
		);

		return f(ast);
	};

	let predefinedTypes = Object
		.entries({
			JSON: tyStructOfCompleted({
				parse: tyLambdaOf(tyString, newRef()),
				stringify: tyLambdaOf(tyPairOf(newRef(), tyPairOf(newRef(), newRef())), tyString),
			}),
			Object: tyStructOfCompleted({
				assign: tyLambdaOf(newRef(), newRef()),
				entries: tyLambdaOf(tyStructOf({}), tyArrayOf(tyTupleOf(vec.cons(tyString, vec.cons(newRef(), vec.empty))))),
				fromEntries: tyLambdaOf(tyArrayOf(tyTupleOf(vec.cons(tyString, vec.cons(newRef(), vec.empty)))), tyStructOf({})),
				keys: tyLambdaOf(tyStructOf({}), tyArrayOf(tyString)),
			}),
			Promise: tyStructOfCompleted({
				reject: tyLambdaOf(tyError, tyPromiseOf(newRef())),
				resolve: function() { let t = newRef(); return tyLambdaOf(t, tyPromiseOf(t)); }(),
			}),
			console: tyStructOfCompleted({
				error: tyLambdaOf(newRef(), tyVoid),
				log: tyLambdaOf(newRef(), tyVoid),
			}),
			fs_readFileSync: tyLambdaOf(tyPairOf(tyString, tyString), tyString),
			process: tyStructOfCompleted({
				argv: tyArrayOf(tyString),
				env: tyStructOf({}),
			}),
			require: tyLambdaOf(tyString, newRef()),
			util_inspect: tyLambdaOf(tyPairOf(newRef(), tyStructOf({})), tyString),
		})
		.reduce((l, vt) => ll.cons(vt, l), ll.empty());

	return { dump, infer: ast => inferType(predefinedTypes, false, ast) };
};

let promiseResolve = _dot(_var('Promise'), 'resolve');
let promisify = ast => _app(promiseResolve, ast);

let unpromisify = ast => {
	let { id, lhs, rhs } = ast;
	return id === 'app' && lhs === promiseResolve ? rhs : undefined;
};

let parseAstType;
let parser = parserModule();

let rewriteAsync;

rewriteAsync = ast => {
	let { id } = ast;

	let _then = (p, bind, expr) => _app(_dot(p, 'then'), _lambda(bind, expr));

	let reduceOp = ({ expr }) => {
		let pe = rewriteAsync(expr);
		let e = unpromisify(pe);
		let ve = e ?? _var(newDummy());
		let p = promisify({ id, expr: ve });
		return e !== undefined ? p : _then(pe, ve, p);
	};

	let reduceBinOp = ({ lhs, rhs }) => {
		let pl = rewriteAsync(lhs);
		let l = unpromisify(pl);
		let vl = l ?? _var(newDummy());
		let pr = rewriteAsync(rhs);
		let r = unpromisify(pr);
		let vr = r ?? _var(newDummy());
		let p;
		p = promisify({ id, lhs: vl, rhs: vr });
		p = l !== undefined ? p : _then(pl, vl, p);
		p = r !== undefined ? p : _then(pr, vr, p);
		return p;
	};

	let f = false ? undefined
	: id === 'add' ?
		reduceBinOp
	: id === 'alloc' ? (({ vn, expr }) => {
		let pe = rewriteAsync(expr);
		let e = unpromisify(pe);
		return e !== undefined ? promisify({ id, vn, expr: e }) : { id, vn, expr: pe };
	})
	: id === 'and' ?
		reduceBinOp
	: id === 'app' ?
		reduceBinOp
	: id === 'await' ? (({ expr }) =>
		expr
	)
	: id === 'coal' ?
		reduceBinOp
	: id === 'cons' ?
		reduceBinOp
	: id === 'deref' ?
		reduceOp
	: id === 'div' ?
		reduceBinOp
	: id === 'dot' ? (({ expr, field }) => {
		let pe = rewriteAsync(expr);
		let e = unpromisify(pe);
		let ve = e ?? _var(newDummy());
		let p = promisify({ id, expr: ve, field });
		return e !== undefined ? p : _then(pe, ve, p);
	})
	: id === 'eq_' ?
		reduceBinOp
	: id === 'fmt' ?
		reduceOp
	: id === 'if' ? (({ if_, then, else_ }) => {
		let pi = rewriteAsync(if_);
		let i = unpromisify(pi);
		let vi = i ?? _var(newDummy());
		let pt = rewriteAsync(then);
		let t = unpromisify(pt);
		let pe = rewriteAsync(else_);
		let e = unpromisify(pe);
		return false ? undefined
		: i !== undefined && t !== undefined && e !== undefined ? promisify({ id, if_: vi, then: t, else_: e })
		: i !== undefined ? { id, if_: vi, then: pt, else_: pe }
		: _then(pi, vi, { id, if_: vi, then: pt, else_: pe });
	})
	: id === 'lambda-async' ? (({ bind, expr }) =>
		promisify(_lambda(bind, rewriteAsync(expr)))
	)
	: id === 'le_' ?
		reduceBinOp
	: id === 'let' ? (({ bind, value, expr }) => {
		let pv = rewriteAsync(value);
		let v = unpromisify(pv);
		let pe = rewriteAsync(expr);
		let e = unpromisify(pe);
		return false ? undefined
		: e !== undefined && v !== undefined ? promisify({ id, bind, value: v, expr: e })
		: e === undefined && v !== undefined ? { id, bind, value: v, expr: pe }
		: _then(pv, bind, pe);
	})
	: id === 'lt_' ?
		reduceBinOp
	: id === 'mod' ?
		reduceBinOp
	: id === 'mul' ?
		reduceBinOp
	: id === 'ne_' ?
		reduceBinOp
	: id === 'neg' ?
		reduceOp
	: id === 'not' ?
		reduceOp
	: id === 'or_' ?
		reduceBinOp
	: id === 'pos' ?
		reduceOp
	: id === 'ref' ?
		reduceOp
	: id === 'segment' ?
		error('BAD')
	: id === 'sub' ?
		reduceBinOp
	: id === 'try' ?
		reduceBinOp
	: id === 'typeof' ?
		reduceOp
	: id === 'while' ? (({ cond, loop, expr }) => {
		let pc = rewriteAsync(cond);
		let c = unpromisify(pc);
		let vc = c ?? _var(newDummy());
		let pl = rewriteAsync(loop);
		let l = unpromisify(pl);
		let pe = rewriteAsync(expr);
		let e = unpromisify(pe);
		return false ? undefined
		: c !== undefined && l !== undefined && e !== undefined ? promisify({ id, cond: vc, loop: l, expr: e })
		: c !== undefined && l !== undefined && e === undefined ? { id, cond: vc, loop: l, expr: pe }
		: function() {
			let vn = newDummy();
			let vp = _var(vn);
			let invoke = _app(vp, _void);
			let if_ = _if(vc, _then(pl, _var(newDummy()), invoke), pe);
			return _alloc(vn, _assign(vp, _lambda(_var(newDummy()), c !== undefined ? if_ : _then(pc, vc, if_)), invoke));
		}();
	})
	: (({}) =>
		promisify(rewrite(ast_ => unpromisify(rewriteAsync(ast_)), ast))
	);

	return f(ast);
};

let rewriteBind = () => {
	let ifBindId = bindId => {
		let ifBind;

		ifBind = (bind, value, then, else_) => {
			let { id } = bind;

			let bindConstant = v => ast => false ? undefined
				: bind.id !== value.id ? _if(_eq(bind, value), then, else_)
				: assumeAny(v) === value.v ? then
				: else_;

			let f = false ? undefined
			: id === 'bool' ?
				bindConstant(bind.b)
			: id === 'cons' ? (({ lhs, rhs }) => {
				let v = _var(newDummy());
				return _let(v, value, id !== value.id
					? ifBind(lhs, _index(v, _num(0)), ifBind(rhs, _app(_dot(v, 'slice'), _num(1)), then, else_), else_)
					: ifBind(lhs, value.lhs, ifBind(rhs, value.rhs, then, else_), else_));
			})
			: id === 'nil' ? (({}) => {
				return id !== value.id
					? _if(_eq(_app(_dot(value, 'length'), _void), _num(0)), then, else_)
					: then;
			})
			: id === 'num' ?
				bindConstant(bind.v)
			: id === 'pair' ? (({ lhs, rhs }) => {
				let v = _var(newDummy());
				return _let(v, value, id !== value.id
					? ifBind(lhs, _pget(v, 0), ifBind(rhs, _pget(v, 1), then, else_), else_)
					: ifBind(lhs, value.lhs, ifBind(rhs, value.rhs, then, else_), else_));
			})
			: id === 'str' ?
				bindConstant(bind.v)
			: id === 'struct' ? (({ kvs }) => {
				let v = _var(newDummy());
				let getValue = k => value.kvs.filter(kv => kv.key === k)[0].value;
				return _let(v, value, kvs.reduce(
					(expr, kv) => ifBind(kv.value, id !== value.id ? _dot(v, kv.key) : getValue(kv.key), expr, else_),
					then));
			})
			: id === 'tuple' ? (({ values }) => {
				let v = _var(newDummy());
				let indices = gen(values.length);
				return _let(v, value, indices.reduce(
					(expr, i) => ifBind(values[i], id !== value.id ? _tget(v, i) : value.values[i], expr, else_),
					then));
			})
			: id === 'var' ? (({ vn }) =>
				({ id: bindId, bind, value, expr: then })
			)
			:
				error(`ifBindId(): cannot destructure ${format(bind)}`);

			return f(bind);
		};

		return ifBind;
	};

	let assignBind = ifBindId('assign');
	let letBind = ifBindId('let');

	let rewriteBind_;

	rewriteBind_ = ast => {
		let { bind, id } = ast;

		let f = false ? undefined
		: id === 'assign'
			&& bind.id !== 'dot'
			&& bind.id !== 'index'
			&& bind.id !== 'pget'
			&& bind.id !== 'tget'
			&& bind.id !== 'var' ? (({ bind, value, expr }) =>
			assignBind(bind, rewriteBind_(value), rewriteBind_(expr), _error)
		)
		: id === 'lambda' && bind.id !== 'var' ? (({ bind, expr }) => {
			let arg = _var(newDummy());
			return _lambda(arg, letBind(bind, arg, rewriteBind_(expr), _error));
		})
		: id === 'lambda-async' && bind.id !== 'var' ? (({ bind, expr }) => {
			let arg = _var(newDummy());
			return _lambdaAsync(arg, letBind(bind, arg, rewriteBind_(expr), _error));
		})
		: id === 'let' && bind.id !== 'var' ? (({ bind, value, expr }) =>
			letBind(bind, rewriteBind_(value), rewriteBind_(expr), _error)
		)
		: (({}) =>
			rewrite(rewriteBind_, ast)
		);

		return f(ast);
	};

	return rewriteBind_;
};

let rewriteCapture = () => {
	let rewriteCaptureVar;

	rewriteCaptureVar = (capture, outsidevs, captures, ast) => {
		let { id } = ast;

		let f = false ? undefined
		: id === 'var' ? (({ vn }) => {
			return !ll.contains(outsidevs, vn) ? ast : function() {
				captures.push(vn);
				return _deref(_dot(_var(capture), vn));
			}();
		})
		: (({}) =>
			rewrite(ast => rewriteCaptureVar(capture, outsidevs, captures, ast), ast)
		);

		return f(ast);
	};

	let rewriteCapture_;

	rewriteCapture_ = (vns, ast) => {
		let { id } = ast;

		let f = false ? undefined
		: id === 'alloc' ? (({ vn, expr }) =>
			_alloc(vn, rewriteCapture_(ll.cons(vn, vns), expr))
		)
		: id === 'lambda' ? (({ bind, expr }) => {
			let capture = newDummy();
			let vns1 = ll.cons(bind.vn, ll.cons(capture, vns));
			let captures = [];
			let expr_ = rewriteCaptureVar(capture, vns, captures, expr);
			let definitions = _struct(captures.map(vn => ({ key: vn, value: _ref(_var(vn)) })));
			return _lambdaCapture(definitions, _var(capture), bind, rewriteCapture_(vns1, expr_));
		})
		: id === 'let' ? (({ bind, value, expr }) =>
			_let(bind,
				rewriteCapture_(vns, value),
				rewriteCapture_(ll.cons(bind.vn, vns), expr))
		)
		: (({}) =>
			rewrite(ast => rewriteCapture_(vns, ast), ast)
		);

		return f(ast);
	};

	return rewriteCapture_;
};

let rewriteFsReadFileSync;

rewriteFsReadFileSync = ast => {
	let { id, lhs, rhs } = ast;

	return false ? undefined
	: id === 'app' && lhs.id === 'var' && ['require',].includes(lhs.vn) && rhs.id === 'str' ?
		parseAstType(require('fs').readFileSync(`${rhs.v}.js`, 'utf8')).ast
	:
		rewrite(rewriteFsReadFileSync, ast);
};

let rewriteIntrinsics = ast => {
	let rewriteIntrinsics_;

	rewriteIntrinsics_ = ast => {
		let { id, lhs, rhs } = ast;

		return false ? undefined
		: id === 'app' && lhs.id === 'dot' && ['apply', 'bind', 'call', 'filter', 'flatMap', 'map', 'reduce',].includes(lhs.field) ?
			_app(_var(`$${lhs.field}`), _pair(rewriteIntrinsics_(lhs.expr), rewriteIntrinsics_(rhs)))
		:
			rewrite(rewriteIntrinsics_, ast);
	};

	let rewriteBind_ = rewriteBind();

	return [ast,]
	.map(rewriteIntrinsics_)
	.map(ast => _let(_var('$apply'), rewriteBind_(parser.parse(`
		(f, u, p) => f(p[0])
	`)), ast))
	.map(ast => _let(_var('$bind'), rewriteBind_(parser.parse(`
		(o, b) => o
	`)), ast))
	.map(ast => _let(_var('$call'), rewriteBind_(parser.parse(`
		(f, o, p) => f(p)
	`)), ast))
	.map(ast => _let(_var('$filter'), rewriteBind_(parser.parse(`
		(es, pred) => {
			let out = [];
			for (let i = 0; i < es.length; i = i + 1) {
				let e = es[i];
				pred(e) ? out.push(e) : undefined;
			};
			return out;
		}
	`)), ast))
	.map(ast => _let(_var('$flatMap'), rewriteBind_(parser.parse(`
		(es, f) => {
			let out = [];
			for (let i = 0; i < es.length; i = i + 1) {
				let list = f(es[i]);
				for (let j = 0; j < list.length; j = j + 1) {
					out.push(list[j]);
				};
			};
			return out;
		}
	`)), ast))
	.map(ast => _let(_var('$map'), rewriteBind_(parser.parse(`
		(es, f) => {
			let out = [];
			for (let i = 0; i < es.length; i = i + 1) {
				let e = es[i];
				out.push(f(e));
			};
			return out;
		}
	`)), ast))
	.map(ast => _let(_var('$reduce'), rewriteBind_(parser.parse(`
		(es, acc, init) => {
			let r = init;
			for (let i = 0; i < es.length; i = i + 1) {
				let e = es[i];
				r = acc(r, e);
			};
			return r;
		}
	`)), ast))
	[0];
};

let rewriteNe;

rewriteNe = ast => {
	let { id } = ast;

	let f = false ? undefined
	: id === 'ne_' ? (({ lhs, rhs }) =>
		_not(_eq(rewriteNe(lhs), rewriteNe(rhs)))
	)
	: (({}) =>
		rewrite(rewriteNe, ast)
	);

	return f(ast);
};

let rewriteRenameVar;

rewriteRenameVar = (scope, vns, ast) => {
	let { id } = ast;

	let f = false ? undefined
	: id === 'alloc' ? (({ vn, expr }) => {
		let vn1 = `${vn}_${scope}`;
		return _alloc(
			vn1,
			rewriteRenameVar(scope, ll.cons([vn, vn1], vns), expr));
	})
	: id === 'lambda' ? (({ bind, expr }) => {
		let { vn } = bind;
		let scope1 = newDummy();
		let vn1 = `${vn}_${scope1}`;
		return _lambda(
			_var(vn1),
			rewriteRenameVar(scope1, ll.cons([vn, vn1], vns), expr));
	})
	: id === 'let' ? (({ bind, value, expr }) => {
		let { vn } = bind;
		let vn1 = `${vn}_${scope}`;
		return _let(
		_var(vn1),
			rewriteRenameVar(scope, vns, value),
			rewriteRenameVar(scope, ll.cons([vn, vn1], vns), expr));
	})
	: id === 'var' ? (({ vn }) =>
		_var(ll.findk(vns, vn))
	)
	: (({}) =>
		rewrite(ast => rewriteRenameVar(scope, vns, ast), ast)
	);

	return f(ast);
};

let rewriteVars;

rewriteVars = (fs, ps, vts, ast) => {
	let fs1 = fs + 1;
	let ps1 = ps + 1;

	let rewriteVars_;

	rewriteVars_ = ast => {
		let { id } = ast;

		let f = false ? undefined
		: id === 'alloc' ? (({ vn, expr }) =>
			_alloc(vn, rewriteVars(fs, ps1, ll.cons([vn, [fs, ps]], vts), expr))
		)
		: id === 'assign' ? (({ bind, value, expr }) => {
			return false ? undefined
			: bind.id === 'var' ? function() {
				let [fs_, ps] = ll.findk(vts, bind.vn);
				return _assign(
					_frame(fs - fs_, ps, bind.vn),
					rewriteVars_(value),
					rewriteVars_(expr));
			}()
			:
				_assign(
					rewriteVars_(bind),
					rewriteVars_(value),
					rewriteVars_(expr));
		})
		: id === 'lambda-capture' ? (({ capture, bindCapture, bind, expr }) =>
			_lambdaCapture(
				rewriteVars_(capture),
				bindCapture,
				bind,
				rewriteVars(fs1, 2, ll.cons([bind.vn, [fs1, 1]], ll.cons([bindCapture.vn, [fs1, 0]], vts)), expr))
		)
		: id === 'let' ? (({ bind, value, expr }) =>
			_alloc(bind.vn, _assign(
				_frame(0, ps, bind.vn),
				rewriteVars(fs, ps1, vts, value),
				rewriteVars(fs, ps1, ll.cons([bind.vn, [fs, ps]], vts), expr)))
		)
		: id === 'try' ? (({ lhs, rhs }) =>
			_try(
				rewriteVars_(lhs),
				rewriteVars(fs, ps1, ll.cons(['e', [fs, ps]], vts), rhs))
		)
		: id === 'var' ? (({ vn }) => {
			let [fs_, ps] = ll.findk(vts, vn);
			return _frame(fs - fs_, ps, vn);
		})
		: (({}) =>
			rewrite(rewriteVars_, ast)
		);

		return f(ast);
	};

	return rewriteVars_(ast);
};

let pairTag = {};

let unwrap = f => arg => {
	let ps = [];
	while (arg !== undefined && assumeAny(arg[2]) === pairTag) {
		ps.push(arg[0]);
		arg = arg[1];
	};
	ps.push(arg);
	return f.apply(undefined, ps);
};

let evaluateVvs =
	[
		['JSON', assumeAny({
			parse: unwrap(JSON.parse),
			stringify: unwrap(JSON.stringify),
		})],
		['Object', assumeAny({
			assign: unwrap(Object.assign),
			entries: unwrap(Object.entries),
			fromEntries: unwrap(Object.fromEntries),
			keys: unwrap(Object.keys),
		})],
		['Promise', assumeAny({
			reject: unwrap(Promise.reject),
			resolve: unwrap(Promise.resolve),
		})],
		['console', assumeAny({
			error: unwrap(console.error),
			log: unwrap(console.log),
		})],
		['process', assumeAny({
			argv: ['parse.js', ...argv,],
			env: process.env,
		})],
		['require', assumeAny(path => false ? undefined
			: path === 'fs' ? { readFileSync: unwrap(require('fs').readFileSync) }
			: path === 'util' ? { inspect: unwrap(require('util').inspect) }
			: require(path))
		],
	]
	.reduce((v, vl) => ll.cons(vl, v), ll.empty());

let evaluate;

evaluate = vvs => {
	let assign = (vn, value) => {
		let vv = ll.find(vvs, ([vn_, value]) => vn_ === vn);
		seti(vv, 1, value);
		return value;
	};

	let evaluate_;

	evaluate_ = ast => {
		let ev_ = ast => evaluate_(ast);
		let { id } = ast;

		let f = false ? undefined
		: id === 'add' ? (({ lhs, rhs }) => assumeAny(ev_(lhs) + ev_(rhs)))
		: id === 'alloc' ? (({ vn, expr }) => evaluate(ll.cons([vn, undefined], vvs))(expr))
		: id === 'and' ? (({ lhs, rhs }) => assumeAny(ev_(lhs) && ev_(rhs)))
		: id === 'app' ? (({ lhs, rhs }) => ev_(lhs).call(undefined, ev_(rhs)))
		: id === 'assign' ? (({ bind, value, expr }) => false ? undefined
			: bind.id === 'deref' ? function() {
				let { vv } = ev_(bind);
				vv[1] = ev_(value);
				return ev_(expr);
			}()
			: bind.id === 'dot' ? function() {
				setp(ev_(bind.expr), bind.field, ev_(value));
				return ev_(expr);
			}()
			: bind.id === 'index' ? function() {
				ev_(bind.lhs)[ev_(bind.rhs)] = ev_(value);
				return ev_(expr);
			}()
			: bind.id === 'pget' ? function() {
				ev_(bind.expr)[bind.i] = ev_(value);
				return ev_(expr);
			}()
			: bind.id === 'tget' ? function() {
				ev_(bind.expr)[bind.i] = ev_(value);
				return ev_(expr);
			}()
			: bind.id === 'var' ? function() {
				assign(bind.vn, ev_(value));
				return ev_(expr);
			}()
			: error('BAD')
		)
		: id === 'await' ? (({ expr }) => error('BAD'))
		: id === 'bool' ? (({ b }) => b)
		: id === 'coal' ? (({ lhs, rhs }) => {
			let v = ev_(lhs);
			return v ?? ev_(rhs);
		})
		: id === 'cons' ? (({ lhs, rhs }) => assumeAny(vec.cons(ev_(lhs), ev_(rhs))))
		: id === 'deref' ? (({ expr }) => {
			let { vv } = ev_(expr);
			return get1(vv);
		})
		: id === 'div' ? (({ lhs, rhs }) => assumeAny(ev_(lhs) / ev_(rhs)))
		: id === 'dot' ? (({ expr, field }) => {
			let object = ev_(expr);
			let value = getp(object, field);
			return false ? undefined
			: field === 'charCodeAt' ? assumeAny(p => assumeAny(object).charCodeAt(p))
			: field === 'concat' ? assumeAny(p => assumeAny(object).concat(p))
			: field === 'endsWith' ? assumeAny(p => assumeAny(object).endsWith(p))
			: field === 'includes' ? assumeAny(p => assumeAny(object).includes(p))
			: field === 'indexOf' ? assumeAny(p => assumeAny(object).indexOf(p))
			: field === 'join' ? assumeAny(p => assumeAny(object).join(p))
			: field === 'pop' ? assumeAny(() => assumeAny(object).pop())
			: field === 'push' ? assumeAny(p => assumeAny(object).push(p))
			: field === 'slice' ? assumeAny(([a, b]) => assumeAny(object).slice(a, b))
			: field === 'startsWith' ? assumeAny(p => assumeAny(object).startsWith(p))
			: field === 'toReversed' ? assumeAny(() => assumeAny(object).toReversed())
			: field === 'toString' ? assumeAny(() => assumeAny(object).toString())
			: field === 'trim' ? assumeAny(() => assumeAny(object).trim())
			: value === undefined ? assumeAny(value)
			: typeof value !== 'function' ? assumeAny(value)
			: ['get', 'has', 'set',].includes(field) ? assumeAny(unwrap(value.bind(object)))
			: fake(value).bind(object);
		})
		: id === 'eq_' ? (({ lhs, rhs }) => assumeAny(ev_(lhs) === ev_(rhs)))
		: id === 'fmt' ? (({ expr }) => assumeAny(`${ev_(expr)}`))
		: id === 'frame' ? error('BAD')
		: id === 'if' ? (({ if_, then, else_ }) => ev_(if_) ? ev_(then) : ev_(else_))
		: id === 'index' ? (({ lhs, rhs }) => ev_(lhs)[ev_(rhs)])
		: id === 'lambda' ? (({ bind, expr }) => assumeAny(value => evaluate(ll.cons([bind.vn, value], vvs))(expr)))
		: id === 'lambda-async' ? (({ bind, expr }) => error('BAD'))
		: id === 'lambda-capture' ? (({ capture, bindCapture, bind, expr }) =>
			assumeAny(value => evaluate(ll.cons([bind.vn, value], ll.cons([bindCapture.vn, ev_(capture)], vvs)))(expr))
		)
		: id === 'le_' ? (({ lhs, rhs }) => assumeAny(ev_(lhs) <= ev_(rhs)))
		: id === 'let' ? (({ bind, value, expr }) => evaluate(ll.cons([bind.vn, ev_(value)], vvs))(expr))
		: id === 'lt_' ? (({ lhs, rhs }) => assumeAny(ev_(lhs) < ev_(rhs)))
		: id === 'mod' ? (({ lhs, rhs }) => assumeAny(ev_(lhs) % ev_(rhs)))
		: id === 'mul' ? (({ lhs, rhs }) => assumeAny(ev_(lhs) * ev_(rhs)))
		: id === 'ne_' ? (({ lhs, rhs }) => assumeAny(ev_(lhs) !== ev_(rhs)))
		: id === 'neg' ? (({ expr }) => assumeAny(-ev_(expr)))
		: id === 'new' ? (({ clazz }) => false ? undefined
			: clazz === 'Error' ? assumeAny(e => new Error(e))
			: clazz === 'Map' ? assumeAny(() => new Map())
			: clazz === 'Promise' ? error('BAD') // assumeAny(f => new Promise(f))
			: error(`unknown class ${clazz}`)
		)
		: id === 'nil' ? (({}) => assumeAny([]))
		: id === 'not' ? (({ expr }) => assumeAny(!ev_(expr)))
		: id === 'num' ? (({ i }) => i)
		: id === 'or_' ? (({ lhs, rhs }) => assumeAny(ev_(lhs) || ev_(rhs)))
		: id === 'pair' ? (({ lhs, rhs }) => assumeAny([ev_(lhs), ev_(rhs), pairTag]))
		: id === 'pget' ? (({ expr, i }) => ev_(expr)[i])
		: id === 'pos' ? (({ expr }) => assumeAny(+ev_(expr)))
		: id === 'ref' ? (({ expr }) =>
			expr.id === 'var' ? assumeAny({ vv: ll.find(vvs, ([k_, v]) => k_ === expr.vn) })
			: error('BAD')
		)
		: id === 'segment' ? (({ opcodes }) => error('BAD'))
		: id === 'str' ? (({ v }) => v)
		: id === 'struct' ? (({ kvs }) => assumeAny(vec.foldl({}, kvs, (struct, kv) => {
			let { key, value } = kv;
			setp(struct, key, ev_(value));
			return struct;
		})))
		: id === 'sub' ? (({ lhs, rhs }) => assumeAny(ev_(lhs) - ev_(rhs)))
		: id === 'tget' ? (({ expr, i }) => ev_(expr)[i])
		: id === 'throw' ? (({ expr }) => { throw ev_(expr); })
		: id === 'try' ? (({ lhs, rhs }) => {
			let result;
			try {
				result = ev_(lhs);
			} catch (e) { return ev_(rhs)(e); };
			return result;
		})
		: id === 'tuple' ? (({ values }) => assumeAny(vec.foldr(vec.empty, values, (tuple, value) => vec.cons(ev_(value), tuple))))
		: id === 'typeof' ? (({ expr }) => assumeAny(typeof (ev_(expr))))
		: id === 'undefined' ? (({}) => undefined)
		: id === 'var' ? (({ vn }) => ll.findk(vvs, vn))
		: id === 'while' ? (({ cond, loop, expr }) => {
			let v;
			while (ev_(cond)) { ev_(loop); };
			return ev_(expr);
		})
		: error(`cannot evaluate ${id}`);

		return f(ast);
	};

	return evaluate_;
};

let generate;

generate = ast => {
	let { id } = ast;

	let generateOp = (({ expr }) => [
		...generate(expr),
		{ id },
	]);

	let generateBinOp = (({ lhs, rhs }) => [
		...generate(lhs),
		...generate(rhs),
		{ id },
	]);

	let f = false ? undefined
	: id === 'add' ?
		generateBinOp
	: id === 'alloc' ? (({ vn, expr }) => [
		{ id: 'frame-alloc' },
		...generate(expr),
		{ id: 'frame-dealloc' },
	])
	: id === 'and' ? (({ lhs, rhs }) =>
		generate(_if(lhs, rhs, _bool(false)))
	)
	: id === 'app' ?
		generateBinOp
	: id === 'assign' ? (({ bind, value, expr }) => false ? undefined
		: bind.id === 'dot' ? [
			...generate(bind.expr),
			...generate(value),
			{ id: 'object-put', key: bind.field },
			{ id: 'discard' },
			...generate(expr),
		]
		: bind.id === 'index' ? [
			...generate(bind.lhs),
			...generate(bind.rhs),
			...generate(value),
			{ id: 'array-set' },
			...generate(expr),
		]
		: bind.id === 'pget' ? [
			...generate(bind.expr),
			...generate(value),
			{ id: 'pair-set', i: bind.i },
			...generate(expr),
		]
		: bind.id === 'tget' ? [
			...generate(bind.expr),
			{ id: 'num', i: bind.i },
			...generate(value),
			{ id: 'tuple-set' },
			...generate(expr),
		]
		: [
			...generate(_ref(bind)),
			...generate(value),
			{ id: 'assign-ref' },
			...generate(expr),
		]
	)
	: id === 'await' ? (({}) =>
		error('BAD')
	)
	: id === 'bool' ? (({}) =>
		[ast,]
	)
	: id === 'coal' ? (({ lhs, rhs }) => {
		let label = newDummy();
		return [
			...generate(lhs),
			{ id: 'dup', i: 0 },
			{ id: 'undefined' },
			{ id: 'eq_' },
			{ id: 'jump-false', label },
			{ id: 'discard' },
			...generate(rhs),
			{ id: ':', label },
		];
	})
	: id === 'cons' ?
		generateBinOp
	: id === 'deref' ?
		generateOp
	: id === 'div' ?
		generateBinOp
	: id === 'dot' ? (({ expr, field }) =>
		false ? undefined
		: ['length',].includes(field) ? [
			...generate(expr),
			{ id: 'service', f: 1, field },
		]
		: ['apply', 'charCodeAt', 'concat', 'endsWith', 'includes', 'indexOf', 'join', 'push', 'startsWith',].includes(field) ? [
			...generate(expr),
			{ id: 'label-segment', name: field, segment: [
				{ id: 'frame-get-ref', fs: 0, ps: 0 },
				{ id: 'deref' },
				{ id: 'frame-get-ref', fs: 0, ps: 1 },
				{ id: 'deref' },
				{ id: 'pair' },
				{ id: 'service', m: 2, field },
				{ id: 'return' },
			] },
			{ id: 'lambda-capture' },
		]
		: ['slice',].includes(field) ? [
			...generate(expr),
			{ id: 'label-segment', name: field, segment: [
				{ id: 'frame-get-ref', fs: 0, ps: 0 },
				{ id: 'deref' },
				{ id: 'frame-get-ref', fs: 0, ps: 1 },
				{ id: 'deref' },
				{ id: 'pair' },
				{ id: 'service', m: 3, field },
				{ id: 'return' },
			] },
			{ id: 'lambda-capture' },
		]
		: ['pop', 'toReversed', 'toString', 'trim',].includes(field) ? [
			...generate(expr),
			{ id: 'label-segment', name: field, segment: [
				{ id: 'frame-get-ref', fs: 0, ps: 0 },
				{ id: 'deref' },
				{ id: 'service', m: 1, field },
				{ id: 'return' },
			] },
			{ id: 'lambda-capture' },
		]
		: [
			...generate(expr),
			{ id: 'object-get', key: field },
		]
	)
	: id === 'eq_' ?
		generateBinOp
	: id === 'fmt' ?
		generateOp
	: id === 'frame' ? (({ fs, ps, vn }) => [
		{ id: 'frame-get-ref', fs, ps, vn },
		{ id: 'deref' },
	])
	: id === 'if' ? (({ if_, then, else_ }) => {
		let elseLabel = newDummy();
		let fiLabel = newDummy();
		return [
			...generate(if_),
			{ id: 'jump-false', label: elseLabel },
			...generate(then),
			{ id: 'jump', label: fiLabel },
			{ id: ':', label: elseLabel },
			...generate(else_),
			{ id: ':', label: fiLabel },
		];
	})
	: id === 'index' ? (({ lhs, rhs }) => [
		...generate(lhs),
		...generate(rhs),
		{ id: 'array-get' },
	])
	: id === 'lambda' ?
		error('BAD')
	: id === 'lambda-async' ?
		error('BAD')
	: id === 'lambda-capture' ? (({ capture, bindCapture, bind, expr }) => {
		return [
			...generate(capture),
			{ id: 'label-segment', name: `${format(bind)} => ...`, segment: [
				...generate(expr),
				{ id: 'return' },
			] },
			{ id },
		];
	})
	: id === 'le_' ?
		generateBinOp
	: id === 'let' ?
		error('BAD')
	: id === 'lt_' ?
		generateBinOp
	: id === 'mod' ?
		generateBinOp
	: id === 'mul' ?
		generateBinOp
	: id === 'ne_' ?
		generateBinOp
	: id === 'neg' ?
		generateOp
	: id === 'new' ? (({ clazz }) =>
		false ? undefined
		: clazz === 'Error' ? [
			{ id: 'object' },
			{ id: 'label-segment', name: `new ${clazz}`, segment: [
				{ id: 'frame-get-ref', fs: 0, ps: 1 },
				{ id: 'deref' },
				{ id: 'return' },
			] },
			{ id: 'lambda-capture' },
		]
		: clazz === 'Map' ? [
			{ id: 'object' },
			{ id: 'label-segment', name: `new ${clazz}`, segment: [
				{ id: 'map' },
				{ id: 'object' },
				{ id: 'dup', i: 1 },
				{ id: 'label-segment', name: `Map.get`, segment: [
					{ id: 'frame-get-ref', fs: 0, ps: 0 },
					{ id: 'deref' },
					{ id: 'frame-get-ref', fs: 0, ps: 1 },
					{ id: 'deref' },
					{ id: 'map-get' },
					{ id: 'return' },
				] },
				{ id: 'lambda-capture' },
				{ id: 'object-put', key: 'get' },
				{ id: 'dup', i: 1 },
				{ id: 'label-segment', name: `Map.has`, segment: [
					{ id: 'frame-get-ref', fs: 0, ps: 0 },
					{ id: 'deref' },
					{ id: 'frame-get-ref', fs: 0, ps: 1 },
					{ id: 'deref' },
					{ id: 'map-has' },
					{ id: 'return' },
				] },
				{ id: 'lambda-capture' },
				{ id: 'object-put', key: 'has' },
				{ id: 'dup', i: 1 },
				{ id: 'label-segment', name: `Map.set`, segment: [
					{ id: 'frame-get-ref', fs: 0, ps: 0 },
					{ id: 'deref' },
					{ id: 'frame-get-ref', fs: 0, ps: 1 },
					{ id: 'deref' },
					{ id: 'pair-left' },
					{ id: 'frame-get-ref', fs: 0, ps: 1 },
					{ id: 'deref' },
					{ id: 'pair-right' },
					{ id: 'map-set' },
					{ id: 'undefined' },
					{ id: 'return' },
				] },
				{ id: 'lambda-capture' },
				{ id: 'object-put', key: 'set' },
				{ id: 'rotate' },
				{ id: 'discard' },
				{ id: 'return' },
			] },
			{ id: 'lambda-capture' },
		]
		: clazz === 'Promise' ? error('BAD')
		: error(`unknown class ${clazz}`)
	)
	: id === 'nil' ? (({}) =>
		[ast,]
	)
	: id === 'not' ?
		generateOp
	: id === 'num' ? (({}) =>
		[ast,]
	)
	: id === 'or_' ? (({ lhs, rhs }) =>
		generate(_if(lhs, _bool(true), rhs))
	)
	: id === 'pair' ?
		generateBinOp
	: id === 'pget' ? (({ expr, i }) => [
		...generate(expr),
		{ id: 'num', i },
		{ id: 'pair-get' },
	])
	: id === 'pos' ?
		generateOp
	: id === 'ref' ? (({ expr }) => false ? undefined
		: expr.id === 'deref' ? generate(expr.expr)
		: expr.id === 'frame' ? [{ id: 'frame-get-ref', fs: expr.fs, ps: expr.ps, vn: expr.vn },]
		: error('BAD')
	)
	: id === 'segment' ? (({ opcodes }) =>
		opcodes
	)
	: id === 'str' ? (({}) =>
		[ast,]
	)
	: id === 'struct' ? (({ kvs }) => [
		{ id: 'object' },
		...kvs.flatMap(({ key, value }) => [...generate(value), { id: 'object-put', key },]),
	])
	: id === 'sub' ?
		generateBinOp
	: id === 'tget' ? (({ expr, i }) => [
		...generate(expr),
		{ id: 'num', i },
		{ id: 'tuple-get' },
	])
	: id === 'throw' ?
		generateOp
	: id === 'try' ? (({ lhs, rhs }) => {
		let finallyLabel = newDummy();
		return [
			{ id: 'label-segment', name: `catch`, segment: [
				...generate(rhs),
				{ id: 'rotate' },
				{ id: 'app' },
				{ id: 'jump', label: finallyLabel },
			] },
			{ id: 'try-push' },
			...generate(lhs),
			{ id: 'rotate' },
			{ id: 'try-pop' },
			{ id: ':', label: finallyLabel },
		];
	})
	: id === 'tuple' ? (({ values }) => [
		{ id: 'nil' },
		...values.toReversed().flatMap(value => [...generate(value), { id: 'rotate' }, { id: 'cons' },]),
	])
	: id === 'typeof' ?
		generateOp
	: id === 'undefined' ? (({}) =>
		[{ id },]
	)
	: id === 'var' ?
		error('BAD')
	: id === 'while' ? (({ cond, loop, expr }) => {
		let loopLabel = newDummy();
		let exitLabel = newDummy();
		return [
			{ id: ':', label: loopLabel },
			...generate(cond),
			{ id: 'jump-false', label: exitLabel },
			...generate(loop),
			{ id: 'discard' },
			{ id: 'jump', label: loopLabel },
			{ id: ':', label: exitLabel },
			...generate(expr),
		];
	})
	: (({ id }) =>
		error(`unknown node ${id}`)
	);

	return f(ast);
};

let expand = opcodes => {
	for (let i = 0; i < opcodes.length; i = i + 1) {
		let { id, name, segment } = opcodes[i];
		let isSegment = id === 'label-segment';
		isSegment && function() {
			let label = newDummy();
			opcodes = [
				...opcodes,
				{ id: ':', label },
				{ id: 'comment', comment: `START ${name}` },
				...segment,
				{ id: 'comment', comment: `END ${name}` },
			];
			setp(opcodes[i], 'name', name);
			setp(opcodes[i], 'id', 'label');
			setp(opcodes[i], 'label', label);
			setp(opcodes[i], 'segment', undefined);
			return true;
		}();
	};

	return opcodes;
};

let interpret = opcodes => {
	let indexByLabel = {};

	for (let i = 0; i < opcodes.length; i = i + 1) {
		let { id, label } = opcodes[i];
		(id === ':' ? setp(indexByLabel, label, i) : undefined);
	};

	let catchHandler = undefined;

	let frames = [[],];
	let fcreate = () => frames.push([]);
	let fremove = () => frames.pop();
	let fpush = v => frames[frames.length - 1].push(v);
	let fpop = () => frames[frames.length - 1].pop();

	let rstack = [];
	let rpush = v => rstack.push(v);
	let rpop = () => assumeAny(rstack.pop());
	let ip = 0;

	let interpretBinOp = f => {
		let b = rpop();
		let a = rpop();
		rpush(f(a, b));
	};

	while (ip < opcodes.length) {
		let opcode = opcodes[ip];
		let { id } = opcode;

		env.LOG && function() {
			let opts = { breakLength: 9999, compact: true, depth: 9 };
			console.error(`----------`);
			console.error(`FRAMES = ${require('util').inspect(frames, opts)}`);
			console.error(`RSTACK = ${require('util').inspect(rstack, opts)}`);
			console.error(`IP = ${ip} ${Object.keys(opcode).map(k => opcode[k]).join(' ')}`);
			return true;
		}();

		ip = ip + 1;

		let f = false ? undefined
		: id === ':' ?
			undefined
		: id === 'add' ?
			interpretBinOp((a, b) => a + b)
		: id === 'and' ?
			error('BAD')
		: id === 'app' ? function() {
			let parameter = rpop();
			let lambda = rpop();
			rpush(ip);
			ip = lambda.label;
			fcreate();
			fpush([lambda.capture]);
			fpush([parameter]);
		}()
		: id === 'array-get' ? function() {
			let i = rpop();
			let array = rpop();
			rpush(array[i]);
		}()
		: id === 'array-set' ? function() {
			let value = rpop();
			let index = rpop();
			let array = rpop();
			fake(array)[fake(index)] = value;
		}()
		: id === 'assign-ref' ? function() {
			let value = rpop();
			let ref = rpop();
			ref[0] = value;
		}()
		: id === 'argv' ?
			rpush(['parse.js', ...argv,])
		: id === 'bool' ?
			rpush(opcode.b)
		: id === 'coal' ?
			error('BAD')
		: id === 'comment' ?
			undefined
		: id === 'cons' ?
			interpretBinOp(vec.cons)
		: id === 'deref' ?
			rpush(rpop()[0])
		: id === 'discard' ?
			rpop()
		: id === 'div' ?
			interpretBinOp((a, b) => a / b)
		: id === 'dup' ?
			rpush(rstack[rstack.length - 1 - opcode.i])
		: id === 'eq_' ?
			interpretBinOp((a, b) => a === b)
		: id === 'exit' ? function() {
			ip = 1 / 0;
			return undefined;
		}()
		: id === 'fmt' ?
			rpush(`${rpop()}`)
		: id === 'frame-alloc' ?
			fpush([undefined])
		: id === 'frame-dealloc' ?
			fpop()
		: id === 'frame-get-ref' ?
			rpush(frames[frames.length - 1 - opcode.fs][opcode.ps])
		: id === 'frame-push' ?
			fpush([rpop()])
		: id === 'jump' ? function() {
			ip = getp(indexByLabel, opcode.label);
			return undefined;
		}()
		: id === 'jump-false' ? function() {
			ip = rpop() ? ip : getp(indexByLabel, opcode.label);
			return undefined;
		}()
		: id === 'label' ?
			rpush(getp(indexByLabel, opcode.label))
		: id === 'lambda-capture' ? function() {
			let label = rpop();
			let capture = rpop();
			rpush({ capture, label });
		}()
		: id === 'le_' ?
			interpretBinOp((a, b) => a <= b)
		: id === 'lt_' ?
			interpretBinOp((a, b) => a < b)
		: id === 'map' ?
			rpush(new Map())
		: id === 'map-get' ?
			interpretBinOp((map, key) => map.get(key))
		: id === 'map-has' ?
			interpretBinOp((map, key) => map.has(key))
		: id === 'map-set' ? function() {
			let value = rpop();
			let key = rpop();
			let map = rpop();
			map.set(key, value);
		}()
		: id === 'mod' ?
			interpretBinOp((a, b) => a % b)
		: id === 'mul' ?
			interpretBinOp((a, b) => a * b)
		: id === 'ne_' ?
			interpretBinOp((a, b) => a !== b)
		: id === 'neg' ?
			rpush(-rpop())
		: id === 'nil' ?
			rpush([])
		: id === 'not' ?
			rpush(!rpop())
		: id === 'num' ?
			rpush(opcode.i)
		: id === 'object' ?
			rpush({})
		: id === 'object-get' ?
			rpush(getp(rpop(), opcode.key))
		: id === 'object-put' ? function() {
			let value = rpop();
			let object = rpop();
			setp(object, opcode.key, value);
			rpush(object);
		}()
		: id === 'or_' ?
			error('BAD')
		: id === 'pair' ?
			interpretBinOp((a, b) => [a, b])
		: id === 'pair-get' ? function() {
			let i = rpop();
			let pair = rpop();
			rpush(pair[i]);
		}()
		: id === 'pair-left' ?
			rpush(rpop()[0])
		: id === 'pair-right' ?
			rpush(rpop()[1])
		: id === 'pair-set' ? function() {
			let value = rpop();
			let pair = rpop();
			seti(pair, opcode.i, value);
		}()
		: id === 'pos' ?
			rpush(+rpop())
		: id === 'return' ? function() {
			let rc = rpop();
			fpop();
			fpop();
			fremove();
			ip = rpop();
			rpush(rc);
		}()
		: id === 'rotate' ? function() {
			let b = rpop();
			let a = rpop();
			rpush(b);
			rpush(a);
		}()
		: id === 'service' && opcode.f === 1 && opcode.field === 'length' ?
			rpush(rpop().length)
		: id === 'service' && opcode.m === 1 && opcode.field === 'pop' ?
			rpush(rpop().pop())
		: id === 'service' && opcode.m === 1 && opcode.field === 'toReversed' ?
			rpush(rpop().toReversed())
		: id === 'service' && opcode.m === 1 && opcode.field === 'toString' ?
			rpush(rpop().toString())
		: id === 'service' && opcode.m === 1 && opcode.field === 'trim' ?
			rpush(rpop().trim())
		: id === 'service' && opcode.m === 2 && opcode.field === 'charCodeAt' ? function() {
			let [a, b] = rpop();
			return rpush(a.charCodeAt(b));
		}()
		: id === 'service' && opcode.m === 2 && opcode.field === 'concat' ? function() {
			let [a, b] = rpop();
			return rpush(a.concat(b));
		}()
		: id === 'service' && opcode.m === 2 && opcode.field === 'endsWith' ? function() {
			let [a, b] = rpop();
			return rpush(a.endsWith(b));
		}()
		: id === 'service' && opcode.m === 2 && opcode.field === 'includes' ? function() {
			let [a, b] = rpop();
			return rpush(a.includes(b));
		}()
		: id === 'service' && opcode.m === 2 && opcode.field === 'indexOf' ? function() {
			let [a, b] = rpop();
			return rpush(a.indexOf(b));
		}()
		: id === 'service' && opcode.m === 2 && opcode.field === 'join' ? function() {
			let [a, b] = rpop();
			return rpush(a.join(b));
		}()
		: id === 'service' && opcode.m === 2 && opcode.field === 'push' ? function() {
			let [a, b] = rpop();
			return rpush(a.push(b));
		}()
		: id === 'service' && opcode.m === 2 && opcode.field === 'startsWith' ? function() {
			let [a, b] = rpop();
			return rpush(a.startsWith(b));
		}()
		: id === 'service' && opcode.m === 3 && opcode.field === 'slice' ? function() {
			let [a, [b, c]] = rpop();
			return rpush(a.slice(b, c));
		}()
		: id === 'service' && opcode.n === 1 && opcode.service === 'JSON.parse' ?
			rpush(JSON.parse(rpop()))
		: id === 'service' && opcode.n === 1 && opcode.service === 'Object.assign' ?
			rpush(Object.assign(rpop()))
		: id === 'service' && opcode.n === 1 && opcode.service === 'Object.entries' ?
			rpush(Object.entries(rpop()))
		: id === 'service' && opcode.n === 1 && opcode.service === 'Object.fromEntries' ?
			rpush(Object.fromEntries(rpop()))
		: id === 'service' && opcode.n === 1 && opcode.service === 'Object.keys' ?
			rpush(Object.keys(rpop()))
		: id === 'service' && opcode.n === 1 && opcode.service === 'Promise.reject' ?
			rpush(Promise.reject(rpop()))
		: id === 'service' && opcode.n === 1 && opcode.service === 'Promise.resolve' ?
			rpush(Promise.resolve(rpop()))
		: id === 'service' && opcode.n === 1 && opcode.service === 'console.error' ?
			rpush(console.error(rpop()))
		: id === 'service' && opcode.n === 1 && opcode.service === 'console.log' ?
			rpush(console.log(rpop()))
		: id === 'service' && opcode.n === 1 && opcode.service === 'require' ?
			rpush(require(rpop()))
		: id === 'service' && opcode.n === 2 && opcode.service === 'require("fs").readFileSync' ? function() {
			let [a, b] = rpop();
			rpush(require('fs').readFileSync(a, b));
		}()
		: id === 'service' && opcode.n === 2 && opcode.service === 'require("util").inspect' ? function() {
			let [a, b] = rpop();
			rpush(require('util').inspect(a, b));
		}()
		: id === 'service' && opcode.n === 3 && opcode.service === 'JSON.stringify' ? function() {
			let [a, [b, c]] = rpop();
			rpush(JSON.stringify(a, b, c));
		}()
		: id === 'str' ?
			rpush(opcode.v)
		: id === 'sub' ?
			interpretBinOp((a, b) => a - b)
		: id === 'throw' ? function() {
			let thrown = rpop();
			return catchHandler !== undefined ? function() {
				let { label, fl, fsl, rsl } = catchHandler;
				ip = label ?? error(`THROWN ${rpop()}`);
				while (fsl < frames.length) { fremove(); };
				while (fl < frames[frames.length - 1].length) { fpop(); };
				while (rsl < rstack.length) { rpop(); };
				catchHandler = rpop();
				rpush(thrown);
				return undefined;
			}()
			: error(thrown);
		}()
		: id === 'try-pop' ? function() {
			catchHandler = rpop();
			return undefined;
		}()
		: id === 'try-push' ? function() {
			let label = rpop();
			rpush(catchHandler);
			catchHandler = {
				label,
				fl: frames[frames.length - 1].length,
				fsl: frames.length,
				rsl: rstack.length,
			};
			return undefined;
		}()
		: id === 'tuple-get' ? function() {
			let i = rpop();
			let tuple = rpop();
			rpush(tuple[i]);
		}()
		: id === 'tuple-set' ? function() {
			let value = rpop();
			let index = rpop();
			let tuple = rpop();
			seti(tuple, index, value);
		}()
		: id === 'typeof' ?
			rpush(typeof (rpop()))
		: id === 'undefined' ?
			rpush(undefined)
		:
			error('BAD');
	};

	return rstack.length !== 1 ? error('RSTACK RESIDUE')
	: frames.length !== 1 && frames[0].length !== 0 ? error('FRAME RESIDUE')
	: rpop();
};

let types = typesModule();

parseAstType = program => {
	let pos0;
	let posx;
	while (function() {
		pos0 = program.indexOf('\/\/ ', 0);
		posx = 0 <= pos0 ? program.indexOf('\n', pos0) : -1;
		return 0 <= posx;
	}()) {
		program = program.slice(0, pos0) + program.slice(posx, undefined);
	};

	let ast = parser.parse(program);

	let ast_ = [ast,]
	.map(ast => rewriteNe(ast))
	.map(ast => rewriteBind()(ast))
	.map(ast => rewriteAsync(ast))
	.map(ast => unpromisify(ast))
	[0];

	return { ast: ast_, type: types.infer(ast) };
};

let processRewrite = ast => {
	let roots = ['JSON', 'Object', 'Promise', 'console', 'fs_readFileSync', 'process', 'require', 'util_inspect',]
		.reduce((v, vl) => ll.cons(vl, v), ll.empty());

	let ast_ = [ast,]
	.map(ast => rewriteFsReadFileSync(ast))
	.map(ast => rewriteIntrinsics(ast))
	.map(ast => rewriteRenameVar(newDummy(), ll.map_(roots, v => [v, v]), ast))
	.map(ast => rewriteCapture()(roots, ast))
	[0];

	return { ast: ast_ };
};

let processGenerate = ast => {
	let proxy = (n, service) => _lambdaCapture(_undefined, _var(newDummy()), _var(newDummy()), _segment([
		{ id: 'frame-get-ref', fs: 0, ps: 1 },
		{ id: 'deref' },
		{ id: 'service', n, service },
	]));

	let add = (ast, c, v) => _let(_var(c), v, ast);

	let addObjectOfFunctions = (ast, c, fns) => add(
		ast,
		c,
		_struct(fns.map(({ f, n }) => ({ key: f, value: proxy(n, `${c}.${f}`) }))));

	let ast_ = [ast,]
	.map(ast => addObjectOfFunctions(ast, 'JSON', [
		{ f: 'parse', n: 1 },
		{ f: 'stringify', n: 3 },
	]))
	.map(ast => addObjectOfFunctions(ast, 'Object', [
		{ f: 'assign', n: 1 },
		{ f: 'entries', n: 1 },
		{ f: 'fromEntries', n: 1 },
		{ f: 'keys', n: 1 },
	]))
	.map(ast => addObjectOfFunctions(ast, 'Promise', [
		{ f: 'reject', n: 1 },
		{ f: 'resolve', n: 1 },
	]))
	.map(ast => addObjectOfFunctions(ast, 'console', [
		{ f: 'error', n: 1 },
		{ f: 'log', n: 1 },
	]))
	.map(ast => add(ast, 'fs_readFileSync', proxy(2, 'require("fs").readFileSync')))
	.map(ast => add(ast, 'process', _struct([
		{ key: 'argv', value: _segment([{ id: 'argv' },]) },
		{ key: 'env', value: _struct(Object.entries(process.env).map(([key, v]) => ({ key, value: _str(v) }))) },
	])))
	.map(ast => add(ast, 'require', proxy(1, 'require')))
	.map(ast => add(ast, 'util_inspect', proxy(2, 'require("util").inspect')))
	.map(ast => rewriteVars(0, 0, ll.empty(), ast))
	[0];

	return expand([...generate(ast_), { id: 'exit' },]);
};

let actual = stringify(parseAstType(`
	let parse = ast => ast;
	console.log(parse(require('fs').readFileSync(0, 'utf8')))
`).ast);

let expect = stringify(
	_let(
		_var('parse'),
		_lambda(_var('ast'), _var('ast')),
		_app(
			_dot(_var('console'), 'log'),
			_app(
				_var('parse'),
				_app(
					_dot(_app(_var('require'), _str('fs')), 'readFileSync'),
					_pair(_num(0), _str('utf8'))
				)
			)
		)
	)
);

return actual === expect
? function() {
	try {
		let prog;
		let pp;
		let pr;
		let pg;

		let input = () => arg === undefined || arg === '-'
			? require('fs').readFileSync(0, 'utf8')
			: require('fs').readFileSync(arg, 'utf8');

		let program = () => {
			prog = prog ?? input();
			return prog;
		};

		let ast0 = () => { pp = pp ?? parseAstType(program()); return pp.ast; };
		let ast1 = () => { pr = pr ?? processRewrite(ast0()); return pr.ast; };
		let opcodes = () => { pg = pg ?? processGenerate(ast1()); return pg; };
		let type = () => { pp = pp ?? parseAstType(program()); return pp.type; };

		env.AST
			? console.error(`ast :: ${stringify(ast1())}`)
			: undefined;

		env.EVALUATE
			? console.error(`evaluate :: ${stringify(evaluate(evaluateVvs)(rewriteIntrinsics(ast0())))}`)
			: undefined;

		env.FORMAT
			? console.error(`format :: ${format(ast1())}`)
			: undefined;

		env.GENERATE0
			? console.log(JSON.stringify(opcodes(), undefined, undefined))
			: undefined;

		env.GENERATE1 && function() {
			let opcodes_ = opcodes();
			let instructions = [];
			for (let i = 0; i < opcodes_.length; i = i + 1) {
				let opcode = opcodes_[i];
				instructions.push(`\n${i} ${Object.keys(opcode).map(k => opcode[k]).join(' ')}`);
			};
			console.error(`generate :: ${instructions.join('')}`);
			return true;
		}();

		env.INTERPRET0
			? console.error(`interpret :: ${stringify(interpret(JSON.parse(input())))}`)
			: undefined;

		env.INTERPRET
			? console.error(`interpret :: ${stringify(interpret(opcodes()))}`)
			: undefined;

		env.TYPE
			? console.error(`type :: ${types.dump(type())}`)
			: undefined;

		env.VERBOSE && function() {
			console.error(`argv :: ${stringify(process.argv)}`);
			console.error(`env :: ${stringify(process.env)}`);
			return true;
		}();

		return true;
	} catch (e) { return console.error(e); }
}() : error(`
test case failed,
actual = ${actual}
expect = ${expect}`);
