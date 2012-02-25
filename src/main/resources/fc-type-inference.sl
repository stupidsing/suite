-------------------------------------------------------------------------------
-- Type inference predicates
--
-- Environment consists of:
-- .ve - list of variables and their corresponding types
-- .te - list of type names and their corresponding types
-- .oe - list of tuples and their corresponding one-of types
-- .tr - type deduction rule to be assembled
--
infer-type .do .ve/.te/.oe .type
	:- infer-type-rule .do .ve/.te/.oe .tr0/.trx .type
	, resolve-types .tr0/.trx -- Resolve all types
#

infer-type-rule (CAST .type .do) .ve/.te/.oe .tr0/.trx .type
	:- !
	, find-one-of-type .type .oe1/.oe
	, infer-type-rule .do .ve/.te/.oe1 .tr0/.tr1 .type0
	, .tr1 = (SUPERTYPE-OF .te/.oe1 .type0 .type, .trx)
#
infer-type-rule (AS .var .varType .do) .ve/.te/.oe .tr .type
	:- !
	, find-one-of-type .varType .oe1/.oe
	, member .ve .var/.varType
	, infer-type-rule .do .ve/.te/.oe1 .tr .type
#
infer-type-rule (FUN .var .do) .ve/.te/.oe .tr/.tr (FUN .varType .type)
	:- !, infer-type-rule .do (.var/.varType, .ve)/.te/.oe .tr1 .type
	, resolve-types .tr1
#
infer-type-rule (DEF-TYPE .name .def .do) .ve/.te/.oe .tr .type
	:- !
	, find-one-of-type .def .oe1/.oe
	, infer-type-rule .do .ve/(.name/.def, .te)/.oe1 .tr .type
#
infer-type-rule (DEF-VAR .name .value .do) .ve/.te/.oe .tr0/.trx .type
	:- !
	, .env1 = (.name/.varType, .ve)/.te/.oe
	, once (infer-type-rule .value .env1 .tr0/.tr1 .varType
		; fc-error "at variable" .name
	)
	, infer-type-rule .do .env1 .tr1/.trx .type
#
infer-type-rule (INVOKE .param .callee) .ve/.te/.oe .tr0/.trx .type
	:- !
	, infer-type-rule .callee .ve/.te/.oe .tr0/.tr1 .funType
	, infer-type-rule .param .ve/.te/.oe .tr1/.tr2 .actualParamType
	, clone .funType (FUN .signParamType .type)
	, .tr2 = (SUPERTYPE-OF .te/.oe .actualParamType .signParamType
		, INSTANCE-OF .funType (FUN .signParamType .type)
		, .trx
	)
#
infer-type-rule (IF .if .then .else) .env .tr0/.trx .type
	:- !, infer-type-rule .if .env .tr0/.tr1 BOOLEAN
	, infer-compatible-types .then .else .env .tr1/.trx .type
#
infer-type-rule (TREE .oper .left .right) .env .tr0/.trx .type
	:- member (' + ',) .oper, !
	, infer-compatible-types .left .right .env .tr0/.tr1 .type
	, .tr1 = (EITHER .type (NUMBER, STRING,), .trx)
	; member (' + ', ' - ', ' * ', ' / ', ' %% ',) .oper, !
	, infer-compatible-types .left .right .env .tr0/.trx .type
	, .type = NUMBER
	; member (' = ', ' != ', ' > ', ' < ', ' >= ', ' <= ',) .oper, !
	, infer-compatible-types .left .right .env .tr0/.trx _
	, .type = BOOLEAN
#
infer-type-rule (BOOLEAN _) _ .tr/.tr BOOLEAN  :- ! #
infer-type-rule (NUMBER _) _ .tr/.tr NUMBER :- ! #
infer-type-rule (STRING _) _ .tr/.tr STRING :- ! #
infer-type-rule (TUPLE () ()) _ .tr/.tr (LIST-OF _) :- ! #
infer-type-rule (TUPLE .name .elems) .env .tr (TUPLE-OF .name .types)
	:- !, infer-type-rules .elems .env .tr .types
#
infer-type-rule (VARIABLE .var) .ve/.te/.oe .tr/.tr .type
	:- (member .ve .var/.type; default-fun-type .var .type), !
#
infer-type-rule (VARIABLE .var) _ _ _ :- !, fc-error "Undefined variable" .var #

infer-type-rules () _ .tr/.tr () :- ! #
infer-type-rules (.e, .es) .env .tr0/.trx (.t, .ts)
	:- infer-type-rule .e .env .tr0/.tr1 .t
	, infer-type-rules .es .env .tr1/.trx .ts
#

infer-compatible-types .a .b .ve/.te/.oe .tr0/.trx .type
	:- infer-type-rule .a .ve/.te/.oe .tr0/.tr1 .type0
	, infer-type-rule .b .ve/.te/.oe .tr1/.tr2 .type1
	, .tr2 = (SUPERTYPE-OF .te/.oe .type0 .type
		, SUPERTYPE-OF .te/.oe .type1 .type
		, .trx
	)
#

-- Finds a reverse-mapping of "one of" relation for looking up
find-one-of-type (ONE-OF .ts) .o0/.ox
	:- !, add-one-of-types (ONE-OF .ts) .ts .o0/.o1
	, find-one-of-types .ts .o1/.ox
#
find-one-of-type .t .o
	:- children-of-type .t .t .ts/() .ts/()
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

resolve-types .tr :- resolve-types0 .tr, ! #
resolve-types _ :- fc-error "Unable to resolve types" #

-- When resolving types:
-- - Flatten all logical-AND conditions;
-- - Do not resolve super-type relation when both types are not clear;
-- - Type instantiations (type object clones) comes at the end.
resolve-types0 .tr0/.trx :- same .tr0 .trx, ! #
--resolve-types0 .p/.q :- dd .p, nl, nl, fail #
resolve-types0 (SUPERTYPE-OF .env .t .t, .tr1)/.trx
	:- resolve-types0 .tr1/.trx
#
resolve-types0 (SUPERTYPE-OF .env .t0 .tx, .tr1)/.trx
	:- bound .t0
	, super-of-type .env .t0 .t1
	, resolve-types0 (SUPERTYPE-OF .env .t1 .tx, .tr1)/.trx
	; bound .tx
	, super-of-type .env .t1 .tx
	, resolve-types0 (SUPERTYPE-OF .env .t0 .t1, .tr1)/.trx
#
resolve-types0 (SUPERTYPE-OF .env .t0 .t1, .tr1)/.trx
	:- (bound .t0; bound .t1), !
	, .t0 = TUPLE-OF _, .t1 = TUPLE-OF _
	, children-of-type .t0 .t1 .ts0/() .ts1/()
	, super-of-types .env .ts0 .ts1 .trx/.trxx
	, resolve-types0 .tr1/.trxx
#
resolve-types0 (INSTANCE-OF .t0 .t1, .tr1)/.trx
	:- !, clone .t0 .t1, resolve-types0 .tr1/.trx
#
resolve-types0 (EITHER .t .ts, .tr1)/.trx
	:- !, member .ts .t, resolve-types0 .tr1/.trx
#
resolve-types0 (REMARK, .tr1)/.trx :- !, resolve-types0 .tr1/.trx #
resolve-types0 (.a, .tr1)/.tr2 -- Shuffles the first one to the back
	:- !, .tr2 = (.a, .trx), resolve-types0 .tr1/.trx
#
resolve-types0 _/_
	:- !, fc-error "Not enough type information"
#

super-of-type .te/_ .t0 (TYPE .name) :- member .te .name/.t0 #
super-of-type _/.oe .t0 .t1 :- member .oe .t0/.t1 #

super-of-types _ () () .tr/.tr :- ! #
super-of-types .env (.t0, .ts0) (.t1, .ts1) .tr0/.trx
	:- .tr0 = (SUPERTYPE-OF .env .t0 .t1, .tr1)
	, super-of-types .env .ts0 .ts1 .tr1/.trx
#

children-of-type (FUN .pt0 .rt0) (FUN .pt1 .rt1) .p0/.px .q0/.qx
	:- !, .p0 = (.pt0, .rt0, .px), .q0 = (.pt1, .rt1, .qx)
#
children-of-type (ONE-OF .ts0) (ONE-OF .ts1) .p .q
	:- !, children-of-types .ts0 .ts1 .p .q
#
children-of-type (LIST-OF .t0) (LIST-OF .t1) .p0/.px .q0/.qx
	:- !, .p0 = (.t0, .px), .q0 = (.t1, .qx)
#
children-of-type (TUPLE-OF .name .ts0) (TUPLE-OF .name .ts1) .p .q
	:- !, children-of-types .ts0 .ts1 .p .q
#
children-of-type .t .t .p/.p .q/.q #

children-of-types () () .p/.p .q/.q :- ! #
children-of-types (.t0, .ts0) (.t1, .ts1) .p0/.px .q0/.qx
	:- .p0 = (.t0, .p1), .q0 = (.t1, .q1)
	, children-of-types .ts0 .ts1 .p1/.px .q1/.qx
#

default-fun-type () (LIST-OF _) #
default-fun-type cons (FUN .type (FUN (LIST-OF .type) (LIST-OF .type))) #
default-fun-type head (FUN (LIST-OF .type) .type) #
default-fun-type is-tree (FUN (LIST-OF .type) BOOLEAN) #
default-fun-type log (FUN .type .type) #
default-fun-type log2 (FUN _ (FUN .type .type)) #
default-fun-type tail (FUN (LIST-OF .type) (LIST-OF .type)) #

-- Special type primitive for corecursive lists
default-fun-type corecursive-cons (FUN .type (FUN (FUN _ .cl) .cl))
	:- .cl = CO-LIST-OF .type
#
default-fun-type head (FUN (CO-LIST-OF .type) .type) #
default-fun-type tail (FUN .cl (FUN _ .cl)) :- .cl = CO-LIST-OF .type #
