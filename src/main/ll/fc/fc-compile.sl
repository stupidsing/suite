-------------------------------------------------------------------------------
-- eager evaluated functional program compiler

fc-compile .do .env .cr
	:- fc-call-default-fun .do .env .cr 0, !
#
fc-compile (APPLY .parameter .callee) .env .c0/.cx/.reg
	:- fc-compile .callee .env .c0/.c1/.r0
	, fc-compile .parameter .env .c1/.c2/.r1
	, .c2 = (PUSH .r1
		, CALL-THUNK .r0
		, ASSIGN-RESULT .reg
		, .cx)
#
fc-compile (ATOM .a) _ .c0/.cx/.reg
	:- .c0 = (ASSIGN-CONSTANT .reg c:.a, .cx)
#
fc-compile (BOOLEAN .b) _ .c0/.cx/.reg
	:- .c0 = (ASSIGN-CONSTANT .reg c:.b, .cx)
#
fc-compile (CHARS .s) _ .c0/.cx/.reg
	:- .c0 = (ASSIGN-CONSTANT .stringReg c:.s, DATA-CHARS .reg .stringReg, .cx)
#
fc-compile (CONS .type .head .tail) .env .cr
	:- member (L/+lcons, P/+pcons,) .type/.fun
	, fc-compile (APPLY .tail (APPLY .head (VAR .fun))) .env .cr
#
fc-compile (DECONS .type .cons .headVar .tailVar .then .else) .frame/.ve0 .c0/.cx/.reg
	:- member (L/IF-NOT-CONS, P/IF-NOT-PAIR,) .type/.insn
	, fc-compile .cons .frame/.ve0 .c0/.c1/.consReg
	, .c1 = (.insn .consReg l:.c3
		, POP .tailReg
		, POP .headReg
		, .c2)
	, fc-dict-add .headVar/(%REG/.headReg/.frame) .ve0/.ve1
	, fc-dict-add .tailVar/(%REG/.tailReg/.frame) .ve1/.ve2
	, fc-compile .then .frame/.ve2 .c2/.cx/.reg
	, fc-compile .else .frame/.ve0 .c3/.cx/.reg
#
fc-compile (DEF-VARS .vvs .do) .frame/.ve .c0/.cx/.reg
	:- fc-define-vars .vvs .vrs .frame .ve/.ve1
	, fc-compile-vars .vrs .frame/.ve1 .c0/.c1
	, fc-compile .do .frame/.ve1 .c1/.cx/.reg
#
fc-compile (ERROR _) _ .c0/.cx/.reg
	:- .c0 = (ERROR .reg, .cx)
#
fc-compile (FUN .var .do) .frame/.ve .c0/.cx/.thunkReg
	:- .c0 = (ASSIGN-THUNK .thunkReg l:(FRAME l:.f,), .cx)
	, .f0 = (ENTER, POP .varReg, .f1)
	, .frame1 = .frame + 1
	, fc-dict-add .var/(%REG/.varReg/.frame1) .ve/.ve1
	, fc-compile .do .frame1/.ve1 .f1/.f2/.returnReg
	, .f2 = (SET-RESULT .returnReg
		, LEAVE
		, RETURN
		,)
	, cg-optimize .f0 .f
#
fc-compile (IF .if .then .else) .env .c0/.cx/.reg
	:- fc-compile .if .env .c0/.c1/.ifReg
	, .c1 = (IF-FALSE .ifReg l:.c4, .c2)
	, fc-compile .then .env .c2/.c3/.thenReg
	, .c3 = (ASSIGN-FRAME-REG .reg 0 .thenReg, .cx)
	, fc-compile .else .env .c4/.c5/.elseReg
	, .c5 = (ASSIGN-FRAME-REG .reg 0 .elseReg, .cx)
#
fc-compile NIL _ .c0/.cx/.reg
	:- .c0 = (ASSIGN-CONSTANT .reg c:(), .cx)
#
fc-compile (NUMBER .i) _ .c0/.cx/.reg
	:- .c0 = (ASSIGN-INT .reg .i, .cx)
#
fc-compile (PRAGMA _ .do) .env .cr
	:- fc-compile .do .env .cr
#
fc-compile (TCO .iter .in) .env .c0/.cx/.reg
	:- fc-compile .iter .env .c0/.c1/.riter
	, fc-compile .in .env .c1/.c2/.rin
	, .c2 = (PUSH .rin
		, CALL-THUNK .riter
		, ASSIGN-RESULT .rpair0
		, PUSH .rpair0
		, HEAD .rhead 1
		, PUSH .rpair0
		, TAIL .rpair1 1
		, PUSH .rpair1
		, HEAD .rin 1
		, IF-FALSE .rhead l:.c2
		, PUSH .rpair1
		, TAIL .reg 1
		, .cx)
#
fc-compile (TREE .oper .left .right) .env .c0/.cx/.reg
	:- fc-compile .left .env .c0/.c1/.r0
	, fc-compile .right .env .c1/.c2/.r1
	, .c2 = (EVALUATE .reg .r0 .oper .r1, .cx)
#
fc-compile (UNWRAP .callee) .env .c0/.cx/.reg
	:- fc-compile .callee .env .c0/.c1/.thunkReg
	, .c1 = (CALL-THUNK .thunkReg
		, ASSIGN-THUNK-RESULT .reg .thunkReg
		, .cx)
#
fc-compile (USING .mode BUILTIN .lib .do) .fve .cr
	:- fc-load-precompiled-library .lib (_ # .eagerPred # .lazyPred #)
	, once (.mode = EAGER, .pred = .eagerPred; .pred = .lazyPred)
	, clone .pred (fc-compile-using-lib .mode .lib .do .fve .cr :- .tail)
	, once .tail
#
fc-compile (USING _ EXTERNAL _ .do) .env .cr
	:- !, fc-compile .do .env .cr
#
fc-compile (VAR .var) .frame/.ve .c0/.cx/.reg1
	:- fc-dict-get .ve .var/(%REG/.reg/.frame0)
	, !, fc-frame-difference .frame0 .frame .frameDiff
	, (.frameDiff = 0, !, .c0 = .cx, .reg = .reg1
		; .c0 = (ASSIGN-FRAME-REG .reg1 .frameDiff .reg, .cx)
	)
#
fc-compile (WRAP .do) .frame/.ve .c0/.cx/.thunkReg
	:- .c0 = (ASSIGN-THUNK .thunkReg l:(FRAME l:(ENTER, .f),), .cx)
	, fc-compile .do (.frame + 1)/.ve .f0/.f1/.returnReg
	, .f1 = (SET-RESULT .returnReg
		, LEAVE
		, RETURN
		,)
	, cg-optimize .f0 .f
#

fc-define-vars (.var .value, .vvs) (.value .varReg, .vrs) .frame .ve0/.vex
	:- fc-dict-add .var/(%REG/.varReg/.frame) .ve0/.ve1
	, fc-define-vars .vvs .vrs .frame .ve1/.vex
#
fc-define-vars () () _ .ve/.ve
#

fc-compile-vars (.value .varReg, .vrs) .env .c0/.cx
	:- fc-compile .value .env .c0/.c1/.varReg
	, fc-compile-vars .vrs .env .c1/.cx
#
fc-compile-vars () _ .c/.c
#

fc-call-default-fun (APPLY .p .pred) .env .c0/.cx/.reg .n
	:- !, let .n1 (.n + 1)
	, fc-compile .p .env .c0/.c1/.r1
	, .c1 = (PUSH .r1, .c2)
	, fc-call-default-fun .pred .env .c2/.cx/.reg .n1
#
fc-call-default-fun (VAR .pred) _ .c0/.cx/.reg .n
	:- fc-define-default-fun .n .pred .call, !
	, .c0 = (.call .reg .n, .cx)
#
