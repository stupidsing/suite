-- Type inference predicate.
--
-- Environment consists of:
-- .ve - list of variables and their corresponding types
-- .te - list of type names and their corresponding types
-- .oe - list of tuples and their corresponding one-of types
--
infer-type .do .ve/.te/.oe .type :- infer-type0 .do .ve/.te/.oe .type #

infer-type0 (CAST .type .do) .ve/.te/.oe .type
	:- !
	, find-one-of-type .type .oe1/.oe
	, infer-type .do .ve/.te/.oe1 .type0
	, superset-of-type .ve/.te/.oe1 .type0 .type
#
infer-type0 (AS .var .varType .do) .ve/.te/.oe .type
	:- !
	, find-one-of-type .varType .oe1/.oe
	, member .ve .var/.varType
	, infer-type .do .ve/.te/.oe1 .type
#
infer-type0 (FUN .var .do) .ve/.te/.oe (FUN .varType .type)
	:- !, infer-type .do (.var/.varType, .ve)/.te/.oe .type
#
infer-type0 (DEF-TYPE .name .def .do) .ve/.te/.oe .type
	:- !
	, find-one-of-type .def .oe1/.oe
	, infer-type .do .ve/(.name/.def, .te)/.oe1 .type
#
infer-type0 (DEF-VAR .name .value .do) .ve/.te/.oe .type
	:- !
	, .env1 = (.name/.varType, .ve)/.te/.oe
	, (infer-type .value .env1 .varType
		; fc-error "Unable to infer type for" .name
	)
	, infer-type .do .env1 .type
#
infer-type0 (INVOKE .parameter .callee) .env .type
	:- !, infer-type .callee .env .funcType
	, infer-type .parameter .env .actualParamType
	, clone .funcType (FUN .signParamType .type)
	, superset-of-type .env .actualParamType .signParamType
#
infer-type0 (IF .if .then .else) .env .type
	:- !, infer-type .if .env BOOLEAN
	, compatible-infer-types .then .else .env .type
#
infer-type0 (TUPLE .name .elems) .env (TUPLE-OF .name .types)
	:- !, infer-types .elems .env .types
#
infer-type0 (TREE .oper .left .right) .env .type
	:- member (' + ',) .oper, !
	, compatible-infer-types .left .right .env .type
	, member (NUMBER, STRING,) .type
	; member (' + ', ' - ', ' * ', ' / ', ' %% ',) .oper, !
	, compatible-infer-types .left .right .env .type
	, .type = NUMBER
	; member (' = ', ' != ', ' > ', '  < ', ' >= ', ' <= ',) .oper, !
	, compatible-infer-types .left .right .env _
	, .type = BOOLEAN
#
infer-type0 (BOOLEAN _) _ BOOLEAN  :- ! #
infer-type0 (NUMBER _) _ NUMBER :- ! #
infer-type0 (STRING _) _ STRING :- ! #
infer-type0 (VARIABLE .var) .ve/.te/.oe .type
	:- (default-fun-type .var .type
		; member .ve .var/.type
	), !
#
infer-type0 (VARIABLE .var) _ _ :- !, fc-error "Undefined variable" .var #
infer-type0 (TUPLE .name .elems) .env (TUPLE-OF .name .types)
	:- !, infer-types .elems .types
#

infer-types () _ () :- ! #
infer-types (.e, .es) .env (.t, .ts)
	:- infer-type .e .env .t
	, infer-types .es .env .ts
#

compatible-infer-types .a .b .env .type
	:- infer-type .a .env .type0
	, infer-type .b .env .type1
	, (superset-of-type .env .type1 .type0, .type = .type0
		; superset-of-type .env .type0 .type1, .type = .type1
	)
#

-- Finds a reverse-mapping of "one of" relation for looking up
find-one-of-type (ONE-OF .ts) .o0/.ox
	:- !, add-one-of-types (ONE-OF .ts) .ts .o0/.o1
	, find-one-of-types .ts .o1/.ox
#
find-one-of-type .t .o
	:- children-of-type .t _ .ts/() _
	, find-one-of-types .ts .o
#

find-one-of-types () .o/.o :- ! #
find-one-of-types (.t, .ts) .o0/.ox
	:- find-one-of-type .t .o0/.o1
	, find-one-of-types .ts .o1/.ox
#

add-one-of-types _ () .o/.o #
add-one-of-types .oe (.t, .ts) .o0/.ox
	:- .o0 = (.t/.oe, .o1)
	, add-one-of-types .oe .ts .o1/.ox
# 

superset-of-type .env .t0 .t1
	:- children-of-type .t0 .t1 .ts0/() .ts1/()
	, superset-of-types .env .ts0 .ts1
#
superset-of-type .env .t0 .t2
	:- bound .t0, superset-of-type0 .env .t0 .t1, superset-of-type .env .t1 .t2
#
superset-of-type .env .t0 .t2
	:- bound .t2, superset-of-type0 .env .t1 .t2, superset-of-type .env .t0 .t1
#

superset-of-type0 _/.te/_ .t0 (TYPE .name) :- member .te .name/.t0 #
superset-of-type0 _/_/.oe .t0 .t1 :- member .oe .t0/.t1 #

superset-of-types _ () () :- ! #
superset-of-types .env (.t0, .ts0) (.t1, .ts1)
	:- superset-of-type .env .t0 .t1
	, superset-of-types .env .ts0 .ts1
#

children-of-types () () .p/.p .q/.q :- ! #
children-of-types (.t0, .ts0) (.t1, .ts1) .p0/.px .q0/.qx
	:- .p0 = (.t0, .p1), .q0 = (.t1, .q1)
	, children-of-types .ts0 .ts1 .p1/.px .q1/.qx
#

children-of-type (FUN .pt0 .rt0) (FUN .pt1 .rt1) .p0/.px .q0/.qx
	:- !, .p0 = (.pt0, .rt0, .px), .q0 = (.pt1, .rt1, .qx)
#
children-of-type (.tag .ts0) (.tag .ts1) .p .q
	:- member (ONE-OF, LIST-OF,) .tag, !
	, children-of-types .ts0 .ts1 .p .q
#
children-of-type (TUPLE-OF .name .ts0) (TUPLE-OF .name .ts1) .p .q
	:- !, children-of-types .ts0 .ts1 .p .q
#
children-of-type .t .t .p/.p .q/.q #

default-fun-type () (LIST-OF _) #
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
