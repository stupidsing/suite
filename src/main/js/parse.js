let repeat = (init, when, iterate) => {
	let value = init;
	while (when(value)) value = iterate(value);
	return value;
};

let error = program => { throw new Error(`cannot parse ${program}`); };

let isAll = pred => list => repeat(
	{ i: 0, b: true },
	({ i, b }) => i < list.length && b,
	({ i, b }) => ({ i: i + 1, b: b && pred(list[i]) }),
).b;

let isIdentifier = isAll(ch => false
	|| '0' <= ch && ch <= '9'
	|| 'A' <= ch && ch <= 'Z'
	|| ch === '_'
	|| 'a' <= ch && ch <= 'z');

let isNumber_ = isAll(ch => '0' <= ch && ch <= '9');
let isNumber = program => program !== '' && isNumber_(program);

let appendTrailing = s => s + (s === '' || s.endsWith(',') ? '' : ',');

let splitl = (s, sep) => {
	return repeat(
		{ i: 0, quote: '', bracket: 0, isMatched: false, result: [s, ''] },
		({ i, isMatched }) => !isMatched && i + sep.length <= s.length,
		({ i, quote, bracket, isMatched, result }) => {
			let j = i + sep.length;
			let ch = s[i];

			let quote1 = quote === '' && (ch === "'" || ch === '"' || ch === '`') ? ch
				: quote === ch ? ''
				: quote;

			let bracket1 = false ? {}
				: quote === '' && (ch === '(' || ch === '[' || ch === '{') ? bracket + 1
				: quote === '' && (ch === ')' || ch === ']' || ch === '}') ? bracket - 1
				: bracket;

			return quote || bracket !== 0 || s.substring(i, j) !== sep
				? { i: i + 1, quote: quote1, bracket: bracket1, isMatched, result }
				: { i: i + 1, quote: quote1, bracket: bracket1, isMatched: true, result: [s.substring(0, i), s.substring(j)] };
		},
	).result;
};

let splitr = (s, sep) => {
	return repeat(
		{ j: s.length, quote: '', bracket: 0, isMatched: false, result: ['', s] },
		({ j, isMatched }) => !isMatched && sep.length <= j,
		({ j, quote, bracket, isMatched, result }) => {
			let i = j - sep.length;
			let ch = s[j - 1];

			let quote1 = quote === '' && (ch === "'" || ch === '"' || ch === '`') ? ch
				: quote === ch ? ''
				: quote;

			let bracket1 = false ? {}
				: quote === '' && (ch === '(' || ch === '[' || ch === '{') ? bracket + 1
				: quote === '' && (ch === ')' || ch === ']' || ch === '}') ? bracket - 1
				: bracket;

			return quote1 || bracket1 !== 0 || s.substring(i, j) !== sep
				? { j: j - 1, quote: quote1, bracket: bracket1, isMatched, result }
				: { j: j - 1, quote: quote1, bracket: bracket1, isMatched: true, result: [s.substring(0, i), s.substring(j)] };
		},
	).result;
};

let keepsplitl = (s, sep, apply) => repeat(
	{ input: s, values: [] },
	({ input }) => input !== '',
	({ input, values }) => {
		let [left, right] = splitl(input, sep);
		return { input: right, values: [apply(left), values] };
	},
).values;

let parseAssocLeft_ = (id, op, parseValue) => {
	let parse = program => {
		let [left, right] = splitr(program, op);
		let rhs = parseValue(right);
		return left === '' ? rhs : { id, lhs: parse(left), rhs };
	};
	return parse;
};

let parseAssocRight = (id, op, parseValue) => {
	let parse = program => {
		let [left, right] = splitl(program, op);
		let lhs = parseValue(left);
		return right === '' ? lhs : { id, lhs, rhs: parse(right) };
	};
	return parse;
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

let parseConstant = program => {
	return false ? {}
		: isNumber(program)
			? { id: 'number', value: program }
		: program.startsWith("'") && program.endsWith("'")
			? { id: 'string', value: program.substring(1, program.length - 1) }
		: program.startsWith('"') && program.endsWith('"')
			? { id: 'string', value: program.substring(1, program.length - 1) }
		: program.startsWith('`') && program.endsWith('`')
			? { id: 'backquote', value: program.substring(1, program.length - 1) }
		: program === 'false'
			? { id: 'false', value: 'false' }
		: program === 'true'
			? { id: 'true', value: 'true' }
		: isIdentifier(program)
			? { id: 'var', value: program }
		: error(program);
};

let parseList = (program, parse) => {
	let listStr = program.substring(1, program.length - 1).trim();
	return {
		id: 'list',
		values: keepsplitl(appendTrailing(listStr), ',', parse),
	};
};

let parseMapInner = (program, parse) => {
	return {
		id: 'map',
		kvs: keepsplitl(appendTrailing(program), ',', kv => {
			let [key_, value] = splitl(kv, ':');
			let key = parseConstant(key_.trim()).value;
			return {
				key,
				value: value !== '' ? parse(value) : { id: 'var', value: key },
			};
		}),
	};
};

let parseMap = (program, parse) => {
	return parseMapInner(program.substring(1, program.length - 1).trim(), parse);
};

let parseValue = program_ => {
	let program = program_.trim();
	return false ? {}
		: program.startsWith('(') && program.endsWith(')')
			? parse(program.substring(1, program.length - 1))
		: program.startsWith('[') && program.endsWith(']')
			? parseList(program, parse)
		: program.startsWith('{') && program.endsWith('}')
			? function() {
				let block = program.substring(1, program.length - 1).trim();
				return block.endsWith(';') ? parse(block) : parseMapInner(block, parse);
			}()
		: parseConstant(program);
};

let parseInvokeIndex = program_ => {
	let program = program_.trim();
	let [expr, field] = splitr(program, '.');

	return false ? {}
		: expr !== '' && isIdentifier(field)
			? { id: 'dot', field, expr: parseInvokeIndex(expr) }
		: program.startsWith('function() {') && program.endsWith('}()')
			? parse(program.substring(12, program.length - 3).trim())
		: !program.startsWith('(') && program.endsWith(')')
			? function() {
				let [expr, paramStr_] = splitr(program, '(');
				let paramStr = paramStr_.substring(0, paramStr_.length - 1).trim();
				let parameters = keepsplitl(appendTrailing(paramStr), ',', parse);
				return {
					id: 'invoke',
					expr: parse(expr),
					parameters,
				};
			}()
		: !program.startsWith('[') && program.endsWith(']')
			? function() {
				let [expr, index] = splitr(program, '[');
				return {
					id: 'index',
					expr: parse(expr),
					index: parse(index.substring(0, index.length - 1)),
				};
			}()
		: parseValue(program);
};

let parseDiv = parseAssocLeft_('div', '/', parseInvokeIndex);
let parseMul = parseAssocRight('mul', '*', parseDiv);
let parseSub = parseAssocLeft_('sub', '-', parseMul);

let parseNeg = program_ => {
	let program = program_.trim();
	return program.startsWith('-')
		? { id: 'neg', expr: parseSub(program.substring(1)) }
		: parseSub(program);
};

let parseAdd = program => {
	let [left, right] = splitl(program, '+');
	let lhs = parseNeg(left);
	return right === '' ? lhs : left === ''
		? { id: 'pos', expr: parseAdd(right) }
		: { id: 'add', lhs, rhs: parseAdd(right) };
};

let parseLt_ = parseAssocRight('lt_', '<', parseAdd);
let parseLe_ = parseAssocRight('le_', '<=', parseLt_);
let parseNot = parsePrefix('not', '!', parseLe_);
let parseNe_ = parseAssocRight('ne_', '!==', parseNot);
let parseEq_ = parseAssocRight('eq_', '===', parseNe_);
let parseAnd = parseAssocRight('and', '&&', parseEq_);
let parseOr_ = parseAssocRight('or_', '||', parseAnd);

let parseIfThenElse = program => {
	let [if_, thenElse] = splitl(program, '?');
	return thenElse === '' ? parseOr_(if_) : function() {
		let [then, else_] = splitl(thenElse, ':');
		return {
			id: 'if',
			'if': parse(if_),
			then: parse(then),
			'else': parse(else_),
		};
	}();
};

let parseBind = program_ => {
	let program = program_.trim();
	return false ? {}
		: program.startsWith('(') && program.endsWith(')')
			? parseBind(program.substring(1, program.length - 1))
		: program.startsWith('[') && program.endsWith(']')
			? parseList(program, parseBind)
		: program.startsWith('{') && program.endsWith('}')
			? parseMap(program, parseBind)
		: isIdentifier(program)
			? { id: 'var', value: program }
		: parseConstant(program);
};

let parseLambdaParameters = program_ => {
	let program = program_.trim();
	return false ? {}
		: program.startsWith("([") && program.endsWith("])")
			? parseBind(program)
		: program.startsWith("({") && program.endsWith("})")
			? parseBind(program)
		: program.startsWith("(") && program.endsWith(")")
			? parseList(program.substring(1, program.length - 1).trim(), parseBind)
		: parseBind(program);
};

let parseLambda = program => {
	let [left, right_] = splitl(program, '=>');
	let right = right_.trim();
	return right === '' ? parseIfThenElse(left) : {
		id: 'lambda',
		bind: parseLambdaParameters(left),
		expr: parse(right ),
	};
};

let parse = program_ => {
	let program = program_.trim();
	return false ? {}
		: program.startsWith('let ')
			? function() {
				let [varValue_, expr] = splitl(program.substring(4), ';');
				let [var_, value] = splitl(varValue_, '=');
				return {
					id: 'let',
					bind: parseBind(var_),
					value: parse(value),
					expr: parse(expr),
				};
			}()
		: program.startsWith('return ') && program.endsWith(';')
			? parse(program.substring(7, program.length - 1))
		: program.startsWith('throw ') && program.endsWith(';')
			? { id: 'error' }
		: parseLambda(program);
};

let stringify = json => JSON.stringify(json,  null, '  ');

let actual = stringify(parse(`
console.log(parse(require('fs').readFileSync(0, 'utf8')))
`));

let expect = stringify({
	"id": "invoke",
	"expr": {
		"id": "dot",
		"field": "log",
		"expr": { "id": "var", "value": "console" }
	},
	"parameters": [
		{
			"id": "invoke",
			"expr": { "id": "var", "value": "parse" },
			"parameters": [
				{
					"id": "invoke",
					"expr": {
						"id": "dot",
						"field": "readFileSync",
						"expr": {
							"id": "invoke",
							"expr": { "id": "var", "value": "require" },
							"parameters": [
								{ "id": "string", "value": "fs" },
								[]
							]
						}
					},
					"parameters": [
						{ "id": "string", "value": "utf8" },
						[
							{ "id": "number", "value": "0" },
							[]
						]
					]
				},
				[]
			]
		},
		[]
	]
});

actual === expect
? console.log(stringify(parse(require('fs').readFileSync(0, 'utf8'))))
: function() {
	throw new Error(`
	test case failed,
	actual = ${actual}
	expect = ${expect}`);
}()
