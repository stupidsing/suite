-------------------------------------------------------------------------------
-- Type inference predicates
--
-- Environment consists of:
-- .ue - dictionary of inside variables / their corresponding types
-- .ve - dictionary of outside variables / their corresponding types
-- .te - list of types / their corresponding belonging classes
-- .tr - type deduction rule to be assembled
--
-- Inside variables include parent function definitions and parameter variables
-- that do not need type specialization.
-- Outside variables are local variables that require type specialization.
--
infer-type-rules () _ .tr/.tr () :- ! #
infer-type-rules (.e, .es) .env .tr0/.trx (.t, .ts)
	:- infer-type-rule .e .env .tr0/.tr1 .t
	, infer-type-rules .es .env .tr1/.trx .ts
#

infer-type-rule .p .env .tr/.tr .type
	:- find-simple-type .p .env .type, !
#
infer-type-rule (USING .lib .do) .env .tr/.tr .type
	:- !, load-precompiled-library .lib
	, infer-type-rule-using-libs (.lib,) .do .env .tr1 .type
	, resolve-types .tr1
#
infer-type-rule (
	OPTION (DEF-TYPE .definedType .classes .typeVars) .do
) .ue/.ve/.te .tr .type
	:- !
	, .te1 = (.definedType/.classes/.typeVars, .te)
	, infer-type-rule .do .ue/.ve/.te1 .tr .type
#
infer-type-rule (DEF-VAR .name .value .do) .ue/.ve/.te .tr0/.trx .type
	:- !
	, fc-dict-add .name/.varType .ue/.ue1
	, .env1 = .ue1/.ve/.te
	, once (infer-type-rule .value .env1 .tr0/.tr1 .varType
		; fc-error "at variable" .name
	)
	, infer-type-rule .do .env1 .tr1/.trx .type
#
infer-type-rule (
	OPTION ALLOW-RECURSIVE-DEFINITION DEF-VAR .name .value .do
) .ue/.ve/.te .tr0/.trx .type
	:- !
	, fc-dict-add .name/.varType .ue/.ue1
	, fc-dict-add .name/.varType .ve/.ve1
	, .insideEnv = .ue1/.ve/.te
	, .outsideEnv = .ue/.ve1/.te
	, once (infer-type-rule .value .insideEnv .tr0/.tr1 .varType
		; fc-error "at variable" .name
	)
	, infer-type-rule .do .outsideEnv .tr1/.trx .type
#
infer-type-rule (INVOKE .param .callee) .ue/.ve/.te .tr0/.trx .type
	:- !
	, infer-type-rule .callee .ue/.ve/.te .tr0/.tr1 .funType
	, infer-type-rule .param .ue/.ve/.te .tr1/.tr2 .actualParamType
	, .tr2 = (SUB-SUPER-TYPES .te (FUN-OF .signParamType .type) .funType
		, SUB-SUPER-TYPES .te .actualParamType .signParamType
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
infer-type-rule (OPTION (CAST .dir .type) .do) .ue/.ve/.te .tr0/.trx .type
	:- !, infer-type-rule .do .ue/.ve/.te .tr0/.tr1 .type0
	, once (
		.dir = DOWN, .subType = .type0, .superType = .type
		; .dir = UP, .subType = .type, .superType = .type0
	)
	, .tr1 = (SUB-SUPER-TYPES .te .subType .superType, .trx)
#
infer-type-rule (OPTION (AS .var .varType) .do) .ue/.ve/.te .tr .type
	:- !, fc-dict-get .ue .var/.varType
	, infer-type-rule .do .ue/.ve/.te .tr .type
#
infer-type-rule (OPTION _ .do) .env .tr .type
	:- !, infer-type-rule .do .env .tr .type
#
infer-type-rule (VARIABLE .var) .ue/.ve/.te .tr0/.trx .type
	:- (fc-dict-get .ve .var/.varType
		, !, .tr0 = (SPEC-GEN-TYPES .type .varType, .trx)
	)
	; !, fc-error "Undefined variable" .var
#

find-simple-type (FUN .var .do) .ue/.ve/.te (FUN-OF .varType .type)
	:- fc-dict-add .var/.varType .ue/.ue1
	, infer-type-rule .do .ue1/.ve/.te .tr .type
	, resolve-types .tr
#
find-simple-type (CONSTANT _) _ _ #
find-simple-type (BOOLEAN _) _ BOOLEAN #
find-simple-type (NUMBER _) _ NUMBER #
find-simple-type (STRING _) _ STRING #
find-simple-type (TUPLE () ()) _ (LIST-OF _) #
find-simple-type (OPTION NO-TYPE-CHECK _) _ _ #
find-simple-type (VARIABLE .var) .ue/.ve/.te .type
	:- fc-dict-get .ue .var/.type
	; default-fun-type .var .type
#

infer-compatible-types .a .b .ue/.ve/.te .tr0/.trx .type
	:- infer-type-rule .a .ue/.ve/.te .tr0/.tr1 .type0
	, infer-type-rule .b .ue/.ve/.te .tr1/.tr2 .type1
	, .tr2 = (SUB-SUPER-TYPES .te .type0 .type
		, SUB-SUPER-TYPES .te .type1 .type
		, .trx
	)
#

resolve-types .tr :- resolve-types0 .tr, ! #
resolve-types _ :- fc-error "Unable to resolve types" #

-- When resolving types:
-- - Flatten all logical-AND conditions;
-- - Try bind equivalent sub-type to super-type relations;
-- - Try bind specialized type to generic type;
-- - Do not resolve super-type relation when both types are not clear;
-- - Morph children types if still cannot resolve.
resolve-types0 .tr0/.trx :- same .tr0 .trx, ! #
resolve-types0 (SUB-SUPER-TYPES .te .t0 .t1, .tr1)/.trx
	:- !, resolve-sub-super-types .te .t0 .t1 .tr1/.trx
#
resolve-types0 (SPEC-GEN-TYPES .t0 .t1, .tr1)/.trx
	:- !, clone .t1 .t0, resolve-types0 .tr1/.trx
#
resolve-types0 (TYPE-IN-TYPES .t .ts, .tr1)/.trx
	:- !, member .ts .t, resolve-types0 .tr1/.trx
#
resolve-types0 (.a, .tr1)/.tr2 -- Shuffles the first one to the back
	:- !, .tr2 = (.a, .trx), resolve-types0 .tr1/.trx
#
resolve-types0 _ :- !, fc-error "Not enough type information" #

resolve-sub-super-types _ .t .t .tr
	:- resolve-types0 .tr
#
resolve-sub-super-types .te .t0 (GENERIC-OF .typeVar .type) .tr
	:- bound .typeVar
	, replace .type/.t0 .typeVar/_
#
resolve-sub-super-types .te .t0 .tx .tr
	:- bound .t0, !
	, sub-super-type-pair .te .t0 .t1
	, resolve-sub-super-types .te .t1 .tx .tr
	; bound .tx, !
	, sub-super-type-pair .te .t1 .tx
	, resolve-sub-super-types .te .t0 .t1 .tr
#

sub-super-type-pair .te .type1 .class1 -- reduce to type classes
	:- member .te .type/.classes/.typeVars
	, member .classes .class
	, instantiate-type .typeVars .type/.class .type1/.class1
#
sub-super-type-pair .te .t0 .t1 -- morph children types to their super
	:- once (bound .t0; bound .t1)
	, children-of-type .t0 .t1 .ts/()
	, choose-one-pair .ts .childType0/.childType1
	, sub-super-type-pair .te .childType0 .childType1
#

sub-super-type-pair-list _ () .tr/.tr :- ! #
sub-super-type-pair-list .te (.t0/.t1, .ts) .tr0/.trx
	:- .tr0 = (SUB-SUPER-TYPES .te .t0 .t1, .tr1)
	, sub-super-type-pair-list .te .ts .tr1/.trx
#

choose-one-pair (.t0/.t1, _) .t0/.t1 #
choose-one-pair (.t/.t, .ts) .tr :- choose-one-pair .ts .tr #

instantiate-type () .tc .tc #
instantiate-type (.typeVar, .typeVars) .tc0 .tcx
	:- replace  .tc0/.tc1 .typeVar/_
	, instantiate-type .typeVars .tc1 .tcx
#

children-of-types () () .pq/.pq :- ! #
children-of-types (.t0, .ts0) (.t1, .ts1) .pq0/.pqx
	:- .pq0 = (.t0/.t1, .pq1)
	, children-of-types .ts0 .ts1 .pq1/.pqx
#

children-of-type (FUN-OF .pt0 .rt0) (FUN-OF .pt1 .rt1) .pq0/.pqx
	:- !, .pq0 = (.pt0/.pt1, .rt0/.rt1, .pqx)
#
children-of-type (LIST-OF .t0) (LIST-OF .t1) .pq0/.pqx
	:- !, .pq0 = (.t0/.t1, .pqx)
#
children-of-type (TUPLE-OF .name .ts0) (TUPLE-OF .name .ts1) .pq
	:- !, children-of-types .ts0 .ts1 .pq
#
children-of-type .t .t .pq/.pq #

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
default-fun-type error _ #
default-fun-type fflush (FUN-OF .type .type) #
default-fun-type fgetc (FUN-OF _ (FUN-OF NUMBER NUMBER)) #
default-fun-type fputc (FUN-OF _ (FUN-OF NUMBER (FUN-OF NUMBER (FUN-OF .type .type)))) #
default-fun-type is-tree (FUN-OF (LIST-OF .type) BOOLEAN) #
default-fun-type is-tuple (FUN-OF (TUPLE-OF _ (_, _)) BOOLEAN) #
default-fun-type log (FUN-OF .type .type) #
default-fun-type log2 (FUN-OF _ (FUN-OF .type .type)) #
