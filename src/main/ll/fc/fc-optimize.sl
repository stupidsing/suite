fc-optimize .do0 .dox
	:- fc-optimize-flow .do0 .do1
	, fc-remove-unref-vars .do1 .dox ()/_
#

fc-optimize-flow .do0 .dox
	:- fc-define-var .do0 .var .value .do1 .dox .var .value .do3
	, .value = .type _
	, member (ATOM, BOOLEAN, CHARS, NUMBER, VAR,) .type
	, not (contains (VAR .var) .value)
	, !, fc-replace-var-by-value .var .value .do1 .do2
	, fc-optimize-flow .do2 .do3
#
fc-optimize-flow (APPLY .value (FUN .var .do0)) .dox
	:- !, fc-optimize-flow (DEF-VARS (.var .value,) .do0) .dox
#
fc-optimize-flow (UNWRAP (WRAP .do0)) .dox
	:- !, fc-optimize-flow .do0 .dox
#
fc-optimize-flow .p0 .p1
	:- fc-rewrite .p0 .p1 .ts/()
	, list.query .ts (.t0 .t1) (fc-optimize-flow .t0 .t1)
#

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
fc-remove-unref-vars (VAR .var) (VAR .var) .rb
	:- !, rbt-bind .var .rb
#
fc-remove-unref-vars .p0 .p1 .rb0/.rbx
	:- fc-rewrite .p0 .p1 .ts/()
	, list.fold .ts/.rb0/.rbx (.p0_ .p1_)/.rb0_/.rbx_ (fc-remove-unref-vars .p0_ .p1_ .rb0_/.rbx_)
#

fc-define-var
(DEF-VARS (.var0 .value0,) .do0) .var0 .value0 .do0
(DEF-VARS (.var1 .value1,) .do1) .var1 .value1 .do1
#
fc-define-var
(PRAGMA DEF-OUTSIDE DEF-VARS (.var0 .value0,) .do0) .var0 .value0 .do0
(PRAGMA DEF-OUTSIDE DEF-VARS (.var1 .value1,) .do1) .var1 .value1 .do1
#

fc-replace-var-by-value .v0 .value (VAR .v0) .value
	:- !
#
fc-replace-var-by-value .v0 _ (PRAGMA DEF-OUTSIDE (DEF-VARS (.v0 .value,) .do)) (DEF-VARS (.v0 .value,) .do)
	:- ! -- Variable scope overrode by another one
#
fc-replace-var-by-value .v0 .v1 (DEF-VARS (.v0 .value0,) .do) (DEF-VARS (.v0 .value1,) .do)
	:- ! -- Variable scope overrode in value field only
	, fc-replace-var-by-value .v0 .v1 .value0 .value1
#
fc-replace-var-by-value .v0 .v1 .p0 .p1
	:- fc-rewrite .p0 .p1 .ts/()
	, list.query .ts (.t0 .t1) (fc-replace-var-by-value .v0 .v1 .t0 .t1)
#
