fc-optimize .do0 .dox
	:- fc-optimize-flow .do0 .do1
	, fc-remove-unref-vars .do1 .dox ()/_
#

fc-optimize-flow .do0 .dox
	:- fc-define-var .do0 .var .value .do1 _
	, .value = .type _
	, member (ATOM, BOOLEAN, NUMBER, VAR,) .type
	, not contains (VAR .var) .value
	, !, fc-replace-var-by-value .var .value .do1 .do2
	, fc-optimize-flow .do2 .dox
#
fc-optimize-flow (UNWRAP WRAP .do0) .dox
	:- !, fc-optimize-flow .do0 .dox
#
fc-optimize-flow (INVOKE .value (FUN .var .do0)) .dox
	:- !, fc-optimize-flow (DEF-VAR .var .value .do0) .dox
#
fc-optimize-flow .p0 .p1 :- fc-rewrite .p0 .p1 .ts/(), fc-optimize-flow-list .ts #

fc-optimize-flow-list () #
fc-optimize-flow-list (.t, .ts) :- fc-optimize-flow .t, fc-optimize-flow-list .ts #

-- Remove unreferenced variables
fc-remove-unref-vars .do0 .dox .rb0/.rbx
	:- fc-define-var .do0 .var .value0 .do1 .dow .var .value1 .do2
	, fc-remove-unref-vars .do1 .do2 ()/.rbs
	, rbt-union-bind .rb0 .rbs .rbw
	, (
		rbt-get .rbs .var
		, !, .dow = .dox, fc-remove-unref-vars .value0 .value1 .rbw/.rbx
		; .do2 = .dox, .rbw = .rbx
	)
	, !
#
fc-remove-unref-vars (VAR .var) (VAR .var) .rb :- !, rbt-bind .var .rb #
fc-remove-unref-vars .p0 .p1 .rb :- fc-rewrite .p0 .p1 .ts/(), fc-remove-unref-vars-list .ts .rb #

fc-remove-unref-vars-list () .rb/.rb #
fc-remove-unref-vars-list (.p0 .p1, .ts) .rb0/.rbx
	:- fc-remove-unref-vars .p0 .p1 .rb0/.rb1, fc-remove-unref-vars-list .ts .rb1/.rbx
#

fc-define-var
(DEF-VAR .var0 .value0 .do0) .var0 .value0 .do0
(DEF-VAR .var1 .value1 .do1) .var1 .value1 .do1
#
fc-define-var
(PRAGMA ALLOW-RECURSIVE DEF-VAR .var0 .value0 .do0) .var0 .value0 .do0
(PRAGMA ALLOW-RECURSIVE DEF-VAR .var1 .value1 .do1) .var1 .value1 .do1
#

fc-replace-var-by-value .v0 .value (VAR .v0) .value
	:- !
#
fc-replace-var-by-value .v0 _ (PRAGMA ALLOW-RECURSIVE (DEF-VAR .v0 .value .do)) (DEF-VAR .v0 .value .do)
	:- ! -- Variable scope overrided by another one
#
fc-replace-var-by-value .v0 .v1 (DEF-VAR .v0 .value0 .do) (DEF-VAR .v0 .value1 .do)
	:- ! -- Variable scope overrided in value field only
	, fc-replace-var-by-value .v0 .v1 .value0 .value1
#
fc-replace-var-by-value .v0 .v1 .p0 .p1
	:- fc-rewrite .p0 .p1 .ts/()
	, fc-replace-var-by-value-list .v0 .v1 .ts
#

fc-replace-var-by-value-list _ _ () #
fc-replace-var-by-value-list .v0 .v1 (.t, .ts)
	:- fc-replace-var-by-value .v0 .v1 .t
	, fc-replace-var-by-value-list .v0 .v1 .ts
#
