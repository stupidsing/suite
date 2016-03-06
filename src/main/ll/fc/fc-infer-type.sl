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
--   Written like B-TREE :t.
--   Resolved by binding the type structures.
-- - Generic type caused by not enough variable information during type inference.
--   They are specialized to bounded type node and put into the outside variable
--   list, and generalized back when being used.
--

fc-infer-type .do .type
	:- try (once (fc-infer-type-rule .do ()/()/() ()/() .type)) .ex (fc-error .ex)
#

fc-infer-type-rule .p .env .tr/.tr .type
	:- fc-find-simple-type .p .env .type
#
fc-infer-type-rule (APPLY .param .callee) .env .tr0/.trx .returnType
	:- fc-infer-type-rule .callee .env .tr0/.tr1 (FUN-OF .paramType .returnType)
	, fc-infer-type-rule .param .env .tr1/.trx .paramType
#
fc-infer-type-rule (CONS L .v0 .v1) .env .tr0/.trx (FUNCTOR-OF LIST .t)
	:- fc-infer-type-rule .v0 .env .tr0/.tr1 .t
	, fc-infer-type-rule .v1 .env .tr1/.trx (FUNCTOR-OF LIST .t)
#
fc-infer-type-rule (CONS P .v0 .v1) .env .tr0/.trx (PAIR-OF .t0 .t1)
	:- fc-infer-type-rule .v0 .env .tr0/.tr1 .t0
	, fc-infer-type-rule .v1 .env .tr1/.trx .t1
#
fc-infer-type-rule (DECONS _ _ .headVar .tailVar .then .else) .ue0/.ve/.te .tr0/.trx .type
	:- fc-dict-add .headVar/_ .ue0/.ue1
	, fc-dict-add .tailVar/_ .ue1/.ue2
	, fc-infer-type-rule .then .ue2/.ve/.te .tr0/.tr1 .type
	, fc-infer-type-rule .else .ue0/.ve/.te .tr1/.trx .type
#
fc-infer-type-rule (DEF-VARS .vvs .do) .ue/.ve/.te .tr0/.trx .type
	:- fc-define-var-types () .vvs .vvts .ue/.ue1
	, .env1 = .ue1/.ve/.te
	, fc-infer-var-types .vvts .env1 .tr0/.tr1
	, fc-infer-type-rule .do .env1 .tr1/.trx .type
#
fc-infer-type-rule ERROR _ .tr/.tr _
#
fc-infer-type-rule (FUN .var .do) .ue/.ve/.te .tr (FUN-OF .varType .type)
	:- fc-dict-add .var/.varType .ue/.ue1
	, fc-infer-type-rule .do .ue1/.ve/.te .tr .type
#
fc-infer-type-rule (IF .if .then .else) .env .tr0/.trx .type
	:- fc-infer-type-rule .if .env .tr0/.tr1 BOOLEAN
	, fc-infer-type-rule .then .env .tr1/.tr2 .type
	, fc-infer-type-rule .else .env .tr2/.trx .type
#
fc-infer-type-rule (
	PRAGMA DEF-OUTSIDE (DEF-VARS .vvs .do)
) .ue/.ve/.te .tr0/.trx .type
	:- !
	, fc-define-var-types () .vvs .vvts .ue/.ue1
	, fc-infer-var-types .vvts .ue1/.ve/.te .tr0/.tr1
	, fc-define-var-types SP .vvs .vvts .ve/.ve1
	, fc-infer-type-rule .do .ue/.ve1/.te .tr1/.trx .type
#
fc-infer-type-rule (
	PRAGMA (DEF-TYPE .definedType .class) .do
) .ue/.ve/.te .tr .type
	:- !
	, fc-infer-type-rule .do .ue/.ve/(.definedType/.class, .te) .tr .type
#
fc-infer-type-rule (PRAGMA (TYPE-OF .type1) .do) .env .tr .type
	:- !
	, graph.generalize .type1 .type
	, fc-infer-type-rule .do .env .tr .type
#
fc-infer-type-rule (PRAGMA TYPE-RESOLVE .do) .env .tr/.tr .type
	:- !
	, fc-infer-type-rule .do .env .tr1/() .type
	, fc-resolve-type-rules .tr1
#
fc-infer-type-rule (PRAGMA TYPE-SKIP-CHECK _) _ .tr/.tr _
	:- !
#
fc-infer-type-rule (PRAGMA TYPE-SUPER .do) .ue/.ve/.te .tr .superType
	:- !
	, fc-infer-type-rule .do .ue/.ve/.te .tr .subType
	, fc-sub-super-type-pair .te .subType .superType
#
fc-infer-type-rule (PRAGMA (TYPE-VERIFY .var .varType) .do) .env .tr0/.trx .type
	:- !
	, graph.generalize .varType .varType1
	, fc-infer-type-rule .var .env .tr0/.tr1 .varType1
	, fc-infer-type-rule .do .env .tr1/.trx .type
#
fc-infer-type-rule (PRAGMA _ .do) .env .tr .type
	:- fc-infer-type-rule .do .env .tr .type
#
fc-infer-type-rule (TREE .oper .left .right) .env .tr0/.trx .type
	:- once (
		member (' + ', ' - ', ' * ', ' / ', ' %% ',) .oper, !
		, .inputType = NUMBER
		, .type = NUMBER
		; member (' = ', ' != ', ' > ', ' < ', ' >= ', ' <= ',) .oper, !
		, .type = BOOLEAN
	)
	, fc-infer-type-rule .left .env .tr0/.tr1 .inputType
	, fc-infer-type-rule .right .env .tr1/.trx .inputType
#
fc-infer-type-rule (USING _ _ .lib .do) .env .tr/.tr .type
	:- fc-load-precompiled-library .lib (.pred # _ # _ #)
	, clone .pred (
		fc-infer-type-rule-using-lib .lib .do .env .tr1/() .type :- .tail
	)
	, once .tail
	, fc-resolve-type-rules .tr1
#
fc-infer-type-rule .do .env .tr .type
	:- (.do = UNWRAP .do1; .do = WRAP .do1)
	, fc-infer-type-rule .do1 .env .tr .type
#
fc-infer-type-rule _ _ _ _
	:- throw "Unmatched types"
#

fc-define-var-types .sp (.var .value, .vvs) (.var .value .varType0, .vvts) .ue0/.uex
	:- once (.sp = SP, graph.specialize .varType0 .varType1; .varType0 = .varType1)
	, fc-dict-add .var/.varType1 .ue0/.ue1
	, fc-define-var-types .sp .vvs .vvts .ue1/.uex
#
fc-define-var-types _ () () .ue/.ue
#

fc-infer-var-types (.var .value .varType, .vvts) .env .tr0/.trx
	:- try (fc-infer-type-rule .value .env .tr0/.tr1 .varType)
	.ex (throw .ex "%0Aat variable" .var)
	, fc-infer-var-types .vvts .env .tr1/.trx
#
fc-infer-var-types () _ .tr/.tr
#

fc-find-simple-type (ATOM .a) _ (ATOM-OF .a) #
fc-find-simple-type (BOOLEAN _) _ BOOLEAN #
fc-find-simple-type (CHARS _) _ (FUNCTOR-OF n PAIR-OF (ATOM-OF Chars) ATOM-OF ()) #
fc-find-simple-type (DO _) _ (FUNCTOR-OF Do _) #
fc-find-simple-type NIL _ (FUNCTOR-OF LIST _) :- ! #
fc-find-simple-type (NUMBER _) _ NUMBER #
fc-find-simple-type (VAR .var) .ue/.ve/_ .type
	:- once (
		fc-dict-get .ue .var/.type
		; fc-dict-get .ve .var/.varType, !, graph.generalize .varType .type
		; fc-define-default-fun _ .var _
		; throw "Undefined variable" .var
	)
#

fc-resolve-type-rules .tr
	:- once (not (is.cyclic .tr); throw "Cyclic types")
	, once (fc-resolve-type-rules0 .tr; throw "Unmatched types")
#

fc-resolve-type-rules0 ()
	:- !
#
fc-resolve-type-rules0 _
	:- !, throw "Not enough type information"
#

fc-sub-super-type-pair .te .subType .superType
	:- once (bound .subType; bound .superType)
	, member .te .tc
	, graph.generalize .tc .subType/.superType
#
