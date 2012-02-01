infer-type .do .env .type :- infer-type0 .do .env .type #

infer-type0 (VAR-TYPE .var .varType .do) .ve/.te .type
	:- !, infer-type .do (.var/.varType, .ve)/.te .type
#
infer-type0 (FUN .var .do) .ve/.te (FUN .varType .type)
	:- !, infer-type .do (.var/.varType, .ve)/.te .type
#
infer-type0 (DEF-TYPE .type .def .do) .ve/.te .type
	:- !, infer-type .do .ve/(.type/.def, .te) .type
#
infer-type0 (DEF-VAR .var .value .do) .ve/.te .type
	:- !, .env1 = (.var/.varType, .ve)/.te
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
	:- !, infer-types .elems .types
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
infer-type0 EMPTY _ (LIST-OF _) #

infer-types () () :- ! #
infer-types (.elem, .elems) (.type, .types)
	:- infer-type .elem .type, infer-types .elems .types
#

equal-infer-types .a .b .env .type
	:- infer-type .a .env .type, infer-type .b .env .type
#

equal-types .t0 .t1 .te :- equal-types0 .t0 .t1 .te #
equal-types .t0 .t1 .te :- equal-types0 .t1 .t0 .te #

equal-types0 .t .t _ #
equal-types0 .t0 .t1 .te
	:- atom .t0, .t1 = _ _
	, member .te .t0/.tr0
	, equal-types .tr0 .t1 .te
#
equal-types0 (FUN .pt0 .rt0) (FUN .pt1 .rt1)
	:- !, equal-type .pt0 .pt1, equal-type .rt0 .rt1
#
equal-types0 .t (ONE-OF .types1)
	:- !, member .types .type, equal-types .t .type
#
equal-types0 (LIST-OF .t0) (LIST-OF .t1) :- !, equal-types .t0 .t1 #
equal-types0 (TUPLE-OF .name .ts0) (TUPLE-OF .name .ts1)
	:- equal-type-lists .ts0 .ts1
#

equal-type-lists () () :- ! #
equal-type-lists (.t0, .ts0) (.t1, .ts1)
	:- equal-types .t0 .t1, equal-type-lists .ts0 .ts1
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
