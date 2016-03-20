fc-lazyify-fun .p0 .px :- fc-lazyify .p0 .p1, .px = UNWRAP .p1 #

fc-lazyify .do0 .dox
	:- fc-lazyify-default-fun 0 _ .returnWrap .do0 .do1
	, !
	, once (.returnWrap = THUNK, .do1 = .dox
		; .returnWrap = VALUE, .dox = WRAP .do1
	)
#
fc-lazyify .const (WRAP .const)
	:- .const = .tag _
	, member (ATOM, BOOLEAN, CHARS, NUMBER,) .tag, !
#
fc-lazyify (APPLY .value0 .callee0) (WRAP UNWRAP (APPLY .value1 (UNWRAP .callee1)))
	:- !
	, fc-lazyify .value0 .value1
	, fc-lazyify .callee0 .callee1
#
fc-lazyify (CONS .type .head0 .tail0) (WRAP (CONS .type .headx .tailx))
	:- !
	, fc-lazyify .head0 .headx
	, fc-lazyify .tail0 .tailx
#
fc-lazyify
(DECONS .type .cons0 .headVar .tailVar .then0 .else0)
(DECONS .type (UNWRAP .consx) .headVar .tailVar .thenx .elsex)
	:- !
	, fc-lazyify .cons0 .consx
	, fc-lazyify .then0 .thenx
	, fc-lazyify .else0 .elsex
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
fc-lazyify NIL (WRAP NIL)
	:- !
#
fc-lazyify (TREE .oper .left0 .right0) .dox
	:- !
	, fc-lazyify .left0 .left1
	, fc-lazyify .right0 .right1
	, .dox = WRAP (TREE .oper (UNWRAP .left1) (UNWRAP .right1))
#
fc-lazyify (USING _ .linkOption .lib .do0) (USING LAZY .linkOption .lib .do1)
	:- !
	, fc-lazyify .do0 .do1
#
fc-lazyify .p0 .p1
	:- fc-rewrite .p0 .p1 .ts/()
	, list.query .ts (.t0 .t1) (fc-lazyify .t0 .t1)
#

fc-lazyify-default-fun .n .paramWraps .returnWrap .var .var
	:- .var = VAR .v
	, member (
		+compare/2/VALUE:VALUE:/VALUE,
		+get%i/1/VALUE:/VALUE,
		+call%i-t1/2/VALUE:THUNK:/THUNK,
		+call%i-t2/3/VALUE:THUNK:THUNK:/THUNK,
		+call%i-t3/4/VALUE:THUNK:THUNK:THUNK:/THUNK,
		+call%i-v1/2/VALUE:VALUE:/VALUE,
		+call%i-v2/3/VALUE:VALUE:VALUE:/VALUE,
		+call%i-v3/4/VALUE:VALUE:VALUE:VALUE:/VALUE,
		+is-list/1/VALUE:/VALUE,
		+is-pair/1/VALUE:/VALUE,
		+lhead/1/VALUE:/THUNK,
		+ltail/1/VALUE:/THUNK,
		+pleft/1/VALUE:/THUNK,
		+pright/1/VALUE:/THUNK,
	) .v/.n/.paramWraps/.returnWrap
	, fc-define-default-fun .n .v _
	, !
#
fc-lazyify-default-fun .n .paramWraps .returnWrap (APPLY .p0 .chain0) (APPLY .px .chain1)
	:- let .n1 (.n + 1)
	, fc-lazyify .p0 .p1
	, fc-lazyify-default-fun .n1 .paramWrap:.paramWraps .returnWrap .chain0 .chain1
	, once (.paramWrap = THUNK, .px = .p1
		; .paramWrap = VALUE, .px = UNWRAP .p1
	)
#
