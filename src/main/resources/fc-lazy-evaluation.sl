-------------------------------------------------------------------------------
-- functional program compiler with lazy evaluation

fc-compile LAZY .do .env .c0/.cx/.d0/.dx/.reg
	:- fc-lazy-compile .do .env .c0/.cx/.d0/.dx/.reg
#

fc-lazy-compile .do .env .c0/.cx/.d0/.dx/.reg
	:- fc-lazy-compile0 .do .env .c0/.c1/.d0/.dx/.closureReg
	, .c1 = (_ CALL-CLOSURE .reg .closureReg
		, _ SET-CLOSURE-RESULT .closureReg .reg
		, .cx
	)
#

fc-lazy-compile0 (OPTION _ .do) .env .cdr :- !, fc-lazy-compile0 .do .env .cdr #
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
	:- member .ve .var/(%REG/.reg/.frame0)
	, !, (.frame = .frame0, !, .c0 = .cx, .reg = .reg1
		; let .frameDifference (.frame0 - .frame)
		, .c0 = (_ ASSIGN-FRAME-REG .reg1 .frameDifference .reg, .cx)
	)
#
fc-lazy-compile0 (INVOKE .p (VARIABLE .var)) .env .c0/.cx/.d0/.dx/.reg
	:- member (_head, _tail,) .var -- Special list processing function
	, fc-define-default-fun 1 .var .call
	, !, fc-lazy-compile .p .env .c0/.c1/.d0/.dx/.paramReg
	, .c1 = (_ PUSH .paramReg, _ SERVICE .reg .call 1, .cx)
#
fc-lazy-compile0 (TUPLE .name (.e, .es)) .env .cdr
	:- !, fc-lazy-compile0 (
		INVOKE (TUPLE .name .es) (INVOKE .e (VARIABLE _cons))
	) .env .cdr
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
fc-lazy-compile-wrapped .do .env .cdr
	:- fc-lazy-compile-default-fun 0 _ .do .env .cdr
#
fc-lazy-compile-wrapped (INVOKE .parameter .callee) .env .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-lazy-compile .callee .env .c0/.c1/.d0/.d1/.r1
	, fc-lazy-compile0 .parameter .env .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ PUSH .r2
		, _ CALL-CLOSURE .closureReg .r1
		, _ CALL-CLOSURE .reg .closureReg
		, _ SET-CLOSURE-RESULT .closureReg .reg
		, .cx
	)
#
fc-lazy-compile-wrapped .do .env .cdr
	:- fc-lazy-default-fun .do .env .cdr, !
#
fc-lazy-compile-wrapped (TUPLE .name ()) .env .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-CONSTANT .reg .name, .cx)
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

fc-lazy-compile-default-fun .n .paramWrapped (VARIABLE .var) .env .c0/.cx/.d/.d/.reg
	:- member (
		_compare/2/PUNWRAPPED,
		_cons/2/PWRAPPED,
		fflush/1/PUNWRAPPED,
		fgetc/1/PUNWRAPPED,
		fputc/3/PUNWRAPPED,
		is-tree/1/PUNWRAPPED,
	) .var/.n/.paramWrapped
	, fc-define-default-fun .n .var .call
	, !, .c0 = (_ SERVICE .reg .call .n, .cx)
#
fc-lazy-compile-default-fun .n .paramWrapped (INVOKE .p .chain) .env .c0/.cx/.d0/.dx/.reg
	:- let .n1 (.n + 1)
	, fc-lazy-compile-default-fun .n1 .paramWrapped .chain .env .c2/.cx/.d1/.dx/.reg
	, (.paramWrapped = PWRAPPED
		, fc-lazy-compile0 .p .env .c0/.c1/.d0/.d1/.paramReg
	; fc-lazy-compile .p .env .c0/.c1/.d0/.d1/.paramReg
	)
	, .c1 = (_ PUSH .paramReg, .c2)
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
	, .c0 = (_ SERVICE .reg .call .n, .cx)
#
