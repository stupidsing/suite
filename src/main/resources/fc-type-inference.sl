-------------------------------------------------------------------------------
-- Type inference predicates
--
-- Environment consists of:
-- .ue - list of inside variables and their corresponding types
-- .ve - list of outside variables and their corresponding types
-- .te - list of type names and their corresponding types
-- .oe - list of tuples and their corresponding one-of types
-- .tr - type deduction rule to be assembled
--
-- Inside variables include parent function definitions and parameter variables
-- that do not need type specialization.
-- Outside variables are local variables that require type specialization.
--
infer-type-rule (FUN .var .do) .ue/.ve/.te/.oe .tr/.tr (FUN .varType .type)
	:- !, infer-type-rule .do (.var/.varType, .ue)/.ve/.te/.oe .tr1 .type
	, resolve-types .tr1
#
infer-type-rule (OPTION (DEF-ONE-OF-TYPE .def) .do) .ue/.ve/.te/.oe .tr .type
	:- !, find-one-of-type .def .oe1/.oe
	, infer-type-rule .do .ue/.ve/.te/.oe1 .tr .type
#
infer-type-rule (OPTION (DEF-TYPE .name .def) .do) .ue/.ve/.te/.oe .tr .type
	:- !
	, infer-type-rule .do .ue/.ve/(.name/.def, .te)/.oe .tr .type
#
infer-type-rule (DEF-VAR .name .value .do) .ue/.ve/.te/.oe .tr0/.trx .type
	:- !
	, .env1 = (.name/.varType, .ue)/.ve/.te/.oe
	, once (infer-type-rule .value .env1 .tr0/.tr1 .varType
		; fc-error "at variable" .name
	)
	, infer-type-rule .do .env1 .tr1/.trx .type
#
infer-type-rule (
	OPTION GENERIC-TYPE DEF-VAR .name .value .do
) .ue/.ve/.te/.oe .tr0/.trx .type
	:- !
	, .insideEnv = (.name/.varType, .ue)/.ve/.te/.oe
	, .outsideEnv = .ue/(.name/.varType, .ve)/.te/.oe
	, once (infer-type-rule .value .insideEnv .tr0/.tr1 .varType
		; fc-error "at variable" .name
	)
	, infer-type-rule .do .outsideEnv .tr1/.trx .type
#
infer-type-rule (INVOKE .param .callee) .ue/.ve/.te/.oe .tr0/.trx .type
	:- !
	, infer-type-rule .callee .ue/.ve/.te/.oe .tr0/.tr1 (FUN .signParamType .type)
	, infer-type-rule .param .ue/.ve/.te/.oe .tr1/.tr2 .actualParamType
	, .tr2 = (SUPERTYPE-OF .te/.oe .actualParamType .signParamType, .trx)
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
infer-type-rule (CONSTANT _) _ .tr/.tr _ :- ! #
infer-type-rule (BOOLEAN _) _ .tr/.tr BOOLEAN  :- ! #
infer-type-rule (NUMBER _) _ .tr/.tr NUMBER :- ! #
infer-type-rule (STRING _) _ .tr/.tr STRING :- ! #
infer-type-rule (TUPLE () ()) _ .tr/.tr (LIST-OF _) :- ! #
infer-type-rule (TUPLE .name .elems) .ue/.ve/.te/.oe .tr .type
	:- !, infer-type-rules .elems .ue/.ve/.te/.oe .tr .types
	, .type = TUPLE-OF .name .types
	, !, (.name = $$ANON
		; member .oe (TUPLE-OF .name _)/_ -- Enforces tuple name checking
		; fc-error "undefined tuple named " .name
	), !
#
infer-type-rule (OPTION (CAST .type) .do) .ue/.ve/.te/.oe .tr0/.trx .type
	:- !, infer-type-rule .do .ue/.ve/.te/.oe .tr0/.tr1 .type0
	, .tr1 = (SUPERTYPE-OF .te/.oe .type0 .type, .trx)
#
infer-type-rule (OPTION (AS .var .varType) .do) .ue/.ve/.te/.oe .tr .type
	:- !, member .ue .var/.varType
	, infer-type-rule .do .ue/.ve/.te/.oe .tr .type
#
infer-type-rule (OPTION NO-TYPE-CHECK _) _ .tr/.tr _ :- ! #
infer-type-rule (OPTION _ .do) .env .tr .type
	:- !, infer-type-rule .do .env .tr .type
#
infer-type-rule (VARIABLE .var) .ue/.ve/.te/.oe .tr0/.trx .type
	:- (member .ue .var/.type, .tr0 = .trx
		; member .ve .var/.varType, .tr0 = (INSTANCE-OF .varType .type, .trx)
		; default-fun-type .var .type, .tr0 = .trx
	), !
#
infer-type-rule (VARIABLE .var) _ _ _ :- !, fc-error "Undefined variable" .var #

infer-type-rules () _ .tr/.tr () :- ! #
infer-type-rules (.e, .es) .env .tr0/.trx (.t, .ts)
	:- infer-type-rule .e .env .tr0/.tr1 .t
	, infer-type-rules .es .env .tr1/.trx .ts
#

infer-compatible-types .a .b .ue/.ve/.te/.oe .tr0/.trx .type
	:- infer-type-rule .a .ue/.ve/.te/.oe .tr0/.tr1 .type0
	, infer-type-rule .b .ue/.ve/.te/.oe .tr1/.tr2 .type1
	, .tr2 = (SUPERTYPE-OF .te/.oe .type0 .type
		, SUPERTYPE-OF .te/.oe .type1 .type
		, .trx
	)
#

-- Finds a reverse-mapping of "one of" relation for looking up
find-one-of-type .t .o/.o :- not bound .t, ! #
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
-- - Try bind equivalent types to super-type relations, if possible;
-- - Do not resolve super-type relation when both types are not clear;
-- - Type instantiations (type object clones) comes at the end.
resolve-types0 .tr0/.trx :- same .tr0 .trx, ! #
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
default-fun-type _cons (FUN .type (FUN (LIST-OF .type) (LIST-OF .type))) #
default-fun-type _head (FUN (LIST-OF .type) .type) #
default-fun-type _prove (FUN _ BOOLEAN) #
default-fun-type _subst (FUN _ (FUN _ _)) #
default-fun-type _tail (FUN (LIST-OF .type) (LIST-OF .type)) #
default-fun-type fflush (FUN .type .type) #
default-fun-type fgetc (FUN NUMBER NUMBER) #
default-fun-type fputc (FUN NUMBER (FUN NUMBER (FUN .type .type))) #
default-fun-type is-tree (FUN (LIST-OF .type) BOOLEAN) #
default-fun-type log (FUN .type .type) #
default-fun-type log2 (FUN _ (FUN .type .type)) #
