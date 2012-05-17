-- functional program compiler
--
-- Also need to import one of the following backends:
-- fc-eager-evaluation.sl, fc-lazy-evaluation.sl
--
-- .mode can be EAGER or LAZY

() :- import.file 'fc-type-inference.sl' #
() :- file.read 'precompiled.rpn' .rpn
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
	, !, infer-type-rule-using-libs .libs .parsed ()/()/()/() .tr0/.trx _
	, !, resolve-types .tr0/.trx
	, !, fc-compile-using-libs .mode .libs .parsed 0/() .c1/.c2/.d0/()/.reg
	, .c2 = (_ EXIT .reg, _ LEAVE, .d0)
	, !, fc-assign-line-number 0 .c0
#

infer-type-rule-using-libs () .do .uvto .tr .type
	:- infer-type-rule .do .uvto .tr .type
#

fc-compile-using-libs .mode () .do .fve .cdr
	:- !, fc-compile .mode .do .fve .cdr
#

--
-- Syntactic sugars
--
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
fc-parse (.l, .r) .parsed :- !, fc-parse (_cons {.l} {.r}) .parsed #
fc-parse (.l . .r) .parsed :- !, temp .v, fc-parse (.v => .l {.r {.v}}) .parsed #
fc-parse (.l | .r) .parsed :- !, fc-parse (.l {.r}) .parsed #
fc-parse (.l << .r) .parsed :- !, fc-parse (.r {.l}) .parsed #
fc-parse ($ => .do) .parsed :- !, temp .v, fc-parse (.v => .do) .parsed #
fc-parse (not .b) .parsed :- !, fc-parse (not {.b}) .parsed #
fc-parse (.a ++ .b) .parsed :- !, fc-parse (concat2 {.a} {.b}) .parsed #
fc-parse (.s until .e) .parsed :- !, fc-parse (range {.s} {.e} {1}) .parsed #
fc-parse (if-match .v1 .thenElse) .parsed
	:- !, temp .v0, fc-parse (.v0 => if-bind (.v0 = .v1) .thenElse) .parsed
#
fc-parse (if-bind (.v0 = .v1) then .then else .else) .parsed
	:- !
	, fc-parse .v0 .vp0
	, fc-parse .v1 .vp1
	, fc-parse .then .thenp
	, fc-parse .else .elsep
	, fc-bind .vp0 .vp1 .thenp .elsep .parsed
#
--
-- Function constructs
--
fc-parse (.var as .type => .do) (FUN .var .do1)
	:- !, fc-parse-type .type .type1
	, .do1 = OPTION (AS .var .type1) .do2
	, fc-parse .do .do2
#
fc-parse (.var => .do) (FUN .var .do1) :- !, fc-parse .do .do1 #
fc-parse (define type .type >> .do) (OPTION (DEF-TYPE .type _) .do1) -- Type variable
	:- !, fc-parse .do .do1
#
fc-parse (define type .type = .def >> .do) (
	OPTION (DEF-ONE-OF-TYPE .def1) OPTION (DEF-TYPE .type .def1) .do1
) :- !, fc-parse-type .def .def1
	, fc-parse .do .do1
#
fc-parse (.value as .type) (OPTION (CAST .type1) .value1)
	:- !, fc-parse-type .type .type1
	, fc-parse .value .value1
#
fc-parse (no-type-check .do) (OPTION NO-TYPE-CHECK .do1)
	:- !, fc-parse .do .do1
#
fc-parse (define .var as .type = .value >> .do) (
	OPTION (DEF-ONE-OF-TYPE .type1) (
		OPTION GENERIC-TYPE DEF-VAR .var .value2 .do1
	)
) :- !
	, fc-parse-type .type .type1
	, fc-parse .value .value1
	, fc-parse .do .do1
	, .value2 = OPTION (CAST .type1) .value1
#
fc-parse (define .var = .value >> .do) (
	OPTION GENERIC-TYPE DEF-VAR .var .value1 .do1
) :- !, fc-parse .value .value1
	, fc-parse .do .do1
#
fc-parse (let .var = .value >> .do) (DEF-VAR .var .value1 .do1)
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
fc-parse (prove-r .vvs .constant .result) .parsed
	:- !, fc-parse (prove | c .vvs (.constant . .result)) .parsed
#
fc-parse (prove-tf .vvs .constant) .parsed
	:- !, fc-parse (prove | c .vvs .constant) .parsed
#
fc-parse (c (.var:.value/.vvs) .constant) .parsed
	:- !, fc-parse (subst {.value} {c .vvs (.constant . .var)}) .parsed
#
fc-parse (c () .constant) (CONSTANT .constant) :- ! #
fc-parse .an0:.an1 (TUPLE $$ANON .elems1)
	:- !, fc-parse-anon-tuple .an0:.an1 .elems, fc-parse-list .elems .elems1
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
	, !, substring .s 0 1 .c, substring .s 1 0 .cs
	, to.int .c .ascii, fc-parse (.ascii, .cs) .n
#
fc-parse .t (TUPLE .t ()) :- fc-is-tuple-name .t, ! #
fc-parse .v (NEW-VARIABLE .nv) :- fc-parse-bind-variable .v .nv, ! #
fc-parse .v (VARIABLE .v) :- is.atom .v, ! #
fc-parse .d _ :- fc-error "Unknown expression" .d #

fc-parse-list () () :- ! #
fc-parse-list (.e, .es) (.p, .ps) :- !, fc-parse .e .p, fc-parse-list .es .ps #

fc-parse-type .t .t :- not bound .t, ! #
fc-parse-type (.returnType {.paramType}) (FUN .paramType1 .returnType1)
	:- !, fc-parse-type .paramType .paramType1
	, fc-parse-type .returnType .returnType1
#
fc-parse-type (one-of .types) (ONE-OF .types1)
	:- !, fc-parse-types .types .types1
#
fc-parse-type (list-of .type) (LIST-OF .type1) :- !, fc-parse-type .type .type1 #
fc-parse-type (.name .types) (TUPLE-OF .name .types2)
	:- !, (
		bound .types, enlist .types .types1, fc-parse-types .types1 .types2
		; fc-parse-types .types1 .types2, enlist .types .types1
	)
#
fc-parse-type boolean BOOLEAN :- ! #
fc-parse-type number NUMBER :- ! #
fc-parse-type string STRING :- ! #
fc-parse-type :.typeVar (TYPE-VAR .typeVar) :- ! #
fc-parse-type .t (TUPLE-OF .t ()) :- fc-is-tuple-name .t, ! #
fc-parse-type .t (TYPE .t) :- is.atom .t #
fc-parse-type (.typeVar => .type) .type2
	:- bound .type
	, !, fc-parse-type .typeVar .typeVar1
	, fc-parse-type .type .type1
	, replace .type1/.type2 .typeVar1/_
#

fc-parse-types () () :- ! #
fc-parse-types (.type, .types) (.type1, .types1)
	:- fc-parse-type .type .type1, fc-parse-types .types .types1
#

fc-parse-bind-variable .v .vd
	:- is.atom .v, to.string .v .s0, substring .s0 0 1 "\"
	, !, substring .s0 1 0 .s1, to.atom .s1 .vd
#

fc-parse-anon-tuple () () :- ! #
fc-parse-anon-tuple .h:.t0 (.h, .t1) :- fc-parse-anon-tuple .t0 .t1 #

fc-bind .v0 .v1 .then .else .parsed
	:- once (.v0 = NEW-VARIABLE _; fc-bind-cons .v0 _ _; .v0 = TUPLE _ _)
	, !, fc-bind0 .v1 .v0 .then .else .parsed
#
fc-bind .v0 .v1 .then .else .parsed :- fc-bind0 .v0 .v1 .then .else .parsed #

fc-bind0 .v0 .v1 .then .else .parsed
	:- (fc-bind-cons .v0 .h0 .t0, fc-bind-cons .v1 .h1 .t1
		; .v0 = TUPLE .n (.h0, .hs0), .v1 = TUPLE .n (.h1, .hs1)
		, .t0 = TUPLE .n .hs0, .t1 = TUPLE .n .hs1
	), !
	, fc-bind-pair .h0 .t0 .h1 .t1 .then .else .parsed
#
fc-bind0 (TUPLE .n0 .e0) (TUPLE .n1 .e1) .then .else .parsed
	:- !
	, once (.n0 = .n1
		, once (.e0 = (), .e1 = (), .parsed = .then
			; .e0 = (.h0, .hs0), .e1 = (.h1, .hs1)
			, .t0 = TUPLE .n0 .hs0, .t1 = TUPLE .n1 .hs1
			, fc-bind-pair .h0 .t0 .h1 .t1 .then .else .parsed
		)
	; .parsed = .else
	)
#
fc-bind0 .v0 (NEW-VARIABLE .nv) .then .else (DEF-VAR .nv .v0 .then)
	:- !
#
fc-bind0 .v0 .v1 .then .else (IF (INVOKE .v0 VARIABLE is-tree) .then1 .else)
	:- fc-bind-cons .v1 .h1 .t1, !
	, .h0 = INVOKE .v0 VARIABLE _lhead
	, .t0 = INVOKE .v0 VARIABLE _ltail
	, fc-bind-pair .h0 .t0 .h1 .t1 .then .else .then1
#
fc-bind0 .v0 (TUPLE .n (.h1, .t1)) .then .else .parsed
	:- !
	, .h0 = INVOKE .v0 VARIABLE _thead
	, .t0 = INVOKE .v0 VARIABLE _ttail
	, fc-bind-pair .h0 .t0 .h1 (TUPLE .n .t1) .then .else .parsed
#
fc-bind0 .v0 .v1 .then .else (
	IF (INVOKE .v0 INVOKE .v1 VARIABLE equals) .then .else
) #

fc-bind-cons (INVOKE .t INVOKE .h VARIABLE _cons) .h .t #

fc-bind-pair .h0 .t0 .h1 .t1 .then .else .parsed
	:- fc-bind .h0 .h1 .then1 .else .parsed
	, fc-bind .t0 .t1 .then .else .then1
#

fc-define-default-fun 2 _compare COMPARE #
fc-define-default-fun 2 _cons CONS #
fc-define-default-fun 1 _lhead HEAD #
fc-define-default-fun 1 _ltail TAIL #
fc-define-default-fun 1 _prove PROVE #
fc-define-default-fun 2 _subst SUBST #
fc-define-default-fun 1 _thead HEAD #
fc-define-default-fun 1 _ttail TAIL #
fc-define-default-fun 1 fflush FFLUSH #
fc-define-default-fun 1 fgetc FGETC #
fc-define-default-fun 3 fputc FPUTC #
fc-define-default-fun 1 is-tree IS-TREE #
fc-define-default-fun 1 log LOG #
fc-define-default-fun 2 log2 LOG2 #

fc-is-tuple-name () :- ! # -- Empty atom is list terminator
fc-is-tuple-name .t
	:- is.atom .t, to.string .t .s, substring .s 0 1 .c
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
	define cons = (head => tail => _cons {head} {tail}) >>
	define head = (list => _lhead {list}) >>
	define prove = (goal => _prove {goal}) >>
	define subst = (var => node => _subst {var} {node}) >>
	define tail = (list => _ltail {list}) >>
	define tuple-head = (list => _thead {list}) >>
	define tuple-tail = (list => _ttail {list}) >>
	define and = (x => y =>
		if x then y else false
	) >>
	define compare as (:t => number {:t} {:t}) = no-type-check (a => b =>
		if (is-tree {a} && is-tree {b}) then
			let c0 = compare {head | a} {head | b} >>
			if: c0 = 0
			then: compare {tail | a} {tail | b}
			else: c0
		else: _compare {a} {b}
	) >>
	define flip = (f => x => y =>
		f {y} {x}
	) >>
	define fold = (fun => list =>
		let h = head {list} >>
		let t = tail {list} >>
		if: is-tree {t}
		then: fun {h} . fold {fun} | t
		else: h
	) >>
	define fold-left = (fun => init => if-match (\h, \t)
		then: fold-left {fun} {fun {init} {h}} {t}
		else: init
	) >>
	define fold-right = (fun => init => if-match (\h, \t)
		then: fun {h} {fold-right {fun} {init} {t}}
		else: init
	) >>
	define tget0 =
		tuple-head
	>>
	define tget1 =
		tuple-head . tuple-tail
	>>
	define tget2 =
		tuple-head . tuple-tail . tuple-tail
	>>
	define id = (v =>
		v
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
	define scan-left = (fun => init => if-match (\h, \t)
		then: init, scan-left {fun} {fun {init} {h}} {t}
		else: init,
	) >>
	define scan-right = (fun => init => if-match (\h, \t) then
			let r = scan-right {fun} {init} {t} >>
			fun {h} {head {r}}, r
		else (init,)
	) >>
	define str-to-int = (s =>
		let unsigned-str-to-int = fold-left {v => d => v * 10 + d - 48} {0} >>
			if: is-tree {s} && head {s} = 45
			then: `0 - ` . unsigned-str-to-int . tail
			else: unsigned-str-to-int
		| s
	) >>
	define tails = if-match (\h, \t)
		then: (h, t), tails {t}
		else: ()
	>>
	define take = (n => list =>
		if: n > 0 && is-tree {list}
		then: head {list}, take {n - 1} {tail {list}}
		else: ()
	) >>
	define take-while = (fun => if-match (\elem, \elems)
		then: if (fun {elem}) then (elem, take-while {fun} {elems}) else ()
		else: ()
	) >>
	define zip = (fun =>
		if-match (\h0, \t0) then
			if-match (\h1, \t1)
			then: fun {h0} {h1}, zip {fun} {t0} {t1}
			else: ()
		else ($ => ())
	) >>
	define apply =
		fold-left {x => f => f {x}}
	>>
	define equals as (:t => boolean {:t} {:t}) = no-type-check (a => b =>
		compare {a} {b} = 0
	) >>
	define concat2 = (if-match (\h, \t)
		then: cons {h} . concat2 {t}
		else: id
	) >>
	define filter = (fun =>
		fold-right {
			item => list => if (fun {item}) then (item, list) else list
		} {}
	) >>
	define get = (n =>
		head . (flip {apply} . repeat {n} | tail)
	) >>
	define length =
		fold-left {v => e => v + 1} {0}
	>>
	define map = (fun =>
		fold-right {i => list => fun {i}, list} {}
	) >>
	define reverse =
		fold-left {a => b => b, a} {}
	>>
	define unfold-right = (fun => init =>
		let r = fun {init} >>
		if: is-tree {r}
		then: cons {head | r} . unfold-right {fun} . head . tail | r
		else: ()
	) >>
	define concat =
		fold-left {concat2} {}
	>>
	define contains = (e =>
		fold {or} . map {`= e`}
	) >>
	define cross = (fun => l1 => l2 =>
		map {e1 => map {fun | e1} | l2} | l1
	) >>
	define int-to-str = (i =>
		let unsigned-int-to-str =
			reverse
			. map {`+ 48`}
			. unfold-right {i => if (i != 0) then (i % 10, i / 10,) else ()}
		>>
		if (i > 0) then: unsigned-int-to-str
		else-if (i < 0) then: concat2 {"-"} . unsigned-int-to-str . `0 -`
		else: $ => "0"
		| i
	) >>
	define range = (start => end => inc =>
		unfold-right {i => if (i < end) then (i, i + inc,) else ()} | start
	) >>
	define split = (separator =>
		map {take-while {`!= separator`} . tail}
		. filter {`= separator` . head}
		. tails . cons {separator}
	) >>
	define transpose = (m =>
		let height = length {m} >>
		let width = if (height > 0) then (length . head | m) else 0 >>
		if (width > 0) then
			let w1 = width - 1 >>
			let gets = (cons {id} . reverse . tails . repeat {w1} | tail) >>
			map {f => map {head . flip {apply} {f}} {m}} | gets
		else ()
	) >>
	define dump as (:t => (list-of number) {:t}) = no-type-check (
		let dump-string = (s =>
			let length = prove-r _s:s/ (string.length _s _l) _l >>
			map {i =>
				prove-r _s:s/_i:i/ (
					substring _s _i 0 _c, to.int _c _asc
				) _asc
			} | 0 until length
		) >>
		let dump0 = (prec => n =>
			if (is-tree {n}) then
				if prec then (s => concat {"(", s, ")",}) else id
				| concat {dump0 {true} {head | n}, ", ", dump0 {false} {tail | n},}
			else-if (equals {n} {}) then "()"
			else-if (prove-tf _n:n/ (is.atom _n)) then
				dump-string | prove-r _n:n/ (to.string _n _s) _s
			else (int-to-str {n})
		) >>
		dump0 {false}
	) >>
	define quick-sort = (cmp => if-match (\pivot, \t)
		then (
			let filter0 = (not . cmp {pivot}) >>
			let filter1 = cmp {pivot} >>
			let l0 = (quick-sort {cmp} . filter {filter0} | t) >>
			let l1 = (quick-sort {cmp} . filter {filter1} | t) >>
			concat {l0, (pivot,), l1,}
		)
		else ()
	) >>
	.p
) #
