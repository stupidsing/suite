-------------------------------------------------------------------------------
-- functional program compiler
--
-- Also need to import one of the following backends:
-- fc-eager-evaluation.sl, fc-lazy-evaluation.sl

() :- import 'fc-type-inference.sl' #

compile-function .do .c0
	:- .c0 = (_ ENTER, .c1)
	, fc-add-standard-funs .do .do1
	, !, fc-parse .do1 .parsed
	, !, infer-type .parsed ()/()/() _
	, !, fc-compile .parsed 0 .c1/.c2/.d0/()/.reg
	, .c2 = (_ EXIT .reg, .d0)
	, !, fc-assign-line-number 0 .c0
#

--
-- Syntax sugars
--
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
fc-parse (.l, .r) .parsed :- !, fc-parse (cons {.l} {.r}) .parsed #
fc-parse (.l . .r) .parsed :- !, fc-parse (join {.r} {.l}) .parsed #
fc-parse (.l << .r) .parsed :- !, fc-parse (l. {.r}) .parsed #
--
-- Function constructs
--
fc-parse (.var as .type => .do) (FUN .var .do1)
	:- !, fc-parse-type .type .type1, .do1 = AS .var .type1 .do2
	, fc-parse .do1 .do2
#
fc-parse (.var => .do) (FUN .var .do1) :- !, fc-parse .do .do1 #
fc-parse (define type .type = .def >> .do) (DEF-TYPE .type .def1 .do1)
	:- !, fc-parse-type .def .def1, fc-parse .do .do1
#
fc-parse (.value as .type) (CAST .type1 .value1)
	:- !, fc-parse-type .type .type1
	, fc-parse .value .value1
#
fc-parse (define .var as .type = .value >> .do) (DEF-VAR .var .value2 .do1)
	:- !
	, fc-parse-type .type .type1
	, fc-parse .value .value1
	, fc-parse .do .do1
	, .value2 = CAST .type1 .value1
#
fc-parse (define .var = .value >> .do) (DEF-VAR .var .value1 .do1)
	:- !
	, fc-parse .value .value1, fc-parse .do .do1
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
fc-parse .s (STRING .s) :- is.string .s, ! #
fc-parse .t (TUPLE .t ()) :- fc-is-tuple-name .t, ! #
fc-parse .v (VARIABLE .v) :- is.atom .v, ! #
fc-parse .d _ _ :- fc-error "Unknown expression" .d #

fc-parse-list () () :- ! #
fc-parse-list (.e, .es) (.p, .ps) :- !, fc-parse .e .p, fc-parse-list .es .ps #

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
fc-parse-type .t (TUPLE-OF .t ()) :- fc-is-tuple-name .t, ! #
fc-parse-type .t (TYPE .t) :- is.atom .t #

fc-parse-types () () :- ! #
fc-parse-types (.type, .types) (.type1, .types1)
	:- fc-parse-type .type .type1, fc-parse-types .types .types1
#

-- "cons" and "corecursive-cons" differ only by type
fc-define-default-fun 0 () EMPTY #
fc-define-default-fun 2 cons CONS #
fc-define-default-fun 2 corecursive-cons CONS #
fc-define-default-fun 1 head HEAD #
fc-define-default-fun 1 is-tree IS-TREE #
fc-define-default-fun 2 log LOG #
fc-define-default-fun 1 tail TAIL #

fc-is-tuple-name () :- ! # -- Empty atom is list terminator
fc-is-tuple-name .t
	:- is.atom .t
	, nth .t 0 1 .c
	, .c >= 'A', .c <= 'Z'
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
	define if-tree = (list => f1 => f2 =>
		if (is-tree {list}) then (
			f1 {head {list}} {tail {list}}
		)
		else f2
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
	define concat2 = (l1 => l2 =>
		if-tree {l1} {h => t => h, concat2 {t} {l2}} {l2}
	) >>
	define fold = (fun => list =>
		define h = head {list} >>
		define t = tail {list} >>
		if (is-tree {t}) then (fun {h} {fold {fun} {t}}) else h
	) >>
	define fold-left = (fun => init => list =>
		if-tree {list}
		{h => t => fold-left {fun} {fun {init} {h}} {t}}
		{init}
	) >>
	define fold-right = (fun => init => list =>
		if-tree {list}
		{h => t => fun {h} {fold-right {fun} {init} {t}}}
		{init}
	) >>
	define split = (fun => list =>
		if-tree {list} {fun} {}
	) >>
	define zip = (fun => l0 => l1 =>
		if-tree {l0} {h0 => t0 =>
			if-tree {l1} {h1 => t1 =>
				fun {h0} {h1}, zip {fun} {t0} {t1}
			} {}
		} {}
	) >>
	define apply =
		fold-left {x => f => f {x}}
	>>
	define concat =
		fold-left {concat2} {}
	>>
	define filter = (fun =>
		fold-right {
			item => list => if (fun {item}) then (item, list) else list
		} {}
	) >>
	define map = (fun =>
		fold-right {i => list => fun {i}, list} {}
	) >>
	define contains = (e =>
		fold {or} . map {e1 => e1 = e}
	) >>
	define cross = (fun => l1 => l2 =>
		map {e1 => map {e2 => fun {e1} {e2}} {l2}} {l1}
	) >>
	.p
) #
