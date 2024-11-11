// cat src/main/js/parse.js | node src/main/js/parse.js
// parse a javascript subset and inference variable types.
// able to parse myself.

// a, b is a pair.
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

let get0 = tuple => { let [a, b] = tuple; return a; };
let get1 = tuple => { let [a, b] = tuple; return b; };
let seti = (m, k, v) => { fake(m)[0 <= k && fake(k)] = v; return v; };
let getp = (m, k) => fake(m)[k !== '' && fake(k)];
let setp = (m, k, v) => { fake(m)[k !== '' && fake(k)] = v; return v; };

let find;
find = (es, op) => isNotEmpty(es) ? function() {
	let e = head(es);
	return op(e) ? e : find(tail(es), op);
}() : undefined;

let contains;
contains = (es, e) => isNotEmpty(es) && (head(es) === e || contains(tail(es), e));

let foldl;
foldl = (init, es, op) => isNotEmpty(es) ? foldl(op(init, head(es)), tail(es), op) : init;

let foldr;
foldr = (init, es, op) => isNotEmpty(es) ? op(foldr(init, tail(es), op), head(es)) : init;

let findk = (kvs, k) => {
	let kv = find(kvs, ([k_, v]) => k_ === k);
	return kv !== undefined ? get1(kv) : error(`variable ${k} not found`);
};

let gen = i => {
	let array = [];
	while (0 < i) (function() {
		i = i - 1;
		array.push(i);
	}());
	return array;
};

let dummyCount = 0;

let newDummy = () => {
	dummyCount = dummyCount + 1;
	return `dummy${dummyCount}`;
};

let dump = v => {
	let dump_;
	dump_ = (vs, v) => false ? undefined
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
		:
			v.toString();
	return dump_(nil, v);
};

let _add = (lhs, rhs) => ({ id: 'add', lhs, rhs });
let _alloc = (vn, expr) => ({ id: 'alloc', vn, expr });
let _app = (lhs, rhs) => ({ id: 'app', lhs, rhs });
let _assign = (bind, value, expr) => ({ id: 'assign', bind, value, expr });
let _bool = v => ({ id: 'bool', v });
let _cons = (lhs, rhs) => ({ id: 'cons', lhs, rhs });
let _dot = (expr, field) => ({ id: 'dot', expr, field });
let _eq = (lhs, rhs) => ({ id: 'eq_', lhs, rhs });
let _error = { id: 'new', clazz: 'Error' };
let _frame = (fs, ps) => ({ id: 'frame', fs, ps });
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
let _str = v => ({ id: 'str', v });
let _struct = kvs => ({ id: 'struct', kvs });
let _throw = expr => ({ id: 'throw', expr });
let _try = (lhs, rhs) => ({ id: 'try', lhs, rhs });
let _tuple = values => ({ id: 'tuple', values });
let _typeof = expr => ({ id: 'typeof', expr });
let _undefined = { id: 'undefined' };
let _var = vn => ({ id: 'var', vn });
let _void = { id: 'struct', completed: true, kvs: [] };
let _while = (cond, loop, expr) => ({ id: 'while', cond, loop, expr });

let lexerModule = () => {
	let isAlphabet = ch => ascii('A') <= ch && ch <= ascii('Z') || ascii('a') <= ch && ch <= ascii('z');
	let isNum = ch => ascii('0') <= ch && ch <= ascii('9');
	let isId = ch => isAlphabet(ch) || isNum(ch) || ch === ascii('_');

	let lex = (s, pos) => {
		let op2s = ['<=', '&&', '||', '??', '|>',];
		let op3s = ['!==', '===',];

		let i = pos;
		let tokenss = [];

		while (i < s.length) (function() {
			let ch = s.charCodeAt(i);
			let op2 = s.slice(i, i + 2);
			let op3 = s.slice(i, i + 3);

			let j = i + 1;

			let skip = f => {
				while (j < s.length && f(s.charCodeAt(j))) (function() {
					j = j + 1;
				}());
			};

			let tokens = false ? undefined
				: ch === 9 || ch === 10 || ch === 13 || ch === 32 ?
					[]
				: ch === ascii("'") || ch === ascii('"') || ch === ascii('`') ? function() {
					skip(c => c !== ch);
					return [{ lex: 'str', s: s.slice(i + 1, j - 1) },];
				}()
				: isAlphabet(ch) ? function() {
					skip(isId);
					return [{ lex: 'id', s: s.slice(i, j) },];
				}()
				: isNum(ch) ? function() {
					skip(isNum);
					return [{ lex: 'num', s: s.slice(i, j) },];
				}()
				: op2 === '//' ? function() {
					skip(c => c !== 10);
					return [];
				}()
				: op2s.includes(op2) ? function() {
					j = i + op2.length;
					return [{ lex: 'sym', s: op2 },];
				}()
				: op3s.includes(op3) ? function() {
					j = i + op3.length;
					return [{ lex: 'sym', s: op3 },];
				}()
				: function() {
					let sym = s.slice(i, j);
					return [{ lex: 'sym', s: sym },];
				}();

			i = j;

			tokenss.push(tokens);

			let t = false ? undefined
				: ch === 9 || ch === 10 || ch === 13 || ch === 32 ? ' '
				: ascii("'") === ch ? "'"
				: ascii('"') === ch ? '"'
				: ascii('`') === ch ? '`'
				: ascii('0') <= ch && ch <= ascii('9') ? 'N'
				: ascii('A') <= ch && ch <= ascii('Z') ? 'A'
				: ch === ascii('_') ? 'A'
				: ascii('a') <= ch && ch <= ascii('z') ? 'A'
				: '$';
		}());

		return tokenss.flatMap(tokens => tokens);
	};

	return { lex };
};

let parserModule = () => {
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

		return false ? undefined
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

			let exprToString = _app(
				_dot(parseApplyBlockFieldIndex(expr_), '.toString'),
				_void);

			return _add(
				_str(program.slice(0, index)),
				_add(exprToString, parseBackquote(right)));
		}() : _str(program);
	};

	let parseConstant = program => {
		let first = program.charCodeAt(0);

		return false ? undefined
		: ascii('0') <= first && first <= ascii('9') ? _num(parseNumber(program))
		: program.startsWith("'") && program.endsWith("'") ? _str(program.slice(1, -1))
		: program.startsWith('"') && program.endsWith('"') ? _str(program.slice(1, -1))
		: program.startsWith('`') && program.endsWith('`') ? parseBackquote(program.slice(1, -1))
		: program === 'false' ? _bool(program)
		: program === 'new Error' ? { id: 'new', clazz: 'Error' }
		: program === 'new Map' ? { id: 'new', clazz: 'Map' }
		: program === 'new Promise' ? { id: 'new', clazz: 'Promise' }
		: program === 'nil' ? _nil
		: program === 'true' ? _bool(program)
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
						let head_ = parseArray_(head.slice(3, 0));
						return tail_ !== '' ? _app(_dot(head_, '.concat'), parseArray_(tail_)) : head_;
					}()
					: _cons(parse(head), parseArray_(tail_));
			}()
			: _nil;
		};
		return parseArray_(program);
	};

	let parseTuple = (program, parse) => _tuple((keepsplitl(program + ',', ',', parse)));

	let parseArrayTuple = (program_, parse) => {
		let program = program_.slice(1, -1).trim();
		return (program === '' || program.endsWith(',') ? parseArray : parseTuple)(program, parse);
	};

	let parseStructInner = (program, parse) => {
		let appendTrailingComma = s => s + (s === '' || s.endsWith(',') ? '' : ',');

		return _struct(keepsplitl(appendTrailingComma(program), ',', kv => {
			let [key_, value_] = splitl(kv, ':');
			let field = parseConstant(key_.trim()).vn;
			let value = value_ !== undefined ? parse(value_) : _var(field);
			return { key: '.' + field, value };
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
			_dot(parseApplyBlockFieldIndex(expr), '.' + field)
		: program.endsWith(']') ? function() {
			let [expr, index_] = splitr(program, '[');
			let index = index_.slice(0, -1);
			return expr === undefined ? parseValue(program)
			: _index(parse(expr), parse(index));
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
			_app(parse(program.slice(0, -2)), _void)
		: program.endsWith(')') ? function() {
			let [expr, paramStr_] = splitr(program, '(');
			let paramStr = paramStr_.slice(0, -1).trim();
			return expr !== undefined ? _app(parse(expr), parse(paramStr)) : parseValue(program);
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
			: statement.startsWith('while ') ? function() {
				let [cond, loop] = splitl(statement.slice(6, undefined), ' ');
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
	: id === 'assign' ? (({ bind, value, expr }) => ({ id, bind, value: rf(value), expr: rf(expr) }))
	: id === 'await' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'bool' ? (({ v }) => ast)
	: id === 'coal' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'cons' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'div' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'dot' ? (({ expr, field }) => ({ id, expr: rf(expr), field }))
	: id === 'eq_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'frame' ? (({ fs, ps }) => ast)
	: id === 'if' ? (({ if_, then, else_ }) => ({ id, if_: rf(if_), then: rf(then), else_: rf(else_) }))
	: id === 'index' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'lambda' ? (({ bind, expr }) => ({ id, bind, expr: rf(expr) }))
	: id === 'lambda-async' ? (({ bind, expr }) => ({ id, bind, expr: rf(expr) }))
	: id === 'lambda-capture' ? (({ capture, bindCapture, bind, expr }) =>
		({ id, capture: rf(capture), bindCapture, bind, expr: rf(expr) })
	)
	: id === 'le_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'let' ? (({ bind, value, expr }) => ({ id, bind, value: rf(value), expr: rf(expr) }))
	: id === 'lt_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'mul' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'ne_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'neg' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'new' ? (({}) => ast)
	: id === 'nil' ? (({}) => ast)
	: id === 'not' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'num' ? (({ i }) => ast)
	: id === 'or_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'pair' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'pos' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'str' ? (({ v }) => ast)
	: id === 'struct' ? (({ kvs }) => ({ id, kvs: kvs.map(({ key, value }) => ({ key, value: rf(value) })) }))
	: id === 'sub' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
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

let precs = function() {
	let precOrder = [
		['bool', 'nil', 'num', 'str', 'struct', 'tuple', 'undefined', 'var',],
		['new',],
		['cons', 'struct', 'tuple',],
		['typeof',],
		['app',],
		['await',],
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
	let i = 0;
	while (i < precOrder.length) (function() {
		precOrder[i].map(id => setp(precs, id, i));
		i = i + 1;
	}());
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
	let priority_ = getp(precs, id);

	let fm = ast => format_(priority_ - 1, ast);
	let fmt = ast => format_(priority_, ast);

	let f = false ? undefined
	: id === 'add' ? (({ lhs, rhs }) => `${fm(lhs)} + ${fmt(rhs)}`)
	: id === 'and' ? (({ lhs, rhs }) => `${fm(lhs)} && ${fmt(rhs)}`)
	: id === 'app' ? (({ lhs, rhs }) => `${fmt(lhs)}(${format(rhs)})`)
	: id === 'await' ? (({ expr }) => `await ${fmt(expr)}`)
	: id === 'bool' ? (({ v }) => v)
	: id === 'coal' ? (({ lhs, rhs }) => `${fm(lhs)} ?? ${fmt(rhs)}`)
	: id === 'cons' ? (({ lhs, rhs }) => `[${fmt(lhs)}, ...${fmt(rhs)}]`)
	: id === 'div' ? (({ lhs, rhs }) => `${fmt(lhs)} / ${fm(rhs)}`)
	: id === 'dot' ? (({ expr, field }) => `${fmt(expr)}${field}`)
	: id === 'eq_' ? (({ lhs, rhs }) => `${fmt(lhs)} === ${fmt(rhs)}`)
	: id === 'frame' ? (({ fs, ps }) => `frame[${fs}][${ps}]`)
	: id === 'if' ? (({ if_, then, else_ }) => `${fm(if_)} ? ${fmt(then)} : ${fmt(else_)}`)
	: id === 'index' ? (({ lhs, rhs }) => `${fmt(lhs)}[${format(rhs)}]`)
	: id === 'lambda' ? (({ bind, expr }) => `${fmt(bind)} => ${fmt(expr)}`)
	: id === 'lambda-async' ? (({ bind, expr }) => `async ${fmt(bind)} => ${fmt(expr)}`)
	: id === 'lambda-capture' ? (({ capture, bindCapture, bind, expr }) =>
		`|${fmt(capture)}| ${fmt(bind)} => |${fmt(bindCapture)}| ${fmt(expr)}`
	)
	: id === 'le_' ? (({ lhs, rhs }) => `${fm(lhs)} <= ${fmt(rhs)}`)
	: id === 'lt_' ? (({ lhs, rhs }) => `${fm(lhs)} < ${fmt(rhs)}`)
	: id === 'mul' ? (({ lhs, rhs }) => `${fm(lhs)} * ${fmt(rhs)}`)
	: id === 'ne_' ? (({ lhs, rhs }) => `${fm(lhs)} !== ${fmt(rhs)}`)
	: id === 'neg' ? (({ expr }) => `- ${fmt(expr)}`)
	: id === 'new' ? (({ clazz }) => `new ${clazz}`)
	: id === 'nil' ? (({}) => '[]')
	: id === 'not' ? (({ expr }) => `! ${fmt(expr)}`)
	: id === 'num' ? (({ i }) => `${i}`)
	: id === 'or_' ? (({ lhs, rhs }) => `${fm(lhs)} || ${fmt(rhs)}`)
	: id === 'pair' ? (({ lhs, rhs }) => `${fm(lhs)}, ${fmt(rhs)}`)
	: id === 'pos' ? (({ expr }) => `+ ${fmt(expr)}`)
	: id === 'str' ? (({ v }) => `'${v}'`)
	: id === 'struct' ? (({ kvs }) => {
		let s = kvs.map(({ key, value }) => {
			let k = key.slice(1, undefined);
			return value.id === 'var' && value.vn === k ? k : `${k}: ${fmt(value)}`;
		}).join(', ');
		return `{ ${s} }`;
	})
	: id === 'sub' ? (({ lhs, rhs }) => `${fmt(lhs)} - ${fm(rhs)}`)
	: id === 'tuple' ? (({ values }) => `[${values.map(fm).join(', ')}]`)
	: id === 'typeof' ? (({ expr }) => `typeof ${fmt(expr)}`)
	: id === 'undefined' ? (({}) => `${id}`)
	: id === 'var' ? (({ vn }) => vn)
	: (({}) => `function() { ${formatBlock(ast)}; }()`);

	return priority_ <= priority ? f(ast) : `(${f(ast)})`;
};

let typesModule = () => {
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

	let dump = v => {
		let dumpType_;
		dumpType_ = (vs, v) => {
			let { ref } = v;
			let listv = assumeList(v);
			return false ? undefined
			: contains(vs, v) ?
				'<recurse>'
			: ref !== undefined ?
				(refs.get(ref) !== v ? dumpType_(cons(v, vs), refs.get(ref)) : `_${ref}`)
			: typeof v === 'object' ? (false ? undefined
				: isEmpty(listv) ?
					''
				: isNotEmpty(listv) ?
					`${dumpType_(vs, head(listv))}:${dumpType_(vs, assumeObject(tail(listv)))}`
				: function() {
					let t = v.t;
					let join = Object
						.entries(v)
						.filter(([k, v_]) => k !== 't')
						.map(([k, v_]) => `${k}:${dumpType_(cons(v, vs), v_)}`)
						.join(' ');
					return t !== undefined ? `${t}(${join})` : `{${join}}`;
				}()
			)
			: typeof v === 'string' ?
				v.toString()
			: JSON.stringify(v, undefined, undefined);
		};
		return dumpType_(nil, v);
	};

	let tryBind;

	tryBind = (a, b) => function() {
		let lista = assumeList(a);
		let listb = assumeList(b);
		let refa = a.ref;
		let refb = b.ref;

		return false ? undefined
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

	let doBind_ = (msg, a, b) => tryBind(a, b) || error(`in ${msg()}:\ncannot bind types between\nfr: ${dump(a)}\nto: ${dump(b)}`);
	let doBind = (ast, a, b) => doBind_(() => format(ast), a, b);

	let cloneRef = v => {
		let fromTos = new Map();
		let cloneRef_;

		cloneRef_ = v => {
			let { ref } = v;
			let vlist = assumeList(v);
			return false ? undefined
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

	let bindTypes;

	bindTypes = (vts, ast) => false ? undefined
		: ast.id === 'nil' ? vts
		: ast.id === 'pair' ? bindTypes(bindTypes(vts, ast.lhs), ast.rhs)
		: ast.id === 'struct' ? foldl(vts, ast.kvs, (vts_, kv) => bindTypes(vts_, kv.value))
		: ast.id === 'tuple' ? foldl(vts, ast.values, bindTypes)
		: ast.id === 'var' ? cons([ast.vn, newRef()], vts)
		: error(`bindTypes(): cannot destructure ${format(ast)}`);

	let tyArrayOf = type => ({ t: 'array', of: type });
	let tyBoolean = ({ t: 'bool' });
	let tyError = ({ t: 'error' });
	let tyLambdaOf = (in_, out) => ({ t: 'lambda', generic: true, in_, out });
	let tyLambdaOfFixed = (in_, out) => ({ t: 'lambda', in_, out });
	let tyNumber = ({ t: 'num' });
	let tyPairOf = (lhs, rhs) => ({ t: 'pair', lhs, rhs });
	let tyPromiseOf = out => ({ t: 'promise', out });
	let tyString = tyArrayOf({ t: 'char' });
	let tyStructOf = kvs => ({ t: 'struct', kvs });
	let tyStructOfCompleted = kvs => { setp(kvs, 'completed', true); return ({ t: 'struct', kvs }); };
	let tyTupleOf = types => ({ t: 'tuple', types });
	let tyVoid = tyStructOfCompleted({});

	let tyMapOf = (tk, tv) => tyStructOfCompleted({
		'.get': tyLambdaOfFixed(tk, tv),
		'.has': tyLambdaOfFixed(tk, tyBoolean),
		'.set': tyLambdaOfFixed(tyPairOf(tk, tv), tyVoid),
	});

	let inferDot = (ast, ts, field) => {
		return false ? undefined
		: field === '.charCodeAt' ?
			doBind(ast, ts, tyString) && tyLambdaOf(tyNumber, tyNumber)
		: field === '.concat' ? function() {
			let ta = tyArrayOf(newRef());
			return doBind(ast, ts, ta) && tyLambdaOf(ta, ta);
		}()
		: field === '.endsWith' ?
			doBind(ast, ts, tyString) && tyLambdaOf(tyString, tyBoolean)
		: field === '.filter' ? function() {
			let ti = newRef();
			return doBind(ast, ts, tyArrayOf(ti)) && tyLambdaOf(tyLambdaOf(ti, tyBoolean), tyArrayOf(ti));
		}()
		: field === '.flatMap' ? function() {
			let ti = newRef();
			let to = newRef();
			return doBind(ast, ts, tyArrayOf(ti)) && tyLambdaOf(tyLambdaOf(ti, tyArrayOf(to)), tyArrayOf(to));
		}()
		: field === '.includes' ? function() {
			let te = newRef();
			return doBind(ast, ts, tyArrayOf(te)) && tyLambdaOf(te, tyBoolean);
		}()
		: field === '.indexOf' ?
			doBind(ast, ts, tyString) && tyLambdaOf(tyPairOf(tyString, tyNumber), tyNumber)
		: field === '.join' ?
			doBind(ast, ts, tyArrayOf(tyString)) && tyLambdaOf(tyString, tyString)
		: field === '.length' ?
			doBind(ast, ts, tyArrayOf(newRef())) && tyNumber
		: field === '.map' ? function() {
			let ti = newRef();
			let to = newRef();
			return doBind(ast, ts, tyArrayOf(ti)) && tyLambdaOf(tyLambdaOf(ti, to), tyArrayOf(to));
		}()
		: field === '.push' ? function() {
			let te = newRef();
			return doBind(ast, ts, tyArrayOf(te)) && tyLambdaOf(te, tyVoid);
		}()
		: field === '.reduce' ? function() {
			let te = newRef();
			let tr = newRef();
			let treducer = tyLambdaOf(tyPairOf(tr, te), tr);
			return doBind(ast, ts, tyArrayOf(te))
				&& tyLambdaOf(tyPairOf(treducer, tr), tr);
		}()
		: field === '.reverse' ? function() {
			let tl = tyArrayOf(newRef());
			return doBind(ast, ts, tl) && tyLambdaOf(tyVoid, tl);
		}()
		: field === '.slice' ? function() {
			let te = newRef();
			let tl = tyArrayOf(te);
			return doBind(ast, ts, tl) && tyLambdaOf(tyPairOf(tyNumber, tyNumber), tl);
		}()
		: field === '.startsWith' ?
			doBind(ast, ts, tyString) && tyLambdaOf(tyString, tyBoolean)
		: field === '.then' ? function() {
			let ti = newRef();
			let to = newRef();
			return doBind(ast, ts, tyPromiseOf(ti)) && tyLambdaOf(ti, tyPromiseOf(to));
		}()
		: field === '.toString' ?
			doBind(ast, ts, newRef()) && tyLambdaOf(tyVoid, tyString)
		: field === '.trim' ?
			doBind(ast, ts, tyString) && tyLambdaOf(tyVoid, tyString)
		: function() {
			let kvs = {};
			let tr = setp(kvs, field, newRef());
			let to = tyStructOf(kvs);
			return doBind(ast, ts, to) && function() {
				let t = finalRef(tr);
				return t.generic !== true ? t : cloneRef(t);
			}();
		}();
	};

	let inferType;

	inferType = (vts, isAsync, ast) => {
		let { id } = ast;

		let infer = ast_ => inferType(vts, isAsync, ast_);

		let inferCmpOp = ({ lhs, rhs }) => function() {
			let t = newRef();
			return true
				&& doBind(ast, infer(lhs), t)
				&& doBind(ast, infer(rhs), t)
				&& (tryBind(t, tyNumber) || tryBind(t, tyString) || error(`cannot compare values with type ${t}`))
				&& tyBoolean;
		}();

		let inferEqOp = ({ lhs, rhs }) => true
			&& doBind(ast, infer(lhs), infer(rhs))
			&& tyBoolean;

		let inferLogicalOp = ({ lhs, rhs }) => true
			&& doBind(ast, infer(lhs), tyBoolean)
			&& infer(rhs);

		let inferMathOp = ({ lhs, rhs }) => true
			&& doBind(ast, infer(lhs), tyNumber)
			&& doBind(ast, infer(rhs), tyNumber)
			&& tyNumber;

		let f = false ? undefined
		: id === 'add' ? (({ lhs, rhs }) => {
			let t = newRef();
			return true
				&& doBind(ast, infer(lhs), t)
				&& doBind(ast, infer(rhs), t)
				&& (tryBind(t, tyNumber) || tryBind(t, tyString) || error(`cannot add values with type ${dump(t)}`))
				&& t;
		})
		: id === 'alloc' ? (({ vn, expr }) =>
			inferType(cons([vn, newRef()], vts), isAsync, expr)
		)
		: id === 'and' ?
			inferLogicalOp
		: id === 'app' ? (({ lhs, rhs }) => {
			let te = infer(lhs);
			let tp = infer(rhs);
			let tr = newRef();
			return doBind(ast, te, tyLambdaOf(tp, tr)) && tr;
		})
		: id === 'assign' ? (({ bind, value, expr }) => function() {
			try {
				let tbind = infer(bind);
				let tvalue = infer(value);
				return doBind(_assign(bind, value, undefined), tbind, tvalue);
			} catch (e) {
				e.message = `in assignment clause of ${format(bind)}\n${e.message}`;
				throw e;
			}
		}() && infer(expr))
		: id === 'await' ? (({ expr }) => {
			let t = newRef();
			return isAsync ? doBind(ast, infer(expr), tyPromiseOf(t)) && t : error(`await not inside async`);
		})
		: id === 'bool' ? (({}) =>
			tyBoolean
		)
		: id === 'coal' ? (({ lhs, rhs }) => {
			let tl = infer(lhs);
			let tr = infer(rhs);
			return doBind(ast, tl, tr) && tr;
		})
		: id === 'cons' ? (({ lhs, rhs }) => {
			let tl = tyArrayOf(infer(lhs));
			return doBind(ast, infer(rhs), tl) && tl;
		})
		: id === 'div' ?
			inferMathOp
		: id === 'dot' ? (({ expr, field }) =>
			inferDot(ast, infer(expr), field)
		)
		: id === 'eq_' ?
			inferEqOp
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
			return doBind(ast, infer(if_), tyBoolean) && doBind(ast, tt, te) && tt;
		})
		: id === 'index' ? (({ lhs, rhs }) => {
			let t = newRef();
			return true
				&& doBind(ast, infer(rhs), tyNumber)
				&& doBind(ast, infer(lhs), tyArrayOf(t))
				&& t;
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
				try {
					let tb = inferType(vts1, false, bind);
					let tv = infer(value);
					return doBind(_let(bind, value, undefined), tb, tv);
				} catch (e) {
					e.message = `in value clause of ${format(bind)}\n${e.message}`;
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
			doBind(ast, infer(expr), tyNumber) && tyNumber
		)
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
		: id === 'not' ? (({ expr }) =>
			doBind(ast, infer(expr), tyBoolean) && tyBoolean
		)
		: id === 'num' ? (({}) =>
			tyNumber
		)
		: id === 'or_' ?
			inferLogicalOp
		: id === 'pair' ? (({ lhs, rhs }) =>
			tyPairOf(infer(lhs), infer(rhs))
		)
		: id === 'pos' ? (({ expr }) =>
			doBind(ast, infer(expr), tyNumber) && tyNumber
		)
		: id === 'str' ? (({}) =>
			tyString
		)
		: id === 'struct' ? (({ kvs }) => {
			return tyStructOf(foldl({}, kvs, (type, kv) => {
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
		: id === 'throw' ? (({}) =>
			newRef()
		)
		: id === 'try' ? (({ lhs, rhs }) =>
			doBind(ast, infer(rhs), newRef()) && infer(lhs)
		)
		: id === 'tuple' ? (({ values }) =>
			tyTupleOf(foldr(nil, values, (tuple, value) => cons(infer(value), tuple)))
		)
		: id === 'typeof' ? (({}) =>
			tyString
		)
		: id === 'undefined' ? (({}) =>
			newRef()
		)
		: id === 'var' ? (({ vn }) => {
			let t = finalRef(findk(vts, vn));
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
				'.stringify': tyLambdaOf(tyPairOf(newRef(), tyPairOf(newRef(), newRef())), tyString),
			}),
			Object: tyStructOfCompleted({
				'.assign': tyLambdaOf(newRef(), newRef()),
				'.entries': tyLambdaOf(tyStructOf({}), tyArrayOf(tyTupleOf(cons(tyString, cons(newRef(), nil))))),
				'.fromEntries': tyLambdaOf(tyArrayOf(tyTupleOf(cons(tyString, cons(newRef(), nil)))), tyStructOf({})),
				'.keys': tyLambdaOf(tyStructOf({}), tyArrayOf(tyString)),
			}),
			Promise: tyStructOfCompleted({
				'.reject': tyLambdaOf(tyError, tyPromiseOf(newRef())),
				'.resolve': function() { let t = newRef(); return tyLambdaOf(t, tyPromiseOf(t)); }(),
			}),
			console: tyStructOfCompleted({
				'.error': tyLambdaOf(newRef(), tyVoid),
				'.log': tyLambdaOf(newRef(), tyVoid),
			}),
			process: tyStructOfCompleted({
				'.env': tyStructOf({}),
			}),
			require: tyLambdaOf(tyString, newRef()),
		})
		.reduce((l, vt) => cons(vt, l), nil);

	return { dump, infer: ast => inferType(predefinedTypes, false, ast) };
};

let promiseResolve = _dot(_var('Promise'), '.resolve');
let promisify = ast => _app(promiseResolve, ast);

let unpromisify = ast => {
	let { id, lhs, rhs } = ast;
	return id === 'app' && lhs === promiseResolve ? rhs : undefined;
};

let rewriteAsync;

rewriteAsync = ast => {
	let { id } = ast;

	let _then = (p, bind, expr) => _app(_dot(p, '.then'), _lambda(bind, expr));

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
	: id === 'await' ? (({ expr }) => expr)
	: id === 'coal' ?
		reduceBinOp
	: id === 'cons' ?
		reduceBinOp
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

let ifBindId = bindId => {
	let ifBind;

	ifBind = (bind, value, then, else_) => {
		let { id } = bind;

		let bindConstant = ast => false ? undefined
			: bind.id !== value.id ? _if(_eq(bind, value), then, else_)
			: bind.v === value.v ? then
			: else_;

		let f = false ? undefined
		: id === 'bool' ?
			bindConstant
		: id === 'cons' ? (({ lhs, rhs }) => {
			return id !== value.id
				? ifBind(lhs, _index(value, _num(0)), ifBind(rhs, _app(_dot(value, '.slice'), _num(1)), then, else_), else_)
				: ifBind(lhs, value.lhs, ifBind(rhs, value.rhs, then, else_), else_);
		})
		: id === 'nil' ? (({}) => {
			return id !== value.id
				? _if(_eq(_app(_dot(value, '.length'), _void), _num(0)), then, else_)
				: then;
		})
		: id === 'num' ?
			bindConstant
		: id === 'pair' ? (({ lhs, rhs }) => {
			return id !== value.id
				? ifBind(lhs, _index(value, _num(0)), ifBind(rhs, _index(value, _num(1)), then, else_), else_)
				: ifBind(lhs, value.lhs, ifBind(rhs, value.rhs, then, else_), else_);
		})
		: id === 'str' ?
			bindConstant
		: id === 'struct' ? (({ kvs }) => {
			let getValue = k => value.kvs.filter(kv => kv.key === k)[0].value;
			return kvs.reduce(
				(expr, kv) => ifBind(kv.value, id !== value.id ? _dot(value, kv.key) : getValue(kv.key), expr, else_),
				then);
		})
		: id === 'tuple' ? (({ values }) => {
			let indices = gen(values.length);
			return indices.reduce(
				(expr, i) => ifBind(values[i], id !== value.id ? _index(value, _num(i)) : value.values[i], expr, else_),
				then);
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

let rewriteBind;

rewriteBind = ast => {
	let { bind, id } = ast;

	let f = false ? undefined
	: id === 'assign' && bind.id !== 'dot' && bind.id !== 'index' && bind.id !== 'var' ? (({ bind, value, expr }) =>
		assignBind(bind, rewriteBind(value), rewriteBind(expr), _error)
	)
	: id === 'lambda' && bind.id !== 'var' ? (({ bind, expr }) => {
		let arg = _var(newDummy());
		return _lambda(arg, letBind(bind, arg, rewriteBind(expr), _error));
	})
	: id === 'lambda-async' && bind.id !== 'var' ? (({ bind, expr }) => {
		let arg = _var(newDummy());
		return _lambdaAsync(arg, letBind(bind, arg, rewriteBind(expr), _error));
	})
	: id === 'let' && bind.id !== 'var' ? (({ bind, value, expr }) =>
		letBind(bind, rewriteBind(value), rewriteBind(expr), _error)
	)
	: (({}) =>
		rewrite(rewriteBind, ast)
	);

	return f(ast);
};

let rewriteCaptureVar;

rewriteCaptureVar = (capture, outsidevs, captures, ast) => {
	let { id } = ast;

	let f = false ? undefined
	: id === 'var' ? (({ vn }) => {
		return !outsidevs.includes(vn) ? ast : function() {
			captures.push(vn);
			return _dot(_var(capture), `.${vn}`);
		}();
	})
	: (({}) =>
		rewrite(ast => rewriteCaptureVar(capture, outsidevs, captures, ast), ast)
	);

	return f(ast);
};

let rewriteCapture;

rewriteCapture = (fs, vfs, ast) => {
	let fs1 = fs + 1;
	let { id } = ast;

	let f = false ? undefined
	: id === 'alloc' ? (({ vn, expr }) =>
		_alloc(vn, rewriteCapture(fs, cons([vn, fs], vfs), expr))
	)
	: id === 'lambda' ? (({ bind, expr }) => function() {
		let vfs1 = cons([bind.vn, fs1], vfs);
		let bindCapture = newDummy();
		let captures = [];
		let expr_ = rewriteCaptureVar(bindCapture, vfs.map(([vn, fs]) => vn), captures, expr);
		let definitions = Object.fromEntries(captures.map(vn => [`.${vn}`, vn]));
		return _lambdaCapture(definitions, { vn: bindCapture }, bind, rewriteCapture(fs1, vfs1, expr_));
	}())
	: id === 'let' ? (({ bind, value, expr }) =>
		_let(bind,
			rewriteCapture(fs, vfs, value),
			rewriteCapture(fs, cons([bind.vn, fs], vfs), expr))
	)
	: (({}) =>
		rewrite(ast => rewriteCapture(fs, vfs, ast), ast)
	);

	return f(ast);
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
	: id === 'alloc' ? (({ vn, expr }) => function() {
		let vn1 = `${vn}_${scope}`;
		return _alloc(vn1, rewriteRenameVar(scope, cons([vn, `${vn}_${scope}`], vns), expr));
	}()
	)
	: id === 'lambda' ? (({ bind, expr }) => function() {
		let { vn } = bind;
		let scope1 = newDummy();
		let vn1 = `${vn}_${scope1}`;
		return _lambda(_var(vn1), rewriteRenameVar(scope1, cons([vn, vn1], vns), expr));
	}()
	)
	: id === 'let' ? (({ bind, value, expr }) => function() {
		let { vn } = bind;
		let vn1 = `${vn}_${scope}`;
		return _let(_var(vn1),
			rewriteRenameVar(scope, vns, value),
			rewriteRenameVar(scope, cons([vn, vn1], vns), expr));
	}()
	)
	: id === 'var' ? (({ vn }) =>
		_var(findk(vns, vn))
	)
	: (({}) =>
		rewrite(ast => rewriteRenameVar(scope, vns, ast), ast)
	);

	return f(ast);
};

let evaluate;

evaluate = vvs => {
	let assign = (vn, value) => {
		let vv = find(vvs, ([vn_, value]) => vn_ === vn);
		seti(vv, 1, value);
		return value;
	};

	let evaluate_;

	evaluate_ = ast => {
		let eval = ast => evaluate_(ast);
		let { id } = ast;

		let f = false ? undefined
		: id === 'add' ? (({ lhs, rhs }) => assumeAny(eval(lhs) + eval(rhs)))
		: id === 'alloc' ? (({ vn, expr }) => evaluate(cons([vn, undefined], vvs))(expr))
		: id === 'and' ? (({ lhs, rhs }) => assumeAny(eval(lhs) && eval(rhs)))
		: id === 'app' ? (({ lhs, rhs }) => eval(lhs)(eval(rhs)))
		: id === 'assign' ? (({ bind, value, expr }) => function() {
			assign(bind.vn, eval(value));
			return eval(expr);
		}()) 
		: id === 'await' ? (({ expr }) => error('BAD'))
		: id === 'bool' ? (({ v }) => v)
		: id === 'coal' ? (({ lhs, rhs }) => function() {
			let v = eval(lhs);
			return v ? v : eval(rhs);
		}())
		: id === 'cons' ? (({ lhs, rhs }) => assumeAny(cons(eval(lhs), eval(rhs))))
		: id === 'div' ? (({ lhs, rhs }) => assumeAny(eval(lhs) / eval(rhs)))
		: id === 'dot' ? (({ expr, field }) => getp(eval(expr), field))
		: id === 'eq_' ? (({ lhs, rhs }) => assumeAny(eval(lhs) === eval(rhs)))
		: id === 'frame' ? error('BAD')
		: id === 'if' ? (({ if_, then, else_ }) => eval(if_) ? eval(then) : eval(else_))
		: id === 'index' ? (({ lhs, rhs }) => eval(lhs)[eval(rhs)])
		: id === 'lambda' ? (({ bind, expr }) => assumeAny(value => evaluate(cons([bind.vn, value], vvs))(expr)))
		: id === 'lambda-async' ? (({ bind, expr }) => error('BAD'))
		: id === 'lambda-capture' ? (({ capture, bindCapture, bind, expr }) =>
			assumeAny(value => evaluate(cons([bind.vn, value], cons([bindCapture.vn, eval(capture)], vvs)))(expr))
		)
		: id === 'le_' ? (({ lhs, rhs }) => assumeAny(eval(lhs) <= eval(rhs)))
		: id === 'let' ? (({ bind, value, expr }) => evaluate(cons([bind.vn, eval(value)], vvs))(expr))
		: id === 'lt_' ? (({ lhs, rhs }) => assumeAny(eval(lhs) < eval(rhs)))
		: id === 'mul' ? (({ lhs, rhs }) => assumeAny(eval(lhs) * eval(rhs)))
		: id === 'ne_' ? (({ lhs, rhs }) => assumeAny(eval(lhs) !== eval(rhs)))
		: id === 'neg' ? (({ expr }) => assumeAny(-eval(expr)))
		: id === 'new' ? (({ clazz }) => false ? undefined
			: clazz === 'Error' ? assumeAny(e => new Error(e))
			: clazz === 'Map' ? assumeAny(() => new Map())
			: clazz === 'Promise' ? assumeAny(f => new Promise(f))
			: error(`unknown class ${clazz}`)
		)
		: id === 'nil' ? (({}) => assumeAny([]))
		: id === 'not' ? (({ expr }) => assumeAny(!eval(expr)))
		: id === 'num' ? (({ i }) => i)
		: id === 'or_' ? (({ lhs, rhs }) => assumeAny(eval(lhs) || eval(rhs)))
		: id === 'pair' ? (({ lhs, rhs }) => assumeAny([eval(lhs), eval(rhs)]))
		: id === 'pos' ? (({ expr }) => assumeAny(+eval(expr)))
		: id === 'str' ? (({ v }) => v)
		: id === 'struct' ? (({ kvs }) => assumeAny(foldl({}, kvs, (struct, kv) => {
			let { key, value } = kv;
			setp(struct, key.slice(1, undefined), eval(value));
			return struct;
		})))
		: id === 'sub' ? (({ lhs, rhs }) => assumeAny(eval(lhs) - eval(rhs)))
		: id === 'throw' ? (({ expr }) => function() { throw eval(expr); }())
		: id === 'try' ? (({ lhs, rhs }) => function() {
			try {
				return eval(lhs);
			} catch (e) { return eval(_app(rhs, e)); }
		}())
		: id === 'tuple' ? (({ values }) => assumeAny(foldr(nil, values, (tuple, value) => cons(eval(value), tuple))))
		: id === 'typeof' ? (({ expr }) => assumeAny(typeof (eval(expr))))
		: id === 'undefined' ? (({}) => undefined)
		: id === 'var' ? (({ vn }) => findk(vvs, vn))
		: id === 'while' ? (({ cond, loop, expr }) => function() {
			let v;
			while (eval(cond)) eval(loop);
			eval(expr);
		}())
		: error(`cannot generate for ${id}`);

		return f(ast);
	};

	return evaluate_;
};

let rewriteVars;

rewriteVars = (fs, ps, vts, ast) => {
	let fs1 = fs + 1;
	let ps1 = ps + 1;
	let { id } = ast;

	let f = false ? undefined
	: id === 'alloc' ? (({ vn, expr }) =>
		_alloc(vn, rewriteVars(fs, ps1, cons([vn, [fs, ps]], vts), expr))
	)
	: id === 'assign' ? (({ bind, value, expr }) => {
		let [fs_, ps] = findk(vts, bind.vn);
		return _assign(_frame(fs - fs_, ps), rewriteVars(fs, ps, vts, value), rewriteVars(fs, ps, vts, expr));
	})
	: id === 'lambda-capture' ? (({ capture, bindCapture, bind, expr }) =>
		_lambdaCapture(
			capture, bindCapture, bind,
			rewriteVars(fs1, 1, cons([bind.vn, [fs1, 1]], cons([bindCapture.vn, [fs1, 0]], vts)), expr))
	)
	: id === 'let' ? (({ bind, value, expr }) =>
		_alloc(bind.vn, _assign(bind,
			rewriteVars(fs, ps, vts, value),
			rewriteVars(fs, ps1, cons([bind.vn, [fs, ps]], vts), expr)))
	)
	: id === 'var' ? (({ vn }) => {
		let [fs_, ps] = findk(vts, vn);
		return _frame(fs - fs_, ps);
	})
	: (({}) =>
		rewrite(ast => rewriteVars(fs, ps, vts, ast), ast)
	);

	return f(ast);
};

let generate;

generate = ast => {
	let { id } = ast;

	let generateOp = (({ expr }) => [
		...generate(expr),
		{ id },
	]);

	let generateBinOp = (({ lhs, rhs }) =>
		[...generate(lhs), ...generate(rhs), { id },]
	);

	let f = false ? undefined
	: id === 'add' ?
		generateBinOp
	: id === 'alloc' ? (({ vn, expr }) => [
		{ id: 'num', i: 0 },
		{ id: 'fpush' },
		...generate(expr),
		{ id: 'fdiscard' },
	])
	: id === 'and' ?
		generateBinOp
	: id === 'app' ?
		generateBinOp
	: id === 'assign' ? (({ bind, value, expr }) => [
		...generate(value),
		{ id, fs: bind.fs, ps: bind.ps },
		...generate(expr),
	])
	: id === 'await' ? (({}) =>
		error('BAD')
	)
	: id === 'bool' ? (({}) =>
		[ast,]
	)
	: id === 'coal' ?
		generateBinOp
	: id === 'cons' ?
		generateBinOp
	: id === 'div' ?
		generateBinOp
	: id === 'dot' ? (({ expr, field }) => [
		...generate(expr),
		{ id, key: field },
	])
	: id === 'eq_' ?
		generateBinOp
	: id === 'frame' ? (({}) =>
		[ast,]
	)
	: id === 'if' ? (({ if_, then, else_ }) => function() {
		let elseLabel = newDummy();
		let fiLabel = newDummy();
		return [
			...generate(if_),
			{ id: 'jump-zero', label: elseLabel },
			...generate(then),
			{ id: 'jump', label: fiLabel },
			{ id: 'l', label: elseLabel },
			...generate(else_),
			{ id: 'l', label: fiLabel },
		];
	}())
	: id === 'index' ?
		generateBinOp
	: id === 'lambda' ?
		error('BAD')
	: id === 'lambda-async' ?
		error('BAD')
	: id === 'lambda-capture' ? (({ capture, bindCapture, bind, expr }) => function() {
		let lambdaLabel = newDummy();
		let skipLabel = newDummy();
		return [
			{ id: 'jump', label: skipLabel },
			{ id: 'l', label: lambdaLabel },
			...generate(capture),
			{ id: 'l', label: skipLabel },
			{ id: 'label', label: lambdaLabel },
			{ id },
		];
	}())
	: id === 'le_' ?
		generateBinOp
	: id === 'let' ? (({ bind, value, expr }) => [
		...generate(value),
		{ id: 'fpush' },
		...generate(expr),
		{ id: 'fdiscard' },
	])
	: id === 'lt_' ?
		generateBinOp
	: id === 'mul' ?
		generateBinOp
	: id === 'ne_' ?
		generateBinOp
	: id === 'neg' ?
		generateOp
	: id === 'new' ?
		error('TODO')
	: id === 'nil' ? (({}) =>
		[ast,]
	)
	: id === 'not' ?
		generateOp
	: id === 'num' ? (({}) =>
		[ast,]
	)
	: id === 'or' ?
		generateBinOp
	: id === 'pair' ?
		generateBinOp
	: id === 'pos' ?
		generateOp
	: id === 'str' ? (({}) =>
		[ast,]
	)
	: id === 'struct' ? (({ kvs }) => [
		{ id: 'object' },
		...kvs.reverse().flatMap(({ key, value }) => [...generate(value), { id: 'put', key },]),
	])
	: id === 'sub' ?
		generateBinOp
	: id === 'throw' ? (({ expr }) => [
		...generate(expr),
		{ id },
	])
	: id === 'try' ? (({ lhs, rhs }) => function() {
		let catchLabel = newDummy();
		let finallyLabel = newDummy();
		return [
			{ id: 'get-catch' },
			{ id: 'label', label: catchLabel },
			{ id: 'set-catch' },
			...generate(lhs),
			{ id: 'rotate' },
			{ id: 'set-catch' },
			{ id: 'jump', label: finallyLabel },
			{ id: 'l', label: catchLabel },
			...generate(rhs),
			{ id: 'l', label: finallyLabel },
		];
	}()
	)
	: id === 'tuple' ? (({ values }) => [
		{ id: 'nil' },
		...values.reverse().flatMap(value => [...generate(value), { id: 'cons' },]),
	])
	: id === 'typeof' ?
		error('BAD')
	: id === 'undefined' ? (({}) =>
		[{ id },]
	)
	: id === 'var' ?
		error('BAD')
	: id === 'while' ? (({ cond, loop, expr }) => {
		let loopLabel = newDummy();
		let exitLabel = newDummy();
		return [
			{ id: 'l', label: loopLabel },
			...generate(cond),
			{ id: 'jump-zero', label: exitLabel },
			...generate(loop),
			{ id: 'discard' },
			{ id: 'l', label: exitLabel },
			...generate(expr),
		];
	})
	: (({}) =>
		error('BAD')
	);

	return f(ast);
};

let parser = parserModule();
let types = typesModule();

let process0 = program => {
	let pos0;
	let posx;
	while (function() {
		pos0 = program.indexOf('\/\/ ', 0);
		posx = 0 <= pos0 ? program.indexOf('\n', pos0) : -1;
		return 0 <= posx;
	}()) (function() {
		program = program.slice(0, pos0) + program.slice(posx, undefined);
	}());

	let ast0 = parser.parse(program);
	let ast1 = rewriteNe(ast0);
	let ast2 = rewriteBind(ast1);
	let ast3 = rewriteAsync(ast2);
	let ast4 = unpromisify(ast3);

	return { ast: ast4, type: types.infer(ast0) };
};

let process1 = program => {
	let roots = ['JSON', 'Object', 'Promise', 'console', 'process', 'require',];

	let { ast: ast4, type } = process0(program);
	let ast5 = rewriteRenameVar(newDummy(), roots.map(v => [v, v]), ast4);
	let ast6 = rewriteCapture(0, roots.map(v => [v, 0]), ast5);

	return { ast: ast6, type };
};

let process2 = ast6 => {
	let ast7 = rewriteVars(0, 0, [], ast6);
	let opcodes = generate(ast7);
	return opcodes;
};

let actual = stringify(process0(`
	let parse = ast => ast;
	console.log(parse(require('fs').readFileSync(0, 'utf8')))
`).ast);

let expect = stringify(
	_let(
		_var('parse'),
		_lambda(_var('ast'), _var('ast')),
		_app(
			_dot(_var('console'), '.log'),
			_app(
				_var('parse'),
				_app(
					_dot(_app(_var('require'), _str('fs')), '.readFileSync'),
					_pair(_num(0), _str('utf8'))
				)
			)
		)
	)
);

return actual === expect
? function() {
	try {
		let { ast, type } = process1(require('fs').readFileSync(0, 'utf8'));
		console.log(`ast :: ${stringify(ast)}`);
		// process.env.EVAL && console.log(`eval :: ${JSON.stringify(evaluate([])(ast))}`);
		process.env.FORMAT && console.log(`format :: ${format(ast)}`);
		process.env.GENERATE && console.log(`generate :: ${process2(ast).map(JSON.stringify).join('\n')}`);
		console.log(`type :: ${types.dump(type)}`);
		return true;
	} catch (e) { return console.error(e); }
}() : error(`
test case failed,
actual = ${actual}
expect = ${expect}`);
