let repeat = (init, when, iterate) => {
	let value = init;
	while (when(value)) value = iterate(value);
	return value;
};

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

let parseTerminal = program => {
	let isNumber = repeat(
		({ i: 0, isNumber: true, }),
		({ i, isNumber, }) => i < program.length && isNumber,
		({ i, isNumber, }) => ({
			i: i + 1,
			isNumber: isNumber && '0' <= program[i] && program[i] <= '9',
		}),
	).isNumber;

	return isNumber ? ({ id: 'number', value: program, }) : ({ id: 'var', value: program, });
};

let parseValue = program_ => {
	let program = program_.trim();
	return false ? ({})
		: program.startsWith("'") && program.endsWith("'")
			? { id: 'string', value: program.substring(1, program.length - 1), }
		: program.startsWith('"') && program.endsWith('"')
			? { id: 'string', value: program.substring(1, program.length - 1), }
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
		: program.startsWith('{') && program.endsWith('}')
			? parse(program.substring(1, program.length - 1))
		: program.startsWith('function() {') && program.endsWith('; }()')
			? parse(program.substring(12, program.length - 3))
		: program.startsWith('return ') && program.endsWith(';')
			? parse(program.substring(7, program.length - 1))
		: parseTerminal(program_);
};

let parseDot = parseAssocLeft_('dot')('.')(parseValue);

let parseInvokeIndex = program_ => {
	let program = program_.trim();
	return !program.startsWith('(') && program.endsWith(')')
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
		: parseDot(program);
};

let parseDiv = parseAssocLeft_('div')('/')(parseInvokeIndex);
let parseMul = parseAssocRight('mul')('*')(parseDiv);
let parseSub = parseAssocLeft_('sub')('-')(parseMul);

let parseNeg = program_ => {
	let program = program_.trim();
	return program.startsWith('-')
		? ({ id: 'neg', expr: parseSub(program.substring(1)), })
		: parseSub(program_);
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
	return ({
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
