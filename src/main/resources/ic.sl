ic-compile .fs .do0 .e0/.ex
	:- ic-compile-sugar .do0 .do1
	, ic-compile .fs .do1 .e0/.ex
#
ic-compile _ () .e/.e
#
ic-compile .fs (.do0; .do1) .e0/.ex
	:- not (.do0 = declare _; .do0 = declare _ = _)
	, ic-compile .fs .do0 .e0/.e1
	, ic-compile .fs .do1 .e1/.ex
#
ic-compile _ ([.vars] .do) .e0/.ex -- Traditional subroutine definition
	:- .e0 = (_ JMP DWORD .label
		, .funLabel PUSH EBP
		, _ MOV (EBP, ESP)
		, .e1)
	, replace this `this` .do .do1
	, ic-replace-parameters .vars 4 .do1 .do2
	, ic-compile 0 .do2 .e1/.e2
	, .e2 = (_ MOV (ESP, EBP)
		, _ POP EBP
		, _ RET ()
		, .label MOV (EAX, .funLabel)
		, .ex)
#
ic-compile .fs (.sub [.params]) .e0/.ex -- Traditional subroutine invocation
	:- ic-push-pop-parameters .fs/.fs1 .params .e0/.e1 .e3/.ex
	, ic-compile .fs1 .sub .e1/.e2
	, .e2 = (_ CALL EAX, .e3)
#
ic-compile _ asm/ .e/.e
#
ic-compile _ asm/(.i, .is) (.i, .e1)/.ex
	:- ic-compile _ asm/.is .e1/.ex
#
ic-compile .fs (declare .var; .do) .e0/.ex
	:- is.atom .var
	, ic-push 0 .fs/.fs1 .e0/.e1
	, let .offset (0 - .fs1)
	, replace .var `this + .offset` .do .do1
	, ic-compile .fs1 .do1 .e1/.e2
	, .e2 = (_ POP EDI, .ex)
#
ic-compile .fs (if .if then .then else .else) .e0/.ex
	:- ic-compile .fs .if .e0/.e1
	, .e1 = (_ OR (EAX, EAX)
		, _ JZ DWORD .elseLabel
		, .e2)
	, ic-compile .fs .then .e2/.e3
	, .e3 = (_ JMP DWORD .endLabel
		, .elseLabel ()
		, .e4)
	, ic-compile .fs .else .e4/.e5
	, .e5 = (.endLabel (), .ex)
#
ic-compile .fs (invoke .this .sub [.params]) .e0/.ex
	:- ic-push EBP .fs/.fs1 .e0/.e1
	, ic-push-pop-parameters .fs1/.fs2 .params .e1/.e2 .e6/.e7
	, ic-compile .fs2 .sub .e2/.e3
	, ic-push EAX .fs2/.fs3 .e3/.e4
	, ic-compile .fs3 .this .e4/.e5
	, .e5 = (_ MOV (EBP, EAX)
		, _ POP EAX
		, _ CALL EAX
		, .e6)
	, .e7 = (_ POP EBP, .ex)
#
ic-compile .fs (while .while do .do) .e0/.ex
	:- .e0 = (.nextLabel (), .e1)
	, ic-compile .fs .while .e1/.e2
	, .e2 = (_ OR (EAX, EAX)
		, _ JZ DWORD .endLabel
		, .e3)
	, ic-compile .fs .do .e3/.e4
	, .e4 = (_ JMP DWORD .nextLabel
		, .endLabel ()
		, .ex)
#
ic-compile .fs (let .var = .value) .e0/.ex
	:- ic-compile .fs (& .var) .e0/.e1
	, ic-push EAX .fs/.fs1 .e1/.e2
	, ic-compile .fs1 .value .e2/.e3
	, .e3 = (_ POP EDI, _ MOV (`EDI`, EAX), .ex)
#
ic-compile _ this (_ MOV (EAX, EBP), .e)/.e
#
ic-compile .fs (& `.pointer`) .e0/.ex
	:- ic-compile .fs .pointer .e0/.ex
#
ic-compile .fs `.value` .e0/.ex
	:- ic-compile .fs .value .e0/.e1
	, .e1 = (_ MOV (EAX, `EAX`), .ex)
#
ic-compile .fs .expr .e0/.ex
	:- (tree .expr .value0 .op .value1; .expr = .value0 .op .value1)
	, ic-operator .op .e3/.ex
	, ic-compile .fs .value0 .e0/.e1
	, ic-push EAX .fs/.fs1 .e1/.e2
	, ic-compile .fs1 .value1 .e2/.e3
#
ic-compile _ .imm (_ MOV (EAX, .imm), .e)/.e
	:- is.int .imm
#

ic-compile-sugar (.var =+ .inc) (declare .p = & .var; declare .o = `.p`; let `.p` = .o + .inc; .o)
	:- temp .p, temp .o
#
ic-compile-sugar (.var += .inc) (declare .p = & .var; let `.p` = `.p` + .inc)
	:- temp .p
#
ic-compile-sugar (declare .var = .value; .do) (declare .var; let .var = .value; .do)
	:- is.atom .var
#
ic-compile-sugar (for (.init; .cond; .step) .do) (.init; while .cond do (.do; .step))
#

ic-replace-parameters () _ .do .do #
ic-replace-parameters (.var, .vars) .s0 .do0 .dox
	:- let .s (.s0 + 4)
	, replace .var `this + .s` .do0 .do1
	, ic-replace-parameters .vars .s .do1 .dox
#

ic-push-pop-parameters .fs/.fs () .e/.e .f/.f #
ic-push-pop-parameters .fs0/.fsx (.p, .ps) .e0/.ex .f0/.fx
	:- ic-push-pop-parameters .fs0/.fs1 .ps .e0/.e1 .f1/.fx
	, ic-compile .fs1 .p .e1/.e2
	, ic-push EAX .fs1/.fsx .e2/.ex
	, .f0 = (_ POP EDI, .f1)
#

ic-push .op .fs0/.fsx (_ PUSH .op, .e)/.e
	:- let .fsx (.fs0 + 4)
#

ic-operator .op (
	_ MOV (EBX, EAX)
	, _ POP EAX
	, _ .insn (EAX, EBX)
	, .e
)/.e
	:- ic-operator-insn .op .insn
#
ic-operator .op (
	_ POP EBX
	, _ CMP (EBX, EAX)
	, _ .setcc AL
	, _ MOVSX (EAX, AL)
	, .e
)/.e
	:- ic-operator-setcc .op .setcc
#
ic-operator ' / ' (
	_ MOV (EBX, EAX)
	, _ XOR (EDX, EDX)
	, _ POP EAX
	, _ IDIV EBX
	, .e
)/.e #
ic-operator ' %% ' .e0/.ex
	:- ic-operator ' / ' .e0/.e1
	, .e1 = (_ MOV (EAX, EDX), .ex)
#
ic-operator .shift (
	_ MOV (ECX, EAX)
	, _ POP EAX
	, _ .insn (EAX, CL)
	, .e
)/.e
	:- ic-operator-shift .shift .insn
#

ic-operator-insn ' + ' ADD #
ic-operator-insn ' - ' SUB #
ic-operator-insn ' * ' IMUL #
ic-operator-insn and AND #
ic-operator-insn or OR #
ic-operator-insn xor XOR #

ic-operator-setcc ' = ' SETE #
ic-operator-setcc ' != ' SETNE #
ic-operator-setcc ' <= ' SETLE #
ic-operator-setcc ' < ' SETL #
ic-operator-setcc ' >= ' SETGE #
ic-operator-setcc ' > ' SETG #

ic-operator-shift shl SAL #
ic-operator-shift shr SAR #
