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
--   Written like B-TREE {:t}.
--   Represented internally as (CLASS (PARAMETERIZED (VAR t) B-TREE)).
--   Resolved by binding the type structures.
-- - Generic type, usually used in method signatures.
--   Written like :t => :t -> :t.
--   Represented internally as (GENERIC-OF (VAR t) FUN-OF (VAR t) (VAR t)).
--   Resolved by SUB-SUPER-TYPES.
-- - Generic type caused by not enough variable information during type inference.
--   Any variable usage, in case having unbinded variables, will also be cloned.
--   Resolved by CLONE-TO-FROM-TYPES.
--

fc-infer-type-rules () _ .tr/.tr () :- ! #
fc-infer-type-rules (.e, .es) .env .tr0/.trx (.t, .ts)
	:- fc-infer-type-rule .e .env .tr0/.tr1 .t
	, fc-infer-type-rules .es .env .tr1/.trx .ts
#

fc-infer-type-rule .p .env .tr/.tr .type
	:- fc-find-simple-type .p .env .type, !
#
fc-infer-type-rule (USING _ _ .lib .do) .env .tr/.tr .type
	:- !
	, fc-load-precompiled-library .lib (.pred # _ # _ #)
	, generalize .pred (
		fc-infer-type-rule-using-lib .lib .do .env .tr1/() .type :- .tail
	)
	, once .tail
	, fc-resolve-type-rules .tr1
#
fc-infer-type-rule (
	PRAGMA (DEF-TYPE .definedType .class .typeVars) .do
) .ue/.ve/.te .tr .type
	:- !
	, .te1 = (.definedType/.class/.typeVars, .te)
	, fc-infer-type-rule .do .ue/.ve/.te1 .tr .type
#
fc-infer-type-rule (PRAGMA (VERIFY-SAME-TYPES .var0 .var1) .do) .env .tr0/.trx .type
	:- !
	, fc-infer-compatible-types .var0 .var1 .env .tr0/.tr1 _
	, fc-infer-type-rule .do .env .tr1/.trx .type
#
fc-infer-type-rule (DEF-VAR .name .value .do) .ue/.ve/.te .tr0/.trx .type
	:- !
	, fc-dict-add .name/.varType .ue/.ue1
	, .env1 = .ue1/.ve/.te
	, once (fc-infer-type-rule .value .env1 .tr0/.tr1 .varType
		; fc-error "at variable" .name
	)
	, fc-infer-type-rule .do .env1 .tr1/.trx .type
#
fc-infer-type-rule (
	PRAGMA ALLOW-RECURSIVE (DEF-VAR .name .value .do)
) .ue/.ve/.te .tr0/.trx .type
	:- !
	, fc-dict-add .name/.varType .ue/.ue1
	, fc-dict-add .name/.varType .ve/.ve1
	, .insideEnv = .ue1/.ve/.te
	, .outsideEnv = .ue/.ve1/.te
	, once (fc-infer-type-rule .value .insideEnv .tr0/.tr1 .varType
		; fc-error "at variable" .name
	)
	, fc-infer-type-rule .do .outsideEnv .tr1/.trx .type
#
fc-infer-type-rule (PRAGMA CAST-TO-CLASS .pair) .env .tr .classType
	:- !
	, .classType = CLASS _
	, fc-infer-type-rule (PRAGMA (CAST DOWN .classType) .pair) .env .tr .classType
#
fc-infer-type-rule (FUN .var .do) .ue/.ve/.te .tr (FUN-OF .varType .type)
	:- !
	, fc-dict-add .var/.varType .ue/.ue1
	, fc-infer-type-rule .do .ue1/.ve/.te .tr .type
#
fc-infer-type-rule (INVOKE .param .callee) .ue/.ve/.te .tr0/.trx .type
	:- !
	, fc-infer-type-rule .callee .ue/.ve/.te .tr0/.tr1 .funType
	, fc-infer-type-rule .param .ue/.ve/.te .tr1/.tr2 .actualParamType
	, .tr2 = (SUB-SUPER-TYPES .te (FUN-OF .signParamType .type) .funType
		, SUB-SUPER-TYPES .te .actualParamType .signParamType
		, .trx
	)
#
fc-infer-type-rule (IF .if .then .else) .env .tr0/.trx .type
	:- !
	, fc-infer-type-rule .if .env .tr0/.tr1 BOOLEAN
	, fc-infer-compatible-types .then .else .env .tr1/.trx .type
#
fc-infer-type-rule (TREE .oper .left .right) .env .tr0/.trx .type
	:- member (' + ',) .oper, !
	, fc-infer-compatible-types .left .right .env .tr0/.tr1 .type
	, .tr1 = (TYPE-IN-TYPES .type (NUMBER,), .trx)
	; member (' - ', ' * ', ' / ', ' %% ',) .oper, !
	, fc-infer-compatible-types .left .right .env .tr0/.trx .type
	, .type = NUMBER
	; member (' = ', ' != ', ' > ', ' < ', ' >= ', ' <= ',) .oper, !
	, fc-infer-compatible-types .left .right .env .tr0/.trx _
	, .type = BOOLEAN
#
fc-infer-type-rule (PAIR .v0 .v1) .env .tr0/.trx (PAIR-OF .t0 .t1)
	:- !
	, fc-infer-type-rule .v0 .env .tr0/.tr1 .t0
	, fc-infer-type-rule .v1 .env .tr1/.trx .t1
#
fc-infer-type-rule (PRAGMA (CAST .dir .type) .do) .ue/.ve/.te .tr0/.trx .type
	:- !, fc-infer-type-rule .do .ue/.ve/.te .tr0/.tr1 .type0
	, once (
		.dir = DOWN, .subType = .type0, .superType = .type
		; .dir = UP, .subType = .type, .superType = .type0
	)
	, .tr1 = (SUB-SUPER-TYPES .te .subType .superType, .trx)
#
fc-infer-type-rule (PRAGMA RESOLVE-TYPE .do) .env .tr/.tr .type
	:- !
	, fc-infer-type-rule .do .env .tr1/() .type
	, fc-resolve-type-rules .tr1
#
fc-infer-type-rule .do .env .tr .type
	:- (.do = PRAGMA _ .do1; .do = UNWRAP .do1; .do = WRAP .do1)
	, !
	, fc-infer-type-rule .do1 .env .tr .type
#
fc-infer-type-rule (.tag .var) _/.ve/_ .tr0/.trx .type
	:- once (.tag = NEW-VAR; .tag = VAR)
	, (fc-dict-get .ve .var/.varType
		, !, .tr0 = (CLONE-TO-FROM-TYPES .type .varType, .trx)
	)
	; !, fc-error "Undefined variable" .var
#

fc-find-simple-type (CONSTANT _) _ _ #
fc-find-simple-type (ATOM ()) _ (LIST-OF _) :- ! #
fc-find-simple-type (ATOM .a) _ (ATOM-OF .a) #
fc-find-simple-type (BOOLEAN _) _ BOOLEAN #
fc-find-simple-type (DO _) _ (DO-OF _) #
fc-find-simple-type (NUMBER _) _ NUMBER #
fc-find-simple-type (PRAGMA SKIP-TYPE-CHECK _) _ _ #
fc-find-simple-type (.tag .var) .ue/_/_ .type
	:- once (.tag = NEW-VAR; .tag = VAR)
	, (fc-dict-get .ue .var/.type
		; fc-default-fun-type .var .type
	)
#

fc-infer-compatible-types .a .b .ue/.ve/.te .tr0/.trx .type
	:- fc-infer-type-rule .a .ue/.ve/.te .tr0/.tr1 .type0
	, fc-infer-type-rule .b .ue/.ve/.te .tr1/.tr2 .type1
	, .tr2 = (SUB-SUPER-TYPES .te .type0 .type
		, SUB-SUPER-TYPES .te .type1 .type
		, .trx
	)
#

fc-resolve-type-rules .tr
	:- once (not is.cyclic .tr; fc-error "Cyclic types")
	, once (fc-resolve-type-rules0 .tr; fc-error "Unmatched types")
#

fc-resolve-type-rules0 .tr
	:- once (fc-sort-resolve-type-rules .tr .ps .nps)
	, (.ps = (), !, fc-resolve-type-rules1 .tr
		; fc-resolve-type-rules1 .ps, fc-resolve-type-rules0 .nps
	)
#

-- Sort the resolve type rules by easiness
fc-sort-resolve-type-rules () () () :- ! #
fc-sort-resolve-type-rules (.tr, .trs) (.tr, .ps) .nps
	:- fc-resolve-easy-type-rule .tr
	, !, fc-sort-resolve-type-rules .trs .ps .nps
#
fc-sort-resolve-type-rules (.tr, .trs) .ps (.tr, .nps)
	:- fc-sort-resolve-type-rules .trs .ps .nps
#

fc-resolve-easy-type-rule (SUB-SUPER-TYPES _ .t _) :- bound .t #
fc-resolve-easy-type-rule (CLONE-TO-FROM-TYPES _ .t) :- bound .t #
fc-resolve-easy-type-rule (TYPE-IN-TYPES _) #

-- When resolving types:
-- - Try bind equivalent sub-type to super-type relation;
--   - Do not resolve relation when both types are not clear;
--   - Try reduce to type classes to resolve;
--   - Try morph children types to resolve;
--   - Generalize generic types to resolve;
--   - Try delay resolve if both types are unbinded;
-- - Try bind generic-type and specialized-type relation;
-- - Try bind type choice relation.
fc-resolve-type-rules1 () :- ! #
fc-resolve-type-rules1 (DUMP .d, .tr1)
	:- !, dump .d, nl, fc-resolve-type-rules1 .tr1
#
fc-resolve-type-rules1 (SUB-SUPER-TYPES .te .t0 .t1, .tr1)
	:- !, fc-resolve-sub-super-types .te .t0 .t1, fc-resolve-type-rules1 .tr1
#
fc-resolve-type-rules1 (CLONE-TO-FROM-TYPES .t0 .t1, .tr1)
	:- !, clone .t1 .t0, fc-resolve-type-rules1 .tr1
#
fc-resolve-type-rules1 (TYPE-IN-TYPES .t .ts, .tr1)
	:- !, member .ts .t, fc-resolve-type-rules1 .tr1
#
fc-resolve-type-rules1 _ :- !, fc-error "Not enough type information" #

fc-resolve-sub-super-types _ .t .t #
fc-resolve-sub-super-types .te .t0 .tx
	:- bound .t0
	, fc-sub-super-type-pair .te .t0 .t1
	, fc-resolve-sub-super-types .te .t1 .tx
	; bound .tx
	, fc-sub-super-type-pair .te .t1 .tx
	, fc-resolve-sub-super-types .te .t0 .t1
#

fc-sub-super-type-pair .te .type1 .class1 -- reduce to type classes
	:- once (bound .type1; bound .class1)
	, member .te .type/.class/.typeVars
	, fc-instantiate-type .typeVars .type/.class .type1/.class1
#
fc-sub-super-type-pair .te .t0 .t1 :- bound .t0, fc-sub-super-type-pair0 .te .t0 .t1 #
fc-sub-super-type-pair _ .t0 .t1 :- fc-generic-specific-pair .t0 .t1 #
fc-sub-super-type-pair _ .t0 .t1 :- fc-generic-specific-pair .t1 .t0 #

-- Morph children types to their supers
fc-sub-super-type-pair0 .te (FUN-OF .it0 .ot) (FUN-OF .it1 .ot)
	:- fc-sub-super-type-pair .te .it1 .it0
#
fc-sub-super-type-pair0 .te (FUN-OF .it .ot0) (FUN-OF .it .ot1)
	:- fc-sub-super-type-pair .te .ot0 .ot1
#
fc-sub-super-type-pair0 .te (LIST-OF .t0) (LIST-OF .t1)
	:- fc-sub-super-type-pair .te .t0 .t1
#
fc-sub-super-type-pair0 .te (PAIR-OF .t0 .t1) (PAIR-OF .st0 .t1)
	:- fc-sub-super-type-pair .te .t0 .st0
#
fc-sub-super-type-pair0 .te (PAIR-OF .t0 .t1) (PAIR-OF .t0 .st1)
	:- fc-sub-super-type-pair .te .t1 .st1
#

fc-generic-specific-pair (GENERIC-OF .typeVar .type) .t1
	:- bound .typeVar
	, replace .typeVar _ .type .t1
#

fc-instantiate-type () .tc .tc #
fc-instantiate-type (.typeVar, .typeVars) .tc0 .tcx
	:- replace .typeVar _ .tc0 .tc1
	, fc-instantiate-type .typeVars .tc1 .tcx
#

fc-default-fun-type () (LIST-OF _) #
fc-default-fun-type _callintrn0 (FUN-OF (DATA-OF Intrinsic) _) #
fc-default-fun-type _callintrn1 (FUN-OF (DATA-OF Intrinsic) (FUN-OF _ _)) #
fc-default-fun-type _callintrn2 (FUN-OF (DATA-OF Intrinsic) (FUN-OF _ (FUN-OF _ _))) #
fc-default-fun-type _callintrn3 (FUN-OF (DATA-OF Intrinsic) (FUN-OF _  (FUN-OF _ (FUN-OF _ _)))) #
fc-default-fun-type _compare (FUN-OF .t (FUN-OF .t NUMBER)) #
fc-default-fun-type _getintrn (FUN-OF (ATOM-OF _) (DATA-OF Intrinsic)) #
fc-default-fun-type _lcons (FUN-OF .t (FUN-OF (LIST-OF .t) (LIST-OF .t))) #
fc-default-fun-type _lhead (FUN-OF (LIST-OF .t) .t) #
fc-default-fun-type _ltail (FUN-OF (LIST-OF .t) (LIST-OF .t)) #
fc-default-fun-type _pcons (FUN-OF .t0 (FUN-OF .t1 (PAIR-OF .t0 .t1))) #
fc-default-fun-type _pleft (FUN-OF (PAIR-OF .t _) .t) #
fc-default-fun-type _pright (FUN-OF (PAIR-OF _ .t) .t) #
fc-default-fun-type is-list (FUN-OF (LIST-OF _) BOOLEAN) #
fc-default-fun-type is-pair (FUN-OF (PAIR-OF _ _) BOOLEAN) #
