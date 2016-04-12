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
	:- try (once (fc-infer-type0 .do ()/()/() .type)) .ex (fc-error .ex)
#

fc-infer-type0 .p .env .type
	:- fc-find-simple-type .env .p .type
#
fc-infer-type0 (APPLY .param .callee) .env .returnType
	:- fc-infer-type0 .callee .env (FUN-OF .paramType .returnType)
	, fc-infer-type0 .param .env .paramType
#
fc-infer-type0 (CONS L .v0 .v1) .env (FUNCTOR-OF LIST .t)
	:- fc-infer-type0 .v0 .env .t
	, fc-infer-type0 .v1 .env (FUNCTOR-OF LIST .t)
#
fc-infer-type0 (CONS P .v0 .v1) .env (PAIR-OF .t0 .t1)
	:- fc-infer-type0 .v0 .env .t0
	, fc-infer-type0 .v1 .env .t1
#
fc-infer-type0 (DECONS _ _ .headVar .tailVar .then .else) .ue0/.ve/.te .type
	:- fc-dict-add .headVar/_ .ue0/.ue1
	, fc-dict-add .tailVar/_ .ue1/.ue2
	, fc-infer-type0 .then .ue2/.ve/.te .type
	, fc-infer-type0 .else .ue0/.ve/.te .type
#
fc-infer-type0 (DEF-VARS .vvs .do) .ue/.ve/.te .type
	:- fc-define-var-types .ue/.ue1 () .vvs .vvts
	, .env1 = .ue1/.ve/.te
	, fc-infer-var-types .env1 .vvts
	, fc-infer-type0 .do .env1 .type
#
fc-infer-type0 ERROR _ _
#
fc-infer-type0 (FUN .var .do) .ue/.ve/.te (FUN-OF .varType .type)
	:- fc-dict-add .var/.varType .ue/.ue1
	, fc-infer-type0 .do .ue1/.ve/.te .type
#
fc-infer-type0 (IF .if .then .else) .env .type
	:- fc-infer-type0 .if .env BOOLEAN
	, fc-infer-type0 .then .env .type
	, fc-infer-type0 .else .env .type
#
fc-infer-type0 (
	PRAGMA DEF-OUTSIDE (DEF-VARS .vvs .do)
) .ue/.ve/.te .type
	:- !
	, fc-define-var-types .ue/.ue1 () .vvs .vvts
	, fc-infer-var-types .ue1/.ve/.te .vvts
	, fc-define-var-types .ve/.ve1 SP .vvs .vvts
	, fc-infer-type0 .do .ue/.ve1/.te .type
#
fc-infer-type0 (
	PRAGMA (DEF-TYPE .definedType .class) .do
) .ue/.ve/.te .type
	:- !
	, fc-infer-type0 .do .ue/.ve/(.definedType/.class, .te) .type
#
fc-infer-type0 (PRAGMA (TYPE-OF .type1) .do) .env .type
	:- !
	, graph.generalize .type1 .type
	, fc-infer-type0 .do .env .type
#
fc-infer-type0 (PRAGMA TYPE-SKIP-CHECK _) _ _
	:- !
#
fc-infer-type0 (PRAGMA TYPE-SUPER .do) .ue/.ve/.te .superType
	:- !
	, fc-infer-type0 .do .ue/.ve/.te .subType
	, fc-sub-super-type-pair .te .subType .superType
#
fc-infer-type0 (PRAGMA (TYPE-VERIFY .var .varType) .do) .env .type
	:- !
	, graph.generalize .varType .varType1
	, fc-infer-type0 .var .env .varType1
	, fc-infer-type0 .do .env .type
#
fc-infer-type0 (PRAGMA _ .do) .env .type
	:- fc-infer-type0 .do .env .type
#
fc-infer-type0 (TREE .oper .left .right) .env .type
	:- once (
		member (' + ', ' - ', ' * ', ' / ', ' %% ',) .oper, !
		, .inputType = NUMBER
		, .type = NUMBER
		; member (' = ', ' != ', ' > ', ' < ', ' >= ', ' <= ',) .oper, !
		, .type = BOOLEAN
	)
	, fc-infer-type0 .left .env .inputType
	, fc-infer-type0 .right .env .inputType
#
fc-infer-type0 (USING _ _ .lib .do) .env .type
	:- fc-load-precompiled-library .lib (.pred # _ # _ #)
	, clone .pred (
		fc-infer-type0-using-lib .lib .env .do .type :- .tail
	)
	, once .tail
#
fc-infer-type0 .do .env .type
	:- (.do = UNWRAP .do1; .do = WRAP .do1)
	, fc-infer-type0 .do1 .env .type
#
fc-infer-type0 _ _ _
	:- throw "Unmatched types"
#

fc-define-var-types .ue0/.uex .sp (.var .value, .vvs) (.var .value .varType0, .vvts)
	:- once (.sp = SP, graph.specialize .varType0 .varType1; .varType0 = .varType1)
	, fc-dict-add .var/.varType1 .ue0/.ue1
	, fc-define-var-types .ue1/.uex .sp .vvs .vvts
#
fc-define-var-types .ue/.ue _ () ()
#

fc-infer-var-types .env (.var .value .varType, .vvts)
	:- try (fc-infer-type0 .value .env .varType)
	.ex (throw .ex "%0Aat variable" .var)
	, fc-infer-var-types .env .vvts
#
fc-infer-var-types _ ()
#

fc-find-simple-type _ (ATOM .a) (ATOM-OF .a) #
fc-find-simple-type _ (BOOLEAN _) BOOLEAN #
fc-find-simple-type _ (CHARS _) (FUNCTOR-OF n PAIR-OF (ATOM-OF Chars) ATOM-OF ()) #
fc-find-simple-type _ (DO _) (FUNCTOR-OF Do _) #
fc-find-simple-type _ NIL (FUNCTOR-OF LIST _) :- ! #
fc-find-simple-type _ (NUMBER _) NUMBER #
fc-find-simple-type .ue/.ve/_ (VAR .var) .type
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
