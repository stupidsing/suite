ic-compile .fs .do .e0/.ex :- once (ic-compile0 .fs .do .e0/.ex) #

ic-compile0 _ (asm .i) (.i, _ R+: (), .e)/.e
	:- ! -- Assembler has unbound variables, skip processing
#
ic-compile0 .fs .do .e0/.ex
	:- ic-compile-better-option .fs .do .e0/.ex, !
#
ic-compile0 .fs .do0 .e0/.ex
	:- ic-compile-sugar .do0 .do1
	, ic-compile0 .fs .do1 .e0/.ex
#
ic-compile0 _ () .e0/.ex
	:- .e0 = (_ R+: (), .ex)
#
ic-compile0 .fs (.do0; .do1) .e0/.ex
	:- not (.do0 = allocate _; .do0 = constant _ = _; .do0 = declare _; .do0 = declare _ = _)
	, ic-compile0 .fs .do0 .e0/.e1
	, .e1 = (_ R-: (), .e2)
	, ic-compile0 .fs .do1 .e2/.ex
#
ic-compile0 _ ([.vars] .do) .e0/.ex -- Traditional subroutine definition
	:- .e0 = (_ JMP (DWORD .label)
		, .funLabel RBEGIN: ()
		, _ PUSH (EBP)
		, _ MOV (EBP, ESP)
		, .e1)
	, replace $$EBP `$$EBP` .do .do1
	, ic-replace-parameters .vars 4 .do1 .do2
	, ic-compile0 0 .do2 .e1/.e2
	, .e2 = (_ MOV (ESP, EBP)
		, _ POP (EBP)
		, _ R-: RET ()
		, _ REND: ()
		, .label R+: MOV ($0, .funLabel)
		, .ex)
#
ic-compile0 .fs (.this:.sub [.params]) .e0/.ex -- Traditional subroutine invocation
	:- .e0 = (_ RSAVE: (), .e1)
	, ic-push EBP .fs/.fs1 .e1/.e2
	, ic-push-pop-parameters .fs1/.fs2 .params .e2/.e3 .e6/.e7
	, ic-compile0 .fs2 .sub .e3/.e4
	, ic-compile0 .fs2 .this .e4/.e5
	, .e5 = (_ R-: MOV (EBP, $0)
		, _ R-: CALL ($0)
		, _ MOV (ECX, EAX)
		, .e6)
	, .e7 = (_ POP (EBP)
		, _ RRESTORE: ()
		, _ R+: MOV ($0, ECX)
		, .ex)
#
ic-compile0 _ $$EBP (_ R+: MOV ($0, EBP), .e)/.e
#
ic-compile0 _ .string .e0/.ex
	:- is.string .string
	, .e0 = (_ JMP (DWORD .label)
		, .strLabel DS (.string)
		, _ D8 (0)
		, .label R+: MOV ($0, .strLabel)
		, .ex)
#
ic-compile0 .fs (allocate .var/.size; .do) .e0/.ex
	:- is.atom .var
	, .e0 = (_ SUB (ESP, .size), .e1)
	, let .fs1 (.fs + .size)
	, let .offset (0 - .fs1)
	, replace .var `$$EBP + .offset` .do .do1
	, ic-compile0 .fs1 .do1 .e1/.e2
	, .e2 = (_ ADD (ESP, .size), .ex)
#
ic-compile0 .fs (if .if then .then else .else) .e0/.ex
	:- ic-compile0 .fs .if .e0/.e1
	, .e1 = (_ R-: OR ($0, $0)
		, _ JZ (DWORD .elseLabel)
		, .e2)
	, ic-compile0 .fs .then .e2/.e3
	, .e3 = (_ JMP (DWORD .endLabel)
		, .elseLabel R-: ()
		, .e4)
	, ic-compile0 .fs .else .e4/.e5
	, .e5 = (.endLabel (), .ex)
#
ic-compile0 .fs (let .var = .value) .e0/.ex
	:- ic-compile0 .fs .value .e0/.e1
	, ic-compile0 .fs (& .var) .e1/.e2
	, .e2 = (_ R-: MOV (`$0`, $1), .ex)
#
ic-compile0 _ (snippet .snippet) .e0/.ex
	:- .e0 = (_ JMP (DWORD .label)
		, .snippetLabel ()
		, .e1)
	, ic-compile0 0 .snippet .e1/.e2
	, .e2 = (_ R-: ()
		, .label R+: MOV ($0, .snippetLabel)
		, .ex)
#
ic-compile0 .fs (while .while do .do) .e0/.ex
	:- .e0 = (.nextLabel (), .e1)
	, ic-compile0 .fs .while .e1/.e2
	, .e2 = (_ R-: OR ($0, $0)
		, _ JZ (DWORD .endLabel)
		, .e3)
	, ic-compile0 .fs .do .e3/.e4
	, .e4 = (_ JMP (DWORD .nextLabel)
		, .endLabel ()
		, .ex)
#
ic-compile0 .fs (& `.pointer`) .e0/.ex
	:- ic-compile0 .fs .pointer .e0/.ex
#
ic-compile0 .fs `.value` .e0/.ex
	:- ic-compile0 .fs .value .e0/.e1
	, .e1 = (_ TOP: MOV ($0, `$0`), .ex)
#
ic-compile0 .fs .expr .e0/.ex
	:- (tree .expr .value0 .op .value1; .expr = .value0 .op .value1)
	, ic-operator .op .e2/.ex
	, once (
		ic-right-associative .op
		, ic-compile0 .fs .value1 .e0/.e1
		, ic-compile0 .fs .value0 .e1/.e2
	;
		, ic-compile0 .fs .value0 .e0/.e1
		, ic-compile0 .fs .value1 .e1/.e2
	)
#
ic-compile0 _ .imm (_ R+: MOV ($0, .imm), .e)/.e
	:- is.int .imm
#
ic-compile0 _ .do _
	:- ic-error "Unknown expression" .do
#

-- Generates faster code
ic-compile-better-option .fs (.do0 + .imm) .e0/.ex
	:- is.int .imm
	, ic-compile0 .fs .do0 .e0/.e1
	, .e1 = (_ TOP: ADD ($0, .imm), .ex)
#
ic-compile-better-option .fs (let `$$EBP + .imm` = .value) .e0/.ex
	:- is.int .imm
	, ic-compile0 .fs .value .e0/.e1
	, .e1 = (_ TOP: MOV (`EBP + .imm`, $0), .ex)
#
ic-compile-better-option .fs (let `$$EBP` = .value) .e0/.ex
	:- ic-compile0 .fs .value .e0/.e1
	, .e1 = (_ TOP: MOV (`EBP`, $0), .ex)
#
ic-compile-better-option .fs (let `.addr + .imm` = .value) .e0/.ex
	:- is.int .imm
	, ic-compile0 .fs .value .e0/.e1
	, ic-compile0 .fs .addr .e1/.e2
	, .e2 = (_ R-: MOV (`$0 + .imm`, $1), .ex)
#
ic-compile-better-option .fs (let `.addr` = .value) .e0/.ex
	:- ic-compile0 .fs .value .e0/.e1
	, ic-compile0 .fs .addr .e1/.e2
	, .e2 = (_ R-: MOV (`$0`, $1), .ex)
#
ic-compile-better-option _ `$$EBP + .imm` .e0/.ex
	:- is.int .imm, .e0 = (_ R+: MOV ($0, `EBP + .imm`), .ex)
#
ic-compile-better-option _ `$$EBP` .e0/.ex
	:- .e0 = (_ R+: MOV ($0, `EBP`), .ex)
#
ic-compile-better-option .fs `.addr + .imm` .e0/.ex
	:- is.int .imm
	, ic-compile0 .fs .addr .e0/.e1
	, .e1 = (_ TOP: MOV ($0, `$0 + .imm`), .ex)
#
ic-compile-better-option .fs `.addr` .e0/.ex
	:- ic-compile0 .fs .addr .e0/.e1
	, .e1 = (_ TOP: MOV ($0, `$0`), .ex)
#
ic-compile-better-option .fs (`.addr` =+ .imm) .e0/.ex
	:- is.int .imm
	, ic-compile0 .fs .addr .e0/.e1
	, .e1 = (_ R+: MOV ($0, $1)
		, _ TOP: MOV ($1, `$1`)
		, _ R-: ADD (`$0`, .imm)
		, .ex)
#
ic-compile-better-option .fs (`.addr` += .imm) .e0/.ex
	:- is.int .imm
	, ic-compile0 .fs .addr .e0/.e1
	, .e1 = (_ TOP: ADD (`$0`, .imm)
		, _ TOP: MOV ($0, `$0`)
		, .ex)
#
ic-compile-better-option _ 0 (_ R+: XOR ($0, $0), .e)/.e #

ic-compile-sugar (.a && .b) (if .a then .b else 0)
#
ic-compile-sugar (.a || .b) (if .a then 1 else .b)
#
ic-compile-sugar (.var =+ .inc) (declare .p = & .var; declare .o = `.p`; let `.p` = .o + .inc; .o)
	:- temp .p, temp .o
#
ic-compile-sugar (.var += .inc) (declare .p = & .var; let `.p` = `.p` + .inc)
	:- temp .p
#
ic-compile-sugar (constant .var = .value; .do) .do1
	:- generalize (.var .value) (.var1 .value1)
	, rewrite .var1 .value1 .do .do1
#
ic-compile-sugar (declare .var; .do) (allocate .var/4; .do)
	:- is.atom .var
#
ic-compile-sugar (declare .var = .value; .do) (declare .var; let .var = .value; .do)
	:- is.atom .var
#
ic-compile-sugar false 0
#
ic-compile-sugar (for (.init; .cond; .step) .do) (.init; while .cond do (.do; .step))
#
ic-compile-sugar (not .b) (if .b then 0 else 1)
#
ic-compile-sugar this $$EBP
#
ic-compile-sugar true 1
#

ic-replace-parameters () _ .do .do #
ic-replace-parameters (.var, .vars) .s0 .do0 .dox
	:- let .s (.s0 + 4)
	, replace .var `$$EBP + .s` .do0 .do1
	, ic-replace-parameters .vars .s .do1 .dox
#

ic-push-pop-parameters .fs/.fs () .e/.e .f/.f #
ic-push-pop-parameters .fs0/.fsx (.p, .ps) .e0/.ex .f0/.fx
	:- ic-push-pop-parameters .fs0/.fs1 .ps .e0/.e1 .f1/.fx
	, ic-compile0 .fs1 .p .e1/.e2
	, ic-push-top .fs1/.fsx .e2/.ex
	, .f0 = (_ POP (EDX), .f1)
#

ic-push-top .fs0/.fsx (_ R-: PUSH ($0), .e)/.e
	:- let .fsx (.fs0 + 4)
#

ic-push .op .fs0/.fsx (_ PUSH .op, .e)/.e
	:- let .fsx (.fs0 + 4)
#

ic-right-associative ' + ' #
ic-right-associative ' * ' #
ic-right-associative and #
ic-right-associative or #
ic-right-associative xor #

ic-operator .op (
	_ R-: .insn ($1, $0),
	.e
)/.e
	:- ic-operator-insn .op .insn
#
ic-operator .op (
	_ R-: CMP ($1, $0),
	_ .setcc (DL),
	_ TOP: MOVSX ($0, DL),
	.e
)/.e
	:- ic-operator-setcc .op .setcc
#
ic-operator ' / ' .e :- ic-divide EAX .e #
ic-operator ' %% ' .e :- ic-divide EDX .e #
ic-operator .shift (
	_ R-: MOV (ECX, $0),
	_ TOP: .insn ($0, CL),
	.e
)/.e
	:- ic-operator-shift .shift .insn
#

ic-divide .reg (
	_ R-: MOV (ECX, $0),
	_ XOR (EDX, EDX),
	_ PUSH (EAX),
	_ R-: MOV (EAX, $0),
	_ IDIV (ECX),
	_ MOV (ECX, .reg),
	_ POP (EAX),
	_ R+: MOV ($0, ECX),
	.e
)/.e #

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
ic-operator-shift ushl SHL #
ic-operator-shift ushr SHR #

ic-error .m :- !, throw .m #
