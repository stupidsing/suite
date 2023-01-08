// cat src/main/js/parse.js | node src/main/js/parse.js
// parsing a javascript subset and inference variable types.
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

let getp = (m, k) => fake(m)[k !== '' && fake(k)];
let setp = (m, k, v) => { fake(m)[k !== '' && fake(k)] = v; return v; };

let contains;
contains = (es, e) => isNotEmpty(es) && (head(es) === e || contains(tail(es), e));

let fold;
fold = (init, es, op) => isNotEmpty(es) ? fold(op(init, head(es)), tail(es), op) : init;

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

let _add = (lhs, rhs) => ({ id: 'add', lhs, rhs });
let _alloc = (vn, expr) => ({ id: 'alloc', vn, expr });
let _app = (lhs, rhs) => ({ id: 'app', lhs, rhs });
let _array = values => ({ id: 'array', values });
let _assign = (bind, value, expr) => ({ id: 'assign', bind, value, expr });
let _bool = v => ({ id: 'bool', v });
let _cons = (lhs, rhs) => ({ id: 'cons', lhs, rhs });
let _dot = (expr, field) => ({ id: 'dot', expr, field });
let _eq = (lhs, rhs) => ({ id: 'eq_', lhs, rhs });
let _if = (if_, then, else_) => ({ id: 'if', if_, then, else_ });
let _index = (lhs, rhs) => ({ id: 'index', lhs, rhs });
let _lambda = (bind, expr) => ({ id: 'lambda', bind, expr });
let _lambdaAsync = (bind, expr) => ({ id: 'lambda-async', bind, expr });
let _let = (bind, value, expr) => ({ id: 'let', bind, value, expr });
let _never = { id: 'never' };
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
let _while = (cond, loop, expr) => ({ id: 'while', cond, loop, expr });

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

		let exprToString = _app(
			_dot(parseApplyBlockFieldIndex(expr_), '.toString'),
			_never);

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
	: program === 'new Error' ? { id: 'new-error' }
	: program === 'new Map' ? { id: 'new-map' }
	: program === 'new Promise' ? { id: 'new-promise' }
	: program === 'nil' ? _array([])
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
			let tail = tail_.trim();
			return _cons(
				parse(head),
				tail.startsWith('...') && tail.endsWith(',') ? parse(tail.slice(3, -1)) : parseArray_(tail));
		}()
		: _array([]);
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
	: program.startsWith('typeof ') ?
		_typeof(parseValue(program.slice(7, undefined)))
	: program.startsWith('(') && program.endsWith(')') ?
		parse(program.slice(1, -1))
	: program.startsWith('[') && program.endsWith(']') ?
		parseArrayTuple(program, parse)
	: program.startsWith('{') && program.endsWith('}') ? function() {
		let block = program.slice(1, -1).trim();
		return block.endsWith(';') ? parse(block) : parseStructInner(block, parse);
	}()
	: parseConstant(program);
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
	: parseValue(program);
};

parseApplyBlockFieldIndex = program_ => {
	let program = program_.trim();

	return false ? undefined
	: program.startsWith('function() {') && program.endsWith('}()') ?
		parse(program.slice(12, -3).trim())
	: program.endsWith('()') ?
		_app(parse(program.slice(0, -2)), _never)
	: program.endsWith(')') ? function() {
		let [expr, paramStr_] = splitr(program, '(');
		let paramStr = paramStr_.slice(0, -1).trim();
		return expr !== undefined ? _app(parse(expr), parse(paramStr)): parseValue(program);
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
	.map(p => parseAssocLeft_('app', '|>', p))
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
		_never
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

let dummyCount = 0;

let newDummy = () => {
	dummyCount = dummyCount + 1;
	return `dummy${dummyCount}`;
};

parse = program => {
	let [statement_, expr_] = splitl(program, ';');

	return expr_ === undefined ? parsePair(statement_, parseLambda) : function() {
		let statement = statement_.trim();
		let expr = expr_.trim();

		return false ? undefined
		: statement.startsWith('let ') ? function() {
			let [vn, value] = splitl(statement.slice(4, undefined), '=');
			let v = vn.trim();

			return false ? undefined
			: value !== undefined ? _let(parseBind(vn), parse(value), parse(expr))
			: isIdentifier(v) ? _alloc(v, parse(expr))
			: error(`cannot parse let variable "${v}"`);
		}()
		: statement.startsWith('return ') && expr === '' ?
			parse(statement.slice(7, undefined))
		: statement.startsWith('throw ') && expr === '' ?
			_throw(parse(statement.slice(6, undefined)))
		: statement.startsWith('while ') ? function() {
			let [cond, loop] = splitl(statement.slice(6, undefined), ' ');
			return _while(parse(cond), parse(loop), parse(expr));
		}()
		: function() {
			let [lhs, rhs] = splitl(statement, '=');

			return rhs !== undefined
			? _assign(parseLvalue(lhs), parse(rhs), parse(expr))
			: _let(_var(newDummy()), parse(lhs), parse(expr));
		}();
	}();
};

let formatBlock;
let formatExpr;
let format;

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

formatExpr = ast => {
	let { id } = ast;

	let f = false ? undefined
	: id === 'add' ? (({ lhs, rhs }) => `${format(lhs)} + ${format(rhs)}`)
	: id === 'and' ? (({ lhs, rhs }) => `${format(lhs)} && ${format(rhs)}`)
	: id === 'await' ? (({ expr }) => `await ${format(expr)}`)
	: id === 'coal' ? (({ lhs, rhs }) => `${format(lhs)} ?? ${format(rhs)}`)
	: id === 'cons' ? (({ lhs, rhs }) => `[${format(lhs)}, ...${format(rhs)}]`)
	: id === 'div' ? (({ lhs, rhs }) => `${format(lhs)} / ${format(rhs)}`)
	: id === 'dot' ? (({ expr, field }) => `${format(expr)}${field}`)
	: id === 'eq_' ? (({ lhs, rhs }) => `${format(lhs)} === ${format(rhs)}`)
	: id === 'if' ? (({ if_, then, else_ }) => `${format(if_)} ? ${format(then)} : ${format(else_)}`)
	: id === 'index' ? (({ lhs, rhs }) => `${format(lhs)}[${format(rhs)}]`)
	: id === 'lambda' ? (({ bind, expr }) => `${format(bind)} => ${format(expr)}`)
	: id === 'lambda-async' ? (({ bind, expr }) => `async ${format(bind)} => ${format(expr)}`)
	: id === 'le_' ? (({ lhs, rhs }) => `${format(lhs)} <= ${format(rhs)}`)
	: id === 'lt_' ? (({ lhs, rhs }) => `${format(lhs)} < ${format(rhs)}`)
	: id === 'mul' ? (({ lhs, rhs }) => `${format(lhs)} * ${format(rhs)}`)
	: id === 'ne_' ? (({ lhs, rhs }) => `${format(lhs)} !== ${format(rhs)}`)
	: id === 'neg' ? (({ expr }) => `- ${format(expr)}`)
	: id === 'never' ? (({}) => `error('${id}')`)
	: id === 'not' ? (({ expr }) => `! ${format(expr)}`)
	: id === 'or_' ? (({ lhs, rhs }) => `${format(lhs)} || ${format(rhs)}`)
	: id === 'pair' ? (({ lhs, rhs }) => `${format(lhs)}, ${format(rhs)}`)
	: id === 'pos' ? (({ expr }) => `+ ${format(expr)}`)
	: id === 'sub' ? (({ lhs, rhs }) => `${format(lhs)} - ${format(rhs)}`)
	: id === 'typeof' ? (({ expr }) => `typeof ${format(expr)}`)
	: (({}) => `function() { ${formatBlock(ast)}; }()`);

	return f(ast);
};

format = ast => {
	let { id } = ast;

	let f = false ? undefined
	: id === 'app' ? (({ lhs, rhs }) => `${format(lhs)}(${format(rhs)})`)
	: id === 'array' ? (({ values }) => `[${values.map(format).join(', ')},]`)
	: id === 'bool' ? (({ v }) => v)
	: id === 'new-error' ? (({}) => 'new Error')
	: id === 'new-map' ? (({}) => 'new Map')
	: id === 'new-promise' ? (({}) => 'new Promise')
	: id === 'num' ? (({ i }) => `${i}`)
	: id === 'struct' ? (({ kvs }) => `{ ${kvs.map(({ key, value }) => `${key.slice(1, undefined)}: ${format(value)}`).join(', ')} }`)
	: id === 'str' ? (({ v }) => `'${v}'`)
	: id === 'tuple' ? (({ values }) => `[${values.map(format).join(', ')}]`)
	: id === 'undefined' ? (({}) => `${id}`)
	: id === 'var' ? (({ vn }) => vn)
	: (({}) => `(${formatExpr(ast)})`);

	return f(ast);
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
		return false ? undefined
		: contains(vs, v) ?
			'<recurse>'
		: ref !== undefined ?
			(refs.get(ref) !== v ? dumpRef_(cons(v, vs), refs.get(ref)) : `_${ref}`)
		: typeof v === 'object' ? (false ? undefined
			: isEmpty(listv) ?
				''
			: isNotEmpty(listv) ?
				`${dumpRef_(vs, head(listv))}:${dumpRef_(vs, assumeObject(tail(listv)))}`
			: function() {
				let t = v.t;
				let join = Object
					.entries(v)
					.filter(([k, v_]) => k !== 'id')
					.map(([k, v_]) => `${k}:${dumpRef_(cons(v, vs), v_)}`)
					.join(' ');
				return t !== undefined ? `${t}(${join})` : `{${join}}`;
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

let doBind_ = (msg, a, b) => tryBind(a, b) || error(`in ${msg()}:\ncannot bind types between\nfr: ${dumpRef(a)}\nto: ${dumpRef(b)}`);
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

let lookup = (vts, v) => {
	let lookup_;
	lookup_ = vts => isNotEmpty(vts) ? function() {
		let [v_, t] = head(vts);
		return v_ === v ? t : lookup_(tail(vts));
	}() : error(`undefined variable ${v}`);
	return lookup_(vts);
};

let bindTypes;

bindTypes = (vts, ast) => false ? undefined
	: ast.id === 'array' ? fold(vts, ast.values, bindTypes)
	: ast.id === 'never' ? vts
	: ast.id === 'pair' ? bindTypes(bindTypes(vts, ast.lhs), ast.rhs)
	: ast.id === 'struct' ? fold(vts, ast.kvs, (vts_, kv) => bindTypes(vts_, kv.value))
	: ast.id === 'tuple' ? fold(vts, ast.values, bindTypes)
	: ast.id === 'var' ? cons([ast.vn, newRef()], vts)
	: error(`cannot destructure ${format(ast)}`);

let typeArrayOf = type => ({ t: 'array', of: type });
let typeBoolean = ({ t: 'bool' });
let typeError = ({ t: 'error' });
let typeLambdaOf = (in_, out) => ({ t: 'lambda', generic: true, in_, out });
let typeLambdaOfFixed = (in_, out) => ({ t: 'lambda', in_, out });
let typeNever = { t: 'never' };
let typeNumber = ({ t: 'num' });
let typePairOf = (lhs, rhs) => ({ t: 'pair', lhs, rhs });
let typePromiseOf = out => ({ t: 'promise', out });
let typeString = typeArrayOf({ t: 'char' });
let typeStructOf = kvs => ({ t: 'struct', kvs });
let typeStructOfCompleted = kvs => { setp(kvs, 'completed', true); return ({ t: 'struct', kvs }); };
let typeTupleOf = types => ({ t: 'tuple', types });
let typeVoid = typeStructOfCompleted({});

let typeMapOf = (tk, tv) => typeStructOfCompleted({
	'.get': typeLambdaOfFixed(tk, tv),
	'.has': typeLambdaOfFixed(tk, typeBoolean),
	'.set': typeLambdaOfFixed(typePairOf(tk, tv), typeNever),
});

let inferDot = (ast, ts, field) => {
	return false ? undefined
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
	let { id } = ast;

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

	let f = false ? undefined
	: id === 'add' ? (({ lhs, rhs }) => {
		let t = newRef();
		return true
			&& doBind(ast, infer(lhs), t)
			&& doBind(ast, infer(rhs), t)
			&& (tryBind(t, typeNumber) || tryBind(t, typeString) || error(`cannot add values with type ${dumpRef(t)}`))
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
		return doBind(ast, te, typeLambdaOf(tp, tr)) && tr;
	})
	: id === 'array' ? (({ values }) => {
		let te = newRef();
		return fold(true, values, (b, value) => b && doBind(ast, infer(value), te)) && typeArrayOf(te);
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
		return isAsync ? doBind(ast, infer(expr), typePromiseOf(t)) && t : error(`await not inside async`);
	})
	: id === 'bool' ? (({}) =>
		typeBoolean
	)
	: id === 'coal' ? (({ lhs, rhs }) => {
		let tl = infer(lhs);
		let tr = infer(rhs);
		return doBind(ast, tl, tr) && tr;
	})
	: id === 'cons' ? (({ lhs, rhs }) => {
		let tl = typeArrayOf(infer(lhs));
		return doBind(ast, infer(rhs), tl) && tl;
	})
	: id === 'div' ?
		inferMathOp
	: id === 'dot' ? (({ expr, field }) =>
		inferDot(ast, infer(expr), field)
	)
	: id === 'eq_' ?
		inferEqOp
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
		return doBind(ast, infer(if_), typeBoolean) && doBind(ast, tt, te) && tt;
	})
	: id === 'index' ? (({ lhs, rhs }) => {
		let t = newRef();
		return true
			&& doBind(ast, infer(rhs), typeNumber)
			&& doBind(ast, infer(lhs), typeArrayOf(t))
			&& t;
	})
	: id === 'lambda' ? (({ bind, expr }) => {
		let vts1 = bindTypes(vts, bind);
		let tb = inferType(vts1, false, bind);
		let te = inferType(vts1, false, expr);
		return typeLambdaOf(tb, te);
	})
	: id === 'lambda-async' ? (({ bind, expr }) => {
		let vts1 = bindTypes(vts, bind);
		let tb = inferType(vts1, false, bind);
		let te = inferType(vts1, true, expr);
		return typeLambdaOf(tb, typePromiseOf(te));
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
		let tres = typeLambdaOf(tr, typeVoid);
		let trej = typeLambdaOf(typeError, typeVoid);
		return typeLambdaOf(typeLambdaOf(typePairOf(tres, trej), typeVoid), typePromiseOf(tr));
	})
	: id === 'not' ? (({ expr }) =>
		doBind(ast, infer(expr), typeBoolean) && typeBoolean
	)
	: id === 'num' ? (({}) =>
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
	: id === 'str' ? (({}) =>
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
	: id === 'try' ? (({ lhs, rhs }) =>
		doBind(ast, infer(rhs), newRef()) && infer(lhs)
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
	: id === 'var' ? (({ vn }) => {
		let t = finalRef(lookup(vts, vn));
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
		Promise: typePromise,
		console: typeConsole,
		require: typeRequire,
	})
	.reduce((l, vt) => cons(vt, l), nil);

let rewrite = (rf, ast) => {
	let { id } = ast;

	let f = false ? undefined
	: id === 'add' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'alloc' ? (({ vn, expr }) => ({ id, vn, expr: rf(expr) }))
	: id === 'and' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'app' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'array' ? (({ values }) => ({ id, values: values.map(rf) }))
	: id === 'assign' ? (({ bind, value, expr }) => ({ id, bind, value: rf(value), expr: rf(expr) }))
	: id === 'await' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'bool' ? (({ v }) => ast)
	: id === 'coal' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'cons' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'div' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'dot' ? (({ expr, field }) => ({ id, expr: rf(expr), field }))
	: id === 'eq_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'if' ? (({ if_, then, else_ }) => ({ id, if_: rf(if_), then: rf(then), else_: rf(else_) }))
	: id === 'index' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'lambda' ? (({ bind, expr }) => ({ id, bind, expr: rf(expr) }))
	: id === 'lambda-async' ? (({ bind, expr }) => ({ id, bind, expr: rf(expr) }))
	: id === 'le_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'let' ? (({ bind, value, expr }) => ({ id, bind, value: rf(value), expr: rf(expr) }))
	: id === 'lt_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'mul' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'ne_' ? (({ lhs, rhs }) => ({ id, lhs: rf(lhs), rhs: rf(rhs) }))
	: id === 'neg' ? (({ expr }) => ({ id, expr: rf(expr) }))
	: id === 'never' ? (({}) => ast)
	: id === 'new-error' ? (({}) => ast)
	: id === 'new-map' ? (({}) => ast)
	: id === 'new-promise' ? (({}) => ast)
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

let promiseResolve = _dot(_var('Promise'), '.resolve');
let promisify = ast => _app(promiseResolve, ast);

let unpromisify = ast => {
	let { id, lhs, rhs } = ast;
	return id === 'app' && lhs === promiseResolve ? rhs : undefined;
};

let reduceAsync;

reduceAsync = ast => {
	let { id } = ast;

	let _then = (p, bind, expr) => _app(_dot(p, '.then'), _lambda(bind, expr));

	let reduceOp = ({ expr }) => {
		let pe = reduceAsync(expr);
		let e = unpromisify(pe);
		let ve = e ?? _var(newDummy());
		let p = promisify({ id, expr: ve });
		return e !== undefined ? p : _then(pe, ve, p);
	};

	let reduceBinOp = ({ lhs, rhs }) => {
		let pl = reduceAsync(lhs);
		let l = unpromisify(pl);
		let vl = l ?? _var(newDummy());
		let pr = reduceAsync(rhs);
		let r = unpromisify(pr);
		let vr = r ?? _var(newDummy());
		let p;
		p = promisify({ id, lhs: vl, rhs: vr });
		p = l !== undefined ? p : _then(pl, vl, p);
		p = r !== undefined ? p : _then(pr, vr, p);
		return p;
	};

	let f = false ? undefined
	: id === 'add' ? reduceBinOp
	: id === 'alloc' ? (({ vn, expr }) => {
		let pe = reduceAsync(expr);
		let e = unpromisify(pe);
		return e !== undefined ? promisify({ id, vn, expr: e }) : { id, vn, expr: pe };
	})
	: id === 'and' ? reduceBinOp
	: id === 'app' ? reduceBinOp
	: id === 'await' ? (({ expr }) => expr)
	: id === 'coal' ? reduceBinOp
	: id === 'cons' ? reduceBinOp
	: id === 'div' ? reduceBinOp
	: id === 'dot' ? (({ expr, field }) => {
		let pe = reduceAsync(expr);
		let e = unpromisify(pe);
		let ve = e ?? _var(newDummy());
		let p = promisify({ id, expr: ve, field });
		return e !== undefined ? p : _then(pe, ve, p);
	})
	: id === 'eq_' ? reduceBinOp
	: id === 'if' ? (({ if_, then, else_ }) => {
		let pi = reduceAsync(if_);
		let i = unpromisify(pi);
		let vi = i ?? _var(newDummy());
		let pt = reduceAsync(then);
		let t = unpromisify(pt);
		let pe = reduceAsync(else_);
		let e = unpromisify(pe);
		return false ? undefined
		: i !== undefined && t !== undefined && e !== undefined ? promisify({ id, if_: vi, then: t, else_: e })
		: i !== undefined ? { id, if_: vi, then: pt, else_: pe }
		: _then(pi, vi, { id, if_: vi, then: pt, else_: pe });
	})
	: id === 'lambda-async' ? (({ bind, expr }) => promisify(_lambda(bind, reduceAsync(expr))))
	: id === 'le_' ? reduceBinOp
	: id === 'let' ? (({ bind, value, expr }) => {
		let pv = reduceAsync(value);
		let v = unpromisify(pv);
		let pe = reduceAsync(expr);
		let e = unpromisify(pe);
		return false ? undefined
		: e !== undefined && v !== undefined ? promisify({ id, bind, value: v, expr: e })
		: e === undefined && v !== undefined ? { id, bind, value: v, expr: pe }
		: _then(pv, bind, pe);
	})
	: id === 'lt_' ? reduceBinOp
	: id === 'mul' ? reduceBinOp
	: id === 'ne_' ? reduceBinOp
	: id === 'neg' ? reduceOp
	: id === 'not' ? reduceOp
	: id === 'or_' ? reduceBinOp
	: id === 'pos' ? reduceOp
	: id === 'sub' ? reduceBinOp
	: id === 'try' ? reduceBinOp
	: id === 'typeof' ? reduceOp
	: id === 'while' ? (({ cond, loop, expr }) => {
		let pc = reduceAsync(cond);
		let c = unpromisify(pc);
		let vc = c ?? _var(newDummy());
		let pl = reduceAsync(loop);
		let l = unpromisify(pl);
		let pe = reduceAsync(expr);
		let e = unpromisify(pe);
		return false ? undefined
		: c !== undefined && l !== undefined && e !== undefined ? promisify({ id, cond: vc, loop: l, expr: e })
		: c !== undefined && l !== undefined && e === undefined ? { id, cond: vc, loop: l, expr: pe }
		: function() {
			let vn = newDummy();
			let vp = _var(vn);
			let invoke = _app(vp, _never);
			let if_ = _if(vc, _then(pl, _var(newDummy()), invoke), pe);
			return _alloc(vn, _assign(vp, _lambda(_var(newDummy()), c !== undefined ? if_ : _then(pc, vc, if_)), invoke));
		}();
	})
	: (({}) => promisify(rewrite(ast_ => unpromisify(reduceAsync(ast_)), ast)));

	return f(ast);
};

let reduceNe;

reduceNe = ast => {
	let { id } = ast;

	let f = false ? undefined
	: id === 'ne_' ? (({ lhs, rhs }) => _not(_eq(reduceNe(lhs), reduceNe(rhs))))
	: (({}) => rewrite(reduceNe, ast));

	return f(ast);
};

let reduces = ast => unpromisify(reduceAsync(reduceNe(ast)));

let generate;

generate = ast => {
	let { id } = ast;

	let f = false ? undefined
	: id === 'add' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'alloc' ? (({ vn, expr }) => error('FIXME'))
	: id === 'and' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'app' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'array' ? (({ values }) => error('FIXME'))
	: id === 'assign' ? (({ bind, value, expr }) => error('FIXME'))
	: id === 'await' ? (({ expr }) => error('FIXME'))
	: id === 'bool' ? (({ v }) => error('FIXME'))
	: id === 'coal' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'cons' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'div' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'dot' ? (({ expr, field }) => error('FIXME'))
	: id === 'eq_' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'if' ? (({ if_, then, else_ }) => error('FIXME'))
	: id === 'index' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'lambda' ? (({ bind, expr }) => error('FIXME'))
	: id === 'lambda-async' ? (({ bind, expr }) => error('FIXME'))
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
	: id === 'not' ? (({ expr }) => error('FIXME'))
	: id === 'num' ? (({ i }) => error('FIXME'))
	: id === 'or_' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'pair' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'pos' ? (({ expr }) => error('FIXME'))
	: id === 'str' ? (({ v }) => error('FIXME'))
	: id === 'struct' ? (({ kvs }) => error('FIXME'))
	: id === 'sub' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'throw' ? (({ expr }) => error('FIXME'))
	: id === 'try' ? (({ lhs, rhs }) => error('FIXME'))
	: id === 'tuple' ? (({ values }) => error('FIXME'))
	: id === 'typeof' ? (({ expr }) => error('FIXME'))
	: id === 'undefined' ? (({}) => error('FIXME'))
	: id === 'var' ? (({ vn }) => error('FIXME'))
	: id === 'while' ? (({ cond, loop, expr }) => error('FIXME'))
	: error(`cannot generate for ${id}`);

	return f(ast);
};

let process_ = program => {
	let ast = parse(program);

	return { ast: reduces(ast), type: inferType(predefinedTypes, false, ast) };
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
		let { ast, type } = process(require('fs').readFileSync(0, 'utf8'));
		console.log(`ast :: ${stringify(ast)}`);
		console.log(`type :: ${dumpRef(type)}`);
		"console.log(`format :: ${format(ast)}`)";
		return true;
	} catch (e) { return console.error(e); }
}() : error(`
test case failed,
actual = ${actual}
expect = ${expect}`);
