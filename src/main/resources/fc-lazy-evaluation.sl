-------------------------------------------------------------------------------
-- functional program compiler with lazy evaluation

fc-compile LAZY .do .env .c0/.cx/.d0/.dx/.reg
	:- fc-lazy-compile .do .env .c0/.cx/.d0/.dx/.reg
#

fc-lazy-compile .do .env .c0/.cx/.d0/.dx/.reg
	:- fc-lazy-compile-to-thunk .do .env .c0/.c1/.d0/.dx/.closureReg
	, .c1 = (_ CALL-CLOSURE .reg .closureReg
		, _ SET-CLOSURE-RESULT .closureReg .reg
		, .cx
	)
#

fc-lazy-compile-to-thunk (OPTION _ .do) .env .cdr
	:- !, fc-lazy-compile-to-thunk .do .env .cdr
#
fc-lazy-compile-to-thunk (DEF-VAR .var .value .do) .frame/.ve .c0/.cx/.d0/.dx/.reg
	:- !
	, .env1 = .frame/(.var/(%REG/.r1/.frame), .ve)
	, fc-lazy-compile-to-thunk .value .env1 .c0/.c1/.d0/.d1/.r1
	, fc-lazy-compile-to-thunk .do .env1 .c1/.cx/.d1/.dx/.reg
#
fc-lazy-compile-to-thunk (IF .if .then .else) .env .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-lazy-compile .if .env .c0/.c1/.d0/.d1/.ifReg
	, .c1 = (_ IF-FALSE .label1 .ifReg, .c2)
	, fc-lazy-compile-to-thunk .then .env .c2/.c3/.d1/.d2/.thenReg
	, .c3 = (_ ASSIGN-FRAME-REG .reg 0 .thenReg
		, _ JUMP .label2
		, .label1 LABEL .label1
		, .c4
	)
	, fc-lazy-compile-to-thunk .else .env .c4/.c5/.d2/.dx/.elseReg
	, .c5 = (_ ASSIGN-FRAME-REG .reg 0 .elseReg
		, .label2 LABEL .label2
		, .cx
	)
#
fc-lazy-compile-to-thunk (VARIABLE .var) .frame/.ve .c0/.cx/.d/.d/.reg1
	:- member .ve .var/(%REG/.reg/.frame0)
	, !, fc-frame-difference .frame0 .frame .frameDiff
	, (.frameDiff = 0, !, .c0 = .cx, .reg = .reg1
		; .c0 = (_ ASSIGN-FRAME-REG .reg1 .frameDiff .reg, .cx)
	)
#
fc-lazy-compile-to-thunk (INVOKE .p (VARIABLE .var)) .env .c0/.cx/.d0/.dx/.reg
	:- member (_lhead, _ltail, _thead, _ttail,) .var
	, fc-define-default-fun 1 .var .call
	, !, fc-lazy-compile .p .env .c0/.c1/.d0/.dx/.paramReg
	, .c1 = (_ PUSH .paramReg, _ SERVICE .reg .call 1, .cx)
#
fc-lazy-compile-to-thunk (TUPLE .name (.e, .es)) .env .cdr
	:- !, fc-lazy-compile-to-thunk (
		INVOKE (TUPLE .name .es) (INVOKE .e (VARIABLE _cons))
	) .env .cdr
#
fc-lazy-compile-to-thunk .do .frame/.ve .c0/.cx/.d0/.dx/.closureReg
	:- .c0 = (_ ASSIGN-CLOSURE .closureReg .funcLabel, .cx)
	, .d0 = (.funcLabel ENTER, .d1)
	, fc-lazy-compile-to-value .do (.frame + 1)/.ve .d1/.d2/.d3/.dx/.returnReg
	, .d2 = (_ RETURN-VALUE .returnReg, _ LEAVE, .d3)
	, !
#

fc-lazy-compile-to-value (FUN .var .do) .frame/.ve .c0/.cx/.d0/.dx/.reg
	:- !
	, .c0 = (_ ASSIGN-CLOSURE .reg .funcLabel, .cx)
	, .d0 = (.funcLabel ENTER, .d1)
	, .d1 = (_ POP .varReg, .d2)
	, .frame1 = .frame + 1
	, .env1 = .frame1/(.var/(%REG/.varReg/.frame1), .ve)
	, fc-lazy-compile-to-thunk .do .env1 .d2/.d3/.d4/.dx/.returnReg
	, .d3 = (_ RETURN-VALUE .returnReg, _ LEAVE, .d4)
#
fc-lazy-compile-to-value .do .env .cdr
	:- fc-lazy-compile-default-fun 0 _ .do .env .cdr
#
fc-lazy-compile-to-value (INVOKE .parameter .callee) .env .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-lazy-compile .callee .env .c0/.c1/.d0/.d1/.r1
	, fc-lazy-compile-to-thunk .parameter .env .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ PUSH .r2
		, _ CALL-CLOSURE .closureReg .r1
		, _ CALL-CLOSURE .reg .closureReg
		, _ SET-CLOSURE-RESULT .closureReg .reg
		, .cx
	)
#
fc-lazy-compile-to-value (TUPLE .name ()) .env .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-CONSTANT .reg .name, .cx)
#
fc-lazy-compile-to-value (TREE .oper .left .right) .env .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-lazy-compile .left .env .c0/.c1/.d0/.d1/.r1
	, fc-lazy-compile .right .env .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ EVALUATE .reg .r1 .oper .r2, .cx)
#
fc-lazy-compile-to-value (CONSTANT .c) _ .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-CONSTANT .reg .c, .cx)
#
fc-lazy-compile-to-value (BOOLEAN .b) _ .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-BOOL .reg .b, .cx)
#
fc-lazy-compile-to-value (NUMBER .i) _ .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-INT .reg .i, .cx)
#
fc-lazy-compile-to-value (STRING .s) _ .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-STR .reg .s, .cx)
#

fc-lazy-compile-default-fun .n .paramWraps (VARIABLE .var) .env .c0/.cx/.d/.d/.reg
	:- member (
		_compare/2/VALUE:VALUE:,
		_cons/2/THUNK:THUNK:,
		_prove/1/VALUE:,
		_subst/2/VALUE:VALUE:,
		fflush/1/VALUE:,
		fgetc/1/VALUE:,
		fputc/3/VALUE:VALUE:VALUE:,
		is-tree/1/VALUE:,
	) .var/.n/.paramWraps
	, fc-define-default-fun .n .var .call
	, !, .c0 = (_ SERVICE .reg .call .n, .cx)
#
fc-lazy-compile-default-fun .n .paramWraps (INVOKE .p .chain) .env .c0/.cx/.d0/.dx/.reg
	:- let .n1 (.n + 1)
	, fc-lazy-compile-default-fun .n1 .paramWrap:.paramWraps .chain .env .c2/.cx/.d1/.dx/.reg
	, (.paramWrap = THUNK
		, fc-lazy-compile-to-thunk .p .env .c0/.c1/.d0/.d1/.paramReg
	; fc-lazy-compile .p .env .c0/.c1/.d0/.d1/.paramReg
	)
	, .c1 = (_ PUSH .paramReg, .c2)
#
