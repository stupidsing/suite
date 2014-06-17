-------------------------------------------------------------------------------
-- eager evaluated functional program compiler

fc-compile .do .env .cdr
	:- fc-default-fun .do .env .cdr, !
#
fc-compile (ATOM .a) _ .c0/.cx/.d/.d/.reg
	:- .c0 = (ASSIGN-CONSTANT .reg c:.a, .cx)
#
fc-compile (BOOLEAN .b) _ .c0/.cx/.d/.d/.reg
	:- .c0 = (ASSIGN-CONSTANT .reg c:.b, .cx)
#
fc-compile (DEF-VAR .var .value .do) .frame/.ve .c0/.cx/.d0/.dx/.reg
	:- fc-dict-add .var/(%REG/.r1/.frame) .ve/.ve1
	, fc-compile .value .frame/.ve1 .c0/.c1/.d0/.d1/.r1
	, fc-compile .do .frame/.ve1 .c1/.cx/.d1/.dx/.reg
#
fc-compile (FUN .var .do) .frame/.ve .c0/.cx/.d0/.dx/.closureReg
	:- .c0 = (ASSIGN-CLOSURE .closureReg l:.d0, .cx)
	, .d0 = (PROC .f0, .dx)
	, .f0 = (POP .varReg, .f1)
	, .frame1 = .frame + 1
	, fc-dict-add .var/(%REG/.varReg/.frame1) .ve/.ve1
	, fc-compile .do .frame1/.ve1 .f1/.f2/.f3/()/.returnReg
	, .f2 = (RETURN-VALUE .returnReg, .f3)
#
fc-compile (IF .if .then .else) .env .c0/.cx/.d0/.dx/.reg
	:- fc-compile .if .env .c0/.c1/.d0/.d1/.ifReg
	, .c1 = (IF-FALSE l:.c4 .ifReg, .c2)
	, fc-compile .then .env .c2/.c3/.d1/.d2/.thenReg
	, .c3 = (ASSIGN-FRAME-REG .reg 0 .thenReg, .cx)
	, fc-compile .else .env .c4/.c5/.d2/.dx/.elseReg
	, .c5 = (ASSIGN-FRAME-REG .reg 0 .elseReg, .cx)
#
fc-compile (INVOKE .parameter .callee) .env .c0/.cx/.d0/.dx/.reg
	:- fc-compile .callee .env .c0/.c1/.d0/.d1/.r0
	, fc-compile .parameter .env .c1/.c2/.d1/.dx/.r1
	, .c2 = (PUSH .r1, CALL-CLOSURE .r0, SET-RESULT .reg, .cx)
#
fc-compile (NUMBER .i) _ .c0/.cx/.d/.d/.reg
	:- .c0 = (ASSIGN-INT .reg .i, .cx)
#
fc-compile (PAIR .left .right) .env .cdr
	:- fc-compile (INVOKE .right (INVOKE .left (VAR +pcons))) .env .cdr
#
fc-compile (PRAGMA _ .do) .env .cdr
	:- fc-compile .do .env .cdr
#
fc-compile (TREE .oper .left .right) .env .c0/.cx/.d0/.dx/.reg
	:- fc-compile .left .env .c0/.c1/.d0/.d1/.r0
	, fc-compile .right .env .c1/.c2/.d1/.dx/.r1
	, .c2 = (EVALUATE .reg .r0 .oper .r1, .cx)
#
fc-compile (UNWRAP .callee) .env .c0/.cx/.d0/.dx/.reg
	:- fc-compile .callee .env .c0/.c1/.d0/.dx/.closureReg
	, .c1 = (CALL-CLOSURE .closureReg, SET-CLOSURE-RESULT .reg .closureReg, .cx)
#
fc-compile (USING .mode BUILTIN .lib .do) .fve .cdr
	:- fc-load-precompiled-library .lib (_ # .eagerPred # .lazyPred #)
	, once (.mode = EAGER, .pred = .eagerPred; .pred = .lazyPred)
	, clone .pred (fc-compile-using-lib .mode .lib .do .fve .cdr :- .tail)
	, once .tail
#
fc-compile (USING _ EXTERNAL _ .do) .env .cdr
	:- !, fc-compile .do .env .cdr
#
fc-compile (VAR .var) .frame/.ve .c0/.cx/.d/.d/.reg1
	:- fc-dict-get .ve .var/(%REG/.reg/.frame0)
	, !, fc-frame-difference .frame0 .frame .frameDiff
	, (.frameDiff = 0, !, .c0 = .cx, .reg = .reg1
		; .c0 = (ASSIGN-FRAME-REG .reg1 .frameDiff .reg, .cx)
	)
#
fc-compile (WRAP .do) .frame/.ve .c0/.cx/.d0/.dx/.closureReg
	:- .c0 = (ASSIGN-CLOSURE .closureReg l:.d0, .cx)
	, .d0 = (PROC .f0, .dx)
	, fc-compile .do (.frame + 1)/.ve .f0/.f1/.f2/()/.returnReg
	, .f1 = (RETURN-VALUE .returnReg, .f2)
#

fc-default-fun .call .frame .result :- fc-default-fun0 .call .frame .result 0 #

fc-default-fun0 (INVOKE .p .pred) .frame .c0/.cx/.d0/.dx/.reg .n
	:- !, let .n1 (.n + 1)
	, fc-compile .p .frame .c0/.c1/.d0/.d1/.r1
	, .c1 = (PUSH .r1, .c2)
	, fc-default-fun0 .pred .frame .c2/.cx/.d1/.dx/.reg .n1
#
fc-default-fun0 (VAR .pred) _ .c0/.cx/.d/.d/.reg .n
	:- fc-define-default-fun .n .pred .call, !
	, .c0 = (.call .reg .n, .cx)
#
