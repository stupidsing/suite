fc-transform (ATOM .a) (ATOM .a) .ts/.ts
#
fc-transform (BOOLEAN .b) (BOOLEAN .b) .ts/.ts
#
fc-transform (CONSTANT .c) (CONSTANT .c) .ts/.ts
#
fc-transform (DEF-VAR .var .value0 .do0) (DEF-VAR .var .value1 .do1) .ts0/.tsx
	:- .tsx = (.value0 .value1, .do0 .do1, .ts0)
#
fc-transform (FUN .var .do0) (FUN .var .do1) .ts0/.tsx
	:- .tsx = (.do0 .do1, .ts0)
#
fc-transform (IF .if0 .then0 .else0) (IF .if1 .then1 .else1) .ts0/.tsx
	:- .tsx = (.if0 .if1, .then0 .then1, .else0 .else1, .ts0)
#
fc-transform (INVOKE .param0 .callee0) (INVOKE .param1 .callee1) .ts0/.tsx
	:- .tsx = (.param0 .param1, .callee0 .callee1, .ts0)
#
fc-transform (NEW-VAR .var) (NEW-VAR .var) .ts/.ts
#
fc-transform (NUMBER .i) (NUMBER .i) .ts/.ts
#
fc-transform (OPTION .option0 .do0) (OPTION .option1 .do1) .ts0/.tsx
	:- fc-transform-option .option0 .option1
	, .tsx = (.do0 .do1, .ts0)
#
fc-transform (PAIR .left0 .right0) (PAIR .left1 .right1) .ts0/.tsx
	:- .tsx = (.left0 .left1, .right0 .right1, .ts0)
#
fc-transform (TREE .oper .left0 .right0) (TREE .oper .left1 .right1) .ts0/.tsx
	:- .tsx = (.left0 .left1, .right0 .right1, .ts0)
#
fc-transform (USING .m .do0) (USING .m .do1) .ts0/.tsx
	:- .tsx = (.do0 .do1, .ts0)
#
fc-transform (UNWRAP .do0) (UNWRAP .do1) .ts0/.tsx
	:- .tsx = (.do0 .do1, .ts0)
#
fc-transform (VAR .var) (VAR .var) .ts/.ts
#
fc-transform (WRAP .do0) (WRAP .do1) .ts0/.tsx
	:- .tsx = (.do0 .do1, .ts0)
#

fc-transform-option ALLOW-RECURSIVE ALLOW-RECURSIVE
#
fc-transform-option (CAST DOWN .type0) (CAST DOWN .type1)
	:- fc-transform-type .type0 .type1
#
fc-transform-option (CAST UP .type0) (CAST UP .type1)
	:- fc-transform-type .type0 .type1
#
fc-transform-option CAST-TO-CLASS CAST-TO-CLASS
#
fc-transform-option (DEF-TYPE .type0 .classes0 .typeVars0) (DEF-TYPE .type1 .classes1 .typeVars1)
	:- fc-transform-type .type0 .type1
	, fc-transform-type-list .classes0 .classes1
	, fc-transform-type-list .typeVars0 .typeVars1
#
fc-transform-option RESOLVE-TYPE RESOLVE-TYPE
#
fc-transform-option NO-TYPE-CHECK NO-TYPE-CHECK
#

fc-transform-type .unbound .unbound
	:- not bound .unbound, !
#
fc-transform-type (ATOM-OF .atom) (ATOM-OF .atom)
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
