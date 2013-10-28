fc-validate (ATOM .a) (ATOM .a)
#
fc-validate (BOOLEAN .b) (BOOLEAN .b)
#
fc-validate (CONSTANT .c) (CONSTANT .c)
#
fc-validate (DEF-VAR .var .value0 .do0) (DEF-VAR .var .value1 .do1)
	:- fc-validate .value0 .value1
	, fc-validate .do0 .do1
#
fc-validate (FUN .var .do0) (FUN .var .do1)
	:- fc-validate .do0 .do1
#
fc-validate (IF .if0 .then0 .else0) (IF .if1 .then1 .else1)
	:- fc-validate .if0 .if1
	, fc-validate .then0 .then1
	, fc-validate .else0 .else1
#
fc-validate (INVOKE .param0 .callee0) (INVOKE .param1 .callee1)
	:- fc-validate .param0 .param1
	, fc-validate .callee0 .callee1
#
fc-validate (NEW-VAR .var) (NEW-VAR .var)
#
fc-validate (NUMBER .i) (NUMBER .i)
#
fc-validate (OPTION .option0 .do0) (OPTION .option1 .do1)
	:- fc-validate-option .option0 .option1
	, fc-validate .do0 .do1
#
fc-validate (PAIR .left0 .right0) (PAIR .left1 .right1)
	:- fc-validate .left0 .left1
	, fc-validate .right0 .right1
#
fc-validate (TREE .oper .left0 .right0) (TREE .oper .left1 .right1)
	:- fc-validate .left0 .left1
	, fc-validate .right0 .right1
#
fc-validate (USING .m .do0) (USING .m .do1)
	:- fc-validate .do0 .do1
#
fc-validate (VAR .var) (VAR .var)
#

fc-validate-option ALLOW-RECURSIVE ALLOW-RECURSIVE
#
fc-validate-option (CAST-DOWN .type0) (CAST-DOWN .type1)
	:- fc-validate-type .type0 .type1
#
fc-validate-option CAST-TO-CLASS CAST-TO-CLASS
#
fc-validate-option (CAST-UP .type0) (CAST-UP .type1)
	:- fc-validate-type .type0 .type1
#
fc-validate-option (DEF-TYPE .type0 .classes0 .typeVars0) (DEF-TYPE .type1 .classes1 .typeVars1)
	:- fc-validate-type .type0 .type1
	, fc-validate-type-list .classes0 .classes1
	, fc-validate-type-list .typeVars0 .typeVars1
#
fc-validate-option RESOLVE-TYPE RESOLVE-TYPE
#
fc-validate-option NO-TYPE-CHECK NO-TYPE-CHECK
#

fc-validate-type-list () ()
#
fc-validate-type-list (.type0, .types0) (.type1, .types1)
	:- fc-validate-type .type0 .type1
	, fc-validate-type-list .types0 .types1
#

fc-validate-type (ATOM-OF .atom) (ATOM-OF .atom)
#
fc-validate-type (CLASS .class) (CLASS .class)
#
fc-validate-type (DO-OF .type0) (DO-OF .type1)
	:- fc-validate-type .type0 .type1
#
fc-validate-type (FUN-OF .paramType0 .returnType0) (FUN-OF .paramType1 .returnType1)
	:- fc-validate-type .paramType0 .paramType1
	, fc-validate-type .returnType0 .returnType1
#
fc-validate-type (GENERIC-OF .typeVar0 .type0) (GENERIC-OF .typeVar1 .type1)
	:- fc-validate-type .typeVar0 .typeVar1
	, fc-validate-type .type0 .type1
#
fc-validate-type (LIST-OF .type0) (LIST-OF .type1)
	:- fc-validate-type .type0 .type1
#
fc-validate-type (PAIR-OF .leftType0 .rightType0) (PAIR-OF .leftType1 .rightType1)
	:- fc-validate-type .leftType0 .leftType1
	, fc-validate-type .rightType0 .rightType1
#
fc-validate-type (TYPEVAR .typeVar) (TYPEVAR .typeVar)
#
