let any = v => Object.assign(v);

let ascii = s => s.charCodeAt(0);
let cons = (head, tail) => [head, ...[tail],];
let error = message => { throw new Error(message); };
let head = list =>  list[0];
let set = (m, k, v) => { any(m)[any(k)] = any(v); return true; };
let tail = list =>  list[1];
let nil = [];

let stringify = json => JSON.stringify(json, undefined, '  ');

let contains = (list, e) => {
	let contains_;
	contains_ = es => es !== nil && (head(es) === e || contains_(tail(es)));
	return contains_(list);
};

let fold = (init, list, op) => {
	let fold_;
	fold_ = (init, list) => list !== nil ? fold_(op(init, head(list)), tail(list)) : init;
	return fold_(init, list);
};

let dump = v => {
	let dump_;
	dump_ = (vs, v) => false ? ''
		: contains(vs, v)
			? '<recurse>'
		: v.id !== undefined
			? function() {
				let join = Object
					.entries(v)
					.filter(([k, v_]) => k !== 'id')
					.map(([k, v_]) => `${k}:${dump_([v, vs,], v_)}`)
					.join(' ');
				return `${v.id}(${join})`;
			}()
		:
			v.toString();
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

let quoteBracket = (quote, bracket, ch) => ({
	quote: quote === 0 && (ch === ascii("'") || ch === ascii('"') || ch === ascii('`')) ? ch
		: quote === ch ? 0
		: quote,
	bracket: false ? 0
		: quote === 0 && (ch === ascii('(') || ch === ascii('[') || ch === ascii('{')) ? bracket + 1
		: quote === 0 && (ch === ascii(')') || ch === ascii(']') || ch === ascii('}')) ? bracket - 1
		: bracket,
});

let appendTrailingComma = s => s + (s === '' || s.endsWith(',') ? '' : ',');

let splitl = (s, sep) => {
	let splitl_;
	splitl_ = (i, quote, bracket) => {
		let j = i + sep.length;
		return j <= s.length ? function() {
			let ch = s.charCodeAt(i);
			let { quote: quote1, bracket: bracket1 } = quoteBracket(quote, bracket, ch);

			return quote !== 0 || bracket !== 0 || s.slice(i, j) !== sep || i === 0
				? splitl_(i + 1, quote1, bracket1)
				: [s.slice(0, i), s.slice(j, undefined)];
		}() : [s, undefined];
	};

	return splitl_(0, 0, 0);
};

let splitr = (s, sep) => {
	let splitr_;
	splitr_ = (j, quote, bracket) => {
		let i = j - sep.length;
		return 0 <= i ? function() {
			let ch = s.charCodeAt(j - 1);
			let { quote: quote1, bracket: bracket1 } = quoteBracket(quote, bracket, ch);

			return quote1 !== 0 || bracket1 !== 0 || s.slice(i, j) !== sep || i === 0
				? splitr_(j - 1, quote1, bracket1)
				: [s.slice(0, i), s.slice(j, undefined)];
		}() : [undefined, s];
	};
	return splitr_(s.length, 0, 0);
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

let parseArray = (program, parse) => {
	let parseArray_;
	parseArray_ = program_ => {
		let program = program_.trim();

		return program !== ''
			? function() {
				let [head, tail_] = splitl(program, ',');
				let tail = tail_.trim();
				return {
					id: 'cons',
					head: parse(head),
					tail: tail.startsWith('...[') && tail.endsWith('],') ? parse(tail.slice(4, tail.length - 2)) : parseArray_(tail)
				};
			}()
		:
			{ id: 'nil' };
	};
	return parseArray_(program);
};

let parseTuple = (program, parse) => ({
	id: 'tuple',
	values: keepsplitl(program + ',', ',', parse),
});

let parseArrayTuple = (program_, parse) => {
	let program = program_.slice(1, program_.length - 1).trim();
	return (program === '' || program.endsWith(',') ? parseArray : parseTuple)(program, parse);
};

let parseStructInner = (program, parse) => ({
	id: 'struct',
	kvs: keepsplitl(appendTrailingComma(program), ',', kv => {
		let [key_, value_] = splitl(kv, ':');
		let field = parseConstant(key_.trim()).value;
		let value = value_ !== undefined ? parse(value_) : { id: 'var', value: field };
		return { key: '.' + field, value };
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
			? parseArrayTuple(program, parseProgram)
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
			? { id: 'dot', field: '.' + field, expr: parseApplyBlockFieldIndex(expr) }
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
				parameter: { id: 'never' },
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

let parsePair;

parsePair = (program, parse) => {
	let parsePair_;
	parsePair_ = program => {
		let [left, right] = splitl(program, ',');
		let lhs = parse(left.trim());
		return right === undefined ? lhs : { id: 'tuple', values: cons(lhs, cons(parsePair_(right), nil)) };
	};
	return parsePair_(program);
};

let parseBind;

parseBind = program_ => {
	let program = program_.trim();

	return false ? {}
		: program === '()'
			? { id: 'never' }
		: program.startsWith('(') && program.endsWith(')')
			? parseBind(program.slice(1, program.length - 1))
		: program.startsWith('[') && program.endsWith(']')
			? parseArrayTuple(program, parseBind)
		: program.startsWith('{') && program.endsWith('}')
			? parseStruct(program, parseBind)
		:
			parsePair(program, parseConstant);
};

let parseLambda = program => {
	let [left, right] = splitl(program, '=>');

	return right === undefined ? parseIf(left) : {
		id: 'lambda',
		bind: parseBind(left),
		expr: parseProgram(right.trim()),
	};
};

parseProgram = program => {
	let [statement_, expr_] = splitl(program, ';');

	return expr_ === undefined ? parsePair(statement_, parseLambda) : function() {
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
						var_: parseLvalue(var_),
						value: parseProgram(value),
						expr: parseProgram(expr),
					};
				}();
	}();
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
	return ref;
};

let dumpRef = v => {
	let dumpRef_;
	dumpRef_ = (vs, v) => false ? ''
		: contains(vs, v)
			? '<recurse>'
		: v.ref !== undefined
			? (refs.get(v.ref) !== v ? dumpRef_(cons(v, vs), refs.get(v.ref)) : `_${v.ref}`)
		: typeof v === 'object'
			? (false ? ''
				: any(v).length === 0
					? ''
				: any(v).length === 2
					? `${dumpRef_(vs, head(v))}:${dumpRef_(vs, tail(v))}`
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
		: typeof v === 'string'
			? v.toString()
		:
			JSON.stringify(v, undefined, undefined);
	return dumpRef_(nil, v);
};

let tryBind;

tryBind = (a, b) => function() {
	let a_ = any(a);
	let b_ = any(b);
	let refa = a.ref;
	let refb = b.ref;
	return false ? false
		: a === b
			? true
		: refa !== undefined
			? function() {
				let olda = refs.get(refa);
				return setRef(refa, b) && tryBind(olda, b) || !setRef(refa, olda);
			}()
		: refb !== undefined
			? function() {
				let oldb = refs.get(refb);
				return setRef(refb, a) && tryBind(a, oldb) || !setRef(refb, oldb);
			}()
		: typeof a === 'object' && typeof b === 'object'
			&& (a_.length !== undefined && a_.length === b_.length
			? function() {
				let tryBindList;
				tryBindList = index => index === a_.length || tryBind(a_[index], b_[index]) && tryBindList(index + 1);
				return tryBindList(0);
			}()
			: true
				&& Object.keys(a).reduce((r, k) => {
					let dummy = b.completed !== true && b_[k] !== undefined || function() { b_[k] = newRef(); return b_[k]; }();
					return r && tryBind(a_[k], b_[k]);
				}, true)
				&& Object.keys(b).reduce((r, k) => {
					let dummy = a.completed !== true && a_[k] !== undefined || function() { a_[k] = newRef(); return a_[k]; }();
					return r && tryBind(a_[k], b_[k]);
				}, true)
			);
}();

let doBind_ = (msg, a, b) => tryBind(a, b) || error(`in ${msg}:\ncannot bind types between\nfr: ${dumpRef(a)}\nto: ${dumpRef(b)}`);
let doBind = (ast, a, b) => doBind_(dump(ast), a, b);

let cloneRef = v => {
	let fromTos = new Map();
	let cloneRef_;

	cloneRef_ = v => {
		let { ref } = v;
		let vlist = any(v);
		return false ? {}
			: ref !== undefined
				? (fromTos.has(ref) ? fromTos.get(ref) : function() {
					let v1 = newRef();
					let dummy = fromTos.set(ref, v1);
					return doBind_('clone reference', v1, cloneRef_(refs.get(ref))) && v1;
				}())
			: typeof v === 'object'
				? (vlist.length !== undefined
					? any(vlist.map(cloneRef_))
					: Object.fromEntries(Object.entries(v).map(([k, v_]) => [k, cloneRef_(v_)]))
				)
			:
				v;
	};

	return cloneRef_(v);
};

let lookup = (vts, v) => {
	let lookup_;
	lookup_ = vts => vts !== nil ? function() {
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
	: ast.id === 'struct' ? fold(vts, ast.kvs, (vts_, kv) => defineBindTypes(vts_, kv.value))
	: ast.id === 'tuple' ? fold(vts, ast.values, defineBindTypes)
	: ast.id === 'var' ? cons([ast.value, newRef()], vts)
	: error(`cannot destructure ${dump(ast)}`);

let typeArrayOf = type => ({ id: 'array', of: type });
let typeBoolean = ({ id: 'boolean' });
let typeLambdaOf = (in_, out) => ({ id: 'lambda', in_, out });
let typeMap = newRef();
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
					&& (tryBind(t, typeNumber) || tryBind(t, typeString) || error(`cannot add values with type ${dumpRef(t)}`))
					&& t;
			})
		: id === 'alloc'
			? (({ v, expr }) => inferType(cons([v, newRef()], vts), expr))
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
			? (({ var_, value, expr }) => {
				return function() {
					try {
						let tvar = inferType(vts, var_);
						let tvalue = inferType(vts, value);
						return doBind({ id: 'assign', var_, value }, tvar, tvalue);
					} catch (e) {
						e.message = `in assignment clause of ${dump(var_)}\n${e.message}`;
						throw e;
					}
				}() && inferType(vts, expr);
			})
		: id === 'backquote'
			? (({}) => typeString)
		: id === 'boolean'
			? (({}) => typeBoolean)
		: id === 'cons'
			? (({ head, tail }) => {
				let tl = typeArrayOf(inferType(vts, head));
				return doBind(ast, inferType(vts, tail), tl) && tl;
			})
		: id === 'div'
			? inferMathOp
		: id === 'dot'
			? (({ field, expr }) => false ? {}
				: field === '.charCodeAt'
					? doBind(ast, inferType(vts, expr), typeString) && typeLambdaOf(typeNumber, typeNumber)
				: field === '.endsWith'
					? doBind(ast, inferType(vts, expr), typeString) && typeLambdaOf(typeString, typeBoolean)
				: field === '.filter'
					? function() {
						let ti = newRef();
						return doBind(ast, inferType(vts, expr), typeArrayOf(ti)) && typeLambdaOf(typeLambdaOf(ti, typeBoolean), typeArrayOf(ti));
					}()
				: field === '.join'
					? doBind(ast, inferType(vts, expr), typeArrayOf(typeString)) && typeLambdaOf(typeString, typeString)
				: field === '.length'
					? doBind(ast, inferType(vts, expr), typeArrayOf(newRef())) && typeNumber
				: field === '.map'
					? function() {
						let ti = newRef();
						let to = newRef();
						return doBind(ast, inferType(vts, expr), typeArrayOf(ti)) && typeLambdaOf(typeLambdaOf(ti, to), typeArrayOf(to));
					}()
				: field === '.reduce'
					? function() {
						let te = newRef();
						let tr = newRef();
						let treducer = typeLambdaOf(typeTupleOf(cons(tr, cons(te, nil))), tr);
						return doBind(ast, inferType(vts, expr), typeArrayOf(te))
							&& typeLambdaOf(typeTupleOf(cons(treducer, cons(tr, nil))), tr);
					}()
				: field === '.slice'
					? doBind(ast, inferType(vts, expr), typeArrayOf(newRef())) && typeLambdaOf(typeTupleOf(cons(typeNumber, cons(typeNumber, nil))), typeString)
				: field === '.startsWith'
					? doBind(ast, inferType(vts, expr), typeString) && typeLambdaOf(typeString, typeBoolean)
				: field === '.toString'
					? doBind(ast, inferType(vts, expr), newRef()) && typeLambdaOf(typeNever, typeString)
				: field === '.trim'
					? doBind(ast, inferType(vts, expr), typeString) && typeLambdaOf(typeNever, typeString)
				:
					function() {
						let tr = newRef();
						let kvs = {};
						let dummy = set(kvs, field, tr);
						let to = typeStructOf(kvs);
						return doBind(ast, inferType(vts, expr), to) && tr;
					}()
			)
		: id === 'element'
			? (({ index, expr }) => {
				let te = newRef();
				let tl = typeArrayOf(te);
				return doBind(ast, inferType(vts, expr), tl) && (index === '0' ? te : index === '1' ? tl : {});
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
		: id === 'never'
			? (({}) => typeNever)
		: id === 'new-error'
			? (({}) => typeLambdaOf(typeString, { id: 'error' }))
		: id === 'new-map'
			? (({}) => typeLambdaOf(typeNever, typeMap))
		: id === 'nil'
			? (({}) => typeArrayOf(newRef()))
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
				let inferKvs;
				inferKvs = kvs => 0 < kvs.length ? function() {
					let { key, value } = head(kvs);
					let type = inferKvs(tail(kvs));
					let dummy = set(type, key, function() {
						try {
							return inferType(vts, value);
						} catch (e) {
							e.message = `in field ${key}\n${e.message}`;
							throw e;
						}
					}());
					return type;
				}() : {};
				return typeStructOf(inferKvs(kvs));
			})
		: id === 'sub'
			? inferMathOp
		: id === 'throw'
			? (({}) => newRef())
		: id === 'try'
			? (({ expr, catch_ }) => doBind(ast, inferType(vts, catch_), newRef()) && inferType(vts, expr))
		: id === 'tuple'
			? (({ values }) => {
				let inferValues;
				inferValues = vs => vs !== nil ? [inferType(vts, head(vs)), ...[inferValues(tail(vs))],] : nil;
				return typeTupleOf(inferValues(values));
			})
		: id === 'typeof'
			? (({}) => typeString)
		: id === 'undefined'
			? (({}) => newRef())
		: id === 'var'
			? (({ value }) => {
				let t = finalRef(lookup(vts, value));
				return t.id !== 'lambda' ? t : cloneRef(t);
			})
		:
			(({}) => error(`cannot infer type for ${id}`));

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

let typeConsole = typeStructOf({
	'.error': typeLambdaOf(newRef(), typeNever),
	'.log': typeLambdaOf(newRef(), typeNever),
});

let typeJSON = typeStructOf({
});

let typeObject = typeStructOf({
	'.assign': typeLambdaOf(newRef(), newRef()),
	'.entries': typeLambdaOf(newRef(), typeArrayOf(newRef())),
	'.fromEntries': typeLambdaOf(typeArrayOf(newRef()), newRef()),
});

let typeRequire = typeLambdaOf(typeString, newRef());

let process = program => {
	let ast = parseProgram(program);

	let type = inferType(
		[
			['JSON', typeJSON],
			['Object', typeObject],
			['console', typeConsole],
			['require', typeRequire],
		].reduce((l, vt) => cons(vt, l), nil),
		ast
	);

	return { ast, type };
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
		parameter: {
			id: 'apply',
			expr: { id: 'var', value: 'parse' },
			parameter: {
				id: 'apply',
				expr: {
					id: 'dot',
					field: '.readFileSync',
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
		let { ast, type } = process(require('fs').readFileSync(0, 'utf8'));
		let dummy1 = console.log(`ast :: ${stringify(ast)}`);
		let dummy2 = console.log(`type :: ${dumpRef(type)}`);
		return true;
	} catch (e) {
		return console.error(e);
	}
}() : error(`
test case failed,
actual = ${actual}
expect = ${expect}`);
