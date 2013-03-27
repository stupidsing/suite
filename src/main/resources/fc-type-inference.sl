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
infer-type-rule .p .env .tr/.tr .type
	:- find-simple-type .p .env .type, !
#
infer-type-rule (USING .lib .do) .env .tr/.tr .type
	:- !, load-precompiled-library .lib
	, infer-type-rule-using-libs (.lib,) .do .env .tr1 .type
	, resolve-types .tr1
#
infer-type-rule (OPTION CHECK-TUPLE-TYPE .do) .ue/.ve/.te/.oe .tr0/.trx .definedType
	:- !, infer-type-rule .do .ue/.ve/.te/.oe .tr0/.tr1 .tupleType
	, .tupleType = TUPLE-OF .name .types
	, once ( -- Enforces tuple name checking
		.definedType = TUPLE-OF .name .types1
		, member .oe .definedType/.oneOfType
		, .tr1 = (
			SUB-SUPER-TYPES .te/.oe .tupleType .definedType
		, .trx)
		; fc-error "Undefined tuple named" .name
	)
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
	OPTION ALLOW-RECURSIVE-DEFINITION DEF-VAR .name .value .do
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
	, infer-type-rule .callee .ue/.ve/.te/.oe .tr0/.tr1 .funType
	, infer-type-rule .param .ue/.ve/.te/.oe .tr1/.tr2 .actualParamType
	, .tr2 = (SUB-SUPER-TYPES .te/.oe (FUN-OF .signParamType .type) .funType
		, SUB-SUPER-TYPES .te/.oe .actualParamType .signParamType
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
	, .tr1 = (TYPE-IN-TYPES .type (NUMBER, STRING,), .trx)
	; member (' - ', ' * ', ' / ', ' %% ',) .oper, !
	, infer-compatible-types .left .right .env .tr0/.trx .type
	, .type = NUMBER
	; member (' = ', ' != ', ' > ', ' < ', ' >= ', ' <= ',) .oper, !
	, infer-compatible-types .left .right .env .tr0/.trx _
	, .type = BOOLEAN
#
infer-type-rule (TUPLE .name .elems) .env .tr (TUPLE-OF .name .types)
	:- !, infer-type-rules .elems .env .tr .types
#
infer-type-rule (OPTION (CAST .type) .do) .ue/.ve/.te/.oe .tr0/.trx .type
	:- !, infer-type-rule .do .ue/.ve/.te/.oe .tr0/.tr1 .type0
	, .tr1 = (SUB-SUPER-TYPES .te/.oe .type0 .type, .trx)
#
infer-type-rule (OPTION (AS .var .varType) .do) .ue/.ve/.te/.oe .tr .type
	:- !, member .ue .var/.varType
	, infer-type-rule .do .ue/.ve/.te/.oe .tr .type
#
infer-type-rule (OPTION _ .do) .env .tr .type
	:- !, infer-type-rule .do .env .tr .type
#
infer-type-rule (VARIABLE .var) .ue/.ve/.te/.oe .tr0/.trx .type
	:- (member .ve .var/.varType
		, !, .tr0 = (GEN-SPEC-TYPES .varType .type, .trx)
	)
	; !, fc-error "Undefined variable" .var
#

infer-type-rules () _ .tr/.tr () :- ! #
infer-type-rules (.e, .es) .env .tr0/.trx (.t, .ts)
	:- infer-type-rule .e .env .tr0/.tr1 .t
	, infer-type-rules .es .env .tr1/.trx .ts
#

find-simple-type (FUN .var .do) .ue/.ve/.te/.oe (FUN-OF .varType .type)
	:- infer-type-rule .do (.var/.varType, .ue)/.ve/.te/.oe .tr .type
	, resolve-types .tr
#
find-simple-type (CONSTANT _) _ _ #
find-simple-type (BOOLEAN _) _ BOOLEAN #
find-simple-type (NUMBER _) _ NUMBER #
find-simple-type (STRING _) _ STRING #
find-simple-type (TUPLE () ()) _ (LIST-OF _) #
find-simple-type (OPTION NO-TYPE-CHECK _) _ _ #
find-simple-type (VARIABLE .var) .ue/.ve/.te/.oe .type
	:- member .ue .var/.type
	; default-fun-type .var .type
#

infer-compatible-types .a .b .ue/.ve/.te/.oe .tr0/.trx .type
	:- infer-type-rule .a .ue/.ve/.te/.oe .tr0/.tr1 .type0
	, infer-type-rule .b .ue/.ve/.te/.oe .tr1/.tr2 .type1
	, .tr2 = (SUB-SUPER-TYPES .te/.oe .type0 .type
		, SUB-SUPER-TYPES .te/.oe .type1 .type
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
resolve-types0 (SUB-SUPER-TYPES .env .t0 .t1, .tr1)/.trx
	:- !, resolve-sub-super-types .env .t0 .t1 .tr1/.trx
#
resolve-types0 (GEN-SPEC-TYPES .t0 .t1, .tr1)/.trx
	:- !, clone .t0 .t1, resolve-types0 .tr1/.trx
#
resolve-types0 (TYPE-IN-TYPES .t .ts, .tr1)/.trx
	:- !, member .ts .t, resolve-types0 .tr1/.trx
#
resolve-types0 (REMARK, .tr1)/.trx :- !, resolve-types0 .tr1/.trx #
resolve-types0 (.a, .tr1)/.tr2 -- Shuffles the first one to the back
	:- !, .tr2 = (.a, .trx), resolve-types0 .tr1/.trx
#
resolve-types0 _ :- !, fc-error "Not enough type information" #

resolve-sub-super-types _ .t .t .tr
	:- resolve-types0 .tr
#
resolve-sub-super-types .env .t0 .tx .tr
	:- bound .t0
	, sub-super-type-pair .env .t0 .t1
	, resolve-sub-super-types .env .t1 .tx .tr
	; bound .tx
	, sub-super-type-pair .env .t1 .tx
	, resolve-sub-super-types .env .t0 .t1 .tr
#
resolve-sub-super-types .env .t0 .t1 .tr
	:- bound .t1
	, .t1 = INSTANCE-OF .typeParam .gt
	, sub-super-type-pair .env (GENERIC-OF .typeVar .type) .gt
	, replace .type/.t0 .typeVar/.typeParam
	. resolve-types0 .tr
#
resolve-sub-super-types .env .t0 .t1 .tr1/.trx
	:- (bound .t0; bound .t1), !
	, .t0 = TUPLE-OF _, .t1 = TUPLE-OF _
	, children-of-type .t0 .t1 .ts0/() .ts1/()
	, sub-super-type-pairs .env .ts0 .ts1 .trx/.trxx
	, resolve-types0 .tr1/.trxx
#

sub-super-type-pair .te/_ .t (TYPE .name) :- member .te .name/.t #
sub-super-type-pair _/.oe .t0 .t1 :- member .oe .t0/.t1 #
sub-super-type-pair .env .t0 .t1
	:- bound .t1
	, .t1 = INSTANCE-OF .typeParam .gt
	, sub-super-type-pair .env (GENERIC-OF .typeVar .type) .gt
	, replace .type/.t0 .typeVar/.typeParam
#
sub-super-type-pair _ .t0 .t1
	:- bound .t1
	, .t1 = GENERIC-OF .typeVar .type
	, replace .type/.t0 .typeVar/_
#

sub-super-type-pairs _ () () .tr/.tr :- ! #
sub-super-type-pairs .env (.t0, .ts0) (.t1, .ts1) .tr0/.trx
	:- .tr0 = (SUB-SUPER-TYPES .env .t0 .t1, .tr1)
	, sub-super-type-pairs .env .ts0 .ts1 .tr1/.trx
#

children-of-type (FUN-OF .pt0 .rt0) (FUN-OF .pt1 .rt1) .p0/.px .q0/.qx
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
children-of-type (GENERIC-OF .tv .t0) (GENERIC-OF .tv .t1) .p0/.px .q0/.qx
	:- !
	, replace .t0/.gt0 .tv/_
	, replace .t1/.gt1 .tv/_
	, .p0 = (.gt0, .px), .q0 = (.gt1, .qx)
#
children-of-type .t .t .p/.p .q/.q #

children-of-types () () .p/.p .q/.q :- ! #
children-of-types (.t0, .ts0) (.t1, .ts1) .p0/.px .q0/.qx
	:- .p0 = (.t0, .p1), .q0 = (.t1, .q1)
	, children-of-types .ts0 .ts1 .p1/.px .q1/.qx
#

default-fun-type () (LIST-OF _) #
default-fun-type _cons (FUN-OF .type (FUN-OF (LIST-OF .type) (LIST-OF .type))) #
default-fun-type _lhead (FUN-OF (LIST-OF .type) .type) #
default-fun-type _log (FUN-OF .type .type) #
default-fun-type _log2 (FUN-OF (LIST-OF NUMBER) (FUN-OF .type .type)) #
default-fun-type _ltail (FUN-OF (LIST-OF .type) (LIST-OF .type)) #
default-fun-type _popen (FUN-OF (LIST-OF NUMBER) (FUN-OF (LIST-OF NUMBER) (LIST-OF NUMBER))) #
default-fun-type _prove (FUN-OF _ BOOLEAN) #
default-fun-type _subst (FUN-OF _ (FUN-OF _ _)) #
default-fun-type _thead (FUN-OF (TUPLE-OF _ (.type, _)) .type) #
default-fun-type _ttail (FUN-OF (TUPLE-OF .n (_, .types)) (TUPLE-OF .n .types)) #
default-fun-type fflush (FUN-OF .type .type) #
default-fun-type fgetc (FUN-OF _ (FUN-OF NUMBER NUMBER)) #
default-fun-type fputc (FUN-OF _ (FUN-OF NUMBER (FUN-OF NUMBER (FUN-OF .type .type)))) #
default-fun-type is-tree (FUN-OF (LIST-OF .type) BOOLEAN) #
default-fun-type log (FUN-OF .type .type) #
default-fun-type log2 (FUN-OF _ (FUN-OF .type .type)) #
