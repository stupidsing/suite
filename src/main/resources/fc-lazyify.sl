--fc-lazyify .const (FUN .var .const)
--	:- .const = .tag .value
--	, member (ATOM, BOOLEAN, NUMBER,) .tag
--	, !, temp .var
--#
--fc-lazyify (TREE .oper .left0 .right0) (TREE .oper .left1 .right1)
--	:- !
--	, .left1 = INVOKE (NUMBER 0) .left0
--	, .right1 = INVOKE (NUMBER 0) .right0
--#
--fc-lazyify .do0 .dox :- fc-lazy-compile-default-fun 0 _ .do0 .dox #
fc-lazyify .p0 .p1 :- fc-transform .p0 .p1 ()/.ts, fc-lazyify-list .ts #

fc-lazyify-list () #
fc-lazyify-list (.t, .ts) :- fc-lazyify .t, fc-lazyify-list .ts #

fc-lazyify-default-fun .n .paramWraps (VAR .var) (VAR .var)
	:- member (
		_compare/2/VALUE:VALUE:,
		_ijavacls/1/THUNK:,
		_ijavaobj0/1/THUNK:,
		_ijavaobj1/2/THUNK:THUNK:,
		_ijavaobj2/3/THUNK:THUNK:THUNK:,
		_lcons/2/THUNK:THUNK:,
		_pcons/2/THUNK:THUNK:,
		is-list/1/VALUE:,
		is-pair/1/VALUE:,
	) .var/.n/.paramWraps
	, fc-define-default-fun .n .var .call
	, !
#
fc-lazyify-default-fun .n .paramWraps (INVOKE .p0 .chain0) (INVOKE .p1 .chain1)
	:- let .n1 (.n + 1)
	, fc-lazyify-default-fun .n1 .paramWrap:.paramWraps .chain0 .chain1
	, (.paramWrap = VALUE, .p0 = .p1
		; .p1 = INVOKE (NUMBER 0) .p0
	)
#
