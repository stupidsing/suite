infer-type .do .te .type :- infer-type0 .do .te .type #

infer-type0 (VAR-TYPE .variable .variableType .do) .te .type
	:- !, infer-type .do (.variable/.variableType, .te) .type
#
infer-type0 (FUNC .variable .do) .te (FUNC .variableType .type)
	:- !, infer-type .do (.variable/.variableType, .te) .type
#
infer-type0 (SUBST .variable .value .do) .te .type
	:- !, .te1 = (.variable/.variableType, .te)
	, infer-type .value .te1 .variableType
	, infer-type .do .te1 .type
#
infer-type0 (INVOKE .parameter .callee) .te .type
	:- !, infer-type .callee .te .funcType
	, infer-type .parameter .te .paramType
	, clone .funcType (FUNC .paramType .type)
#
infer-type0 (IF .if .then .else) .te .type
	:- !, infer-type .if .te BOOLEAN
	, equal-types .then .else .te .type
#
infer-type0 (TREE .oper .left .right) .te .type
	:- member (' + ', ' - ', ' * ', ' / ', ' %% ',) .oper, !
	, equal-types .left .right .te .type
	, member (NUMBER, STRING,) .type
	; member (' = ', ' != ', ' > ', '  < ', ' >= ', ' <= ',) .oper, !
	, equal-types .left .right .te _
	, .type = BOOLEAN
#
infer-type0 (BOOLEAN _) _ BOOLEAN  :- ! #
infer-type0 (NUMBER _) _ NUMBER :- ! #
infer-type0 (STRING _) _ STRING :- ! #
infer-type0 (VARIABLE .pred) _ .type :- default-function-type .pred .type #
infer-type0 (VARIABLE .variable) .te .type :- member .te .variable/.type, ! #
infer-type0 EMPTY _ (LIST-OF _) #

default-function-type cons (FUNC .type (FUNC (LIST-OF .type) (LIST-OF .type))) #
default-function-type head (FUNC (LIST-OF .type) .type) #
default-function-type is-tree (FUNC (LIST-OF .type) BOOLEAN) #
default-function-type log (FUNC _ (FUNC .type .type)) #
default-function-type tail (FUNC (LIST-OF .type) (LIST-OF .type)) #

-- Special type primitive for corecursive lists
default-function-type corecursive-cons (FUNC .type (FUNC (FUNC _ .cl) .cl))
	:- .cl = CO-LIST-OF .type
#
default-function-type head (FUNC (CO-LIST-OF .type) .type) #
default-function-type tail (FUNC .cl (FUNC _ .cl)) :- .cl = CO-LIST-OF .type #

equal-types .a .b .te .type :- infer-type .a .te .type, infer-type .b .te .type #
