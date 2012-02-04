-------------------------------------------------------------------------------
-- functional program compiler with eager evaluation

fc-compile (AS _ _ .do) .frame .cdr
	:- !, fc-compile .do .frame .cdr
#
fc-compile (FUN .var .do) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, .c0 = (_ ASSIGN-CLOSURE .reg .funcLabel, .cx)
	, let .frame1 (.frame + 1)
	, .d0 = (.funcLabel ENTER, .d1)
	, .d1 = (_ POP .varReg, .d2)
	, replace .do/.do1 (VARIABLE .var)/(%REG/.varReg/.frame1)
	, fc-compile .do1 .frame1 .d2/.d3/.d4/.dx/.returnReg
	, .d3 = (_ RETURN-VALUE .returnReg, _ LEAVE, .d4)
#
fc-compile (DEF-VAR .var .value .do) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, replace .value/.value1 (VARIABLE .var)/(%REG/.r1/.frame) -- Allows recursion
	, replace .do/.do1 (VARIABLE .var)/(%REG/.r1/.frame)
	, fc-compile .value1 .frame .c0/.c1/.d0/.d1/.r1
	, fc-compile .do1 .frame .c1/.cx/.d1/.dx/.reg
#
fc-compile (INVOKE .parameter .callee) .frame .cdr
	:- fc-default-fun (INVOKE .parameter .callee) .frame .cdr, !
#
fc-compile (INVOKE .parameter .callee) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile .callee .frame .c0/.c1/.d0/.d1/.r1
	, fc-compile .parameter .frame .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ PUSH .r2, _ CALL-CLOSURE .reg .r1, .cx)
#
fc-compile (IF .if .then .else) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile .if .frame .c0/.c1/.d0/.d1/.ifReg
	, .c1 = (_ IF-FALSE .label1 .ifReg, .c2)
	, fc-compile0 .then .frame .c2/.c3/.d1/.d2/.thenReg
	, .c3 = (_ ASSIGN-FRAME-REG .reg 0 .thenReg, _ JUMP .label2, .label1 LABEL .label1, .c4)
	, fc-compile0 .else .frame .c4/.c5/.d2/.dx/.elseReg
	, .c5 = (_ ASSIGN-FRAME-REG .reg 0 .elseReg, .label2 LABEL .label2, .cx)
#
fc-compile (TUPLE .name ()) .frame .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-CONSTANT .reg .name, .cx)
#
fc-compile (TUPLE .name (.e, .es)) .frame .c0/.cx/.d0/.dx/.reg
	:- !, fc-compile .e .frame .c0/.c1/.d0/.d1/.headReg
	, fc-compile (TUPLE .name .es) .frame .c1/.c2/.d1/.dx/.tailReg
	, .c2 = (_ PUSH .headReg, .c3)
	, .c3 = (_ PUSH .tailReg, .c4)
	, .c4 = (_ SYS CONS .reg 2, .cx)
#
fc-compile (TREE .oper .left .right) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile .left .frame .c0/.c1/.d0/.d1/.r1
	, fc-compile .right .frame .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ EVALUATE .reg .r1 .oper .r2, .cx)
#
fc-compile %REG/.reg/.frame .frame .c/.c/.d/.d/.reg :- ! #
fc-compile %REG/.reg/.frame0 .frame .c0/.cx/.d/.d/.reg1
	:- !, let .frameDifference (.frame0 - .frame)
	, .c0 = (_ ASSIGN-FRAME-REG .reg1 .frameDifference .reg, .cx)
#
fc-compile (BOOLEAN .b) _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-BOOL .reg .b, .cx) #
fc-compile (NUMBER .i) _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-INT .reg .i, .cx) #
fc-compile (STRING .s) _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-STR .reg .s, .cx) #
fc-compile EMPTY _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-CONSTANT .reg (), .cx) #
fc-compile .d _ _ :- write "Unknown expression" .d, nl, fail #

fc-default-fun .call .frame .result
	:- fc-default-fun0 .call .frame .result 0
#

fc-default-fun0 (INVOKE .p .pred) .frame .c0/.cx/.d0/.dx/.reg .n
	:- !, let .n1 (.n + 1)
	, fc-compile .p .frame .c0/.c1/.d0/.d1/.r1
	, .c1 = (_ PUSH .r1, .c2)
	, fc-default-fun0 .pred .frame .c2/.cx/.d1/.dx/.reg .n1
#
fc-default-fun0 (VARIABLE .pred) _ .c0/.cx/.d/.d/.reg .n
	:- fc-define-default-fun .n .pred .call, !
	, .c0 = (_ SYS .call .reg .n, .cx)
#
