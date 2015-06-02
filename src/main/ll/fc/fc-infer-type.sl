-------------------------------------------------------------------------------
-- Type inference predicates
--
-- Environment consists of:
-- .ue - dictionary of inside variables / their corresponding types
-- .ve - dictionary of outside variables / their corresponding types, specialized
-- .te - list of types / their corresponding belonging classes
-- .tr - type deduction rule to be assembled
--
-- Inside variables include parent function definitions and parameter variables
-- that do not need type specialization.
-- Outside variables are local variables that require type specialization.
--
-- Notes about generic types:
-- - Generic type class, usually used in abstract data structures.
--   Written like B-TREE {:t}.
--   Represented internally as (CLASS (PARAMETERIZED (VAR t) B-TREE)).
--   Resolved by binding the type structures.
-- - Generic type caused by not enough variable information during type inference.
--   They are specialized to bounded type node and put into the outside variable
--   list, and generalized back when being used.
--

fc-infer-type-rule0 .p .env .tr/.tr .type
	:- fc-find-simple-type .p .env .type
#
fc-infer-type-rule0 (DEF-VARS .vvs .do) .ue/.ve/.te .tr0/.trx .type
	:- fc-define-var-types () .vvs .vvts .ue/.ue1
	, .env1 = .ue1/.ve/.te
	, fc-infer-var-types .vvts .env1 .tr0/.tr1
	, fc-infer-type-rule0 .do .env1 .tr1/.trx .type
#
fc-infer-type-rule0 ERROR _ .tr/.tr _
#
fc-infer-type-rule0 (FUN .var .do) .ue/.ve/.te .tr (FUN-OF .varType .type)
	:- fc-dict-add .var/.varType .ue/.ue1
	, fc-infer-type-rule0 .do .ue1/.ve/.te .tr .type
#
fc-infer-type-rule0 (IF .if .then .else) .env .tr0/.trx .type
	:- fc-infer-type-rule0 .if .env .tr0/.tr1 BOOLEAN
	, fc-infer-type-rule0 .then .env .tr1/.tr2 .type
	, fc-infer-type-rule0 .else .env .tr2/.trx .type
#
fc-infer-type-rule0 (INVOKE .param .callee) .env .tr0/.trx .returnType
	:- fc-infer-type-rule0 .callee .env .tr0/.tr1 (FUN-OF .paramType .returnType)
	, fc-infer-type-rule0 .param .env .tr1/.trx .paramType
#
fc-infer-type-rule0 (PAIR .v0 .v1) .env .tr0/.trx (PAIR-OF .t0 .t1)
	:- fc-infer-type-rule0 .v0 .env .tr0/.tr1 .t0
	, fc-infer-type-rule0 .v1 .env .tr1/.trx .t1
#
fc-infer-type-rule0 (
	PRAGMA DEF-OUTSIDE (DEF-VARS .vvs .do)
) .ue/.ve/.te .tr0/.trx .type
	:- !
	, fc-define-var-types () .vvs .vvts .ue/.ue1
	, fc-infer-var-types .vvts .ue1/.ve/.te .tr0/.tr1
	, fc-define-var-types SP .vvs .vvts .ve/.ve1
	, fc-infer-type-rule0 .do .ue/.ve1/.te .tr1/.trx .type
#
fc-infer-type-rule0 (
	PRAGMA (DEF-TYPE .definedType .class) .do
) .ue/.ve/.te .tr .type
	:- !
	, specialize .definedType/.class .pair
	, fc-infer-type-rule0 .do .ue/.ve/(.pair, .te) .tr .type
#
fc-infer-type-rule0 (PRAGMA (TYPE-CAST .superType) .do) .ue/.ve/.te .tr0/.trx .type
	:- !
	, .type = .superType
	, fc-infer-type-rule0 .do .ue/.ve/.te .tr0/.tr1 .subType
	, .tr1 = (SUB-SUPER-TYPES .te .subType .superType, .trx)
#
fc-infer-type-rule0 (PRAGMA TYPE-CAST-TO-CLASS .pair) .env .tr .classType
	:- !
	, .classType = CLASS _
	, fc-infer-type-rule0 (PRAGMA (TYPE-CAST .classType) .pair) .env .tr .classType
#
fc-infer-type-rule0 (PRAGMA TYPE-RESOLVE .do) .env .tr/.tr .type
	:- !
	, fc-infer-type-rule0 .do .env .tr1/() .type
	, fc-resolve-type-rules .tr1
#
fc-infer-type-rule0 (PRAGMA TYPE-SKIP-CHECK _) _ .tr/.tr _
	:- !
#
fc-infer-type-rule0 (PRAGMA (TYPE-VERIFY .var .varType) .do) .env .tr0/.trx .type
	:- !
	, fc-infer-type-rule0 .var .env .tr0/.tr1 .varType
	, fc-infer-type-rule0 .do .env .tr1/.trx .type
#
fc-infer-type-rule0 (PRAGMA _ .do) .env .tr .type
	:- fc-infer-type-rule0 .do .env .tr .type
#
fc-infer-type-rule0 (TREE .oper .left .right) .env .tr0/.trx .type
	:- once (
		member (' + ', ' - ', ' * ', ' / ', ' %% ',) .oper, !
		, .inputType = NUMBER
		, .type = NUMBER
		; member (' = ', ' != ', ' > ', ' < ', ' >= ', ' <= ',) .oper, !
		, .type = BOOLEAN
	)
	, fc-infer-type-rule0 .left .env .tr0/.tr1 .inputType
	, fc-infer-type-rule0 .right .env .tr1/.trx .inputType
#
fc-infer-type-rule0 (USING _ _ .lib .do) .env .tr/.tr .type
	:- fc-load-precompiled-library .lib (.pred # _ # _ #)
	, clone .pred (
		fc-infer-type-rule-using-lib .lib .do .env .tr1/() .type :- .tail
	)
	, once .tail
	, fc-resolve-type-rules .tr1
#
fc-infer-type-rule0 .do .env .tr .type
	:- (.do = UNWRAP .do1; .do = WRAP .do1)
	, fc-infer-type-rule0 .do1 .env .tr .type
#

fc-define-var-types .sp (.var .value, .vvs) (.var .value .varType0, .vvts) .ue0/.uex
	:- once (.sp = SP, graph.specialize .varType0 .varType1; .varType0 = .varType1)
	, fc-dict-add .var/.varType1 .ue0/.ue1
	, fc-define-var-types .sp .vvs .vvts .ue1/.uex
#
fc-define-var-types _ () () .ue/.ue
#

fc-infer-var-types (.var .value .varType, .vvts) .env .tr0/.trx
	:- once (fc-infer-type-rule0 .value .env .tr0/.tr1 .varType
		; fc-error "at variable" .var
	)
	, fc-infer-var-types .vvts .env .tr1/.trx
#
fc-infer-var-types () _ .tr/.tr
#

fc-find-simple-type (ATOM ()) _ (LIST-OF _) :- ! #
fc-find-simple-type (ATOM .a) _ (ATOM-OF .a) #
fc-find-simple-type (BOOLEAN _) _ BOOLEAN #
fc-find-simple-type (DO _) _ (FUNCTOR-OF Do _) #
fc-find-simple-type (NUMBER _) _ NUMBER #
fc-find-simple-type (VAR .var) .ue/.ve/_ .type
	:- once (
		fc-dict-get .ue .var/.type
		; fc-dict-get .ve .var/.varType, !, graph.generalize .varType .type
		; fc-define-default-fun _ .var _
		; fc-error "Undefined variable" .var
	)
#

fc-resolve-type-rules .tr
	:- once (not is.cyclic .tr; fc-error "Cyclic types")
	, once (fc-resolve-type-rules0 .tr; fc-error "Unmatched types")
#

fc-resolve-type-rules0 ()
	:- !
#
fc-resolve-type-rules0 (SUB-SUPER-TYPES .te .t0 .tx, .tr1)
	:- !, fc-resolve-sub-super-types .te .t0 .tx, fc-resolve-type-rules0 .tr1
#
fc-resolve-type-rules0 _
	:- !, fc-error "Not enough type information"
#

fc-resolve-sub-super-types _ .t .t
#
fc-resolve-sub-super-types .te .t0 .tx
	:- bound .t0
	, fc-sub-super-type-pair .te .t0 .t1
	, fc-resolve-sub-super-types .te .t1 .tx
	; bound .tx
	, fc-sub-super-type-pair .te .t1 .tx
	, fc-resolve-sub-super-types .te .t0 .t1
#

fc-sub-super-type-pair .te .subType .superType
	:- once (bound .subType; bound .superType)
	, member .te .tc
	, generalize .tc .subType/.superType
#
