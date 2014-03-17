-- Syntactic sugars
ic-compile .fs (declare .var := .value >> .do) .e0/.ex
	:- ic-compile .fs (declare .var >> .var = .value; .do) .e0/.ex
#

ic-compile _ () .e/.e
#
ic-compile .fs (.do0; .do1) .e0/.ex
	:- ic-compile .fs .do0 .e0/.e1
	, ic-compile .fs .do1 .e1/.ex
#
ic-compile _ asm/ .e/.e
#
ic-compile _ asm/(.i, .is) (.i, .e1)/.ex
	:- ic-compile _ asm/.is .e1/.ex
#
ic-compile .fs (declare .var >> .do) .e0/.ex
	:- .e1 = (_ PUSH 0, .e2)
	, replace .var `EBP - .fs` .do .do1
	, let .fs1 (.fs + 4)
	, ic-compile .fs1 .do1 .e2/.e3
	, .e3 = (_ POP EDI, .ex)
#
ic-compile _ (.var => .do) .e0/.ex
	:- .e0 = (_ JMP DWORD .label
		, .funLabel PUSH EBP
		, _ MOV (EBP, ESP)
		, .e1)
	, replace EBP `EBP` .do .do1
	, replace .var `EBP + 4` .do .do1
	, ic-compile 0 .do .e1/.e2
	, .e2 = (_ MOV (ESP, EBP)
		, _ POP EBP
		, _ RET
		, .label MOV (EAX, .funLabel)
	, .ex)
#
ic-compile .fs (.fun {.param}) .e0/.ex
	:- , ic-compile .fs .param .e0/.e1
	, .e1 = (_ PUSH EAX, .e2)
	, ic-compile .fs .fun .e2/.e3
	, .e3 = (_ CALL `EAX`, .ex)
#
ic-compile .fs (while .while do .do) .e0/.ex
	:- ic-compile .fs .while .e0/.e1
	, .e1 = (_ OR (EAX, EAX)
		, _ JZ .endLabel
		, .nextLabel () ()
		, .e2)
	, ic-compile .fs .do .e2/.e3
	, .e2 = (_ JMP .nextLabel
		, .endLabel () ()
		, .ex)
#
ic-compile .fs (if .if then .then else .else) .e0/.ex
	:- ic-compile .fs .if .e0/.e1
	, .e1 = (_ OR (EAX, EAX)
		, _ JZ .elseLabel
		, .e2)
	, ic-compile .fs .then .e2/.e3
	, .e2 = (_ JMP .endLabel
		, .elseLabel () ()
		, .e3)
	, ic-compile .fs .else .e2/.e3
	, .e3 = (.endLabel () (), .ex)
#
ic-compile .fs (& `.pointer`) .e0/.ex
	:- ic-compile .fs .pointer .e0/.ex
#
ic-compile .fs (`.pointer` = .value) .e0/.ex
	:- ic-compile .fs .pointer .e0/.e1
	, .e1 = (_ PUSH EAX, .e2)
	, ic-compile .fs .value .e2/.e3
	, .e3 = (_ POP EDI, _ MOV (`EDI`, EAX), .ex)
#
ic-compile .fs .expr .e0/.ex
	:- (tree .expr .value0 .op .value1; .expr = .value0 .op .value1)
	, ic-operator .op .e3/.ex
	, ic-compile .fs .value0 .e0/.e1
	, .e1 = (_ PUSH EAX, .e2)
	, ic-compile .fs .value1 .e2/.e3
#
ic-compile .fs `.value` .e0/.ex
	:- ic-compile .fs .value .e0/.e1
	, .e1 = (_ MOV (EAX, `EAX`), .ex)
#
ic-compile _ EBP (_ MOV (EAX, EBP), .e)/.e
#
ic-compile _ .imm (_ PUSH .imm, .e)/.e
	:- is.int .imm
#

ic-operator .op (
	_ POP EBX
	, _ .insn (EAX, EBX)
	, .e)/.e
	:- ic-operator-insn .op .insn
#
ic-operator .op (
	_ POP EBX
	, _ CMP (EAX, EBX)
	, _ .setcc AL
	, _ MOVSX (EAX, AL)
	, .e)/.e
	:- ic-operator-setcc .op .jump
#
ic-operator ' / ' (
	_ MOV (EBX, EAX)
	, _ XOR (EDX, EDX)
	, _ POP EAX
	, _ IDIV EBX
	, .e)/.e #
ic-operator ' % ' .e0/.ex
	:- ic-operator ' / ' .e0/.e1
	, .e1 = (_ MOV (EAX, EDX), .ex)
#
ic-operator .shift (
	_ MOV (ECX, EAX)
	, _ POP EAX
	, _ .insn (EAX, CL)
	, .e)/.e
	:- ic-operator-shift .shift .insn
#

ic-operator-insn ' + ' ADD #
ic-operator-insn ' - ' SUB #
ic-operator-insn ' * ' IMUL #
ic-operator-insn and AND #
ic-operator-insn or OR #

ic-operator-setcc ' = ' SETE #
ic-operator-setcc ' != ' SETNE #
ic-operator-setcc ' <= ' SETLE #
ic-operator-setcc ' < ' SETL #
ic-operator-setcc ' >= ' SETGE #
ic-operator-setcc ' > ' SETG #

ic-operator-shift shl SAL #
ic-operator-shift shr SAR #
