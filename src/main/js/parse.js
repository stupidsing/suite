let error = message => { throw new Error(message); };

let ascii = s => s.charCodeAt(0);

let contains = (list, e) => {
	let f;
	f = es => 0 < es.length && (es[0] === e || f(es[1], e));
	return f(list);
};

let repeat = (init, when, iterate) => {
	let f;
	f = value => when(value) ? f(iterate(value)) : value;
	return f(init);
};

let fold = (init, list, op) => {
	let f;
	f = (init, list) => 0 < list.length ? f(op(init, list[0]), list[1]) : init;
	return f(init, list);
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
		}() : [s, null];
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
		}() : [null, s];
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
		return left === null ? rhs : { id, lhs: f(left), rhs };
	};
	return f;
};

let parseAssocRight = (id, op, parseValue) => {
	let f;
	f = program_ => {
		let program = program_.trim();
		let [left, right] = splitl(program, op);
		let lhs = parseValue(left);
		return right === null ? lhs : { id, lhs, rhs: f(right) };
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
		: program === 'null'
			? { id: 'empty' }
		: program === 'true'
			? { id: 'boolean', value: 'true' }
		: program === 'undefined'
			? { id: 'empty' }
		: isIdentifier(program)
			? { id: 'var', value: program }
		: error(`cannot parse "${program}"`);
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
		let value = value_ !== null ? parse(value_) : { id: 'var', value: key };
		return { key, value };
	}),
});

let parseStruct = (program, parse) => parseStructInner(program.substring(1, program.length - 1).trim(), parse);

let parseProgram;

let parseValue = program_ => {
	let program = program_.trim();

	return false ? {}
		: program.startsWith('(') && program.endsWith(')')
			? parseProgram(program.substring(1, program.length - 1))
		: program.startsWith('[') && program.endsWith(']')
			? parseList(program, parseProgram)
		: program.startsWith('{') && program.endsWith('}')
			? function() {
				let block = program.substring(1, program.length - 1).trim();
				return block.endsWith(';') ? parseProgram(block) : parseStructInner(block, parseProgram);
			}()
		: parseConstant(program);
};

let parseApplyBlockFieldIndex = program_ => {
	let program = program_.trim();
	let [expr, field] = splitr(program, '.');

	return false ? {}
		: expr !== null && isIdentifier(field)
			? { id: 'dot', field, expr: parseApplyBlockFieldIndex(expr) }
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
				return expr !== null ? {
					id: 'apply',
					expr: parseProgram(expr),
					parameter: parseProgram(paramStr),
				} : parseValue(program);
			}()
		: program.endsWith(']')
			? function() {
				let [expr, index] = splitr(program, '[');
				return expr !== null ? {
					id: 'index',
					expr: parseProgram(expr),
					index: parseProgram(index.substring(0, index.length - 1)),
				} : parseValue(program);
			}()
		: parseValue(program);
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

	return thenElse === null ? parseApp(if_) : function() {
		let [then, else_] = splitl(thenElse, ':');

		return {
			id: 'if',
			'if': parseProgram(if_),
			then: parseProgram(then),
			'else': parseProgram(else_),
		};
	}();
};

let parseBindPair = program => {
	let [left, right] = splitl(program, ',');
	let lhs = parseConstant(left.trim());

	return right === null ? lhs : { id: 'pair', lhs, rhs: parseBindPair(right) };
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
			: parseBindPair(program);
	};
	return f(program);
};

let parseLambda = program => {
	let [left, right] = splitl(program, '=>');

	return right === null ? parseIf(left) : {
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

				return value !== null
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
					: error(`cannot parse let variable "${v}"`);
			}()
		: statement.startsWith('return ') && expr === ''
			? parseProgram(statement.substring(7))
		: statement.startsWith('throw ') && expr === ''
			? { id: 'error' }
		: expr !== null
			? function() {
				let [var_, value] = splitl(statement, '=');
				let v = var_.trim();

				return isIdentifier(v) ? {
					id: 'assign',
					v,
					value: parseProgram(value),
					expr: parseProgram(expr),
				} : error(`cannot parse assign variable "${v}"`);
			}()
		: parsePair(statement);
};

let mergeBindVariables;

mergeBindVariables = (vs, ast) => {
	return false ? {}
		: ast.id === 'list' ? fold(vs, ast.values, mergeBindVariables)
		: ast.id === 'pair' ? mergeBindVariables(mergeBindVariables(vs, ast.lhs), ast.rhs)
		: ast.id === 'struct' ? fold(vs, ast.kvs, (vs_, kv) => mergeBindVariables(vs_, kv.value))
		: ast.id === 'var' ? [ast.value, vs]
		: vs;
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

let rewrite;

rewrite = f => ast0 => {
	return ast0.id === null ? ast0 : function() {
		let ast1 = f(ast0.id)(ast0);
		return ast1 === null
			? Object.fromEntries(Object.entries(ast0).map(([k, v]) => [k, rewrite(v)]))
			: ast1;
	}();
};

let stringify = json => JSON.stringify(json, null, '  ');

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
	return checkVariables([
		'JSON', [
			'Object', [
				'console', [
					'require', []
				]
			]
		]
	], ast) && console.log(stringify(ast));
}() : error(`
test case failed,
actual = ${actual}
expect = ${expect}`)
