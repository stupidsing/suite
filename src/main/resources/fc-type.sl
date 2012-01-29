infer-type (VAR-TYPE .variable .variableType .do) .te .type
	:- infer-type .do (.variable/.variableType, .te) .type
#
infer-type (FUNC .variable .do) .te (CALLABLE .variableType .type)
	:- !, infer-type .do (.variable/.variableType, .te) .type
#
infer-type (SUBST .variable .value .do) .te .type
	:- !, .te1 = (.variable/.variableType, .te)
	, infer-type .value .te1 .variableType
	, infer-type .do .te1 .type
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
infer-type (TREE .oper .left .right) .te .type
	:- member (' + ', ' - ', ' * ', ' / ', ' %% ',) .oper, !
	, equal-types .left .right .te .type
	, member (NUMBER, STRING, ) .type
	; member (' = ', ' != ', ' > ', '  < ', ' >= ', ' <= ',) .oper, !
	, equal-types .left .right .te _
	, .type = BOOLEAN
#
infer-type (BOOLEAN _) _ BOOLEAN  :- ! #
infer-type (NUMBER _) _ NUMBER :- ! #
infer-type (STRING _) _ STRING :- ! #
infer-type (VARIABLE .pred) _ .type :- system-predicate-type .pred .type, ! #
infer-type (VARIABLE .variable) .te .type :- member .te .variable/.type, ! #
infer-type EMPTY _ (LIST-OF _) #

system-predicate-type cons (CALLABLE .type (CALLABLE (LIST-OF .type) (LIST-OF .type))) #
system-predicate-type head (CALLABLE (LIST-OF .type) .type) #
system-predicate-type is-tree (CALLABLE (LIST-OF .type) BOOLEAN) #
system-predicate-type log (CALLABLE _ (CALLABLE .type .type)) #
system-predicate-type tail (CALLABLE (LIST-OF .type) (LIST-OF .type)) #

equal-types .a .b .te .type :- infer-type .a .te .type, infer-type .b .te .type #
