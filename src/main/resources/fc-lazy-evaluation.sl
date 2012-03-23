-------------------------------------------------------------------------------
-- functional program compiler with lazy evaluation

fc-compile LAZY .do .env .c0/.cx/.d0/.dx/.reg
	:- fc-lazy-compile .do .env .c0/.cx/.d0/.dx/.reg
#

fc-lazy-compile .do .env .c0/.cx/.d0/.dx/.reg
	:- fc-lazy-compile0 .do .env .c0/.c1/.d0/.dx/.closureReg
	, .c1 = (_ CALL-CLOSURE .reg .closureReg, .cx)
#

fc-lazy-compile0 (CAST _ .do) .env .cdr :- !, fc-lazy-compile0 .do .env .cdr #
fc-lazy-compile0 (AS _ _ .do) .env .cdr :- !, fc-lazy-compile0 .do .env .cdr #
fc-lazy-compile0 (DEF-VAR .var .value .do) .frame/.ve .c0/.cx/.d0/.dx/.reg
	:- !
	, .env1 = .frame/(.var/(%REG/.r1/.frame), .ve)
	, fc-lazy-compile0 .value .env1 .c0/.c1/.d0/.d1/.r1
	, fc-lazy-compile0 .do .env1 .c1/.cx/.d1/.dx/.reg
#
fc-lazy-compile0 (IF .if .then .else) .env .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-lazy-compile .if .env .c0/.c1/.d0/.d1/.ifReg
	, .c1 = (_ IF-FALSE .label1 .ifReg, .c2)
	, fc-lazy-compile0 .then .env .c2/.c3/.d1/.d2/.thenReg
	, .c3 = (_ ASSIGN-FRAME-REG .reg 0 .thenReg
		, _ JUMP .label2
		, .label1 LABEL .label1
		, .c4
	)
	, fc-lazy-compile0 .else .env .c4/.c5/.d2/.dx/.elseReg
	, .c5 = (_ ASSIGN-FRAME-REG .reg 0 .elseReg
		, .label2 LABEL .label2
		, .cx
	)
#
fc-lazy-compile0 (VARIABLE .var) .frame/.ve .c0/.cx/.d/.d/.reg1
	:- member .ve .var/(%REG/.reg/.frame0), !
	, (.frame = .frame0, !, .c0 = .cx, .reg = .reg1
		; let .frameDifference (.frame0 - .frame)
		, .c0 = (_ ASSIGN-FRAME-REG .reg1 .frameDifference .reg, .cx)
	)
#
fc-lazy-compile0 (INVOKE .p (VARIABLE .var)) .env .c0/.cx/.d0/.dx/.reg
	:- member (head, tail,) .var -- Special list processing function
	, fc-define-default-fun 1 .var .call, !
	, fc-lazy-compile .p .env .c0/.c1/.d0/.dx/.paramReg
	, .c1 = (_ PUSH .paramReg, _ SYS .reg .call 1, .cx)
#
fc-lazy-compile0 .do .frame/.ve .c0/.cx/.d0/.dx/.closureReg
	:- .c0 = (_ ASSIGN-CLOSURE .closureReg .funcLabel, .cx)
	, let .frame1 (.frame + 1)
	, .d0 = (.funcLabel ENTER, .d1)
	, fc-lazy-compile-wrapped .do .frame1/.ve .d1/.d2/.d3/.dx/.returnReg
	, .d2 = (_ RETURN-VALUE .returnReg, _ LEAVE, .d3)
	, !
#

fc-lazy-compile-wrapped (FUN .var .do) .frame/.ve .c0/.cx/.d0/.dx/.reg
	:- !
	, .c0 = (_ ASSIGN-CLOSURE .reg .funcLabel, .cx)
	, let .frame1 (.frame + 1)
	, .d0 = (.funcLabel ENTER, .d1)
	, .d1 = (_ POP .varReg, .d2)
	, .env1 = .frame1/(.var/(%REG/.varReg/.frame1), .ve)
	, fc-lazy-compile0 .do .env1 .d2/.d3/.d4/.dx/.returnReg
	, .d3 = (_ RETURN-VALUE .returnReg, _ LEAVE, .d4)
#
fc-lazy-compile-wrapped (INVOKE .p (VARIABLE .var)) .env .c0/.cx/.d0/.dx/.reg
	:- member (is-tree,) .var -- Special list processing function
	, fc-define-default-fun 1 .var .call, !
	, fc-lazy-compile .p .env .c0/.c1/.d0/.dx/.paramReg
	, .c1 = (_ PUSH .paramReg, _ SYS .reg .call 1, .cx)
#
fc-lazy-compile-wrapped (INVOKE .p0 (INVOKE .p1 (VARIABLE .var))) .env .c0/.cx/.d0/.dx/.reg
	:- member (cons,) .var -- Special list processing function
	, fc-define-default-fun 2 .var .call, !
	, fc-lazy-compile0 .p0 .env .c0/.c1/.d0/.d1/.param0Reg
	, fc-lazy-compile0 .p1 .env .c1/.c2/.d1/.dx/.param1Reg
	, .c2 = (_ PUSH .param0Reg, _ PUSH .param1Reg, _ SYS .reg .call 2, .cx)
#
fc-lazy-compile-wrapped (INVOKE .parameter .callee) .env .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-lazy-compile .callee .env .c0/.c1/.d0/.d1/.r1
	, fc-lazy-compile0 .parameter .env .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ PUSH .r2
		, _ CALL-CLOSURE .closureReg .r1
		, _ CALL-CLOSURE .reg .closureReg
		, .cx
	)
#
fc-lazy-compile-wrapped .do .env .cdr
	:- fc-lazy-default-fun .do .env .cdr, !
#
fc-lazy-compile-wrapped (TUPLE .name .es) .env .cdr
	:- !, fc-lazy-compile-tuple .name .es .env .cdr
#
fc-lazy-compile-wrapped (TREE .oper .left .right) .env .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-lazy-compile .left .env .c0/.c1/.d0/.d1/.r1
	, fc-lazy-compile .right .env .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ EVALUATE .reg .r1 .oper .r2, .cx)
#
fc-lazy-compile-wrapped (BOOLEAN .b) _ .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-BOOL .reg .b, .cx)
#
fc-lazy-compile-wrapped (NUMBER .i) _ .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-INT .reg .i, .cx)
#
fc-lazy-compile-wrapped (STRING .s) _ .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-STR .reg .s, .cx)
#

fc-lazy-compile-tuple .name () .env .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-CONSTANT .reg .name, .cx)
#
fc-lazy-compile-tuple .name (.e, .es) .env .c0/.cx/.d0/.dx/.reg
	:- !, fc-lazy-compile .e .env .c0/.c1/.d0/.d1/.headReg
	, fc-lazy-compile-tuple .name .es .env .c1/.c2/.d1/.dx/.tailReg
	, .c2 = (_ PUSH .headReg
		, _ PUSH .tailReg
		, _ SYS .reg CONS 2
		, .cx
	)
#

fc-lazy-default-fun .call .env .result
	:- fc-lazy-default-fun0 .call .env .result 0
#

fc-lazy-default-fun0 (INVOKE .p .pred) .env .c0/.cx/.d0/.dx/.reg .n
	:- !, let .n1 (.n + 1)
	, fc-lazy-compile .p .env .c0/.c1/.d0/.d1/.r1
	, .c1 = (_ PUSH .r1, .c2)
	, fc-lazy-default-fun0 .pred .env .c2/.cx/.d1/.dx/.reg .n1
#
fc-lazy-default-fun0 (VARIABLE .pred) _ .c0/.cx/.d/.d/.reg .n
	:- fc-define-default-fun .n .pred .call, !
	, .c0 = (_ SYS .reg .call .n, .cx)
#
