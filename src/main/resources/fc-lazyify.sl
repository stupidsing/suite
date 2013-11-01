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
--fc-lazyify .do0 .dox
--	:- fc-lazy-compile-default-fun 0 _ .returnWrap .do0 .do1
--	, (.returnWrap = THUNK, .do1 = .dox
--		; temp .var, .dox = FUN .var .do1
--	)
--#
fc-lazyify .p0 .p1 :- fc-transform .p0 .p1 ()/.ts, fc-lazyify-list .ts #

fc-lazyify-list () #
fc-lazyify-list (.t, .ts) :- fc-lazyify .t, fc-lazyify-list .ts #

fc-lazyify-default-fun .n .paramWraps .returnWrap (VAR .var) (VAR .var)
	:- member (
		_compare/2/VALUE:VALUE:/VALUE:,
		_ijavacls/1/THUNK:/VALUE:,
		_ijavaobj0/1/VALUE:/VALUE:,
		_ijavaobj1/2/VALUE:VALUE:/VALUE:,
		_ijavaobj2/3/VALUE:VALUE:VALUE:/VALUE:,
		_lcons/2/THUNK:THUNK:/VALUE:,
		_lhead/1/VALUE:/THUNK:,
		_ltail/1/VALUE:/THUNK:,
		_pcons/2/THUNK:THUNK:/VALUE:,
		_pleft/1/VALUE:/THUNK:,
		_pright/1/VALUE:/THUNK:,
		is-list/1/VALUE:/VALUE:,
		is-pair/1/VALUE:/VALUE:,
	) .var/.n/.paramWraps/.returnWrap
	, fc-define-default-fun .n .var .call
	, !
#
fc-lazyify-default-fun .n .paramWraps .returnWrap (INVOKE .p0 .chain0) (INVOKE .p1 .chain1)
	:- let .n1 (.n + 1)
	, fc-lazyify-default-fun .n1 .paramWrap:.paramWraps .returnWrap .chain0 .chain1
	, (.paramWrap = THUNK, .p0 = .p1
		; .p1 = INVOKE (NUMBER 0) .p0
	)
#
