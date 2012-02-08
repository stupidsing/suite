-------------------------------------------------------------------------------
-- functional program compiler with lazy evaluation

fc-compile .do .frame .c0/.cx/.d0/.dx/.reg
	:- fc-compile0 .do .frame .c0/.c1/.d0/.dx/.closureReg
	, .c1 = ( _ CALL-CLOSURE .reg .closureReg, .cx)
#

fc-compile0 (CAST _ .do) .frame .cdr :- !, fc-compile0 .do .frame .cdr #
fc-compile0 (AS _ _ .do) .frame .cdr :- !, fc-compile0 .do .frame .cdr #
fc-compile0 (DEF-VAR .var .value .do) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, replace .value/.value1 (VARIABLE .var)/(%REG/.r1/.frame) -- Allows recursion
	, replace .do/.do1 (VARIABLE .var)/(%REG/.r1/.frame)
	, fc-compile0 .value1 .frame .c0/.c1/.d0/.d1/.r1
	, fc-compile0 .do1 .frame .c1/.cx/.d1/.dx/.reg
#
fc-compile0 (IF .if .then .else) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile .if .frame .c0/.c1/.d0/.d1/.ifReg
	, .c1 = (_ IF-FALSE .label1 .ifReg, .c2)
	, fc-compile0 .then .frame .c2/.c3/.d1/.d2/.thenReg
	, .c3 = (_ ASSIGN-FRAME-REG .reg 0 .thenReg, _ JUMP .label2, .label1 LABEL .label1, .c4)
	, fc-compile0 .else .frame .c4/.c5/.d2/.dx/.elseReg
	, .c5 = (_ ASSIGN-FRAME-REG .reg 0 .elseReg, .label2 LABEL .label2, .cx)
#
fc-compile0 %REG/.reg/.frame .frame .c/.c/.d/.d/.reg :- ! #
fc-compile0 %REG/.reg/.frame0 .frame .c0/.cx/.d/.d/.reg1
	:- !, let .frameDifference (.frame0 - .frame)
	, .c0 = (_ ASSIGN-FRAME-REG .reg1 .frameDifference .reg, .cx)
#
-=		fc-compile0 (INVOKE .p (VARIABLE .var)) .frame .c0/.cx/.d0/.dx/.reg
			:- member (head, tail,) .var
			, fc-define-default-fun 1 .var .call, !
			, fc-compile .p .frame .c0/.c1/.d0/.dx/.paramReg
			, .c1 = (_ PUSH .paramReg, _ SYS .call .reg 1, .cx)
		#
=-
fc-compile0 .do .frame .c0/.cx/.d0/.dx/.closureReg
	:- .c0 = (_ ASSIGN-CLOSURE .closureReg .funcLabel, .cx)
	, let .frame1 (.frame + 1)
	, .d0 = (.funcLabel ENTER, .d1)
	, fc-compile-wrapped .do .frame1 .d1/.d2/.d3/.dx/.returnReg
	, .d2 = (_ RETURN-VALUE .returnReg, _ LEAVE, .d3)
	, !
#
fc-compile0 (INVOKE .parameter .callee) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile .callee .frame .c0/.c1/.d0/.d1/.r1
	, fc-compile0 .parameter .frame .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ PUSH .r2, _ CALL-CLOSURE .reg .r1, .cx)
#

fc-compile-wrapped (FUN .var .do) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, .c0 = (_ ASSIGN-CLOSURE .reg .funcLabel, .cx)
	, let .frame1 (.frame + 1)
	, .d0 = (.funcLabel ENTER, .d1)
	, .d1 = (_ POP .varReg, .d2)
	, replace .do/.do1 (VARIABLE .var)/(%REG/.varReg/.frame1)
	, fc-compile0 .do1 .frame1 .d2/.d3/.d4/.dx/.returnReg
	, .d3 = (_ RETURN-VALUE .returnReg, _ LEAVE, .d4)
#
-=		fc-compile-wrapped (INVOKE .p0 (INVOKE .p1 (VARIABLE .var))) .frame .c0/.cx/.d0/.dx/.reg
			:- member (cons,) .var
			, fc-define-default-fun 2 .var .call, !
			, fc-compile0 .p0 .frame .c0/.c1/.d0/.d1/.param0Reg
			, fc-compile0 .p1 .frame .c1/.c2/.d1/.dx/.param1Reg
			, .c2 = (_ PUSH .param0Reg, _ PUSH .param1Reg, _ SYS .call .reg 2, .cx)
		#
=-
fc-compile-wrapped .do .frame .cdr
	:- fc-default-fun .do .frame .cdr, !
#
fc-compile-wrapped (TUPLE .name .es) .frame .cdr
	:- !, fc-compile-tuple .name .es .frame .cdr
#
fc-compile-wrapped (TREE .oper .left .right) .frame .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile .left .frame .c0/.c1/.d0/.d1/.r1
	, fc-compile .right .frame .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ EVALUATE .reg .r1 .oper .r2, .cx)
#
fc-compile-wrapped (BOOLEAN .b) _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-BOOL .reg .b, .cx) #
fc-compile-wrapped (NUMBER .i) _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-INT .reg .i, .cx) #
fc-compile-wrapped (STRING .s) _ .c0/.cx/.d/.d/.reg :- !, .c0 = (_ ASSIGN-STR .reg .s, .cx) #

fc-compile-tuple  .name () .frame .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-CONSTANT .reg .name, .cx)
#
fc-compile-tuple .name (.e, .es) .frame .c0/.cx/.d0/.dx/.reg
	:- !, fc-compile .e .frame .c0/.c1/.d0/.d1/.headReg
	, fc-compile-tuple .name .es .frame .c1/.c2/.d1/.dx/.tailReg
	, .c2 = (_ PUSH .headReg, .c3)
	, .c3 = (_ PUSH .tailReg, .c4)
	, .c4 = (_ SYS CONS .reg 2, .cx)
#

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
