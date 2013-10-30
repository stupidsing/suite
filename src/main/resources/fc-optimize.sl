fc-optimize-disabled (DEF-VAR .var .value .do0) .dox
	:- once (
		(complexity .value .c; complexity .do0 .c)
		, .c < 4
	)
	, not contains .value (VAR .var)
	, dump "Inlining" .var "=" .value "in" .do0, nl
	, fc-optimize-substitution .var .value .do0 .dox
#
fc-optimize (INVOKE .value (FUN .var .do0)) .dox
	:- !, fc-optimize-substitution .var .value .do0 .dox
#
fc-optimize .p0 .p1 :- fc-transform .p0 .p1 ()/.ts, fc-optimize-list .ts #

fc-optimize-list () #
fc-optimize-list (.t, .ts) :- fc-optimize .t, fc-optimize-list .ts #

fc-optimize-substitution .var .value .do0 .do1
	:- replace (VAR .var) .value .do0 .do1
	, fc-optimize .do1 .dox
#
