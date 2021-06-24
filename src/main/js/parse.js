let ascii = s => s.charCodeAt(0);

let error = message => { throw new Error(message); };

let nil = [];

let contains = (list, e) => {
	let f;
	f = es => es !== nil && (es[0] === e || f(es[1]));
	return f(list);
};

let fold = (init, list, op) => {
	let f;
	f = (init, list) => list !== nil ? f(op(init, list[0]), list[1]) : init;
	return f(init, list);
};

let dump = v => {
	let f;
	f = (vs, v) => false ? ''
		: contains(vs, v)
			? '<recurse>'
		: v.id !== undefined
			? function() {
				let join = Object
					.entries(v)
					.filter(([k, v_]) => k !== 'id')
					.map(([k, v_]) => `${k}:${f([v, vs], v_)}`)
					.join(' ');
				return `${v.id}(${join})`;
			}()
		: typeof v === 'string'
			? v.toString()
		:
			JSON.stringify(v);
	return f(nil, v);
};

let isAll = pred => s => {
	let f;
	f = i => i < s.length ? pred(s.charCodeAt(i)) && f(i + 1) : true;
	return f(0);
};

let isIdentifier_ = isAll(ch => false
	|| ascii('0') <= ch && ch <= ascii('9')
	|| ascii('A') <= ch && ch <= ascii('Z')
	|| ch === ascii('_')
	|| ascii('a') <= ch && ch <= ascii('z'));

let isIdentifier = s => 0 < s.length && isIdentifier_(s);

let quoteBracket = (quote, bracket, ch) => {
	return {
		quote: quote === 0 && (ch === ascii("'") || ch === ascii('"') || ch === ascii('`')) ? ch
			: quote === ch ? 0
			: quote,
		bracket: false ? nil
			: quote === 0 && (ch === ascii('(') || ch === ascii('[') || ch === ascii('{')) ? bracket + 1
			: quote === 0 && (ch === ascii(')') || ch === ascii(']') || ch === ascii('}')) ? bracket - 1
			: bracket,
	};
};

let appendTrailingComma = s => s + (s === '' || s.endsWith(',') ? '' : ',');

let splitl = (s, sep) => {
	let f;
	f = (i, quote, bracket) => {
		let j = i + sep.length;
		return j <= s.length ? function() {
			let ch = s.charCodeAt(i);
			let { quote: quote1, bracket: bracket1 } = quoteBracket(quote, bracket, ch);

			return quote !== 0 || bracket !== 0 || s.slice(i, j) !== sep || i === 0
				? f(i + 1, quote1, bracket1)
				: [s.slice(0, i), s.slice(j, undefined)];
		}() : [s, undefined];
	};

	return f(0, 0, 0);
};

let splitr = (s, sep) => {
	let f;
	f = (j, quote, bracket) => {
		let i = j - sep.length;
		return 0 <= i ? function() {
			let ch = s.charCodeAt(j - 1);
			let { quote: quote1, bracket: bracket1 } = quoteBracket(quote, bracket, ch);

			return quote1 !== 0 || bracket1 !== 0 || s.slice(i, j) !== sep || i === 0
				? f(j - 1, quote1, bracket1)
				: [s.slice(0, i), s.slice(j, undefined)];
		}() : [undefined, s];
	};
	return f(s.length, 0, 0);
};

let keepsplitl = (s, sep, apply) => {
	let f;
	f = input => input !== '' ? function() {
		let [left, right] = splitl(input, sep);
		return [apply(left), f(right)];
	}() : nil;
	return f(s);
};

let parseAssocLeft_ = (id, op, parseValue) => {
	let f;
	f = program_ => {
		let program = program_.trim();
		let [left, right] = splitr(program, op);
		let rhs = parseValue(right);
		return left === undefined ? rhs : { id, lhs: f(left), rhs };
	};
	return f;
};

let parseAssocRight = (id, op, parseValue) => {
	let f;
	f = program_ => {
		let program = program_.trim();
		let [left, right] = splitl(program, op);
		let lhs = parseValue(left);
		return right === undefined ? lhs : { id, lhs, rhs: f(right) };
	};
	return f;
};

let parsePrefix = (id, op, parseValue) => {
	let f;
	f = program_ => {
		let program = program_.trim();
		return !program.startsWith(op)
			? parseValue(program)
			: { id, expr: f(program.slice(op.length, undefined)) };
	};
	return f;
};

let parseNumber = program => {
	let f;
	f = i => 0 <= i ? function() {
		let ch = program.charCodeAt(i);
		return ascii('0') <= ch && ch <= ascii('9')
			? f(i - 1) * 10 + ch - ascii('0')
			: error(`invalid number ${program}`);
	}() : 0;
	return f(program.length - 1);
};

let parseConstant = program => {
	let first = program.charCodeAt(0);

	return false ? {}
		: ascii('0') <= first && first <= ascii('9')
			? { id: 'number', value: program, i: parseNumber(program) }
		: program.startsWith("'") && program.endsWith("'")
			? { id: 'string', value: program.slice(1, program.length - 1) }
		: program.startsWith('"') && program.endsWith('"')
			? { id: 'string', value: program.slice(1, program.length - 1) }
		: program.startsWith('`') && program.endsWith('`')
			? { id: 'backquote', value: program.slice(1, program.length - 1) }
		: program === 'false'
			? { id: 'boolean', value: 'false' }
		: program === 'new Error'
			? { id: 'new-error' }
		: program === 'new Map'
			? { id: 'new-map' }
		: program === 'nil'
			? { id: 'nil' }
		: program === 'true'
			? { id: 'boolean', value: 'true' }
		: program === 'undefined'
			? { id: 'undefined' }
		: isIdentifier(program)
			? { id: 'var', value: program }
		:
			error(`cannot parse "${program}"`);
};

let parseArray = (program, parse) => ({
	id: 'array',
	values: keepsplitl(appendTrailingComma(program.slice(1, program.length - 1).trim()), ',', parse),
});

let parseTuple = (program, parse) => ({
	id: 'tuple',
	values: keepsplitl(appendTrailingComma(program.slice(1, program.length - 1).trim()), ',', parse),
});

let parseStructInner = (program, parse) => ({
	id: 'struct',
	kvs: keepsplitl(appendTrailingComma(program), ',', kv => {
		let [key_, value_] = splitl(kv, ':');
		let key = parseConstant(key_.trim()).value;
		let value = value_ !== undefined ? parse(value_) : { id: 'var', value: key };
		return { key, value };
	}),
});

let parseStruct = (program, parse) => parseStructInner(program.slice(1, program.length - 1).trim(), parse);

let parseProgram;

let parseValue;

parseValue = program_ => {
	let program = program_.trim();

	return false ? {}
		: program.startsWith('try {') && program.endsWith('}')
			? function() {
				let [try_, catch_] = splitl(program.slice(4, undefined), 'catch (e)');
				return {
					id: 'try',
					expr: parseProgram(try_),
					catch_: { id: 'lambda', bind: { id: 'var', value: 'e' }, expr: parseProgram(catch_) }
				};
			}()
		: program.startsWith('typeof ')
			? { id: 'typeof', expr: parseValue(program.slice(7, undefined)) }
		: program.startsWith('(') && program.endsWith(')')
			? parseProgram(program.slice(1, program.length - 1))
		: program.startsWith('[') && program.endsWith(']')
			? parseTuple(program, parseProgram)
		: program.startsWith('{') && program.endsWith('}')
			? function() {
				let block = program.slice(1, program.length - 1).trim();
				return block.endsWith(';') ? parseProgram(block) : parseStructInner(block, parseProgram);
			}()
		:
			parseConstant(program);
};

let parseApplyBlockFieldIndex;

let parseLvalue = program_ => {
	let program = program_.trim();
	let [expr, field] = splitr(program, '.');

	return false ? {}
		: expr !== undefined && isIdentifier(field)
			? { id: 'dot', field, expr: parseApplyBlockFieldIndex(expr) }
		: program.endsWith(']')
			? function() {
				let [expr, index_] = splitr(program, '[');
				let index = index_.slice(0, index_.length - 1);
				return expr === undefined ? parseValue(program)
					: index === '0' || index === '1'
						? {
							id: 'element',
							expr: parseProgram(expr),
							index,
						}
					:
						{
							id: 'index',
							expr: parseProgram(expr),
							index: parseProgram(index),
						};
			}()
		:
			parseValue(program);
};

parseApplyBlockFieldIndex = program_ => {
	let program = program_.trim();

	return false ? {}
		: program.startsWith('function() {') && program.endsWith('}()')
			? parseProgram(program.slice(12, program.length - 3).trim())
		: program.endsWith('()')
			? {
				id: 'apply',
				expr: parseProgram(program.slice(0, program.length - 2)),
				parameter: { id: 'array', values: nil },
			}
		: program.endsWith(')')
			? function() {
				let [expr, paramStr_] = splitr(program, '(');
				let paramStr = paramStr_.slice(0, paramStr_.length - 1).trim();
				return expr !== undefined ? {
					id: 'apply',
					expr: parseProgram(expr),
					parameter: parseProgram(paramStr),
				} : parseValue(program);
			}()
		:
			parseLvalue(program);
};

let parseDiv = parseAssocLeft_('div', '/', parseApplyBlockFieldIndex);
let parseMul = parseAssocRight('mul', '*', parseDiv);
let parseNeg = parsePrefix('neg', '-', parseMul);
let parseSub = parseAssocLeft_('sub', '-', parseNeg);
let parsePos = parsePrefix('pos', '+', parseSub);
let parseAdd = parseAssocRight('add', '+', parsePos);
let parseLt_ = parseAssocRight('lt_', '<', parseAdd);
let parseLe_ = parseAssocRight('le_', '<=', parseLt_);
let parseNot = parsePrefix('not', '!', parseLe_);
let parseNe_ = parseAssocRight('ne_', '!==', parseNot);
let parseEq_ = parseAssocRight('eq_', '===', parseNe_);
let parseAnd = parseAssocRight('and', '&&', parseEq_);
let parseOr_ = parseAssocRight('or_', '||', parseAnd);
let parseApp = parseAssocLeft_('app', '|>', parseOr_);

let parseIf = program => {
	let [if_, thenElse] = splitl(program, '?');

	return thenElse === undefined ? parseApp(if_) : function() {
		let [then, else_] = splitl(thenElse, ':');

		return {
			id: 'if',
			if_: parseProgram(if_),
			then: parseProgram(then),
			else_: parseProgram(else_),
		};
	}();
};

let parseBindPair;

parseBindPair = program => {
	let [left, right] = splitl(program, ',');
	let lhs = parseConstant(left.trim());

	return right === undefined ? lhs : { id: 'tuple', values: [lhs, [parseBindPair(right), nil]] };
};

let parseBind;

parseBind = program_ => {
	let program = program_.trim();

	return false ? {}
		: program === '()'
			? { id: 'array', values: nil }
		: program.startsWith('(') && program.endsWith(')')
			? parseBind(program.slice(1, program.length - 1))
		: program.startsWith('[') && program.endsWith(']')
			? parseTuple(program, parseBind)
		: program.startsWith('{') && program.endsWith('}')
			? parseStruct(program, parseBind)
		:
			parseBindPair(program);
};

let parseLambda = program => {
	let [left, right] = splitl(program, '=>');

	return right === undefined ? parseIf(left) : {
		id: 'lambda',
		bind: parseBind(left),
		expr: parseProgram(right.trim()),
	};
};

let parsePair;

parsePair = program_ => {
	let program = program_.trim();
	let [left, right] = splitl(program, ',');
	let lhs = parseLambda(left);
	return right === undefined ? lhs : { id: 'tuple', values: [lhs, [parsePair(right), nil]] };
};

parseProgram = program => {
	let [statement_, expr_] = splitl(program, ';');

	return expr_ === undefined ? parsePair(statement_) : function() {
		let statement = statement_.trim();
		let expr = expr_.trim();

		return false ? {}
			: statement.startsWith('let ')
				? function() {
					let [var_, value] = splitl(statement.slice(4, undefined), '=');
					let v = var_.trim();
	
					return value !== undefined
						? {
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
						:
							error(`cannot parse let variable "${v}"`);
				}()
			: statement.startsWith('return ') && expr === ''
				? parseProgram(statement.slice(7, undefined))
			: statement.startsWith('throw ') && expr === ''
				? { id: 'throw', expr: parseProgram(statement.slice(6, undefined)) }
			:
				function() {
					let [var_, value] = splitl(statement, '=');
	
					return {
						id: 'assign',
						v: parseLvalue(var_),
						value: parseProgram(value),
						expr: parseProgram(expr),
					};
				}();
	}();
};

let mergeBindVariables;

mergeBindVariables = (vs, ast) => false ? vs
	: ast.id === 'array' ? fold(vs, ast.values, mergeBindVariables)
	: ast.id === 'nil' ? vs
	: ast.id === 'struct' ? fold(vs, ast.kvs, (vs_, kv) => mergeBindVariables(vs_, kv.value))
	: ast.id === 'tuple' ? fold(vs, ast.values, mergeBindVariables)
	: ast.id === 'var' ? [ast.value, vs]
	: error(`cannot destructure ${ast}`);

let checkVariables = (vs, ast) => {
	let f;
	f = ast => {
		let g = id => id === undefined ? (ast => true)
			: id === 'alloc'
				? (({ v, expr }) => f([v, vs], expr))
			: id === 'assign'
				? (({ v, value, expr }) => contains(vs, v) && f(vs, value) && f(vs, expr))
			: id === 'lambda'
				? (({ bind, expr }) => f(mergeBindVariables(vs, bind), expr))
			: id === 'let'
				? (({ bind, value, expr }) => function() {
					try {
						return f(vs, value);
					} catch (e) {
						e.message = `in value clause of ${dump(bind)}\n${e.message}`;
						throw e;
					}
				}() && f(mergeBindVariables(vs, bind), expr))
			: id === 'var'
				? (({ value: v }) => contains(vs, v) || error(`undefined variable ${v}`))
			:
				(ast => {
					let kvs = Object.entries(ast);
					let h;
					h = i => i < kvs.length ? f(vs, kvs[i][1]) && h(i + 1) : true;
					return h(0);
				});
		return g(ast.id)(ast);
	};
	return f(ast);
};

let refs = new Map();
let refCount;

refCount = 0;

let finalRef;

finalRef = v => v.ref !== undefined && refs.get(v.ref) !== v ? finalRef(refs.get(v.ref)) : v;

let setRef = (ref, target) => {
	let dummy = refs.set(ref, target);
	return true;
};

let newRef = () => {
	refCount = refCount + 1;
	let ref = { ref: refCount };
	let dummy = refs.set(refCount, ref);
	return setRef(refCount, ref) && ref;
};

let dumpRef = v => {
	let f;
	f = (vs, v) => false ? ''
		: contains(vs, v)
			? '<recurse>'
		: v.ref !== undefined
			? (refs.get(v.ref) !== v ? f([v, vs], refs.get(v.ref)) : `_${v.ref}`)
		: v.length === 0
			? ''
		: v.length === 2
			? `${f(vs, v[0])}:${f(vs, v[1])}`
		: typeof v === 'object'
			? function() {
				let id = v.id;
				let join = Object
					.entries(v)
					.filter(([k, v_]) => k !== 'id')
					.map(([k, v_]) => `${k}:${f([v, vs], v_)}`)
					.join(' ');
				return id !== undefined ? `${id}(${join})` : `{${join}}`;
			}()
		: typeof v === 'string'
			? v.toString()
		:
			JSON.stringify(v);
	return f(nil, v);
};

let cloneRef = v => {
	let fromTos = new Map();

	let f;
	f = (vs, v) => false ? ''
		: contains(vs, v)
			? '<recurse>'
		: v.ref !== undefined && refs.get(v.ref) !== v
			? f([v, vs], refs.get(v.ref))
		: v.ref !== undefined
			? function() {
				let w = fromTos.get(v.ref);
				return w !== undefined ? w : function() {
					let w1 = newRef();
					let dummy = fromTos.set(v.ref, w1);
					return w1;
				}();
			}()
		: typeof v === 'string'
			? v
		: v.length !== undefined
			? v.map(v_ => f([v, vs], v_))
		: typeof v === 'object'
			? Object.fromEntries(Object.entries(v).map(([k, v_]) => [k, f([v, vs], v_)]))
		:
			error(`cannot clone ${dumpRef(v)}`);
	return f(nil, v);
};

let tryBind;

tryBind = (a, b) => function() {
	let refa = a.ref;
	let refb = b.ref;
	return false ? true
		: refa !== undefined && refs.get(refa) !== a
			? tryBind(refs.get(refa), b)
		: refb !== undefined && refs.get(refb) !== b
			? tryBind(a, refs.get(refb))
		: refa !== undefined && refb !== undefined
			? (refa < refb ? setRef(refb, a) : setRef(refa, b))
		: refa !== undefined
			? setRef(refa, b)
		: refb !== undefined
			? setRef(refb, a)
		: a === b
			? true
		: a.length !== undefined
			? (a.length === b.length && function() {
				let f;
				f = index => index === a.length || tryBind(a[index], b[index]) && f(index + 1);
				return f(0);
			}())
		: typeof a === 'object' && typeof b === 'object'
			&& Object.keys(a).reduce((r, k) => {
				let dummy = b.completed !== true && b[k] !== undefined || function() { b[k] = newRef(); return b[k]; }();
				return r && tryBind(a[k], b[k]);
			}, true)
			&& Object.keys(b).reduce((r, k) => {
				let dummy = a.completed !== true && a[k] !== undefined || function() { a[k] = newRef(); return a[k]; }();
				return r && tryBind(a[k], b[k]);
			}, true);
}();

let doBind = (ast, a, b) => tryBind(a, b) || error(`cannot bind type\nfr: ${dumpRef(a)}\nto: ${dumpRef(b)}\nin ${dump(ast)}`);

let lookup = (vts, v) => {
	let f;
	f = vts => vts !== nil ? function() {
		let [[v_, t], vts_] = vts;
		return v_ === v ? t : f(vts_, v);
	}() : error(`undefined variable ${v}`);
	return f(vts, v);
};

let defineBindTypes;

defineBindTypes = (vts, ast) => {
return false ? vts
	: ast.id === 'array' ? fold(vts, ast.values, defineBindTypes)
	: ast.id === 'nil' ? vts
	: ast.id === 'struct' ? fold(vts, ast.kvs, (vts_, kv) => defineBindTypes(vts_, kv.value))
	: ast.id === 'tuple' ? fold(vts, ast.values, defineBindTypes)
	: ast.id === 'var' ? [[ast.value, newRef()], vts]
	: error(`cannot destructure ${ast}`);
};

let typeArrayOf = type => ({ id: 'array', of: type });
let typeBoolean = ({ id: 'boolean' });
let typeLambdaOf = (in_, out) => ({ id: 'lambda', in_, out });
let typeNever = { id: 'never' };
let typeNumber = ({ id: 'number' });
let typeString = typeArrayOf({ id: 'char' });
let typeStructOf = kvs => ({ id: 'struct', kvs });
let typeTupleOf = types => ({ id: 'tuple', types });

let inferType;

inferType = (vts, ast) => {
	let id = ast.id;

	let inferCmpOp = ({ lhs, rhs }) => function() {
		let t = newRef();
		return true
			&& doBind(ast, inferType(vts, lhs), t)
			&& doBind(ast, inferType(vts, rhs), t)
			&& (tryBind(t, typeNumber) || tryBind(t, typeString) || error(`cannot compare values with type ${t}`))
			&& typeBoolean;
	}();

	let inferEqOp = ({ lhs, rhs }) => true
		&& doBind(ast, inferType(vts, lhs), inferType(vts, rhs))
		&& typeBoolean;

	let inferLogicalOp = ({ lhs, rhs }) => true
		&& doBind(ast, inferType(vts, lhs), typeBoolean)
		&& inferType(vts, rhs);

	let inferMathOp = ({ lhs, rhs }) => true
		&& doBind(ast, inferType(vts, lhs), typeNumber)
		&& doBind(ast, inferType(vts, rhs), typeNumber)
		&& typeNumber;

	let f = false ? (({}) => {})
		: id === 'add'
			? (({ lhs, rhs }) => {
				let t = newRef();
				return true
					&& doBind(ast, inferType(vts, lhs), t)
					&& doBind(ast, inferType(vts, rhs), t)
					&& (tryBind(t, typeNumber) || tryBind(t, typeString) || error(`cannot add values with type ${t}`))
					&& t;
			})
		: id === 'alloc'
			? (({ v, expr }) => inferType([[v, newRef()], vts], expr))
		: id === 'and'
			? inferLogicalOp
		: id === 'app'
			? (({ lhs, rhs }) => {
				let te = inferType(vts, lhs);
				let tp = inferType(vts, rhs);
				let tr = newRef();
				return doBind(ast, te, typeLambdaOf(tp, tr)) && tr;
			})
		: id === 'apply'
			? (({ parameter, expr }) => {
				let te = inferType(vts, expr);
				let tp = inferType(vts, parameter);
				let tr = newRef();
				return doBind(ast, te, typeLambdaOf(tp, tr)) && tr;
			})
		: id === 'array'
			? (({ values }) => {
				let te = newRef();
				return fold(true, values, (b, value) => b && doBind(ast, inferType(vts, value), te)) && typeArrayOf(te);
			})
		: id === 'assign'
			? (({ v, value, expr }) => {
				return function() {
					try {
						let tvar = inferType(vts, v);
						let tvalue = inferType(vts, value);
						return doBind({ id: 'assign', v, value }, tvar, tvalue);
					} catch (e) {
						e.message = `in assignment clause of ${dump(v)}\n${e.message}`;
						throw e;
					}
				}() && inferType(vts, expr);
			})
		: id === 'backquote'
			? (({}) => typeString)
		: id === 'boolean'
			? (({}) => typeBoolean)
		: id === 'div'
			? inferMathOp
		: id === 'dot'
			? (({ field, expr }) => false ? {}
				: field === 'charCodeAt'
					? doBind(ast, inferType(vts, expr), typeString) && typeLambdaOf(typeNumber, typeNumber)
				: field === 'endsWith'
					? doBind(ast, inferType(vts, expr), typeString) && typeLambdaOf(typeString, typeBoolean)
				: field === 'length'
					? doBind(ast, inferType(vts, expr), typeArrayOf(newRef())) && typeNumber
				: field === 'slice'
					? doBind(ast, inferType(vts, expr), typeArrayOf(newRef())) && typeLambdaOf(typeTupleOf([typeNumber, [typeNumber, nil]]), typeString)
				: field === 'startsWith'
					? doBind(ast, inferType(vts, expr), typeString) && typeLambdaOf(typeString, typeBoolean)
				: field === 'toString'
					? doBind(ast, inferType(vts, expr), newRef()) && typeLambdaOf(typeArrayOf(typeNever), typeString)
				: field === 'trim'
					? doBind(ast, inferType(vts, expr), typeString) && typeLambdaOf(typeArrayOf(typeNever), typeString)
				: function() {
					let tr = newRef();
					let kvs = {};
					kvs[field] = tr;
					let to = typeStructOf(kvs);
					return doBind(ast, inferType(vts, expr), to) && tr;
				}())
		: id === 'element'
			? (({ index, expr }) => {
				let te = newRef();
				return doBind(ast, inferType(vts, expr), ((false ? {}
					: index === '0' ? typeTupleOf([te, newRef()])
					: index === '1' ? typeTupleOf([newRef(), [te, newRef()]])
					: {}))) && te;
			})
		: id === 'eq_'
			? inferEqOp
		: id === 'if'
			? (({ if_, then, else_ }) => {
				let tt = function() {
					try {
						return inferType(vts, then);
					} catch (e) {
						e.message = `in then clause of ${dump(if_)}\n${e.message}`;
						throw e;
					}
				}();

				let te = inferType(vts, else_);
				return doBind(ast, inferType(vts, if_), typeBoolean) && doBind(ast, tt, te) && tt;
			})
		: id === 'index'
			? (({ index, expr }) => {
				let t = newRef();
				return true
					&& doBind(ast, inferType(vts, index), typeNumber)
					&& doBind(ast, inferType(vts, expr), typeArrayOf(t))
					&& t;
			})
		: id === 'lambda'
			? (({ bind, expr }) => {
				let vts1 = defineBindTypes(vts, bind);
				let tb = inferType(vts1, bind);
				let te = inferType(vts1, expr);
				return typeLambdaOf(tb, te);
			})
		: id === 'le_'
			? inferCmpOp
		: id === 'let'
			? (({ bind, value, expr }) => {
				let vts1 = defineBindTypes(vts, bind);
				return function() {
					try {
						let tb = inferType(vts1, bind);
						let tv = inferType(vts, value);
						return doBind({ id: 'let', bind, value }, tb, tv);
					} catch (e) {
						e.message = `in value clause of ${dump(bind)}\n${e.message}`;
						throw e;
					}
				}() && inferType(vts1, expr);
			})
		: id === 'lt_'
			? inferCmpOp
		: id === 'mul'
			? inferMathOp
		: id === 'ne_'
			? inferEqOp
		: id === 'neg'
			? (({ expr }) => doBind(ast, inferType(vts, expr), typeNumber) && typeNumber)
		: id === 'new-error'
			? (({}) => typeLambdaOf(typeString, { id: 'error' }))
		: id === 'new-map'
			? (({}) => typeLambdaOf(typeArrayOf(newRef()), { id: 'map' }))
		: id === 'nil'
			? (({}) => newRef())
		: id === 'not'
			? (({ expr }) => doBind(ast, inferType(vts, expr), typeBoolean) && typeBoolean)
		: id === 'number'
			? (({}) => typeNumber)
		: id === 'or_'
			? inferLogicalOp
		: id === 'pos'
			? (({ expr }) => doBind(ast, inferType(vts, expr), typeNumber) && typeNumber)
		: id === 'string'
			? (({}) => typeString)
		: id === 'struct'
			? (({ kvs }) => {
				let g;
				g = kvs => 0 < kvs.length ? function() {
					let { key, value } = kvs[0];
					let type = g(kvs[1]);
					type[key] = inferType(vts, value);
					return type;
				}() : {};
				return typeStructOf(g(kvs));
			})
		: id === 'sub'
			? inferMathOp
		: id === 'throw'
			? (({}) => newRef())
		: id === 'try'
			? (({ expr, catch_ }) => doBind(ast, inferType(vts, catch_), newRef()) && inferType(vts, expr))
		: id === 'tuple'
			? (({ values }) => {
				let g;
				g = vs => vs !== nil ? function() {
					let [head, tail] = vs;
					return [inferType(vts, head), g(tail)];
				}() : nil;
				return typeTupleOf(g(values));
			})
		: id === 'typeof'
			? (({}) => typeString)
		: id === 'undefined'
			? (({}) => newRef())
		: id === 'var'
			? (({ value }) => cloneRef(lookup(vts, value)))
		:
			(({}) => error(`cannot infer type for ${id}`));

	return f(ast);
};

let rewrite = r => ast => {
	let f;
	f = ast0 => ast0.id === undefined ? ast0 : function() {
		let ast1 = r(ast0.id)(ast0);
		return ast1 === undefined
			? Object.fromEntries(Object.entries(ast0).map(([k, v]) => [k, f(v)]))
			: ast1;
	}();
	return f(ast);
};

let process = program => {
	let ast = parseProgram(program);

	let b = checkVariables([
		'JSON', [
			'Object', [
				'console', [
					'require', nil
				]
			]
		]
	], ast);

	let type = inferType([
		['JSON', newRef()], [
			['Object', newRef()], [
				['console', newRef()], [
					['require', newRef()], nil
				]
			]
		]
	], ast);

	return { b, ast, type };
};

let stringify = json => JSON.stringify(json, undefined, '  ');

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
			field: 'log',
			expr: { id: 'var', value: 'console' },
		},
		parameter: {
			id: 'apply',
			expr: { id: 'var', value: 'parse' },
			parameter: {
				id: 'apply',
				expr: {
					id: 'dot',
					field: 'readFileSync',
					expr: {
						id: 'apply',
						expr: { id: 'var', value: 'require' },
						parameter: { id: 'string', value: 'fs' },
					},
				},
				parameter: {
					id: 'tuple',
					values: [{ id: 'number', value: '0', i: 0 }, [{ id: 'string', value: 'utf8' }, nil]],
				},
			},
		},
	},
});

return actual === expect
? function() {
	try {
		let { b, ast, type } = process(require('fs').readFileSync(0, 'utf8'));
		let dummy1 = console.log(`ast :: ${stringify(ast)}`);
		let dummy2 = console.log(`type :: ${dumpRef(type)}`);
		return b;
	} catch (e) {
		return console.error(e);
	}
}() : error(`
test case failed,
actual = ${actual}
expect = ${expect}`);
