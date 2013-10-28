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
-- Kinds of generic types:
-- - Generic type class, usually used in abstract data structures.
--   Written like B-TREE/:t.
--   Represented internally as (CLASS (PARAMETERIZED (VAR t) B-TREE)).
--   Resolved by binding the type structures.
-- - Generic type, usually used in method signatures.
--   Written like :t :- .t => .t.
--   Represented internally as (GENERIC-OF (VAR t) FUN-OF (VAR t) (VAR t)).
--   Resolved by SUB-SUPER-TYPES.
-- - Generic type caused by not enough variable information during type inference.
--   Any variable usage, in case having unbinded variables, will also be cloned.
--   Resolved by CLONE-TO-FROM-TYPES.
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
	, infer-type-rule-using-lib .lib .do .env .tr1/() .type
	, resolve-type-rules .tr1
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
	OPTION ALLOW-RECURSIVE (DEF-VAR .name .value .do)
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
infer-type-rule (OPTION CAST-TO-CLASS .pair) .env .tr .classType
	:- !
	, .classType = CLASS _
	, infer-type-rule (OPTION (CAST DOWN .classType) .pair) .env .tr .classType
#
infer-type-rule (FUN .var .do) .ue/.ve/.te .tr (FUN-OF .varType .type)
	:- !
	, fc-dict-add .var/.varType .ue/.ue1
	, infer-type-rule .do .ue1/.ve/.te .tr .type
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
	:- !
	, infer-type-rule .if .env .tr0/.tr1 BOOLEAN
	, infer-compatible-types .then .else .env .tr1/.trx .type
#
infer-type-rule (TREE .oper .left .right) .env .tr0/.trx .type
	:- member (' + ',) .oper, !
	, infer-compatible-types .left .right .env .tr0/.tr1 .type
	, .tr1 = (TYPE-IN-TYPES .type (NUMBER,), .trx)
	; member (' - ', ' * ', ' / ', ' %% ',) .oper, !
	, infer-compatible-types .left .right .env .tr0/.trx .type
	, .type = NUMBER
	; member (' = ', ' != ', ' > ', ' < ', ' >= ', ' <= ',) .oper, !
	, infer-compatible-types .left .right .env .tr0/.trx _
	, .type = BOOLEAN
#
infer-type-rule (PAIR .v0 .v1) .env .tr0/.trx (PAIR-OF .t0 .t1)
	:- !
	, infer-type-rule .v0 .env .tr0/.tr1 .t0
	, infer-type-rule .v1 .env .tr1/.trx .t1
#
infer-type-rule (OPTION (CAST .dir .type) .do) .ue/.ve/.te .tr0/.trx .type
	:- !, infer-type-rule .do .ue/.ve/.te .tr0/.tr1 .type0
	, once (
		.dir = DOWN, .subType = .type0, .superType = .type
		; .dir = UP, .subType = .type, .superType = .type0
	)
	, .tr1 = (SUB-SUPER-TYPES .te .subType .superType, .trx)
#
infer-type-rule (OPTION RESOLVE-TYPE .do) .env .tr/.tr .type
	:- !
	, infer-type-rule .do .env .tr1/() .type
	, resolve-type-rules .tr1
#
infer-type-rule (OPTION _ .do) .env .tr .type
	:- !
	, infer-type-rule .do .env .tr .type
#
infer-type-rule (VAR .var) .ue/.ve/.te .tr0/.trx .type
	:- (fc-dict-get .ve .var/.varType
		, !, .tr0 = (CLONE-TO-FROM-TYPES .type .varType, .trx)
	)
	; !, fc-error "Undefined variable" .var
#

find-simple-type (CONSTANT _) _ _ #
find-simple-type (ATOM ()) _ (LIST-OF _) #
find-simple-type (ATOM .a) _ (ATOM-OF .a) #
find-simple-type (BOOLEAN _) _ BOOLEAN #
find-simple-type (DO _) _ (DO-OF _) #
find-simple-type (NUMBER _) _ NUMBER #
find-simple-type (OPTION NO-TYPE-CHECK _) _ _ #
find-simple-type (VAR .var) .ue/.ve/.te .type
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

resolve-type-rules .tr
	:- once (not is.cyclic .tr; fc-error "Cyclic types")
	, once (resolve-type-rules0 .tr; fc-error "Unmatched types")
#

resolve-type-rules0 .tr
	:- once (sort-resolve-type-rules .tr .ps .nps)
	, (.ps = (), !, resolve-type-rules1 .tr
		; resolve-type-rules1 .ps, resolve-type-rules0 .nps
	)
#

-- Sort the resolve type rules by easiness
sort-resolve-type-rules () () () :- ! #
sort-resolve-type-rules (.tr, .trs) (.tr, .ps) .nps
	:- easy-resolve-type-rule .tr
	, !, sort-resolve-type-rules .trs .ps .nps
#
sort-resolve-type-rules (.tr, .trs) .ps (.tr, .nps)
	:- sort-resolve-type-rules .trs .ps .nps
#

easy-resolve-type-rule (SUB-SUPER-TYPES _ .t _) :- bound .t #
easy-resolve-type-rule (CLONE-TO-FROM-TYPES _ .t) :- bound .t #
easy-resolve-type-rule (TYPE-IN-TYPES _) #

-- When resolving types:
-- - Try bind equivalent sub-type to super-type relation;
--   - Do not resolve relation when both types are not clear;
--   - Try reduce to type classes to resolve;
--   - Try morph children types to resolve;
--   - Generalize generic types to resolve;
--   - Try delay resolve if both types are unbinded;
-- - Try bind generic-type and specialized-type relation;
-- - Try bind type choice relation.
resolve-type-rules1 () :- ! #
resolve-type-rules1 (DUMP .d, .tr1)
	:- !, dump .d, nl, resolve-type-rules1 .tr1
#
resolve-type-rules1 (SUB-SUPER-TYPES .te .t0 .t1, .tr1)
	:- !, resolve-sub-super-types .te .t0 .t1, resolve-type-rules1 .tr1
#
resolve-type-rules1 (CLONE-TO-FROM-TYPES .t0 .t1, .tr1)
	:- !, clone .t1 .t0, resolve-type-rules1 .tr1
#
resolve-type-rules1 (TYPE-IN-TYPES .t .ts, .tr1)
	:- !, member .ts .t, resolve-type-rules1 .tr1
#
resolve-type-rules1 _ :- !, fc-error "Not enough type information" #

resolve-sub-super-types _ .t .t #
resolve-sub-super-types .te .t0 .tx
	:- bound .t0
	, sub-super-type-pair .te .t0 .t1
	, resolve-sub-super-types .te .t1 .tx
	; bound .tx
	, sub-super-type-pair .te .t1 .tx
	, resolve-sub-super-types .te .t0 .t1
#

sub-super-type-pair .te .type1 .class1 -- reduce to type classes
	:- once (bound .type1; bound .class1)
	, member .te .type/.classes/.typeVars
	, member .classes .class
	, instantiate-type .typeVars .type/.class .type1/.class1
#
sub-super-type-pair .te .t0 .t1 :- bound .t0, sub-super-type-pair0 .te .t0 .t1 #
sub-super-type-pair _ .t0 .t1 :- generic-specific-pair .t0 .t1 #
sub-super-type-pair _ .t0 .t1 :- generic-specific-pair .t1 .t0 #

-- Morph children types to their supers
sub-super-type-pair0 .te (FUN-OF .it0 .ot) (FUN-OF .it1 .ot)
	:- sub-super-type-pair .te .it1 .it0
#
sub-super-type-pair0 .te (FUN-OF .it .ot0) (FUN-OF .it .ot1)
	:- sub-super-type-pair .te .ot0 .ot1
#
sub-super-type-pair0 .te (LIST-OF .t0) (LIST-OF .t1)
	:- sub-super-type-pair .te .t0 .t1
#
sub-super-type-pair0 .te (PAIR-OF .t0 .t1) (PAIR-OF .st0 .t1)
	:- sub-super-type-pair .te .t0 .st0
#
sub-super-type-pair0 .te (PAIR-OF .t0 .t1) (PAIR-OF .t0 .st1)
	:- sub-super-type-pair .te .t1 .st1
#

generic-specific-pair (GENERIC-OF .typeVar .type) .t1
	:- bound .typeVar
	, replace .typeVar _ .type .t1
#

instantiate-type () .tc .tc #
instantiate-type (.typeVar, .typeVars) .tc0 .tcx
	:- replace .typeVar _ .tc0 .tc1
	, instantiate-type .typeVars .tc1 .tcx
#

default-fun-type () (LIST-OF _) #
default-fun-type _compare (FUN-OF .t (FUN-OF .t NUMBER)) #
default-fun-type _ijavacls (FUN-OF (ATOM-OF _) (ATOM-OF $JAVA-OBJECT)) #
default-fun-type _ijavaobj0 (FUN-OF (ATOM-OF $JAVA-OBJECT) _) #
default-fun-type _ijavaobj1 (FUN-OF (ATOM-OF $JAVA-OBJECT) (FUN-OF _ _)) #
default-fun-type _ijavaobj2 (FUN-OF (ATOM-OF $JAVA-OBJECT) (FUN-OF _ (FUN-OF _ _))) #
default-fun-type _lcons (FUN-OF .t (FUN-OF (LIST-OF .t) (LIST-OF .t))) #
default-fun-type _lhead (FUN-OF (LIST-OF .t) .t) #
default-fun-type _ltail (FUN-OF (LIST-OF .t) (LIST-OF .t)) #
default-fun-type _pcons (FUN-OF .t0 (FUN-OF .t1 (PAIR-OF .t0 .t1))) #
default-fun-type _pleft (FUN-OF (PAIR-OF .t _) .t) #
default-fun-type _pright (FUN-OF (PAIR-OF _ .t) .t) #
default-fun-type is-list (FUN-OF (LIST-OF .t) BOOLEAN) #
default-fun-type is-pair (FUN-OF (PAIR-OF _ _) BOOLEAN) #
