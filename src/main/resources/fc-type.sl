infer-type (VAR-TYPE .variable .variableType .do) .te .type
	:- infer-type .do (.te, .variable/.variableType) .type
#
infer-type (FUNC .variable .do) .te (CALLABLE .variableType .type)
	:- member .te .variable/.variableType, !
	, infer-type .do .te .type
#
infer-type (SUBST .variable .value .do) .te .type
	:- member .te .variable/.variableType, !
	, infer-type .value .te .variableType
	, infer-type .do .te .type
#
infer-type (INVOKE .parameter .callee) .te .type
	:- infer-type .callee .te .callableType
	, infer-type .parameter .te .paramType
	, clone .callableType (CALLABLE .paramType .type)
#
infer-type (IF .if .then .else) .te .type
	:- infer-type .if .te BOOLEAN
	, equal-types .then .else .te .type
#
infer-type .a:.b .te .listType
	:- infer-type .a .te .type
	, .listType = LIST-OF .type
	, infer-type .b .te .listType
#
infer-type (TREE .oper .left .right) .te .type
	:- member (' + ', ' - ', ' * ', ' / ',) .oper, !
	, equal-types .left .right .te .type
	, member (NUMBER, STRING, ) .type
	; member (' = ', ' != ', ' > ', '  < ', ' >= ', ' <= ',) .oper, !
	, equal-types .left .right .te _
	, .type = BOOLEAN
#
infer-type (BOOLEAN _) _ BOOLEAN #
infer-type (NUMBER _) _ NUMBER :- ! #
infer-type (STRING _) _ STRING :- ! #
infer-type (VARIABLE .variable) .te .type :- member .te .variable/.type, ! #

equal-types .a .b .te .type :- infer-type .a .te .type, infer-type .b .te .type #
