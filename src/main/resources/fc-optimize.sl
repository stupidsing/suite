fc-optimize .do0 .dox
	:- fc-optimize-flow .do0 .do1
	, fc-remove-unref-vars .do1 .dox ()/_
#

fc-optimize-flow-disabled .do0 .dox
	:- fc-define-var .do0 .var .value .do1
	, once (
		(complexity .value .c; complexity .do1 .c)
		, .c < 4
	)
	, not contains .value (VAR .var)
	, dump "Inlining" .var "=" .value "in" .do1, nl
	, fc-optimize-flow-substitution .var .value .do1 .dox
#
fc-optimize-flow (INVOKE .value (FUN .var .do0)) .dox
	:- !, fc-optimize-flow-substitution .var .value .do0 .dox
#
fc-optimize-flow .p0 .p1 :- fc-transform .p0 .p1 ()/.ts, fc-optimize-flow-list .ts #

fc-optimize-flow-list () #
fc-optimize-flow-list (.t, .ts) :- fc-optimize-flow .t, fc-optimize-flow-list .ts #

fc-optimize-flow-substitution .var .value .do0 .do1
	:- replace (VAR .var) .value .do0 .do1
	, fc-optimize-flow .do1 .dox
#

-- Remove unreferenced variables
fc-remove-unref-vars .do0 .dox .rb0/.rbx
	:- fc-define-var .do0 .var _ .do1
	, fc-remove-unref-vars .do1 .dox ()/.rbs
	, not rbt-get .rbs .var
	, !, rbt-merge-bind .rb0 .rbs .rbx
#
fc-remove-unref-vars (VAR .var) (VAR .var) .rb :- !, rbt-bind .var .rb #
fc-remove-unref-vars .p0 .p1 .rb :- fc-transform .p0 .p1 ()/.ts, fc-remove-unref-vars-list .ts .rb #

fc-remove-unref-vars-list () .rb/.rb #
fc-remove-unref-vars-list (.p0 .p1, .ts) .rb0/.rbx
	:- fc-remove-unref-vars .p0 .p1 .rb0/.rb1, fc-remove-unref-vars-list .ts .rb1/.rbx
#

fc-define-var (DEF-VAR .var .value .do) .var .value .do #
fc-define-var (OPTION ALLOW-RECURSIVE DEF-VAR .var .value .do) .var .value .do #
