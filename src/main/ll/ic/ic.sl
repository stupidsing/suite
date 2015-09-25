() :- import.path "ic/ic-erase-type.sl"
	, import.path "ic/ic-erase-variable.sl"
	, import.path "ic/ic-infer-type.sl"
	, import.path "ic/ic-parse.sl"
	, import.path "ic/ic-rewrite.sl"
#

compile-imperative .do0 .e0/.ex
	:- ic-parse .do0 .do1
	, !, ic-infer-type () .do1 .type
	, !, ic-return-type .type
	, !, ic-erase-type .do1 .do2
	, !, ic-erase-variable ()/() .do2 .do3
	, !, ic-compile .do3 .e0/.ex
	, !
#

-- Register usage
-- EAX - expression stack
-- EBX - expression stack
-- ECX - scratch; shift counter; divisor
-- EDX - scratch; dividend
-- ESI - expression stack
-- EDI - scratch
-- EBP - stack frame
-- ESP - stack pointer
ic-compile .do .e0/.ex
	:- ic-compile-operand0 .do .e0/.e1 .op
	, !, .e1 = (_ MOV ($0, .op), .ex)
#
ic-compile .do .e
	:- ic-compile0 .do .e
#

ic-compile-operand .do .e .op
	:- ic-compile-operand0 .do .e .op
	, !
#
ic-compile-operand .do .e $0
	:- ic-compile0 .do .e
#

ic-compile0 .do .e0/.ex
	:- ic-compile-better-option .do .e0/.ex, !
#
ic-compile0 (ALLOC _ .offset .size .do) .e0/.ex
	:- .e0 = (_ FR+ (.size)
		, _ FR-GET (.offset)
		, .e1)
	, ic-compile .do .e1/.e2
	, .e2 = (_ FR- (.size)
		, .ex)
#
ic-compile0 (ASM .i) (.i, _ R+, .e)/.e
	:- ! -- Assembler might have variables, skip processing
#
ic-compile0 (INVOKE .mr .params) .e0/.ex
	:- once (
		.mr = METHOD .this .sub
		; .mr = MEMORY 8 .pointer
		, .this = MEMORY 4 .pointer
		, .sub = MEMORY 4 (TREE ' + ' .pointer (NUMBER 4))
	)
	, .e0 = (_ RSAVE
		, _ FR-PUSH (EBP)
		, .e1)
	, ic-push-pop-parameters .params .e1/.e2 .e5/.e6
	, ic-compile .sub .e2/.e3
	, ic-compile-operand .this .e3/.e4 .thisOp
	, .e4 = (_ MOV (EBP, .thisOp)
		, _ R-
		, _ CALL ($0)
		, _ R-
		, _ MOV (ECX, EAX)
		, .e5)
	, .e6 = (_ FR-POP (EBP)
		, _ RRESTORE
		, _ R+
		, _ MOV ($0, ECX)
		, .ex)
#
ic-compile0 (IF .if .then .else) .e0/.ex
	:- ic-compile .if .e0/.e1
	, .e1 = (_ OR ($0, $0)
		, _ R-
		, _ JZ (DWORD .elseLabel)
		, .e2)
	, ic-compile .then .e2/.e3
	, .e3 = (_ JMP (DWORD .endLabel)
		, .elseLabel R-
		, .e4)
	, ic-compile .else .e4/.e5
	, .e5 = (.endLabel ()
		, .ex)
#
ic-compile0 (LET .var .value) .e0/.ex
	:- ic-let .value .var .e0/.ex
#
ic-compile0 (MEMORY .size .pointer) _
	:- .size != 4, ic-error "Cannot load non-dword memory " .pointer
#
ic-compile0 (METHOD0 _ .do) .e0/.ex
	:- .e0 = (_ JMP (DWORD .label)
		, .funLabel FR-BEGIN
		, _ PUSH (EBP)
		, _ MOV (EBP, ESP)
		, .e1)
	, ic-compile .do .e1/.e2
	, .e2 = (_ MOV (ESP, EBP)
		, _ POP (EBP)
		, _ RET ()
		, _ R-
		, _ FR-END
		, .label R+
		, _ MOV ($0, .funLabel)
		, .ex)
#
ic-compile0 NOP .e0/.ex
	:- .e0 = (_ R+, .ex)
#
ic-compile0 (POST-ADD-NUMBER (MEMORY 4 .pointer) .i) .e0/.ex
	:- ic-compile .pointer .e0/.e1
	, .e1 = (_ ADD (DWORD `$0`, .i)
		, _ MOV ($0, `$0`)
		, .ex)
#
ic-compile0 (PRE-ADD-NUMBER (MEMORY 4 .pointer) .i) .e0/.ex
	:- ic-compile .pointer .e0/.e1
	, .e1 = (_ R+
		, _ MOV ($0, $1)
		, _ MOV ($1, `$0`)
		, _ ADD (DWORD `$0`, .i)
		, _ R-
		, .ex)
#
ic-compile0 (REF MEMORY _ .pointer) .e0/.ex
	:- ic-compile .pointer .e0/.ex
#
ic-compile0 (SEQ .do0 .do1) .e0/.ex
	:- ic-compile .do0 .e0/.e1
	, .e1 = (_ R-, .e2)
	, ic-compile .do1 .e2/.ex
#
ic-compile0 (SNIPPET .snippet) .e0/.ex
	:- .e0 = (_ JMP (DWORD .label)
		, .snippetLabel ()
		, _ FR-BEGIN
		, .e1)
	, ic-compile .snippet .e1/.e2
	, .e2 = (_ R-
		, _ FR-END
		, .label R+
		, _ MOV ($0, .snippetLabel)
		, .ex)
#
ic-compile0 (TREE .op .value0 .value1) .e0/.ex
	:- ic-operator .op .e2/.ex
	, once (
		ic-right-associative .op
		, ic-compile .value1 .e0/.e1
		, ic-compile .value0 .e1/.e2
	;
		, ic-compile .value0 .e0/.e1
		, ic-compile .value1 .e1/.e2
	)
#
ic-compile0 (WHILE .while .do) .e0/.ex
	:- .e0 = (.nextLabel ()
		, .e1)
	, ic-compile .while .e1/.e2
	, .e2 = (_ OR ($0, $0)
		, _ R-
		, _ JZ (DWORD .endLabel)
		, .e3)
	, ic-compile .do .e3/.e4
	, .e4 = (_ JMP (DWORD .nextLabel)
		, .endLabel ()
		, .ex)
#

ic-compile-operand0 (MEMORY 4 (TREE ' + ' (TREE ' + ' .pointer (NUMBER .i0)) (NUMBER .i1))) .e0/.ex .op
	:- .e0 = (_ LET (.i, .i0 + .i1), .e1)
	, ic-compile-operand0 (MEMORY 4 (TREE ' + ' .pointer (NUMBER .i))) .e1/.ex .op
#
ic-compile-operand0 (MEMORY 4 (TREE ' + ' THIS (NUMBER .i))) .e0/.ex .op
	:- .e0 = (_ R+, .ex), .op = `EBP + .i`
#
ic-compile-operand0 (MEMORY 4 THIS) .e0/.ex .op
	:- .e0 = (_ R+, .ex), .op = `EBP`
#
ic-compile-operand0 (MEMORY 4 (TREE ' + ' .pointer (NUMBER .i))) .e0/.ex .op
	:- ic-compile .pointer .e0/.ex, .op = `$0 + .i`
#
ic-compile-operand0 (MEMORY 4 .pointer) .e0/.ex .op
	:- ic-compile .pointer .e0/.ex, .op = `$0`
#
ic-compile-operand0 (NUMBER .i) (_ R+, .e)/.e (DWORD .i)
#
ic-compile-operand0 (REG .reg) (_ R+, .e)/.e .reg
#
ic-compile-operand0 (STRING .s) .e0/.ex .strLabel
	:- .e0 = (_ JMP (DWORD .label)
		, .strLabel DS (.s)
		, _ D8 (0)
		, .label R+
		, .ex)
#
ic-compile-operand0 THIS (_ R+, .e)/.e EBP
#

-- Generates faster code
ic-compile-better-option (LET .memory .value) .e0/.ex
	:- ic-compile-operand0 .memory .e1/.e2 .op
	, ic-compile .value .e0/.e1
	, .e2 = (_ MOV (.op, $1), _ R-, .ex)
#
ic-compile-better-option (TREE ' + ' THIS (NUMBER .i)) .e0/.ex
	:- .e0 = (_ R+, _ LEA ($0, `EBP + .i`), .ex)
#
ic-compile-better-option (TREE ' + ' .do0 (NUMBER .i)) .e0/.ex
	:- ic-compile .do0 .e0/.e1
	, .e1 = (_ ADD ($0, .i), .ex)
#
ic-compile-better-option (NUMBER 0) (_ R+, _ XOR ($0, $0), .e)/.e
#

ic-let (METHOD .this .sub) (MEMORY 8 .pointer) .e0/.ex
	:- ic-compile .pointer .e0/.e1
	, ic-compile .this .e1/.e2
	, .e2 = (_ MOV (`$1`, $0)
		, _ R-
		, .e3)
	, ic-compile .sub .e3/.e4
	, .e4 = (_ MOV (`$1 + 4`, $0)
		, _ R-
		, .ex)
#
ic-let (MEMORY .size .pointer0) (MEMORY .size .pointer1) .e0/.ex
	:- ic-compile .pointer0 .e0/.e1
	, ic-compile .pointer1 .e1/.e2
	, ic-copy-memory 0 .size .e2/.e3
	, .e3 = (_ R-, .ex)
#

ic-push-pop-parameters () .e/.e .f/.f
#
ic-push-pop-parameters (.p, .ps) .e0/.ex .f0/.fx
	:- ic-push-pop-parameters .ps .e0/.e1 .f1/.fx
	, once (
		ic-compile-operand .p .e1/.e2 .op
		, .e2 = (_ FR-PUSH (.op), _ R-, .ex)
		, .f0 = (_ FR-POP (EDX), .f1)
		; .e1 = (_ FR+ (.size), .e2)
		, .f0 = (_ FR- (.size), .f1)
		, ic-let .p (MEMORY .size (REG ESP)) .e2/.e3
		, .e3 = (_ R-, .ex)
	)
#

ic-right-associative ' + ' #
ic-right-associative ' * ' #
ic-right-associative and #
ic-right-associative or #
ic-right-associative xor #

ic-operator .op
	(_ .insn ($1, $0)
	, _ R-
	, .e
)/.e
	:- ic-operator-insn .op .insn
#
ic-operator .op
	(_ CMP ($1, $0)
	, _ R-
	, _ .setcc (DL)
	, _ MOVSX ($0, DL)
	, .e
)/.e
	:- ic-operator-setcc .op .setcc
#
ic-operator ' / ' .e :- ic-divide EAX .e
#
ic-operator ' %% ' .e :- ic-divide EDX .e
#
ic-operator .shift
	(_ MOV (ECX, $0)
	, _ R-
	, _ .insn ($0, CL)
	, .e
)/.e
	:- ic-operator-shift .shift .insn
#

ic-divide .reg
	(_ MOV (ECX, $0)
	, _ R-
	, _ XOR (EDX, EDX)
	, _ FR-PUSH (EAX)
	, _ MOV (EAX, $0)
	, _ R-
	, _ IDIV (ECX)
	, _ MOV (ECX, .reg)
	, _ FR-POP (EAX)
	, _ R+
	, _ MOV ($0, ECX)
	, .e
)/.e #

ic-copy-memory _ 0 .e/.e
#
ic-copy-memory .o 1 (_ MOV (CL, `$1 + .o`), _ MOV (`$0 + .o`, CL), .e)/.e
#
ic-copy-memory .o 2 (_ MOV (CX, `$1 + .o`), _ MOV (`$0 + .o`, CX), .e)/.e
#
ic-copy-memory .o 4 (_ MOV (ECX, `$1 + .o`), _ MOV (`$0 + .o`, ECX), .e)/.e
#
ic-copy-memory 0 .size .e0/.ex
	:- .size > 16
	, let .div4 (.size / 4)
	, let .mod4 (.size % 4)
	, .e0 = (_ CLD ()
		, _ MOV (EDX, ESI)
		, _ MOV (EDI, $0)
		, _ MOV (ESI, $1)
		, _ MOV (ECX, .div4)
		, _ REP MOVSD ()
		, .e1)
	, once (.mod4 >= 1, .e1 = (_ MOVSB (), .e2); .e1 = .e2)
	, once (.mod4 >= 2, .e2 = (_ MOVSB (), .e3); .e2 = .e3)
	, once (.mod4 >= 3, .e3 = (_ MOVSB (), .e4); .e3 = .e4)
	, .e4 = (_ MOV (ESI, EDX), .ex)
#
ic-copy-memory .o .size .e0/.ex
  :- .size > 4
  , ic-copy-memory .o 4 .e0/.e1
  , let .size1 (.size - 4)
  , let .o1 (.o + 4)
  , ic-copy-memory .o1 .size1 .e1/.ex
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
ic-operator-shift ushl SHL #
ic-operator-shift ushr SHR #

ic-error .m :- !, throw .m #
