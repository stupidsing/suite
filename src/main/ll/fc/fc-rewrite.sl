fc-rewrite (APPLY .param0 .callee0) (APPLY .param1 .callee1) .ts0/.tsx
	:- .ts0 = (.param0 .param1, .callee0 .callee1, .tsx)
#
fc-rewrite (ATOM .a) (ATOM .a) .ts/.ts
#
fc-rewrite (BOOLEAN .b) (BOOLEAN .b) .ts/.ts
#
fc-rewrite (CHARS .cs) (CHARS .cs) .ts/.ts
#
fc-rewrite (CONS .type .head0 .tail0) (CONS .type .head1 .tail1) .ts0/.tsx
	:- .ts0 = (.head0 .head1, .tail0 .tail1, .tsx)
#
fc-rewrite
(DECONS .type .cons0 .headVar .tailVar .then0 .else0)
(DECONS .type .cons1 .headVar .tailVar .then1 .else1)
.ts0/.tsx
	:- .ts0 = (.cons0 .cons1, .then0 .then1, .else0 .else1, .tsx)
#
fc-rewrite (DEF-VARS (.var .value0, .list0) .do0) (DEF-VARS (.var .value1, .list1) .do1) .ts0/.tsx
	:- .ts0 = (.value0 .value1, .ts1)
	, fc-rewrite (DEF-VARS .list0 .do0) (DEF-VARS .list1 .do1) .ts1/.tsx
#
fc-rewrite (DEF-VARS () .do0) (DEF-VARS () .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#
fc-rewrite (ERROR .m) (ERROR .m) .ts/.ts
#
fc-rewrite (FUN .var .do0) (FUN .var .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#
fc-rewrite (IF .if0 .then0 .else0) (IF .if1 .then1 .else1) .ts0/.tsx
	:- .ts0 = (.if0 .if1, .then0 .then1, .else0 .else1, .tsx)
#
fc-rewrite NIL NIL .ts/.ts
#
fc-rewrite (NUMBER .i) (NUMBER .i) .ts/.ts
#
fc-rewrite (PRAGMA .pragma0 .do0) (PRAGMA .pragma1 .do1) .ts0/.tsx
	:- fc-rewrite-pragma .pragma0 .pragma1 .ts0/.ts1
	, .ts1 = (.do0 .do1, .tsx)
#
fc-rewrite (TCO .iter0 .in0) (TCO .iter1 .in1) .ts0/.tsx
	:- .ts0 = (.iter0 .iter1, .in0 .in1, .tsx)
#
fc-rewrite (TREE .oper .left0 .right0) (TREE .oper .left1 .right1) .ts0/.tsx
	:- .ts0 = (.left0 .left1, .right0 .right1, .tsx)
#
fc-rewrite (USE .mode .linkOption .m .do0) (USE .mode .linkOption .m .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#
fc-rewrite (UNWRAP .do0) (UNWRAP .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#
fc-rewrite (VAR .var) (VAR .var) .ts/.ts
#
fc-rewrite (WRAP .do0) (WRAP .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#

fc-rewrite-pragma DEF-OUTSIDE DEF-OUTSIDE .ts/.ts
#
fc-rewrite-pragma NEW NEW .ts/.ts
#
fc-rewrite-pragma (TYPE-CAST .type0) (TYPE-CAST .type1) .ts/.ts
	:- fc-rewrite-type .type0 .type1
#
fc-rewrite-pragma (TYPE-DEF .type0 .class0) (TYPE-DEF .type1 .class1) .ts/.ts
	:- fc-rewrite-type .type0 .type1
	, fc-rewrite-type .class0 .class1
#
fc-rewrite-pragma TYPE-SKIP-CHECK TYPE-SKIP-CHECK .ts/.ts
#
fc-rewrite-pragma TYPE-SUPER TYPE-SUPER .ts/.ts
#
fc-rewrite-pragma (TYPE-VERIFY .v0 .type0) (TYPE-VERIFY .v1 .type1) .ts0/.tsx
	:- fc-rewrite .v0 .v1 .ts0/.tsx
	, fc-rewrite-type .type0 .type1
	, !
#

fc-rewrite-type .tv .tv
	:- fc-is-type-variable .tv
#
fc-rewrite-type (ATOM-OF .atom) (ATOM-OF .atom)
#
fc-rewrite-type BOOLEAN BOOLEAN
#
fc-rewrite-type (FUN-OF .paramType0 .returnType0) (FUN-OF .paramType1 .returnType1)
	:- fc-rewrite-type .paramType0 .paramType1
	, fc-rewrite-type .returnType0 .returnType1
#
fc-rewrite-type (FUNCTOR-OF .functor .type0) (FUNCTOR-OF .functor .type1)
	:- fc-rewrite-type .type0 .type1
#
fc-rewrite-type NUMBER NUMBER
#
fc-rewrite-type (PAIR-OF .leftType0 .rightType0) (PAIR-OF .leftType1 .rightType1)
	:- fc-rewrite-type .leftType0 .leftType1
	, fc-rewrite-type .rightType0 .rightType1
#
