-------------------------------------------------------------------------------
-- functional program compiler with eager evaluation

fc-compile EAGER (OPTION _ .do) .env .cdr
	:- !, fc-compile EAGER .do .env .cdr
#
fc-compile EAGER (FUN .var .do) .frame/.ve .c0/.cx/.d0/.dx/.closureReg
	:- !
	, .c0 = (_ ASSIGN-CLOSURE .closureReg .funcLabel, .cx)
	, .d0 = (.funcLabel LABEL, _ ENTER, .d1)
	, .d1 = (_ POP .varReg, .d2)
	, .frame1 = .frame + 1
	, fc-dict-add .var/(%REG/.varReg/.frame1) .ve/.ve1
	, fc-compile EAGER .do .frame1/.ve1 .d2/.d3/.d4/.dx/.returnReg
	, .d3 = (_ RETURN-VALUE .returnReg, _ LEAVE, .d4)
#
fc-compile EAGER (DEF-VAR .var .value .do) .frame/.ve .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-dict-add .var/(%REG/.r1/.frame) .ve/.ve1
	, fc-compile EAGER .value .frame/.ve1 .c0/.c1/.d0/.d1/.r1
	, fc-compile EAGER .do .frame/.ve1 .c1/.cx/.d1/.dx/.reg
#
fc-compile EAGER .do .env .cdr :- fc-eager-default-fun .do .env .cdr, ! #
fc-compile EAGER (INVOKE .parameter .callee) .env .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile EAGER .callee .env .c0/.c1/.d0/.d1/.r1
	, fc-compile EAGER .parameter .env .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ PUSH .r2, _ CALL-CLOSURE .r1, _ SET-RESULT .reg, .cx)
#
fc-compile EAGER (IF .if .then .else) .env .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile EAGER .if .env .c0/.c1/.d0/.d1/.ifReg
	, .c1 = (_ IF-FALSE .label1 .ifReg, .c2)
	, fc-compile EAGER .then .env .c2/.c3/.d1/.d2/.thenReg
	, .c3 = (_ ASSIGN-FRAME-REG .reg 0 .thenReg
		, _ JUMP .label2
		, .label1 LABEL
		, .c4
	)
	, fc-compile EAGER .else .env .c4/.c5/.d2/.dx/.elseReg
	, .c5 = (_ ASSIGN-FRAME-REG .reg 0 .elseReg
		, .label2 LABEL
		, .cx
	)
#
fc-compile EAGER (TUPLE .name ()) .env .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-CONSTANT .reg .name, .cx)
#
fc-compile EAGER (TUPLE .name (.e, .es)) .env .cdr
	:- !, fc-compile EAGER (
		INVOKE (TUPLE .name .es) (INVOKE .e (VARIABLE _cons))
	) .env .cdr
#
fc-compile EAGER (TREE .oper .left .right) .env .c0/.cx/.d0/.dx/.reg
	:- !
	, fc-compile EAGER .left .env .c0/.c1/.d0/.d1/.r1
	, fc-compile EAGER .right .env .c1/.c2/.d1/.dx/.r2
	, .c2 = (_ EVALUATE .reg .r1 .oper .r2, .cx)
#
fc-compile EAGER (VARIABLE .var) .frame/.ve  .c0/.cx/.d/.d/.reg1
	:- fc-dict-get .ve .var/(%REG/.reg/.frame0)
	, !, fc-frame-difference .frame0 .frame .frameDiff
	, (.frameDiff = 0, !, .c0 = .cx, .reg = .reg1
		; .c0 = (_ ASSIGN-FRAME-REG .reg1 .frameDiff .reg, .cx)
	)
#
fc-compile EAGER (CONSTANT .c) _ .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-CONSTANT .reg .c, .cx)
#
fc-compile EAGER (BOOLEAN .b) _ .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-BOOL .reg .b, .cx)
#
fc-compile EAGER (NUMBER .i) _ .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-INT .reg .i, .cx)
#
fc-compile EAGER (STRING .s) _ .c0/.cx/.d/.d/.reg
	:- !, .c0 = (_ ASSIGN-STR .reg .s, .cx)
#

fc-eager-default-fun .call .frame .result
	:- fc-eager-default-fun0 .call .frame .result 0
#

fc-eager-default-fun0 (INVOKE .p .pred) .frame .c0/.cx/.d0/.dx/.reg .n
	:- !, let .n1 (.n + 1)
	, fc-compile EAGER .p .frame .c0/.c1/.d0/.d1/.r1
	, .c1 = (_ PUSH .r1, .c2)
	, fc-eager-default-fun0 .pred .frame .c2/.cx/.d1/.dx/.reg .n1
#
fc-eager-default-fun0 (VARIABLE .pred) _ .c0/.cx/.d/.d/.reg .n
	:- fc-define-default-fun .n .pred .call, !
	, .c0 = (_ .call .reg .n, .cx)
#
