fc-parse .t .parsed
	:- bound .t, fc-parse-sugar .t .t1, !, fc-parse .t1 .parsed
#
fc-parse (.var => .do) (FUN .var .do1) :- !, fc-parse .do .do1 #
fc-parse (define type .type of .classes for any .typeVars >> .do) (
	OPTION (DEF-TYPE .type1 .classes1 .typeVars1) .do1
) :- !, fc-parse-type .type .type1
	, fc-parse-type-list .classes .classes1
	, fc-parse-type-list .typeVars .typeVars1
	, fc-parse .do .do1
#
fc-parse (define type .type of .classes >> .do) .do1
	:- !, fc-parse (define type .type of .classes for any () >> .do) .do1
#
fc-parse .do (OPTION (CAST DOWN .type1) .value1)
	:- (.do = .value as .type
		; .do = type .type .value
	), !
	, fc-parse-type .type .type1
	, fc-parse .value .value1
#
fc-parse (no-type-check .do) (OPTION NO-TYPE-CHECK .do1)
	:- !, fc-parse .do .do1
#
fc-parse (using .lib >> .do) (USING .lib .do1)
	:- !, fc-parse .do .do1
#
fc-parse (define .var = .value >> .do) (
	OPTION ALLOW-RECURSIVE-DEFINITION DEF-VAR .var .value1 .do1
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
		; .otherwise = else-if .elseif, !, fc-parse (if .elseif) .else1
		; fc-error "Unknown else clause" .otherwise
	)
#
fc-parse (if-bind (.v0 = .v1) then .then else .else) .parsed
	:- !
	, fc-parse .v0 .vp0
	, fc-parse .v1 .vp1
	, fc-parse .then .thenp
	, fc-parse .else .elsep
	, fc-bind .vp0 .vp1 .thenp .elsep .parsed
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
fc-parse (.name .elem .elems) (OPTION CHECK-TUPLE-TYPE TUPLE .name (.elem1, .elems1))
	:- !
	, fc-parse .elem .elem1
	, fc-parse (.name .elems) (OPTION CHECK-TUPLE-TYPE TUPLE .name .elems1)
#
fc-parse (.name %) (OPTION CHECK-TUPLE-TYPE TUPLE .name ()) :- ! #
fc-parse .tree (TREE .oper .left1 .right1)
	:- tree .tree .left .oper .right
	, fc-operator .oper
	, !
	, fc-parse .left .left1
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
fc-parse .v (NEW-VARIABLE .nv) :- fc-parse-bind-variable .v .nv, ! #
fc-parse .v (VARIABLE .v) :- is.atom .v, ! #
fc-parse .d _ :- fc-error "Unknown expression" .d #

fc-parse-list () () :- ! #
fc-parse-list (.e, .es) (.p, .ps) :- !, fc-parse .e .p, fc-parse-list .es .ps #

fc-parse-sugar (match || .bind => .then || .otherwise) .p1
	:- !, temp .var
	, .p1 = (.var =>
		if-bind (.var = .bind)
		then .then
		else ((match || .otherwise) {.var})
	)
#
fc-parse-sugar (match || .p) .p :- ! #
fc-parse-sugar (.l && .r) ((and {.l} {.r})) :- ! #
fc-parse-sugar (.l || .r) (or {.l} {.r}) :- ! #
fc-parse-sugar .t (.var => .t1)
	:- tree .t () .op .right
	, (fc-operator .op; .op = '{')
	, !, temp .var, tree .t1 .var .op .right
#
fc-parse-sugar .t (.var => .t1)
	:- tree .t .left .op ()
	, fc-operator .op
	, !, temp .var, tree .t1 .left .op .var
#
fc-parse-sugar (.l, .r) (_cons {.l} {.r}) :- ! #
fc-parse-sugar (.l . .r) (.v => .l {.r {.v}}) :- !, temp .v #
fc-parse-sugar (.l; .r) (.r . .l) :- ! #
fc-parse-sugar (.l | .r) (.r {.l}) :- ! #
fc-parse-sugar (otherwise .do) (anything => .do) :- ! #
fc-parse-sugar (anything => .do) (.v => .do) :- !, temp .v #
fc-parse-sugar (not .b) (not {.b}) :- ! #
fc-parse-sugar (.a ++ .b) (append {.a} {.b}) :- ! #
fc-parse-sugar (.s until .e) (range {.s} {.e} {1}) :- ! #
fc-parse-sugar (.f/) (flip {.f}) :- ! #

fc-parse-anon-tuple () () :- ! #
fc-parse-anon-tuple .h:.t0 (.h, .t1) :- fc-parse-anon-tuple .t0 .t1 #

fc-parse-type .t .t :- not bound .t, ! #
fc-parse-type (.paramType => .returnType) (FUN-OF .paramType1 .returnType1)
	:- !
	, fc-parse-type .paramType .paramType1
	, fc-parse-type .returnType .returnType1
#
fc-parse-type (list-of .type) (LIST-OF .type1) :- !, fc-parse-type .type .type1 #
fc-parse-type (.name .type .types) (TUPLE-OF .name (.type1, .types1))
	:- !
	, fc-parse-type .type .type1
	, fc-parse-type (.name .types) (TUPLE-OF .name .types1)
#
fc-parse-type (.name %) (TUPLE-OF .name ()) :- ! #
fc-parse-type (.typeVar :- .type) (GENERIC-OF .typeVar1 .type1)
	:- !
	, fc-parse-type .type .type1
	, fc-parse-type .typeVar .typeVar1
#
-- Keeps contained in class definition for tuple matching.
fc-parse-type .type/.paramType (CLASS (PARAMETERIZED .paramType1 .class))
	:- !
	, fc-parse-type .type (CLASS .class)
	, fc-parse-type .paramType .paramType1
#
fc-parse-type :.typeVar (TYPE-VAR .typeVar) :- ! #
fc-parse-type boolean BOOLEAN :- ! #
fc-parse-type number NUMBER :- ! #
fc-parse-type string STRING :- ! #
fc-parse-type .t (CLASS .t) :- is.atom .t #

fc-parse-type-list () () :- ! #
fc-parse-type-list (.type, .types) (.type1, .types1)
	:- fc-parse-type .type .type1, fc-parse-type-list .types .types1
#

fc-parse-bind-variable .v .vd
	:- is.atom .v, to.string .v .s0, substring .s0 0 1 "$"
	, !, substring .s0 1 0 .s1, to.atom .s1 .vd
#

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
fc-bind0 .v0 .v1 .then .else (
	IF (INVOKE .v0 VARIABLE is-tree) .then1 .else
) :- fc-bind-cons .v1 .h1 .t1, !
	, .h0 = INVOKE .v0 VARIABLE _lhead
	, .t0 = INVOKE .v0 VARIABLE _ltail
	, fc-bind-pair .h0 .t0 .h1 .t1 .then .else .then1
#
fc-bind0 .v0 (TUPLE .n (.h1, .t1)) .then .else ( -- Upcast type class to tuple types
	DEF-VAR .tupleVar (OPTION (CAST UP _) .v0) (
		IF (INVOKE (VARIABLE .tupleVar) (VARIABLE is-tuple)) .then1 .else
	)
) :- !
	, temp .tupleVar
	, .h0 = INVOKE (VARIABLE .tupleVar) VARIABLE _thead
	, .t0 = INVOKE (VARIABLE .tupleVar) VARIABLE _ttail
	, fc-bind-pair .h0 .t0 .h1 (TUPLE .n .t1) .then .else .then1
#
fc-bind0 .v0 (OPTION _ .v1) .then .else .parsed
	:- !
	, fc-bind .v0 .v1 .then .else .parsed
#
fc-bind0 .v0 .v1 .then .else (
	IF (INVOKE .v0 INVOKE .v1 VARIABLE equals) .then .else
) #

fc-bind-cons (INVOKE .t INVOKE .h VARIABLE _cons) .h .t #

fc-bind-pair .h0 .t0 .h1 .t1 .then .else (DEF-VAR .elseVar (FUN BOOLEAN .else) .parsed)
	:- temp .elseVar
	, .else1 = INVOKE (BOOLEAN TRUE) (VARIABLE .elseVar)
	, fc-bind .h0 .h1 .then1 .else1 .parsed
	, fc-bind .t0 .t1 .then .else1 .then1
#
