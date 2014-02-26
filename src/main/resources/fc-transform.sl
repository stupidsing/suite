fc-transform (ATOM .a) (ATOM .a) .ts/.ts
#
fc-transform (BOOLEAN .b) (BOOLEAN .b) .ts/.ts
#
fc-transform (CONSTANT .c) (CONSTANT .c) .ts/.ts
#
fc-transform (DEF-VAR .var .value0 .do0) (DEF-VAR .var .value1 .do1) .ts0/.tsx
	:- .ts0 = (.value0 .value1, .do0 .do1, .tsx)
#
fc-transform (FUN .var .do0) (FUN .var .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#
fc-transform (IF .if0 .then0 .else0) (IF .if1 .then1 .else1) .ts0/.tsx
	:- .ts0 = (.if0 .if1, .then0 .then1, .else0 .else1, .tsx)
#
fc-transform (INVOKE .param0 .callee0) (INVOKE .param1 .callee1) .ts0/.tsx
	:- .ts0 = (.param0 .param1, .callee0 .callee1, .tsx)
#
fc-transform (NEW-VAR .var) (NEW-VAR .var) .ts/.ts
#
fc-transform (NUMBER .i) (NUMBER .i) .ts/.ts
#
fc-transform (PAIR .left0 .right0) (PAIR .left1 .right1) .ts0/.tsx
	:- .ts0 = (.left0 .left1, .right0 .right1, .tsx)
#
fc-transform (PRAGMA .pragma0 .do0) (PRAGMA .pragma1 .do1) .ts0/.tsx
	:- fc-transform-pragma .pragma0 .pragma1
	, .ts0 = (.do0 .do1, .tsx)
#
fc-transform (TREE .oper .left0 .right0) (TREE .oper .left1 .right1) .ts0/.tsx
	:- .ts0 = (.left0 .left1, .right0 .right1, .tsx)
#
fc-transform (USING .mode .linkOption .m .do0) (USING .mode .linkOption .m .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#
fc-transform (UNWRAP .do0) (UNWRAP .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#
fc-transform (VAR .var) (VAR .var) .ts/.ts
#
fc-transform (WRAP .do0) (WRAP .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#

fc-transform-pragma ALLOW-RECURSIVE ALLOW-RECURSIVE
#
fc-transform-pragma (CAST DOWN .type0) (CAST DOWN .type1)
	:- fc-transform-type .type0 .type1
#
fc-transform-pragma (CAST UP .type0) (CAST UP .type1)
	:- fc-transform-type .type0 .type1
#
fc-transform-pragma CAST-TO-CLASS CAST-TO-CLASS
#
fc-transform-pragma (DEF-TYPE .type0 .class0 .typeVars0) (DEF-TYPE .type1 .class1 .typeVars1)
	:- fc-transform-type .type0 .type1
	, fc-transform-type .class0 .class1
	, fc-transform-type-list .typeVars0 .typeVars1
#
fc-transform-pragma RESOLVE-TYPE RESOLVE-TYPE
#
fc-transform-pragma SKIP-TYPE-CHECK SKIP-TYPE-CHECK
#

fc-transform-type .unbound .unbound
	:- not bound .unbound, !
#
fc-transform-type (ATOM-OF .atom) (ATOM-OF .atom)
#
fc-transform-type (DATA-OF .data) (DATA-OF .data)
#
fc-transform-type BOOLEAN BOOLEAN
#
fc-transform-type (CLASS .t) (CLASS .t)
#
fc-transform-type (DO-OF .type0) (DO-OF .type1)
	:- fc-transform-type .type0 .type1
#
fc-transform-type (FUN-OF .paramType0 .returnType0) (FUN-OF .paramType1 .returnType1)
	:- fc-transform-type .paramType0 .paramType1
	, fc-transform-type .returnType0 .returnType1
#
fc-transform-type (GENERIC-OF .typeVar0 .type0) (GENERIC-OF .typeVar1 .type1)
	:- fc-transform-type .typeVar0 .typeVar1
	, fc-transform-type .type0 .type1
#
fc-transform-type (LIST-OF .type0) (LIST-OF .type1)
	:- fc-transform-type .type0 .type1
#
fc-transform-type NUMBER NUMBER
#
fc-transform-type (PAIR-OF .leftType0 .rightType0) (PAIR-OF .leftType1 .rightType1)
	:- fc-transform-type .leftType0 .leftType1
	, fc-transform-type .rightType0 .rightType1
#
fc-transform-type (TYPE-VAR .typeVar) (TYPE-VAR .typeVar)
#

fc-transform-type-list () ()
#
fc-transform-type-list (.type0, .types0) (.type1, .types1)
	:- fc-transform-type .type0 .type1
	, fc-transform-type-list .types0 .types1
#
