let repeat = (init, when, iterate) => {
	let value = init;
	while (when(value)) value = iterate(value);
	return value;
};

let isAll = pred => list => repeat(
	({ i: 0, b: true, }),
	({ i, b, }) => i < list.length && b,
	({ i, b, }) => ({ i: i + 1, b: b && pred(list[i]), }),
).b;

let splitl = (s, sep) => {
	return repeat(
		({ i: 0, quote: false, bracket: 0, isMatched: false, result: [s, ''], }),
		({ i, isMatched, }) => !isMatched && i + sep.length <= s.length,
		({ i, quote, bracket, isMatched, result, }) => {
			let j = i + sep.length;
			let ch = s[i];
			let quote1 = ch === "'" || ch === '"' || ch === '`' ? !quote : quote;

			let bracket1 =
				!quote && (ch === '(' || ch === '[' || ch === '{') ? bracket + 1
				: !quote && (ch === ')' || ch === ']' || ch === '}') ? bracket - 1
				: bracket;

			return quote || bracket !== 0 || s.substring(i, j) !== sep
				? ({ i: i + 1, quote: quote1, bracket: bracket1, isMatched, result, })
				: ({ i: i + 1, quote: quote1, bracket: bracket1, isMatched: true, result: [s.substring(0, i), s.substring(j),]});
		},
	).result;
};

let splitr = (s, sep) => {
	return repeat(
		({ j: s.length, quote: false, bracket: 0, isMatched: false, result: ['', s], }),
		({ j, isMatched, }) => !isMatched && sep.length <= j,
		({ j, quote, bracket, isMatched, result, }) => {
			let i = j - sep.length;
			let ch = s[j - 1];
			let quote1 = ch === "'" || ch === '"' || ch === '`' ? !quote : quote;

			let bracket1 =
				!quote && (ch === '(' || ch === '[' || ch === '{') ? bracket + 1
				: !quote && (ch === ')' || ch === ']' || ch === '}') ? bracket - 1
				: bracket;

			return quote1 || bracket1 !== 0 || s.substring(i, j) !== sep
				? ({ j: j - 1, quote: quote1, bracket: bracket1, isMatched, result, })
				: ({ j: j - 1, quote: quote1, bracket: bracket1, isMatched: true, result: [s.substring(0, i), s.substring(j),]});
		},
	).result;
};

let parseAssocLeft_ = id => op => parseValue => {
	let parse = program => {
		let [left, right,] = splitr(program, op);
		let rhs = parseValue(right);
		return left === '' ? rhs : ({ id, lhs: parse(left), rhs, });
	};
	return parse;
};

let parseAssocRight = id => op => parseValue => {
	let parse = program => {
		let [left, right,] = splitl(program, op);
		let lhs = parseValue(left);
		return right === '' ? lhs : ({ id, lhs, rhs: parse(right), });
	};
	return parse;
};

let parsePrefix = id => op => parseValue => {
	let parse = program_ => {
		let program = program_.trim();
		return !program.startsWith(op)
			? parseValue(program)
			: ({ id, expr: parse(program) });
	};
	return parse;
};

let parseConstant = program => {
	let isNumber = isAll(ch => '0' <= ch && ch <= '9')(program);

	return false ? ({})
		: isNumber
			? ({ id: 'number', value: program, })
		: program.startsWith("'") && program.endsWith("'")
			? { id: 'string', value: program.substring(1, program.length - 1), }
		: program.startsWith('"') && program.endsWith('"')
			? { id: 'string', value: program.substring(1, program.length - 1), }
		: program === 'false'
			? { id: 'false', }
		: program === 'true'
			? { id: 'true', }
		: ({ id: 'var', value: program, });
};

let parseValue = program_ => {
	let program = program_.trim();
	return false ? ({})
		: program.startsWith('({') && program.endsWith('})')
			? ({
				id: 'map',
				kvs: splitl(program, ',').map(kv => function() {
					let [key, value,] = kv.splitl(':');
					return ({ key, value: parse(value) });
				}()),
			})
		: program.startsWith('(') && program.endsWith(')')
			? parse(program.substring(1, program.length - 1))
		: program.startsWith('[') && program.endsWith(']')
			? function() {
				let listStr = program.substring(1, program.length - 1);
				return ({
					id: 'list',
					values: repeat(
						({ input: listStr, values: [], }),
						({ input, }) => input !== '',
						({ input, values, }) => {
							let [left, right,] = splitl(input, ',');
							return ({
								input: right,
								values: [parse(left), values],
							});
						},
					).values,
				});
			}()
		: program.startsWith('{') && program.endsWith('}')
			? parse(program.substring(1, program.length - 1))
		: program.startsWith('function() {') && program.endsWith('; }()')
			? parse(program.substring(12, program.length - 3))
		: program.startsWith('return ') && program.endsWith(';')
			? parse(program.substring(7, program.length - 1))
		: parseConstant(program);
};

let parseInvokeIndex = program_ => {
	let program = program_.trim();
	let [expr, field,] = splitr(program, '.');

	let isField = isAll(ch => false
		|| '0' <= ch && ch <= '9'
		|| 'A' <= ch && ch <= 'Z'
		|| ch === '_'
		|| 'a' <= ch && ch <= 'z')(field);

	return false ? ({})
		: expr !== '' && isField
			? ({ id: 'dot', field, expr: parseInvokeIndex(expr), })
		: !program.startsWith('(') && program.endsWith(')')
			? function() {
				let [expr, paramStr_,] = splitr(program, '(');
				let paramStr = paramStr_.substring(0, paramStr_.length - 1);
				return ({
					id: 'invoke',
					expr: parse(expr),
					parameters: repeat(
						({ input: paramStr, parameters: [], }),
						({ input, }) => input !== '',
						({ input, parameters, }) => {
							let [left, right,] = splitl(input, ',');
							return ({
								input: right,
								parameters: [parse(left), parameters],
							});
						},
					).parameters,
				});
			}()
		: !program.startsWith('[') && program.endsWith(']')
			? function() {
				let [expr, index,] = splitr(program, '[');
				return ({
					id: 'index',
					expr: parse(expr),
					index: parse(index.substring(0, index.length - 1)),
				});
			}()
		: parseValue(program);
};

let parseDiv = parseAssocLeft_('div')('/')(parseInvokeIndex);
let parseMul = parseAssocRight('mul')('*')(parseDiv);
let parseSub = parseAssocLeft_('sub')('-')(parseMul);

let parseNeg = program_ => {
	let program = program_.trim();
	return program.startsWith('-')
		? ({ id: 'neg', expr: parseSub(program.substring(1)), })
		: parseSub(program);
};

let parseAdd = program => {
	let [left, right,] = splitl(program, '+');
	let lhs = parseNeg(left);
	return right === '' ? lhs : left === '' ? ({ id: 'pos', expr: parseAdd(right), }) : ({ id: 'add', lhs, rhs: parseAdd(right), });
};

let parseLt_ = parseAssocRight('lt_')('<')(parseAdd);
let parseLe_ = parseAssocRight('le_')('<=')(parseLt_);
let parseNot = parsePrefix('not')('!')(parseLe_);
let parseNe_ = parseAssocRight('ne_')('!==')(parseNot);
let parseEq_ = parseAssocRight('eq_')('===')(parseNe_);
let parseAnd = parseAssocRight('and')('&&')(parseEq_);
let parseOr_ = parseAssocRight('or_')('||')(parseAnd);

let parseIfThenElse = program => {
	let [if_, thenElse] = splitl(program, '?');
	return thenElse === '' ? parseOr_(if_) : function() {
		let [then, else_] = splitl(thenElse, ':');
		return ({
			id: 'ifThenElse',
			if_: parse(if_),
			then: parse(then),
			else_: parse(else_),
		});
	}();
};

let parseBind = program_ => {
	let program = program_.trim();
	return false ? ({})
		: ({
		id: 'var',
		v: program,
	});
};

let parseLambda = program => {
	let [left, right,] = splitl(program, '=>');
	return right === '' ? parseIfThenElse(left) : ({
		id: 'lambda',
		bind: parseBind(left),
		expr: parse(right),
	});
};

let parse = program_ => {
	let program = program_.trim();
	return program.startsWith('let ')
		? function() {
			let [varValue_, expr,] = splitl(program.substring(4), ';');
			let [var_, value,] = splitl(varValue_, '=');
			return ({
				id: 'let',
				bind: parseBind(var_),
				value: parse(value),
				expr: parse(expr),
			});
		}()
		: parseLambda(program);
};

console.log(JSON.stringify(parse(require('fs').readFileSync(0, 'utf8')), null, '  '));
