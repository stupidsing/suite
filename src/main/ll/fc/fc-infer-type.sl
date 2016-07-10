-------------------------------------------------------------------------------
-- Type inference predicates
--
-- Environment consists of:
-- .ve - dictionary of variables / specialization, and their corresponding types
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
	:- try (once (fc-infer-type0 ()/() .do .type)) .ex (fc-error .ex)
#

fc-infer-type0 .env .p .type
	:- fc-find-simple-type .env .p .type
#
fc-infer-type0 .env (APPLY .param .callee) .returnType
	:- fc-infer-type0 .env .callee (FUN-OF .paramType .returnType)
	, fc-infer-type0 .env .param .paramType
#
fc-infer-type0 .env (CONS L .v0 .v1) (FUNCTOR-OF LIST .t)
	:- fc-infer-type0 .env .v0 .t
	, fc-infer-type0 .env .v1 (FUNCTOR-OF LIST .t)
#
fc-infer-type0 .env (CONS P .v0 .v1) (PAIR-OF .t0 .t1)
	:- fc-infer-type0 .env .v0 .t0
	, fc-infer-type0 .env .v1 .t1
#
fc-infer-type0 .ve0/.te (DECONS _ _ .headVar .tailVar .then .else) .type
	:- fc-dict-add .headVar/_ .ve0/.ve1
	, fc-dict-add .tailVar/_ .ve1/.ve2
	, fc-infer-type0 .ve2/.te .then .type
	, fc-infer-type0 .ve0/.te .else .type
#
fc-infer-type0 .ve/.te (DEF-VARS .vvs .do) .type
	:- fc-define-var-types MONO .vvs .vvts .ve/.ve1
	, .env1 = .ve1/.te
	, fc-infer-var-types .env1 .vvts
	, fc-infer-type0 .env1 .do .type
#
fc-infer-type0 _ ERROR _
#
fc-infer-type0 .ve/.te (FUN .var .do) (FUN-OF .varType .type)
	:- fc-dict-add .var/(MONO .varType) .ve/.ve1
	, fc-infer-type0 .ve1/.te .do .type
#
fc-infer-type0 .env (IF .if .then .else) .type
	:- fc-infer-type0 .env .if BOOLEAN
	, fc-infer-type0 .env .then .type
	, fc-infer-type0 .env .else .type
#
fc-infer-type0 .ve/.te (PRAGMA DEF-OUTSIDE (DEF-VARS .vvs .do)) .type
	:- !
	, fc-define-var-types MONO .vvs .vvts .ve/.vea
	, fc-infer-var-types .vea/.te .vvts
	, fc-define-var-types POLY .vvs .vvts .ve/.veb
	, fc-infer-type0 .veb/.te .do .type
#
fc-infer-type0 .env (PRAGMA (TYPE-CAST .type1) .do) .type
	:- !
	, graph.generalize .type1 .type
	, fc-infer-type0 .env .do .type
#
fc-infer-type0 .ve/.te (PRAGMA (TYPE-DEF .definedType .class) .do) .type
	:- !
	, fc-infer-type0 .ve/(.definedType/.class, .te) .do .type
#
fc-infer-type0 _ (PRAGMA TYPE-SKIP-CHECK _) _
	:- !
#
fc-infer-type0 .ve/.te (PRAGMA TYPE-SUPER .do) .superType
	:- !
	, fc-infer-type0 .ve/.te .do .subType
	, fc-sub-super-type-pair .te .subType .superType
#
fc-infer-type0 .env (PRAGMA (TYPE-VERIFY .var .varType) .do) .type
	:- !
	, graph.generalize .varType .varType1
	, fc-infer-type0 .env .var .varType1
	, fc-infer-type0 .env .do .type
#
fc-infer-type0 .env (PRAGMA _ .do) .type
	:- fc-infer-type0 .env .do .type
#
fc-infer-type0 .env (TREE .oper .left .right) .type
	:- once (
		member (' + ', ' - ', ' * ', ' / ', ' %% ',) .oper, !
		, .inputType = NUMBER
		, .type = NUMBER
		; member (' = ', ' != ', ' > ', ' < ', ' >= ', ' <= ',) .oper, !
		, .type = BOOLEAN
	)
	, fc-infer-type0 .env .left .inputType
	, fc-infer-type0 .env .right .inputType
#
fc-infer-type0 .env (USING _ _ .lib .do) .type
	:- fc-load-precompiled-library .lib (.pred # _ # _ #)
	, clone .pred (
		fc-infer-type0-using-lib .lib .env .do .type :- .tail
	)
	, once .tail
#
fc-infer-type0 .env .do .type
	:- (.do = UNWRAP .do1; .do = WRAP .do1)
	, fc-infer-type0 .env .do1 .type
#
fc-infer-type0 _ _ _
	:- throw "Unmatched types"
#

fc-define-var-types .mp (.var .value, .vvs) (.var .value .varType0, .vvts) .ve0/.vex
	:- once (.mp = POLY, graph.specialize .varType0 .varType1; .varType0 = .varType1)
	, fc-dict-add .var/(.mp .varType1) .ve0/.ve1
	, fc-define-var-types .mp .vvs .vvts .ve1/.vex
#
fc-define-var-types _ () () .ve/.ve
#

fc-infer-var-types .env (.var .value .varType, .vvts)
	:- try (fc-infer-type0 .env .value .varType)
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
fc-find-simple-type .ve/_ (VAR .var) .type
	:- once (
		fc-dict-get .ve .var/(MONO .type)
		; fc-dict-get .ve .var/(POLY .varType), !, graph.generalize .varType .type
		; fc-define-default-fun _ .var _
		; throw "Undefined variable" .var
	)
#

fc-sub-super-type-pair .te .subType .superType
	:- once (bound .subType; bound .superType)
	, member .te .tc
	, graph.generalize .tc .subType/.superType
#
