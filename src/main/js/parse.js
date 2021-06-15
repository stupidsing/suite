let error = message => { throw new Error(message); };

let ascii = s => s.charCodeAt(0);

let contains = (list, e) => {
	let f;
	f = es => 0 < es.length && (es[0] === e || f(es[1]));
	return f(list);
};

let repeat = (init, when, iterate) => {
	let f;
	f = value => when(value) ? f(iterate(value)) : value;
	return f(init);
};

let fold = (init, list, op) => {
	let f;
	f = (init, list) => list.length === 2 ? f(op(init, list[0]), list[1]) : init;
	return f(init, list);
};

let zip = (lhs, rhs) => {
	let f;
	f = (lhs, rhs) => lhs.length !== 0 || rhs.length !== 0
			? [[lhs[0], rhs[0]], f(lhs[1], rhs[1])]
			: [];
	return f(lhs, rhs);
};

let isAll = pred => list => {
	let f;
	f = i => i < list.length ? pred(list.charCodeAt(i)) && f(i + 1) : true;
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
		quote: quote === '' && (ch === ascii("'") || ch === ascii('"') || ch === ascii('`')) ? ch
			: quote === ch ? ''
			: quote,
		bracket: false ? {}
			: quote === '' && (ch === ascii('(') || ch === ascii('[') || ch === ascii('{')) ? bracket + 1
			: quote === '' && (ch === ascii(')') || ch === ascii(']') || ch === ascii('}')) ? bracket - 1
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

			return quote || bracket !== 0 || s.substring(i, j) !== sep || i === 0
				? f(i + 1, quote1, bracket1)
				: [s.substring(0, i), s.substring(j)];
		}() : [s, undefined];
	};

	return f(0, '', 0);
};

let splitr = (s, sep) => {
	let f;
	f = (j, quote, bracket) => {
		let i = j - sep.length;
		return 0 <= i ? function() {
			let ch = s.charCodeAt(j - 1);
			let { quote: quote1, bracket: bracket1 } = quoteBracket(quote, bracket, ch);

			return quote1 || bracket1 !== 0 || s.substring(i, j) !== sep || i === 0
				? f(j - 1, quote1, bracket1)
				: [s.substring(0, i), s.substring(j)];
		}() : [undefined, s];
	};
	return f(s.length, '', 0);
};

let keepsplitl = (s, sep, apply) => {
	let f;
	f = input => input !== '' ? function() {
		let [left, right] = splitl(input, sep);
		return [apply(left), f(right)];
	}() : [];
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
	let parse = program_ => {
		let program = program_.trim();
		return !program.startsWith(op)
			? parseValue(program)
			: { id, expr: parse(program.substring(op.length)) };
	};
	return parse;
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
			? { id: 'number', value: parseNumber(program) }
		: program.startsWith("'") && program.endsWith("'")
			? { id: 'string', value: program.substring(1, program.length - 1) }
		: program.startsWith('"') && program.endsWith('"')
			? { id: 'string', value: program.substring(1, program.length - 1) }
		: program.startsWith('`') && program.endsWith('`')
			? { id: 'backquote', value: program.substring(1, program.length - 1) }
		: program === 'false'
			? { id: 'boolean', value: 'false' }
		: program === 'new Map'
			? { id: 'new-map' }
		: program === 'true'
			? { id: 'boolean', value: 'true' }
		: program === 'undefined'
			? { id: 'empty' }
		: isIdentifier(program)
			? { id: 'var', value: program }
		:
			error(`cannot parse "${program}"`);
};

let parseList = (program, parse) => ({
	id: 'list',
	values: keepsplitl(appendTrailingComma(program.substring(1, program.length - 1).trim()), ',', parse),
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

let parseStruct = (program, parse) => parseStructInner(program.substring(1, program.length - 1).trim(), parse);

let parseProgram;

let parseValue = program_ => {
	let program = program_.trim();

	return false ? {}
		: program.startsWith('typeof ')
			? { id: 'typeof', expr: parseValue(program.substring(7)) }
		: program.startsWith('(') && program.endsWith(')')
			? parseProgram(program.substring(1, program.length - 1))
		: program.startsWith('[') && program.endsWith(']')
			? parseList(program, parseProgram)
		: program.startsWith('{') && program.endsWith('}')
			? function() {
				let block = program.substring(1, program.length - 1).trim();
				return block.endsWith(';') ? parseProgram(block) : parseStructInner(block, parseProgram);
			}()
		:
			parseConstant(program);
};

let parseLvalue = program_ => {
	let program = program_.trim();
	let [expr, field] = splitr(program, '.');

	return false ? {}
		: expr !== undefined && isIdentifier(field)
			? { id: 'dot', field, expr: parseApplyBlockFieldIndex(expr) }
		: program.endsWith(']')
			? function() {
				let [expr, index] = splitr(program, '[');
				return expr !== undefined ? {
					id: 'index',
					expr: parseProgram(expr),
					index: parseProgram(index.substring(0, index.length - 1)),
				} : parseValue(program);
			}()
		:
			parseValue(program);
};

let parseApplyBlockFieldIndex = program_ => {
	let program = program_.trim();

	return false ? {}
		: program.startsWith('function() {') && program.endsWith('}()')
			? parseProgram(program.substring(12, program.length - 3).trim())
		: program.endsWith('()')
			? {
				id: 'apply',
				expr: parseProgram(program.substring(0, program.length - 2)),
				parameter: { id: 'empty' },
			}
		: program.endsWith(')')
			? function() {
				let [expr, paramStr_] = splitr(program, '(');
				let paramStr = paramStr_.substring(0, paramStr_.length - 1).trim();
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

let parseBindPair = program => {
	let [left, right] = splitl(program, ',');
	let lhs = parseConstant(left.trim());

	return right === undefined ? lhs : { id: 'pair', lhs, rhs: parseBindPair(right) };
};

let parseBind = program => {
	let f;
	f = program_ => {
		let program = program_.trim();

		return false ? {}
			: program === '()'
				? { id: 'empty' }
			: program.startsWith('(') && program.endsWith(')')
				? f(program.substring(1, program.length - 1))
			: program.startsWith('[') && program.endsWith(']')
				? parseList(program, f)
			: program.startsWith('{') && program.endsWith('}')
				? parseStruct(program, f)
			:
				parseBindPair(program);
	};
	return f(program);
};

let parseLambda = program => {
	let [left, right] = splitl(program, '=>');

	return right === undefined ? parseIf(left) : {
		id: 'lambda',
		bind: parseBind(left),
		expr: parseProgram(right.trim()),
	};
};

let parsePair = parseAssocRight('pair', ',', parseLambda);

parseProgram = program => {
	let [statement_, expr] = splitl(program, ';');
	let statement = statement_.trim();

	return false ? {}
		: statement.startsWith('let ')
			? function() {
				let [var_, value] = splitl(statement.substring(4), '=');
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
			? parseProgram(statement.substring(7))
		: statement.startsWith('throw ') && expr === ''
			? { id: 'error' }
		: expr !== undefined
			? function() {
				let [var_, value] = splitl(statement, '=');

				return {
					id: 'assign',
					v: parseLvalue(var_),
					value: parseProgram(value),
					expr: parseProgram(expr),
				};
			}()
		:
			parsePair(statement);
};

let mergeBindVariables = (vs, ast) => {
	let f;
	f = (vs, ast) => {
	return false ? {}
		: ast.id === 'list' ? fold(vs, ast.values, f)
		: ast.id === 'pair' ? f(f(vs, ast.lhs), ast.rhs)
		: ast.id === 'struct' ? fold(vs, ast.kvs, (vs_, kv) => f(vs_, kv.value))
		: ast.id === 'var' ? [ast.value, vs]
		: vs;
	};
	return f(vs, ast);
};

let checkVariables;

checkVariables = (vs, ast) => {
	let f = id => id === undefined ? (ast => true)
		: id === 'alloc' ? (({ v, expr }) => {
			return checkVariables([v, vs], expr);
		})
		: id === 'assign' ? (({ v, value, expr }) => {
			return contains(vs, v) && checkVariables(vs, value) && checkVariables(vs, expr);
		})
		: id === 'lambda' ? (({ bind, expr }) => {
			return checkVariables(mergeBindVariables(vs, bind), expr);
		})
		: id === 'let' ? (({ bind, value, expr }) => {
			let vs1 = mergeBindVariables(vs, bind);
			return checkVariables(vs, value) && checkVariables(vs1, expr);
		})
		: id === 'var' ? (({ value: v }) => {
			return contains(vs, v) || error(`undefined variable ${v}`);
		})
		: (ast => {
			let kvs = Object.entries(ast);
			let g;
			g = i => i < kvs.length ? checkVariables(vs, kvs[i][1]) && g(i + 1) : true;
			return g(0);
		});
	return f(ast.id)(ast);
};

let refs = new Map();
let refCount;

refCount = 0;

let setRef = (ref, target) => {
	let dummy = refs.set(ref, target);
	return true;
};


let newRef = () => {
	refCount = refCount + 1;
	let ref = { ref: refCount };
	let dummy = refs.set(refCount, ref);
	return ref;
};

let tryBind = (a, b) => {
	let f;
	f = (a, b) => function() {
		let refa = a.ref;
		let refb = b.ref;
		return false ? true
			: refa !== undefined && refs.get(refa) !== a
				? f(refs.get(refa), b)
			: refb !== undefined && refs.get(refb) !== b
				? f(a, refs.get(refb))
			: refa !== undefined && refb !== undefined
				? (refa < refb ? setRef(refa, b) : setRef(refb, a))
			: refa !== undefined
				? setRef(refa, b)
			: refb !== undefined
				? setRef(refb, a)
			: typeof a === 'string' && typeof b === 'string'
				? a === b
			: a.length === 0 && b.length === 0
				? true
			: a.length !== undefined && b.length !== undefined
				? f(a[0], b[0]) && f(a.slice(1), b.slice(1))
			: typeof a === 'object' && typeof b === 'object'
					&& Object.keys(a).reduce((b, k) => {
						let dummy = b.fixed !== true && b[k] !== undefined || function() { b[k] = newRef(); return b[k]; }();
						return b && f(a[k], b[k]);
					}, true)
					&& Object.keys(b).reduce((b, k) => {
						let dummy = a.fixed !== true && a[k] !== undefined || function() { a[k] = newRef(); return a[k]; }();
						return b && f(a[k], b[k]);
					});
	}();
	return f(a, b);
};

let solveBind = (a, b) => tryBind(a, b) || error(`cannot bind type ${a} to ${b}`);

let lookup = (vts, v) => {
	let f;
	f = vts => {
		let [v_, t, vts_] = vts;
		return v_ !== undefined ? (v_ === v ? t : f(vts_, v)) : error(`undefined variable ${v}`);
	};
	return f(vts, v);
};

let defineBindTypes = (vs, ast) => {
	let f;
	f = (vs, ast) => {
	return false ? {}
		: ast.id === 'list' ? fold(vs, ast.values, f)
		: ast.id === 'pair' ? f(f(vs, ast.lhs), ast.rhs)
		: ast.id === 'struct' ? fold(vs, ast.kvs, (vs_, kv) => f(vs_, kv.value))
		: ast.id === 'var' ? [ast.value, newRef(), vs]
		: error(`cannot destructure ${ast}`);
	};
	return f(vs, ast);
};

let typeString = ['list', 'string'];

let inferType = (vts, ast) => {
	let f;
	f = (vts, ast) => function() {
		let id = ast.id;

		let inferCmpOp = ({ lhs, rhs }) => function() {
			let t = newRef();
			return true
				&& solveBind(f(vts, lhs), t)
				&& solveBind(f(vts, rhs), t)
				&& (tryBind(t, 'number') || tryBind(t, typeString) || error(`cannot compare values with type ${t}`))
				&& 'boolean';
		}();

		let inferEqOp = ({ lhs, rhs }) => true
			&& solveBind(f(vts, lhs), f(vts, rhs))
			&& 'boolean';

		let inferLogicalOp = ({ lhs, rhs }) => true
			&& solveBind(f(vts, lhs), 'boolean')
			&& f(vts, rhs);

		let inferMathOp = ({ lhs, rhs }) => true
			&& solveBind(f(vts, lhs), 'number')
			&& solveBind(f(vts, rhs), 'number')
			&& 'number';

		let g = false ? {}
			: id === 'add'
				? inferMathOp
			: id === 'alloc'
				? (({ v, expr }) => f([v, newRef(), vts], expr))
			: id === 'and'
				? inferLogicalOp
			: id === 'app'
				? (({ lhs, rhs }) => {
					let te = f(vts, lhs);
					let tp = f(vts, rhs);
					let tr = newRef();
					return solveBind(te, ['lambda', tp, tr]) && tr;
				})
			: id === 'apply'
				? (({ parameter, expr }) => {
					let te = f(vts, expr);
					let tp = f(vts, parameter);
					let tr = newRef();
					return solveBind(te, ['lambda', tp, tr]) && tr;
				})
			: id === 'assign'
				? (({ v, value, expr }) => {
					let tvar = f(vts, v);
					let tvalue = f(vts, value);
					return solveBind(tvar, tvalue) && f(vts, expr);
				})
			: id === 'backquote'
				? (({}) => typeString)
			: id === 'boolean'
				? (({}) => id)
			: id === 'div'
				? inferMathOp
			: id === 'dot'
				? (({ field, expr }) => false ? {}
					:field === 'charCodeAt'
						?  solveBind(f(vts, expr), typeString) && ['lambda', 'number', 'number']
					:field === 'length'
						?  solveBind(f(vts, expr), ['list', newRef()]) && 'number'
					: function() {
						let t = f(vts, expr)[field];
						return t !== undefined ? t : error(`field ${field} not found`);
					}())
			: id === 'empty'
				? (({}) => ['list',  newRef()])
			: id === 'eq_'
				? inferEqOp
			: id === 'error'
				? (({}) => newRef())
			: id === 'if'
				? (({ if_, then, else_ }) => {
					let tt = f(vts, then);
					let te = f(vts, else_);
					return solveBind(if_, 'boolean') && solveBind(tt, te) && tt;
				})
			: id === 'index'
				? (({ index, expr }) => {
					let t = newRef();
					return true
						&& solveBind(f(vts, index), 'number')
						&& solveBind(f(vts, expr), ['list', t])
						&& t;
				})
			: id === 'lambda'
				? (({ bind, expr }) => {
					let vts1 = defineBindTypes(vts, bind);
					let tb = f(vts1, bind);
					let te = f(vts1, expr);
					return ['lambda', tb, te];
				})
			: id === 'le_'
				? inferCmpOp
			: id === 'let'
				? (({ bind, value, expr }) => {
					let vts1 = defineBindTypes(vts, bind);
					let tb = f(vts1, bind);
					let tv = f(vts1, value);
					return solveBind(tb, tv) && f(vts1, expr);
				})
			: id === 'list'
				? (({ values }) => {
					let g;
					g = (vts, values) => values.length === 2 ? function() {
						let [head, tail] = values;
						return [f(vts, head), g(vts, tail)];
					}() : [];
					return g(ast, values);
				})
			: id === 'lt_'
				? inferCmpOp
			: id === 'mul'
				? inferMathOp
			: id === 'ne_'
				? inferEqOp
			: id === 'new-map'
				? (({}) => 'map')
			: id === 'number'
				? (({}) => id)
			: id === 'or_'
				? inferLogicalOp
			: id === 'pair'
				? (({ lhs, rhs }) => ['pair', f(vts, lhs), f(vts, rhs)])
			: id === 'string'
				? (({}) => typeString)
			: id === 'struct'
				? (({ kvs }) => {
					let g;
					g = (struct, kvs) => kvs.length === 2 ? function() {
						let { key, value } = kvs[0];
						struct[key] = f(vts, value);
						return g(kvs[1]);
					}() : {};
					let dummy = g({ id: 'struct' }, kvs);
					return struct;
				})
			: id === 'sub'
				? inferMathOp
			: id === 'typeof'
				? (({}) => typeString)
			: id === 'var'
				? (({ value }) => lookup(vts, value))
			:
				(({}) => error(`cannot infer type for ${id}`));

		return g(ast);
	}();
	return f(vts, ast);
};

let rewrite;

rewrite = f => ast0 => {
	return ast0.id === undefined ? ast0 : function() {
		let ast1 = f(ast0.id)(ast0);
		return ast1 === undefined
			? Object.fromEntries(Object.entries(ast0).map(([k, v]) => [k, rewrite(v)]))
			: ast1;
	}();
};

let stringify = json => JSON.stringify(json, undefined, '  ');

let actual = stringify(parseProgram(`
	console.log(parse(require('fs').readFileSync(0, 'utf8')))
`));

let expect = stringify({
	id: 'apply',
	expr: {
		id: 'dot',
		field: 'log',
		expr: { id: 'var', value: 'console' }
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
					parameter: { id: 'string', value: 'fs' }
				}
			},
			parameter: {
				id: 'pair',
				lhs: { id: 'number', value: 0 },
				rhs: { id: 'string', value: 'utf8' }
			}
		}
	}
});

actual === expect
? function() {
	let ast = parseProgram(require('fs').readFileSync(0, 'utf8'));
	let dummy0 = checkVariables([
		'JSON', [
			'Object', [
				'console', [
					'require', []
				]
			]
		]
	], ast);
	let type = newRef();
	return true
		&& console.log('ast', stringify(ast))
		&& console.log('type', stringify(type));
}() : error(`
test case failed,
actual = ${actual}
expect = ${expect}`)
