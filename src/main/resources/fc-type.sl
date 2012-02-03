infer-type .do .ve/.te .type
	:- infer-type0 .do .ve/.te .type
#

infer-types () _ () :- ! #
infer-types (.e, .es) .env (.t, .ts)
	:- infer-type .e .env .t
	, infer-types .es .env .ts
#

infer-type0 (AS .var .varType0 .do) .ve/.te .type
	:- !, member .ve .var/.varType1
	, equal-type .te .varType0 .varType1
	, infer-type .do (.var/.varType, .ve)/.te .type
#
infer-type0 (FUN .var .do) .ve/.te (FUN .varType .type)
	:- !, infer-type .do (.var/.varType, .ve)/.te .type
#
infer-type0 (DEF-TYPE .name .def .do) .ve/.te .type
	:- !, infer-type .do .ve/(.name/.def, .te) .type
#
infer-type0 (DEF-VAR .name .value .do) .ve/.te .type
	:- !, .env1 = (.name/.varType, .ve)/.te
	, infer-type .value .env1 .varType
	, infer-type .do .env1 .type
#
infer-type0 (INVOKE .parameter .callee) .env .type
	:- !, infer-type .callee .env .funcType
	, infer-type .parameter .env .paramType
	, clone .funcType (FUN .paramType .type)
#
infer-type0 (IF .if .then .else) .env .type
	:- !, infer-type .if .env BOOLEAN
	, equal-infer-types .then .else .env .type
#
infer-type0 (TUPLE .name .elems) .env (TUPLE-OF .name .types)
	:- !, infer-types .elems .env .types
#
infer-type0 (TREE .oper .left .right) .env .type
	:- member (' + ', ' - ', ' * ', ' / ', ' %% ',) .oper, !
	, equal-infer-types .left .right .env .type
	, member (NUMBER, STRING,) .type
	; member (' = ', ' != ', ' > ', '  < ', ' >= ', ' <= ',) .oper, !
	, equal-infer-types .left .right .env _
	, .type = BOOLEAN
#
infer-type0 (BOOLEAN _) _ BOOLEAN  :- ! #
infer-type0 (NUMBER _) _ NUMBER :- ! #
infer-type0 (STRING _) _ STRING :- ! #
infer-type0 (VARIABLE .pred) _ .type :- default-fun-type .pred .type #
infer-type0 (VARIABLE .var) .ve/.te .type :- member .ve .var/.type, ! #
infer-type0 EMPTY _ (LIST-OF _) :- ! #
infer-type0 (TUPLE .name .elems) .env (TUPLE-OF .name .types)
	:- infer-types .elems .types
#

expand-type (TYPE .typeName) .t1 .te
	:- append .teh (.typeName/.t0, .tel) .te
	, !, append .teh .tel .te1 -- Avoids infinite recursion
	, expand-type .t0 .t1 .te1
	; .t1 = TYPE .typeName
#
expand-type (FUN .pt0 .rt0) (FUN .pt1 .rt1) .te
	:- !, expand-type .pt0 .pt1 .te, expand-type .rt0 .rt1 .te
#
expand-type (ONE-OF .ts0) (ONE-OF .ts1) .te
	:- !, expand-types .ts0 .ts1 .te
#
expand-type (LIST-OF .t0) (LIST-OF .t1) .te
	:- !, expand-type .t0 .t1 .te
#
expand-type (TUPLE-OF .name .ts0) (TUPLE-OF .name .ts1) .te
	:- !, expand-types .ts0 .ts1 .te
#
expand-type .t .t _ #

expand-types () () _ :- ! #
expand-types (.t0, .ts0) (.t1, .ts1) .te
	:- expand-types .t0 .t1 .te
	, expand-types .ts0 .ts1 .te
#

equal-infer-types .a .b .ve/.te .type0
	:- infer-type .a .ve/.te .type0
	, infer-type .b .ve/.te .type1
	, equal-type .te .type0 .type1
#

equal-type _ .t .t #
equal-type .te .t0 .t1 :- equal-type0 .te .t0 .t1 #
equal-type .te .t0 .t1 :- equal-type0 .te .t1 .t0 #

equal-types _ () () :- ! #
equal-types .te (.t0, .ts0) (.t1, .ts1)
	:- equal-type .te .t0 .t1
	, equal-types .te .ts0 .ts1
#

equal-type0 .te (TYPE .name) .t0
	:- !, member .te .name/.t1, equal-type .te .t0 .t1
#
equal-type0 .te (FUN .pt0 .rt0) (FUN .pt1 .rt1)
	:- !, equal-type .te .pt0 .pt1, equal-type .te .rt0 .rt1
#
equal-type0 .te (ONE-OF .types) .t0
	:- !, member .types .t1, equal-type .te .t0 .t1
#
equal-type0 .te (LIST-OF .t0) (LIST-OF .t1)
	:- !, equal-type .te .t0 .t1 
#
equal-type0 .te (TUPLE-OF .name .ts0) (TUPLE-OF .name .ts1)
	:- equal-types .te .ts0 .ts1
#

default-fun-type cons (FUN .type (FUN (LIST-OF .type) (LIST-OF .type))) #
default-fun-type head (FUN (LIST-OF .type) .type) #
default-fun-type is-tree (FUN (LIST-OF .type) BOOLEAN) #
default-fun-type log (FUN _ (FUN .type .type)) #
default-fun-type tail (FUN (LIST-OF .type) (LIST-OF .type)) #

-- Special type primitive for corecursive lists
default-fun-type corecursive-cons (FUN .type (FUN (FUN _ .cl) .cl))
	:- .cl = CO-LIST-OF .type
#
default-fun-type head (FUN (CO-LIST-OF .type) .type) #
default-fun-type tail (FUN .cl (FUN _ .cl)) :- .cl = CO-LIST-OF .type #
