-------------------------------------------------------------------------------
-- Type inference predicates
--
-- Environment consists of:
-- .ue - dictionary of inside variables / their corresponding types
-- .ve - dictionary of outside variables / their corresponding types, specialized
-- .te - list of types / their corresponding belonging classes
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
	:- try (once (fc-infer-type-rule .do ()/()/() .type)) .ex (fc-error .ex)
#

fc-infer-type-rule .p .env .type
	:- fc-find-simple-type .p .env .type
#
fc-infer-type-rule (APPLY .param .callee) .env .returnType
	:- fc-infer-type-rule .callee .env (FUN-OF .paramType .returnType)
	, fc-infer-type-rule .param .env .paramType
#
fc-infer-type-rule (CONS L .v0 .v1) .env (FUNCTOR-OF LIST .t)
	:- fc-infer-type-rule .v0 .env .t
	, fc-infer-type-rule .v1 .env (FUNCTOR-OF LIST .t)
#
fc-infer-type-rule (CONS P .v0 .v1) .env (PAIR-OF .t0 .t1)
	:- fc-infer-type-rule .v0 .env .t0
	, fc-infer-type-rule .v1 .env .t1
#
fc-infer-type-rule (DECONS _ _ .headVar .tailVar .then .else) .ue0/.ve/.te .type
	:- fc-dict-add .headVar/_ .ue0/.ue1
	, fc-dict-add .tailVar/_ .ue1/.ue2
	, fc-infer-type-rule .then .ue2/.ve/.te .type
	, fc-infer-type-rule .else .ue0/.ve/.te .type
#
fc-infer-type-rule (DEF-VARS .vvs .do) .ue/.ve/.te .type
	:- fc-define-var-types () .vvs .vvts .ue/.ue1
	, .env1 = .ue1/.ve/.te
	, fc-infer-var-types .vvts .env1
	, fc-infer-type-rule .do .env1 .type
#
fc-infer-type-rule ERROR _ _
#
fc-infer-type-rule (FUN .var .do) .ue/.ve/.te (FUN-OF .varType .type)
	:- fc-dict-add .var/.varType .ue/.ue1
	, fc-infer-type-rule .do .ue1/.ve/.te .type
#
fc-infer-type-rule (IF .if .then .else) .env .type
	:- fc-infer-type-rule .if .env BOOLEAN
	, fc-infer-type-rule .then .env .type
	, fc-infer-type-rule .else .env .type
#
fc-infer-type-rule (
	PRAGMA DEF-OUTSIDE (DEF-VARS .vvs .do)
) .ue/.ve/.te .type
	:- !
	, fc-define-var-types () .vvs .vvts .ue/.ue1
	, fc-infer-var-types .vvts .ue1/.ve/.te
	, fc-define-var-types SP .vvs .vvts .ve/.ve1
	, fc-infer-type-rule .do .ue/.ve1/.te .type
#
fc-infer-type-rule (
	PRAGMA (DEF-TYPE .definedType .class) .do
) .ue/.ve/.te .type
	:- !
	, fc-infer-type-rule .do .ue/.ve/(.definedType/.class, .te) .type
#
fc-infer-type-rule (PRAGMA (TYPE-OF .type1) .do) .env .type
	:- !
	, graph.generalize .type1 .type
	, fc-infer-type-rule .do .env .type
#
fc-infer-type-rule (PRAGMA TYPE-SKIP-CHECK _) _ _
	:- !
#
fc-infer-type-rule (PRAGMA TYPE-SUPER .do) .ue/.ve/.te .superType
	:- !
	, fc-infer-type-rule .do .ue/.ve/.te .subType
	, fc-sub-super-type-pair .te .subType .superType
#
fc-infer-type-rule (PRAGMA (TYPE-VERIFY .var .varType) .do) .env .type
	:- !
	, graph.generalize .varType .varType1
	, fc-infer-type-rule .var .env .varType1
	, fc-infer-type-rule .do .env .type
#
fc-infer-type-rule (PRAGMA _ .do) .env .type
	:- fc-infer-type-rule .do .env .type
#
fc-infer-type-rule (TREE .oper .left .right) .env .type
	:- once (
		member (' + ', ' - ', ' * ', ' / ', ' %% ',) .oper, !
		, .inputType = NUMBER
		, .type = NUMBER
		; member (' = ', ' != ', ' > ', ' < ', ' >= ', ' <= ',) .oper, !
		, .type = BOOLEAN
	)
	, fc-infer-type-rule .left .env .inputType
	, fc-infer-type-rule .right .env .inputType
#
fc-infer-type-rule (USING _ _ .lib .do) .env .type
	:- fc-load-precompiled-library .lib (.pred # _ # _ #)
	, clone .pred (
		fc-infer-type-rule-using-lib .lib .do .env .type :- .tail
	)
	, once .tail
#
fc-infer-type-rule .do .env .type
	:- (.do = UNWRAP .do1; .do = WRAP .do1)
	, fc-infer-type-rule .do1 .env .type
#
fc-infer-type-rule _ _ _
	:- throw "Unmatched types"
#

fc-define-var-types .sp (.var .value, .vvs) (.var .value .varType0, .vvts) .ue0/.uex
	:- once (.sp = SP, graph.specialize .varType0 .varType1; .varType0 = .varType1)
	, fc-dict-add .var/.varType1 .ue0/.ue1
	, fc-define-var-types .sp .vvs .vvts .ue1/.uex
#
fc-define-var-types _ () () .ue/.ue
#

fc-infer-var-types (.var .value .varType, .vvts) .env
	:- try (fc-infer-type-rule .value .env .varType)
	.ex (throw .ex "%0Aat variable" .var)
	, fc-infer-var-types .vvts .env
#
fc-infer-var-types () _
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

fc-sub-super-type-pair .te .subType .superType
	:- once (bound .subType; bound .superType)
	, member .te .tc
	, graph.generalize .tc .subType/.superType
#
