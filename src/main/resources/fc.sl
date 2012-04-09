-------------------------------------------------------------------------------
-- functional program compiler
--
-- Also need to import one of the following backends:
-- fc-eager-evaluation.sl, fc-lazy-evaluation.sl
--
-- .mode can be EAGER or LAZY

() :- import.file 'fc-type-inference.sl' #
() :- file-read 'precompiled.rpn' .rpn
	, rpn .node .rpn
	, import .node
--	, enable-trace
#

compile-function .mode .do .c0
	:- compile-function-using-libs .mode (STANDARD,) .do .c0
#

compile-function0 .mode .do .c0
	:- fc-add-standard-funs .do .do1
	, compile-function-using-libs .mode () .do1 .c0
#

compile-function-using-libs .mode .libs .do .c0
	:- .c0 = (_ ENTER, .c1)
	, !, fc-parse .do .parsed
	, !, infer-type-rule-using-libs .libs .parsed ()/()/() .tr0/.trx _
	, !, resolve-types .tr0/.trx
	, !, fc-compile-using-libs .mode .libs .parsed 0/() .c1/.c2/.d0/()/.reg
	, .c2 = (_ EXIT .reg, _ LEAVE, .d0)
	, !, fc-assign-line-number 0 .c0
#

infer-type-rule-using-libs () .do .vto .tr .type
	:- infer-type-rule .do .vto .tr .type
#

fc-compile-using-libs .mode () .do .fve .cdr
	:- !, fc-compile .mode .do .fve .cdr
#

--
-- Syntactic sugars
--
fc-parse (if-match (.h, .t) then .then else .else) .parsed
	:- !, temp .list
	, fc-parse (
		.list => if (is-tree {.list}) then (
			define .h = _head {.list} >>
			define .t = _tail {.list} >>
			.then
		)
		else .else
	) .parsed
#
fc-parse (case || .if .then || .cases) (IF .if1 .then1 .else)
	:- !, fc-parse .if .if1
	, fc-parse .then .then1
	, fc-parse (case || .cases) .else
#
fc-parse (case || .do) .parsed :- !, fc-parse .do .parsed #
fc-parse (.l && .r) .parsed :- !, fc-parse (and {.l} {.r}) .parsed #
fc-parse (.l || .r) .parsed :- !, fc-parse (or {.l} {.r}) .parsed #
fc-parse .t .parsed
	:- tree .t () .op .right, fc-operator .op, !
	, temp .var, tree .t1 .var .op .right, fc-parse (.var => .t1) .parsed
#
fc-parse .t .parsed
	:- tree .t .left .op (), fc-operator .op, !
	, temp .var, tree .t1 .left .op .var, fc-parse (.var => .t1) .parsed
#
fc-parse .l/.r .parsed :- !, fc-parse (corecursive-cons {.l} {.r}) .parsed #
fc-parse (.l, .r) .parsed :- !, fc-parse (_cons {.l} {.r}) .parsed #
fc-parse (.l . .r) .parsed :- !, temp .v, fc-parse (.v => .l {.r {.v}}) .parsed #
fc-parse (.l | .r) .parsed :- !, fc-parse (.l {.r}) .parsed #
fc-parse (.l << .r) .parsed :- !, fc-parse (.r {.l}) .parsed #
fc-parse ($ => .do) .parsed :- !, temp .v, fc-parse (.v => .do) .parsed #
--
-- Function constructs
--
fc-parse (.var as .type => .do) (FUN .var .do1)
	:- !, fc-parse-type .type .type1, .do1 = OPTION (AS .var .type1) .do2
	, fc-parse .do1 .do2
#
fc-parse (.var => .do) (FUN .var .do1) :- !, fc-parse .do .do1 #
fc-parse (define type .type >> .do) (DEF-TYPE .type _ .do1) -- Type variable
	:- !, fc-parse .do .do1
#
fc-parse (define type .type = .def >> .do) (DEF-TYPE .type .def1 .do1)
	:- !, fc-parse-type .def .def1
	, fc-parse .do .do1
#
fc-parse (.value as .type) (OPTION (CAST .type1) .value1)
	:- !, fc-parse-type .type .type1
	, fc-parse .value .value1
#
fc-parse (no-type-check .do) (OPTION NO-TYPE-CHECK .do1)
	:- !, fc-parse .do .do1
#
fc-parse (define .var as .type = .value >> .do) (DEF-VAR .var .value2 .do1)
	:- !
	, fc-parse-type .type .type1
	, fc-parse .value .value1
	, fc-parse .do .do1
	, .value2 = OPTION (CAST .type1) .value1
#
fc-parse (define .var = .value >> .do) (DEF-VAR .var .value1 .do1)
	:- !, fc-parse .value .value1
	, fc-parse .do .do1
#
fc-parse (.callee {.parameter}) (INVOKE .parameter1 .callee1)
	:- !, fc-parse .callee .callee1
	, fc-parse .parameter .parameter1
#
fc-parse (if .if then .then .otherwise) (IF .if1 .then1 .else1)
	:- !
	, fc-parse .if .if1
	, fc-parse .then .then1
	, (.otherwise = else .else, !, fc-parse .else .else1
		; .otherwise = else-if .elseif, fc-parse (if .elseif) .else1
	)
#
fc-parse .an0:.an1 (TUPLE $$ANON .elems1)
	:- fc-parse-anon-tuple .an0:.an1 .elems, fc-parse-list .elems .elems1
#
fc-parse (.name .elems) (TUPLE .name .elems2)
	:- !, enlist .elems .elems1, fc-parse-list .elems1 .elems2
#
fc-parse .tree (TREE .oper .left1 .right1)
	:- tree .tree .left .oper .right
	, fc-operator .oper
	, !, fc-parse .left .left1
	, fc-parse .right .right1
#
fc-parse .b (BOOLEAN .b) :- fc-is-boolean .b, ! #
fc-parse .i (NUMBER .i) :- is.int .i, ! #
--fc-parse .s (STRING .s) :- is.string .s, ! #
fc-parse "" .n :- !, fc-parse () .n #
fc-parse .s .n
	:- is.string .s
	, !, nth .s 0 1 .c, nth .s 1 0 .cs
	, to.int .c .ascii, fc-parse (.ascii, .cs) .n
#
fc-parse .t (TUPLE .t ()) :- fc-is-tuple-name .t, ! #
fc-parse .v (VARIABLE .v) :- is.atom .v, ! #
fc-parse .d _ :- fc-error "Unknown expression" .d #

fc-parse-list () () :- ! #
fc-parse-list (.e, .es) (.p, .ps) :- !, fc-parse .e .p, fc-parse-list .es .ps #

fc-parse-type (.typeVar => .type) .type2
	:- !, fc-parse-type .typeVar .typeVar1
	, fc-parse-type .type .type1
	, replace .type1/.type2 .typeVar1/_
#
fc-parse-type (.returnType {.paramType}) (FUN .paramType1 .returnType1)
	:- !, fc-parse-type .paramType .paramType1
	, fc-parse-type .returnType .returnType1
#
fc-parse-type (one of .types) (ONE-OF .types1)
	:- !, fc-parse-types .types .types1
#
fc-parse-type (list of .type) (LIST-OF .type1) :- !, fc-parse-type .type .type1 #
fc-parse-type (.name .types) (TUPLE-OF .name .types2)
	:- !, enlist .types .types1
	, fc-parse-types .types1 .types2
#
fc-parse-type boolean BOOLEAN :- ! #
fc-parse-type number NUMBER :- ! #
fc-parse-type string STRING :- ! #
fc-parse-type :.typeVar (TYPE-VAR .typeVar) :- ! #
fc-parse-type .t (TUPLE-OF .t ()) :- fc-is-tuple-name .t, ! #
fc-parse-type .t (TYPE .t) :- is.atom .t #

fc-parse-types () () :- ! #
fc-parse-types (.type, .types) (.type1, .types1)
	:- fc-parse-type .type .type1, fc-parse-types .types .types1
#

fc-parse-anon-tuple () () :- ! #
fc-parse-anon-tuple .h:.t0 (.h, .t1) :- fc-parse-anon-tuple .t0 .t1 #

-- "cons" and "corecursive-cons" differ only by type
fc-define-default-fun 2 _cons CONS #
fc-define-default-fun 1 _head HEAD #
fc-define-default-fun 1 _tail TAIL #
fc-define-default-fun 2 compare COMPARE #
fc-define-default-fun 2 corecursive-cons CONS #
fc-define-default-fun 1 fflush FFLUSH #
fc-define-default-fun 1 fgetc FGETC #
fc-define-default-fun 3 fputc FPUTC #
fc-define-default-fun 1 is-tree IS-TREE #
fc-define-default-fun 1 log LOG #
fc-define-default-fun 2 log2 LOG2 #

fc-is-tuple-name () :- ! # -- Empty atom is list terminator
fc-is-tuple-name .t
	:- is.atom .t, to.string .t .s, nth .s 0 1 .c
	, .c >= "A", .c <= "Z"
#

fc-operator .oper
	:- member (' + ', ' - ', ' * ', ' / ', ' %% ',
		' = ', ' != ',
		' > ', ' < ', ' >= ', ' <= ',
	) .oper
#

fc-is-boolean true #
fc-is-boolean false #

fc-assign-line-number _ () #
fc-assign-line-number .n (.n _, .remains)
	:- let .n1 (.n + 1), fc-assign-line-number .n1 .remains
#

fc-error .m :- !, write .m, nl, fail #

fc-add-standard-funs .p (
	define and = (x => y =>
		if x then y else false
	) >>
	define cons = (head => tail =>
		_cons {head} {tail}
	) >>
	define flip = (f => x => y =>
		f {y} {x}
	) >>
	define fold-left = (fun => init => if-match (h, t)
		then (fold-left {fun} {fun {init} {h}} {t})
		else init
	) >>
	define fold-right = (fun => init => if-match (h, t)
		then (fun {h} {fold-right {fun} {init} {t}})
		else init
	) >>
	define head = (list =>
		_head {list}
	) >>
	define id = (v =>
		v
	) >>
	define join = (f => g => x =>
		g {f {x}}
	) >>
	define not = (x =>
		if x then false else true
	) >>
	define or = (x => y =>
		if x then true else y
	) >>
	define repeat = (n => elem =>
		if (n > 0) then (elem, repeat {n - 1} {elem}) else ()
	) >>
	define scan-left = (fun => init => if-match (h, t)
		then (init, scan-left {fun} {fun {init} {h}} {t})
		else (init,)
	) >>
	define scan-right = (fun => init => if-match (h, t)
		then (
			define r = scan-right {fun} {init} {t} >>
			fun {h} {head {r}}, r
		)
		else (init,)
	) >>
	define tail = (list =>
		_tail {list}
	) >>
	define tails = if-match (h, t)
		then ((h, t), tails {t})
		else ()
	>>
	define take-while = (fun => if-match (elem, elems)
		then (if (fun {elem}) then (elem, take-while {fun} {elems}) else ())
		else ()
	) >>
	define zip = (fun =>
		if-match (h0, t0)
		then (
			if-match (h1, t1)
			then (fun {h0} {h1}, zip {fun} {t0} {t1})
			else ()
		)
		else ($ => ())
	) >>
	define apply =
		fold-left {x => f => f {x}}
	>>
	define concat2 = (if-match (h, t)
		then (cons {h} . concat2 {t})
		else id
	) >>
	define equals as (:t => boolean {:t} {:t}) = no-type-check (a => b =>
		if (is-tree {a} && is-tree {b}) then (
			equals {head {a}} {head {b}} && equals {tail {a}} {tail {b}}
		)
		else (compare {a} {b} = 0)
	) >>
	define filter = (fun =>
		fold-right {
			item => list => if (fun {item}) then (item, list) else list
		} {}
	) >>
	define fold = (fun => list =>
		define h = head {list} >>
		define t = tail {list} >>
		if (is-tree {t}) then (fun {h} {fold {fun} {t}}) else h
	) >>
	define length = (
		fold-left {v => e => v + 1} {0}
	) >>
	define map = (fun =>
		fold-right {i => list => fun {i}, list} {}
	) >>
	define reverse =
		fold-left {a => b => b, a} {}
	>>
	define take = (n => list =>
		if (n > 0 && is-tree {list}) then (
			head {list}, take {n - 1} {tail {list}}
		)
		else ()
	) >>
	define concat =
		fold-left {concat2} {}
	>>
	define contains = (e =>
		fold {or} . map {e1 => e1 = e}
	) >>
	define cross = (fun => l1 => l2 =>
		map {e1 => map {e2 => fun {e1} {e2}} {l2}} {l1}
	) >>
	define get = (n =>
		head . flip {apply} {repeat {n} {tail}}
	) >>
	define quick-sort = (cmp => if-match (pivot, t)
		then (
			define cmp0 = (not . cmp {pivot}) >>
			define cmp1 = cmp {pivot} >>
			define l0 = quick-sort {cmp} {filter {cmp0} {t}} >>
			define l1 = quick-sort {cmp} {filter {cmp1} {t}} >>
			concat {l0, (pivot,), l1,}
		)
		else ()
	) >>
	define split = (separator =>
			map {take-while {`!= separator`} . tail}
			. filter {`= separator` . head}
			. tails
			. cons {separator}
	) >>
	.p
) #
