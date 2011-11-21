infer-type (FUNC .variable/.variableType .do) .te (CALLABLE .variableType .type)
	:- infer-type .do (.te, .variable/.variableType) .type
#
infer-type (SUBST .variable/.variableType _ .do) .te .type
	:- infer-type .do (.te, .variable/.variableType) .type
#
infer-type (INVOKE .callee .parameter) .te .type
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
	, member (NUMBER, STRING) .type
	; member (' = ', ' != ', ' > ', ' < ', ' >= ', ' <= ',) .oper
	, equal-types .left .right .te _
	, .type = BOOLEAN
#
infer-type (BOOLEAN _) _ BOOLEAN #
infer-type (NUMBER _) _ NUMBER :- is.int .i #
infer-type (STRING _) _ STRING :- is.string .i #
infer-type .var .te .type :- member .te .var/.type, ! #

equal-types .a .b .te .type :- infer-type .a .te .type, infer-type .b .te .type #
