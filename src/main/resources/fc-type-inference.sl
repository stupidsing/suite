infer-type .do .ve/.te/.oe .type :- infer-type0 .do .ve/.te/.oe .type #

infer-types () _ () :- ! #
infer-types (.e, .es) .env (.t, .ts)
	:- infer-type .e .env .t
	, infer-types .es .env .ts
#

infer-type0 (AS .var .varType0 .do) .ve/.te/.oe .type
	:- !
	, find-one-of-type .varType0 .oe1/.oe
	, member .ve .var/.varType1
	, equal-type .te/.oe .varType0 .varType1
	, infer-type .do (.var/.varType, .ve)/.te/.oe1 .type
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
	, once (infer-type .value .env1 .varType
		; fc-error "Unable to infer type for" .name
	)
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
infer-type0 (VARIABLE .var) .ve/.te/.oe .type :- member .ve .var/.type, ! #
infer-type0 (TUPLE .name .elems) .env (TUPLE-OF .name .types)
	:- !, infer-types .elems .types
#

equal-infer-types .a .b .ve/.te/.oe .type0
	:- infer-type .a .ve/.te/.oe .type0
	, infer-type .b .ve/.te/.oe .type1
	, equal-type .te/.oe .type0 .type1
#

-- Finds a reverse-mapping of "one of" relation for looking up
find-one-of-types () .o/.o :- ! #
find-one-of-types (.t, .ts) .o0/.ox
	:- find-one-of-type .ts .o0/.o1
	, find-one-of-types .t .o1/.ox
#

find-one-of-type (TYPE _) .o/.o :- ! #
find-one-of-type (FUN .pt .rt) .o0/.ox
	:- !, find-one-of-type .pt .o0/.o1 , find-one-of-type .rt .o1/.ox
#
find-one-of-type (ONE-OF .ts) .o
	:- !, temp .t
	, concat "$$TEMP-ONE-OF" .temp .token -- Assigns a temporary name
	, add-one-of-types (TYPE .token) .ts .o
#
find-one-of-type (LIST-OF .t) .o :- !, find-one-of-type .t .o #
find-one-of-type (TUPLE-OF _ .ts) .o :- !, find-one-of-types .ts .o #
find-one-of-type _ .o/.o #

add-one-of-types .oe () .o/.o # 
add-one-of-types .oe (.t, .ts) .o0/.ox
	:- .o0 = (.t/.oe, .o1)
	, add-one-of-types .oe .ts .o1/.ox
# 

equal-types _ () () :- ! #
equal-types .env (.t0, .ts0) (.t1, .ts1)
	:- equal-type .env .t0 .t1
	, equal-types .env .ts0 .ts1
#

equal-type _ .t .t #
equal-type .env .t0 .t1 :- equal-type0 .env .t0 .t1 #
equal-type .env .t0 .t1 :- equal-type0 .env .t1 .t0 #

equal-type0 .te/.oe .t0 .t1
	:- member .oe .t0/.o0, equal-type .te/.oe .o0 .t1
#
equal-type0 .te/.oe (TYPE .name) .t0
	:- !, member .te .name/.t1, equal-type .te/.oe .t0 .t1
#
equal-type0 .env (FUN .pt0 .rt0) (FUN .pt1 .rt1)
	:- !, equal-type .env .pt0 .pt1, equal-type .env .rt0 .rt1
#
equal-type0 .env (ONE-OF .types) .t0
	:- !, member .types .t1, equal-type .env .t0 .t1
#
equal-type0 .env (LIST-OF .t0) (LIST-OF .t1)
	:- !, equal-type .env .t0 .t1 
#
equal-type0 .env (TUPLE-OF .name .ts0) (TUPLE-OF .name .ts1)
	:- equal-types .env .ts0 .ts1
#

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
