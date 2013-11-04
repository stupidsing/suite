fc-lazyify-fun .p0 (UNWRAP .p1) :- fc-lazyify .p0 .p1 #

fc-lazyify .const (WRAP .const)
	:- .const = .tag .value
	, member (ATOM, BOOLEAN, NUMBER,) .tag, !
#
fc-lazyify (FUN .var .do0) (WRAP (FUN .var .do1))
	:- !
	, fc-lazyify .do0 .do1
#
fc-lazyify (IF .if0 .then0 .else0) (WRAP UNWRAP (IF (UNWRAP .if1) .then1 .else1))
	:- !
	, fc-lazyify .if0 .if1
	, fc-lazyify .then0 .then1
	, fc-lazyify .else0 .else1
#
fc-lazyify .do0 .dox
	:- fc-lazyify-default-fun 0 _ .returnWrap .do0 .do1
	, !
	, once (.returnWrap = THUNK, .do1 = .dox
		; .returnWrap = VALUE, .dox = WRAP .do1
	)
#
fc-lazyify (INVOKE .value0 .callee0) (WRAP UNWRAP (INVOKE .value1 (UNWRAP .callee1)))
	:- !
	, fc-lazyify .value0 .value1
	, fc-lazyify .callee0 .callee1
#
fc-lazyify (PAIR .left0 .right0) (WRAP (PAIR .leftx .rightx))
	:- !
	, fc-lazyify .left0 .leftx
	, fc-lazyify .right0 .rightx
#
fc-lazyify (TREE .oper .left0 .right0) .dox
	:- !
	, fc-lazyify .left0 .left1
	, fc-lazyify .right0 .right1
	, .dox = WRAP (TREE .oper (UNWRAP .left1) (UNWRAP .right1))
#
fc-lazyify .p0 .p1 :- fc-transform .p0 .p1 ()/.ts, fc-lazyify-list .ts #

fc-lazyify-list () #
fc-lazyify-list (.t, .ts) :- fc-lazyify .t, fc-lazyify-list .ts #

fc-lazyify-default-fun .n .paramWraps .returnWrap (VAR .var) (VAR .var)
	:- member (
		_compare/2/VALUE:VALUE:/VALUE,
		_ijavacls/1/VALUE:/VALUE,
		_ijavaobj0/1/VALUE:/VALUE,
		_ijavaobj1/2/VALUE:VALUE:/VALUE,
		_ijavaobj2/3/VALUE:VALUE:VALUE:/VALUE,
		_lcons/2/THUNK:THUNK:/VALUE,
		_lhead/1/VALUE:/THUNK,
		_ltail/1/VALUE:/THUNK,
		_pcons/2/THUNK:THUNK:/VALUE,
		_pleft/1/VALUE:/THUNK,
		_pright/1/VALUE:/THUNK,
		is-list/1/VALUE:/VALUE,
		is-pair/1/VALUE:/VALUE,
	) .var/.n/.paramWraps/.returnWrap
	, fc-define-default-fun .n .var .call
	, !
#
fc-lazyify-default-fun .n .paramWraps .returnWrap (INVOKE .p0 .chain0) (INVOKE .px .chain1)
	:- let .n1 (.n + 1)
	, fc-lazyify .p0 .p1
	, fc-lazyify-default-fun .n1 .paramWrap:.paramWraps .returnWrap .chain0 .chain1
	, once (.paramWrap = THUNK, .p1 = .px
		; .paramWrap = VALUE, .px = UNWRAP .p1
	)
#
