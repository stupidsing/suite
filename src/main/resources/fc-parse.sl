fc-parse .t .parsed
	:- bound .t, fc-parse-sugar .t .parsed, !
#
fc-parse (.var as .type => .do) (FUN .var .do1)
	:- !, fc-parse-type .type .type1
	, .do1 = OPTION (AS .var .type1) .do2
	, fc-parse .do .do2
#
fc-parse (.var => .do) (FUN .var .do1) :- !, fc-parse .do .do1 #
fc-parse (define type .type = .def >> .do) (
	OPTION (DEF-ONE-OF-TYPE .def1) OPTION (DEF-TYPE .type .def1) .do1
) :- !, fc-parse-type .def .def1
	, fc-parse .do .do1
#
fc-parse (define type .type >> .do) (OPTION (DEF-TYPE .type _) .do1) -- Type variable
	:- !, fc-parse .do .do1
#
fc-parse (.value as .type) (OPTION (CAST .type1) .value1)
	:- !, fc-parse-type .type .type1
	, fc-parse .value .value1
#
fc-parse (no-type-check .do) (OPTION NO-TYPE-CHECK .do1)
	:- !, fc-parse .do .do1
#
fc-parse (using .lib >> .do) (USING .lib .do1)
	:- !, fc-parse .do .do1
#
fc-parse (define .var as .type = .value >> .do) (
	OPTION (DEF-ONE-OF-TYPE .type1) (
		OPTION ALLOW-RECURSIVE-DEFINITION DEF-VAR .var .value2 .do1
	)
) :- !, fc-parse-type .type .type1
	, fc-parse .value .value1
	, fc-parse .do .do1
	, .value2 = OPTION (CAST .type1) .value1
#
fc-parse (define .var = .value >> .do) (
	OPTION ALLOW-RECURSIVE-DEFINITION DEF-VAR .var .value1 .do1
) :- !, fc-parse .value .value1
	, fc-parse .do .do1
#
fc-parse (let .var as .type = .value >> .do) (
	OPTION (DEF-ONE-OF-TYPE .type1) (
		DEF-VAR .var .value2 .do1
	)
) :- !, fc-parse-type .type .type1
	, fc-parse .value .value1
	, fc-parse .do .do1
	, .value2 = OPTION (CAST .type1) .value1
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
fc-parse (prove-with-result .vvs .constant .result) .parsed
	:- !, fc-parse (c .vvs (.constant . .result) | prove) .parsed
#
fc-parse (prove .vvs .constant) .parsed
	:- !, fc-parse (c .vvs .constant | prove) .parsed
#
fc-parse (c (.vvs/.var:.value) .constant) .parsed
	:- !, fc-parse (subst {.value} {c .vvs (.constant . .var)}) .parsed
#
fc-parse (c () .constant) (CONSTANT .constant) :- ! #
fc-parse .an0:.an1 (TUPLE $$ANON .elems1)
	:- !, fc-parse-anon-tuple .an0:.an1 .elems, fc-parse-list .elems .elems1
#
fc-parse (.name .elems) (OPTION CHECK-TUPLE-TYPE (TUPLE .name .elems2))
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
fc-parse () (TUPLE () ()) :- ! #
fc-parse .t (OPTION CHECK-TUPLE-TYPE (TUPLE .t ())) :- fc-is-tuple-name .t, ! #
fc-parse .v (NEW-VARIABLE .nv) :- fc-parse-bind-variable .v .nv, ! #
fc-parse .v (VARIABLE .v) :- is.atom .v, ! #
fc-parse .d _ :- fc-error "Unknown expression" .d #

fc-parse-list () () :- ! #
fc-parse-list (.e, .es) (.p, .ps) :- !, fc-parse .e .p, fc-parse-list .es .ps #

fc-parse-sugar (case => .if | .then => .cases) (IF .if1 .then1 .else)
	:- !, fc-parse .if .if1
	, fc-parse .then .then1
	, fc-parse (case => .cases) .else
#
fc-parse-sugar (case => otherwise .do) .p1 :- !, fc-parse .do .p1 #
fc-parse-sugar (match => .matches) .p
	:- !, temp .var
	, fc-parse-matches .var .matches .p1
	, fc-parse (.var => .p1) .p
#
fc-parse-sugar (.l && .r) .p1 :- !, fc-parse (and {.l} {.r}) .p1 #
fc-parse-sugar (.l || .r) .p1 :- !, fc-parse (or {.l} {.r}) .p1 #
fc-parse-sugar .t .p1
	:- tree .t () .op .right, fc-operator .op, !
	, temp .var, tree .t1 .var .op .right, fc-parse (.var => .t1) .p1
#
fc-parse-sugar .t .p1
	:- tree .t .left .op (), fc-operator .op, !
	, temp .var, tree .t1 .left .op .var, fc-parse (.var => .t1) .p1
#
fc-parse-sugar (.l, .r) .p1 :- !, fc-parse (_cons {.l} {.r}) .p1 #
fc-parse-sugar (.l . .r) .p1 :- !, temp .v, fc-parse (.v => .l {.r {.v}}) .p1 #
fc-parse-sugar (.l | .r) .p1 :- !, fc-parse (.r {.l}) .p1 #
fc-parse-sugar (anything => .do) .p1 :- !, temp .v, fc-parse (.v => .do) .p1 #
fc-parse-sugar (not .b) .p1 :- !, fc-parse (not {.b}) .p1 #
fc-parse-sugar (.a ++ .b) .p1 :- !, fc-parse (append {.a} {.b}) .p1 #
fc-parse-sugar (.s until .e) .p1 :- !, fc-parse (range {.s} {.e} {1}) .p1 #
fc-parse-sugar (/.f/) .p1 :- !, fc-parse .f .p1 #
fc-parse-sugar (.a /.f/) .p1 :- !, fc-parse (.b => .f {.a} {.b}) .p1 #
fc-parse-sugar (/.f/ .b) .p1 :- !, fc-parse (.a => .f {.a} {.b}) .p1 #
fc-parse-sugar (.a /.f/ .b) .p1 :- !, fc-parse (.f {.a} {.b}) .p1 #
fc-parse-sugar (if-bind (.v0 = .v1) then .then else .else) .parsed
	:- !
	, fc-parse .v0 .vp0
	, fc-parse .v1 .vp1
	, fc-parse .then .thenp
	, fc-parse .else .elsep
	, fc-bind .vp0 .vp1 .thenp .elsep .parsed
#

fc-parse-matches .var (.bind | .then => .matches) .p1
	:- !
	, fc-parse-matches .var .matches .else
	, .p1 = if-bind (.var = .bind) then .then else .else
#
fc-parse-matches .var (otherwise .p) .p #

fc-parse-type .t .t :- not bound .t, ! #
fc-parse-type (.paramType => .returnType) (FUN-OF .paramType1 .returnType1)
	:- !, fc-parse-type .paramType .paramType1
	, fc-parse-type .returnType .returnType1
#
fc-parse-type (.type {.paramType}) (INSTANCE-OF .paramType1 .type1)
	:- !, fc-parse-type .type .type1
	, fc-parse-type .paramType .paramType1
#
fc-parse-type (one-of .types) (ONE-OF .types1)
	:- !, fc-parse-tuple-type-list .types .types1
#
fc-parse-type (list-of .type) (LIST-OF .type1) :- !, fc-parse-type .type .type1 #
fc-parse-type (any .typeVar in .type) (GENERIC-OF .typeVar1 .type1)
	:- !, fc-parse-type .typeVar .typeVar1
	, fc-parse-type .type .type1
#
fc-parse-type boolean BOOLEAN :- ! #
fc-parse-type number NUMBER :- ! #
fc-parse-type string STRING :- ! #
fc-parse-type :.typeVar (TYPE-VAR .typeVar) :- ! #
fc-parse-type .t (TYPE .t) :- is.atom .t #

fc-parse-tuple-type-list () () :- ! #
fc-parse-tuple-type-list (.type, .types) (.type1, .types1)
	:- fc-parse-tuple-type .type .type1, fc-parse-tuple-type-list .types .types1
#

fc-parse-tuple-type (.name .types) (TUPLE-OF .name .types2)
	:- !, once (
		bound .types, enlist .types .types1, fc-parse-type-list .types1 .types2
		; fc-parse-type-list .types1 .types2, enlist .types .types1
	)
#
fc-parse-tuple-type .t (TUPLE-OF .t ()) :- fc-is-tuple-name .t, ! #

fc-parse-type-list () () :- ! #
fc-parse-type-list (.type, .types) (.type1, .types1)
	:- fc-parse-type .type .type1, fc-parse-type-list .types .types1
#

fc-parse-bind-variable .v .vd
	:- is.atom .v, to.string .v .s0, substring .s0 0 1 "\"
	, !, substring .s0 1 0 .s1, to.atom .s1 .vd
#

fc-parse-anon-tuple () () :- ! #
fc-parse-anon-tuple .h:.t0 (.h, .t1) :- fc-parse-anon-tuple .t0 .t1 #

fc-bind .v0 .v1 .tep :- .v0 = NEW-VARIABLE _, !, fc-bind0 .v1 .v0 .tep #
fc-bind .v0 .v1 .tep :- .v1 = NEW-VARIABLE _, !, fc-bind0 .v0 .v1 .tep #
fc-bind .v0 .v1 .tep
	:- once (fc-bind-cons .v0 _ _
		; .v0 = TUPLE _ _
		; .v0 = OPTION _
	)
	, !, fc-bind0 .v1 .v0 .tep
#
fc-bind .v0 .v1 .tep :- fc-bind0 .v0 .v1 .tep #

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
fc-bind0 .v0 (OPTION _ .v1) .then .else .parsed
	:- !
	, fc-bind .v0 .v1 .then .else .parsed
#
fc-bind0 .v0 .v1 .then .else (
	IF (INVOKE .v0 INVOKE .v1 VARIABLE equals) .then .else
) #

fc-bind-cons (INVOKE .t INVOKE .h VARIABLE _cons) .h .t #

fc-bind-pair .h0 .t0 .h1 .t1 .then .else .parsed
	:- fc-bind .h0 .h1 .then1 .else .parsed
	, fc-bind .t0 .t1 .then .else .then1
#
