fc-parse `.t` .parsed
	:- fc-parse-op-sugar .t .t1
	, !, fc-parse .t1 .parsed
#
fc-parse .t .parsed
	:- bound .t
	, fc-parse-sugar .t .t1
	, !, fc-parse .t1 .parsed
#
fc-parse (.var => .do) (FUN .var .do1)
	:- !, fc-parse .do .do1
#
fc-parse (.callee_{.parameter}) (APPLY .parameter1 .callee1)
	:- !, fc-parse .callee .callee1
	, fc-parse .parameter .parameter1
#
fc-parse (.type of .value) (PRAGMA (TYPE-CAST .type1) .value1)
	:- fc-parse-type .type .type1
	, !, fc-parse .value .value1
#
fc-parse (data .class over some .typeVars as .type ~ .do) (
	PRAGMA (TYPE-DEF .type2 .class2) .do1
) :- fc-instantiate .typeVars .type/.class .type1/.class1
	, fc-parse-type .type1 .type2
	, fc-parse-type .class1 .class2
	, !, fc-parse .do .do1
#
fc-parse (data .class over .typeVar as .type ~ .do) .do1
	:- !, fc-parse (data .class over some (.typeVar,) as .type ~ .do) .do1
#
fc-parse (data .class as .type ~ .do) .do1
	:- !, fc-parse (data .class over some () as .type ~ .do) .do1
#
fc-parse (define .var := .value ~ .do) (
	PRAGMA DEF-OUTSIDE (DEF-VARS (.var .value1,) .do1)
) :- !
	, once (fc-parse .value .value1
		; fc-error "at variable" .var
	)
	, fc-parse .do .do1
#
fc-parse (error .m) (ERROR .m)
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
fc-parse (if-bind (.v0 := .v1) then .then else .else) .parsed
	:- !
	, fc-parse .v0 .vp0
	, fc-parse .v1 .vp1
	, fc-parse .then .thenp
	, fc-parse .else .elsep
	, fc-bind .vp0 .vp1 .thenp .elsep .parsed
#
fc-parse (lets (.var := .value # .list) ~ .do) (
	DEF-VARS (.var .value1, .list1) .do1
) :- !
	, once (fc-parse .value .value1
		; fc-error "at variable" .var
	)
	, fc-parse (lets .list ~ .do) (DEF-VARS .list1 .do1)
#
fc-parse (lets () ~ .do) (DEF-VARS () .do1)
	:- !, fc-parse .do .do1
#
fc-parse (skip-type-check .do) (PRAGMA TYPE-SKIP-CHECK .do1)
	:- !, fc-parse .do .do1
#
fc-parse (tco .iter .in) (TCO .iter1 .in1)
	:- !
	, fc-parse .iter .iter1
	, fc-parse .in .in1
#
fc-parse (use source .lib ~ .do) .dox
	:- !, fc-load-library .lib .do .do1
	, fc-parse .do1 .dox
#
fc-parse (use external .lib ~ .do) (USE EAGER EXTERNAL .lib .do1)
	:- !, fc-parse .do .do1
#
fc-parse (use .lib ~ .do) (USE EAGER BUILTIN .lib .do1)
	:- !, fc-parse .do .do1
#
fc-parse (.p0; .p1) (CONS L .parsed0 .parsed1)
	:- !
	, fc-parse .p0 .parsed0
	, fc-parse .p1 .parsed1
#
fc-parse (.p0, .p1) (CONS P .parsed0 .parsed1)
	:- !
	, fc-parse .p0 .parsed0
	, fc-parse .p1 .parsed1
#
fc-parse (.a .p1) (PRAGMA TYPE-SUPER (CONS P (ATOM .a) .parsed1))
	:- fc-is-atom .a
	, !, fc-parse .p1 .parsed1
#
fc-parse .tree (TREE .oper .left1 .right1)
	:- tree .tree .left .oper .right
	, fc-operator .oper
	, !
	, fc-parse .left .left1
	, fc-parse .right .right1
#
fc-parse atom:.a (ATOM .a) :- ! #
fc-parse .b (BOOLEAN .b) :- fc-is-boolean .b, ! #
fc-parse chars:.s (CHARS .s) :- ! #
fc-parse () NIL :- ! #
fc-parse .i (NUMBER .i) :- is.int .i, ! #
fc-parse .v (PRAGMA NEW (VAR .nv)) :- to.string .v "_", temp .nv, ! #
fc-parse .v (PRAGMA NEW (VAR .nv)) :- fc-parse-bind-variable .v .nv, ! #
fc-parse .v (VAR .v) :- is.atom .v, ! #
fc-parse .d _ :- fc-error "Unknown expression" .d #

fc-parse-list .l .l1 :- zip .l .l1 .list, list.query .list .p:.p1 (fc-parse .p .p1) #

fc-parse-op-sugar .t .do
	:- tree .t .left .op .right
	, once (
		fc-operator .op
		, fc-parse-op-sugar1 .op .left .right .do
	)
#

fc-parse-op-sugar1 .op () () (.var0 => .var1 => .t1)
	:- !, temp .var0, temp .var1, tree .t1 .var0 .op .var1
#
fc-parse-op-sugar1 .op () .right (.var => .t1)
	:- !, temp .var, tree .t1 .var .op .right
#
fc-parse-op-sugar1 .op .left () (.var => .t1)
	:- !, temp .var, tree .t1 .left .op .var
#

fc-parse-sugar (anything => .do) (.var => .do) :- !, temp .var
#
fc-parse-sugar (case || .bind => .then || .otherwise) .p1
	:- !, temp .var
	, .p1 = (.var =>
		case
		|| (.var = .bind) .then
		|| ((case || .otherwise)_{.var})
	)
#
fc-parse-sugar (case || .if .then || .otherwise) .p1
	:- !
	, .p1 = if .if then .then else (case || .otherwise)
#
fc-parse-sugar (case || .p) .p :- !
#
fc-parse-sugar (definem .type .mv # .monad) (
	define .mv := (n^Mutable .type) of (erase-type_{atom:.atom}) ~ .monad
)
	:- !, temp .atom
#
fc-parse-sugar (do .do) (
	define fun-to-monad := (:t => (number -> :t) -> Do^:t) of erase-type ~
	define monad-to-fun := (:t => Do^:t -> (number -> :t)) of erase-type ~
	fun-to-monad_{scope =>
		define perform := {scope} . monad-to-fun ~
		expand getm := getm*_{scope} ~
		expand setm := setm*_{scope} ~
		.do
	}
) :- !
#
fc-parse-sugar (expand .var := .value ~ .do) .do1
	:- !
	, generalize .var/.value .var1/.value1
	, rewrite .var1 .value1 .do .do1
#
fc-parse-sugar (if (.p = `.q`) .thenElse) (if-bind (.p := .q) .thenElse) :- !
#
fc-parse-sugar (if (`.p` = .q) .thenElse) (if-bind (.q := .p) .thenElse) :- !
#
fc-parse-sugar (let `.binds` := .value ~ .do) (if-bind (.value := .binds) then .do else error (`.binds` := .value)) :- !
#
fc-parse-sugar (let .var := .value ~ .do) (lets (.var := .value #) ~ .do) :- !
#
fc-parse-sugar (not .b) (not_{.b}) :- !
#
fc-parse-sugar (.l && .r) ((and_{.l}_{.r})) :- !
#
fc-parse-sugar (.l || .r) (or_{.l}_{.r}) :- !
#
fc-parse-sugar ({.t}) (.var => .var_{.t}) :- !, temp .var
#
fc-parse-sugar (.l . .r) (.var => .l_{.r_{.var}}) :- !, temp .var
#
fc-parse-sugar (.l | .r) (.r_{.l}) :- !
#
fc-parse-sugar (.mv := .value # .monad) (setm*_{scope}_{.mv}_{.value} # .monad) :- !
#
fc-parse-sugar (.monad #) (perform_{.monad}) :- !
#
fc-parse-sugar (.monad # .monads0) (perform_{seqm_{.monad}_{.monads1}})
	:- fc-parse-sugar .monads0 (perform_{.monads1}), !
#
fc-parse-sugar (.var as .type => .do) (.var1 => (define .var :=  .type of .var1 ~ .do))
	:- !, temp .var1
#
fc-parse-sugar (`.bind` => .do) (.var => (if-bind (.var := .bind) then .do else error (`.bind`)))
	:- !, temp .var
#
fc-parse-sugar .a (.a atom:()) :- fc-is-atom .a, !
#
fc-parse-sugar (.a ++ .b) (append_{.a}_{.b}) :- !
#
fc-parse-sugar (.s until .e) (range_{.s}_{.e}_{1}) :- !
#
fc-parse-sugar (.f/) (flip_{.f}) :- !
#
fc-parse-sugar "" () :- !
#
fc-parse-sugar .s (string of (skip-type-check (
	chars:.s | (atom:INTRN!CharsIntrinsics.charsString | get%i | call%i-v1)
)))
	:- is.string .s, string.length .s .l, 2 < .l, !
#
fc-parse-sugar .s (.ascii; .cs)
	:- is.string .s
	, !, substring .s 0 1 .c, substring .s 1 0 .cs
	, to.int .c .ascii
#

fc-parse-type _ .t
	:- is.cyclic .t
	, !, fc-error "Cyclic type" .t
#
fc-parse-type .type .t
	:- bound .type
	, .type = any
	, !, graph.specialize _ .t
#
fc-parse-type (.paramType -> .returnType) (FUN-OF .paramType1 .returnType1)
	:- !
	, fc-parse-type .paramType .paramType1
	, fc-parse-type .returnType .returnType1
#
fc-parse-type ([.type]) (FUNCTOR-OF LIST .type1)
	:- !, fc-parse-type .type .type1
#
fc-parse-type (.functor^.type0) (FUNCTOR-OF .functor .type1)
	:- !, fc-parse-type .type0 .type1
#
fc-parse-type .a .type
	:- fc-is-atom .a
	, !, fc-parse-type (.a atom:()) .type
#
fc-parse-type (.a .t) (PAIR-OF (ATOM-OF .a) .type)
	:- fc-is-atom .a
	, !, fc-parse-type .t .type
#
fc-parse-type (.t0, .t1) (PAIR-OF .type0 .type1)
	:- !
	, fc-parse-type .t0 .type0
	, fc-parse-type .t1 .type1
#
fc-parse-type (.typeVar => .type) .type2
	:- bound .typeVar
	, !
	, fc-instantiate (.typeVar,) .type .type1
	, fc-parse-type .type1 .type2
#
fc-parse-type atom:.a (ATOM-OF .a) :- !
#
fc-parse-type boolean BOOLEAN :- !
#
fc-parse-type number NUMBER :- !
#
fc-parse-type string (FUNCTOR-OF LIST NUMBER) :- !
#
fc-parse-type .type .type
	:- fc-is-type-variable .type
#

fc-parse-bind-variable .v .vd
	:- is.atom .v, to.string .v .s0, substring .s0 0 1 "$"
	, !, substring .s0 1 0 .s1, to.atom .s1 .vd
#

fc-instantiate .list .type0 .typex
	:- list.fold .list/.type0/.typex .v/.t0/.tx (
		clone _ .f0
		, graph.specialize .f0 .f1
		, replace .v .f1 .t0 .tx
	)
#

fc-operator .oper
	:- member (' + ', ' - ', ' * ', ' / ', ' %% ',
		' = ', ' != ',
		' < ', ' <= ',
		',', ';',
		' . ',
	) .oper
#

fc-is-type-variable .t
	:- is.atom .t
	, to.string .t .s
	, substring .s 0 1 "."
#

fc-is-atom .a
	:- is.atom .a
	, to.string .a .s
	, substring .s 0 1 .ch
	, "A" <= .ch, .ch <= "Z"
#

fc-is-boolean true #
fc-is-boolean false #
