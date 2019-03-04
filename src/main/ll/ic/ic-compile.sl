-- register usage
-- EAX - expression stack
-- EBX - expression stack
-- ECX - scratch; shift counter; divisor
-- EDX - scratch; dividend
-- ESI - expression stack
-- EDI - scratch
-- EBP - stack frame
-- ESP - stack pointer
ic-compile-register .do .e0/.ex
	:- ic-compile-operand-better-option .do .e0/.e1 .op
	, !, .e1 = (_ MOV ($0, .op), .ex)
#
ic-compile-register .do .e0/.ex
	:- ic-compile-register-better-option .do .e0/.ex, !
#
ic-compile-register .do .e0/.ex
	:- ic-compile-register0 .do .e0/.ex
#

ic-compile-memory (LET .var .value) .e0/.ex .size .pointer
	:- ic-compile-let .value .var .e0/.e1
	, ic-compile-memory .value .e1/.ex .size .pointer
#
ic-compile-memory (MEMORY .size .pointer) .e/.e .size .pointer
#

ic-compile-operand .do .e .op
	:- ic-compile-operand-better-option .do .e .op
	, !
#
ic-compile-operand .do .e $0
	:- ic-compile-register .do .e
#

ic-compile-register0 (ASM .i) (.i, _ R+, .e)/.e
	:- ! -- assembler might have variables, skip processing
#
ic-compile-register0 (DECLARES _ .offset .size .do) .e0/.ex
	:- .e0 = (_ FR-PUSHN (.size)
		, _ SUB (ESP, .size)
		, _ FR-GET (.offset)
		, .e1)
	, ic-compile-register .do .e1/.e2
	, .e2 = (_ ADD (ESP, .size)
		, _ FR-POPN (.size)
		, .ex)
#
ic-compile-register0 (EXTEND-SIGNED .memory) .e0/.ex
	:- ic-compile-memory .memory .e0/.e1 1 .pointer
	, ic-compile-operand (MEMORY 4 .pointer) .e1/.e2 .op
	, .e2 = (_ MOVSX ($0, .op), .ex)
#
ic-compile-register0 (INVOKE .mr .ips) .e0/.ex
	:- once (
		.mr = METHOD .this .sub
		, .e0 = .e1
		; ic-compile-memory .mr .e0/.e1 8 .pointer
		, .this = MEMORY 4 .pointer
		, .sub = MEMORY 4 (TREE ' + ' (NUMBER 4) .pointer)
	)
	, .e1 = (_ RSAVE-ALL
		, .e2)
	, ic-push-pop-invoke-parameters .ips .e2/.e3 .e6/.e7
	, ic-compile-register .sub .e3/.e4
	, ic-compile-operand .this .e4/.e5 .thisOp
	, .e5 = (_ FR-PUSH (EBP)
		, _ MOV (EBP, .thisOp)
		, _ R-
		, _ CALL ($0)
		, _ MOV ($0, EAX)
		, _ FR-POP (EBP)
		, .e6)
	, .e7 = (_ R-
		, _ RRESTORE-ALL
		, _ R+
		, .ex)
#
ic-compile-register0 (IF .if .then .else) .e0/.ex
	:- ic-compile-jump-if-false .if .e0/.e1 .elseLabel
	, ic-compile-then-else .then .else .elseLabel .e1/.ex
#
ic-compile-register0 (LET .var .value) .e0/.ex
	:- ic-compile-let .value .var .e0/.ex
#
ic-compile-register0 (METHOD0 _ .do) .e0/.ex
	:- .e0 = (_ JMP (DWORD .label)
		, .funLabel FR-BEGIN ()
		, _ PUSH (EBP)
		, _ MOV (EBP, ESP)
		, .e1)
	, ic-compile-register .do .e1/.e2
	, .e2 = (_ POP (EBP)
		, _ RET ()
		, _ R-
		, _ FR-END ()
		, .label R+
		, _ MOV ($0, .funLabel)
		, .ex)
#
ic-compile-register0 NOP .e0/.ex
	:- .e0 = (_ R+, .ex)
#
ic-compile-register0 (PRAGMA _ .do) .e0/.ex
	:- ic-compile-register .do .e0/.ex
#
ic-compile-register0 (POST-ADD-NUMBER .memory .i) .e0/.ex
	:- ic-compile-memory .memory .e0/.e1 4 .pointer
	, ic-compile-register .pointer .e1/.e2
	, .e2 = (_ ADDI (DWORD `$0`, .i)
		, _ MOV ($0, `$0`)
		, .ex)
#
ic-compile-register0 (PRE-ADD-NUMBER .memory .i) .e0/.ex
	:- ic-compile-memory .memory .e0/.e1 4 .pointer
	, ic-compile-register .pointer .e1/.e2
	, .e2 = (_ R+
		, _ MOV ($0, $1)
		, _ MOV ($1, `$0`)
		, _ ADDI (DWORD `$0`, .i)
		, _ R-
		, .ex)
#
ic-compile-register0 (REF .memory) .e0/.ex
	:- ic-compile-memory .memory .e0/.e1 _ .pointer
	, ic-compile-register .pointer .e1/.ex
#
ic-compile-register0 (SEQ .do0 .do1) .e0/.ex
	:- ic-compile-register .do0 .e0/.e1
	, .e1 = (_ R-, .e2)
	, ic-compile-register .do1 .e2/.ex
#
ic-compile-register0 (SNIPPET .snippet) .e0/.ex
	:- .e0 = (_ JMP (DWORD .label)
		, .snippetLabel ()
		, _ FR-BEGIN ()
		, .e1)
	, ic-compile-register .snippet .e1/.e2
	, .e2 = (_ R-
		, _ FR-END ()
		, .label R+
		, _ MOV ($0, .snippetLabel)
		, .ex)
#
ic-compile-register0 (TREE .operator .value0 .value1) .e0/.ex
	:- ic-operator .operator .op .e2/.ex
	, once (
		ic-right-associative .operator
		, ic-compile-register .value1 .e0/.e1
		, ic-compile-operand .value0 .e1/.e2 .op
	;
		, ic-compile-register .value0 .e0/.e1
		, ic-compile-operand .value1 .e1/.e2 .op
	)
#
ic-compile-register0 (WHILE .while .do) .e0/.ex
	:- .e0 = (.nextLabel ()
		, .e1)
	, ic-compile-jump-if-false .while .e1/.e2 .endLabel
	, ic-compile-register .do .e2/.e3
	, .e3 = (_ JMP (DWORD .nextLabel)
		, .endLabel ()
		, .ex)
#

-- generates smaller code
ic-compile-register-better-option (LET .memory .value) .e0/.ex
	:- ic-compile-register .value .e0/.e1
	, ic-compile-operand-better-option .memory .e1/.e2 .op
	, .e2 = (_ MOV (.op, $1), _ R-, .ex)
#
ic-compile-register-better-option (NUMBER 0) (_ R+, _ XOR ($0, $0), .e)/.e
#
ic-compile-register-better-option (TREE ' + ' (NUMBER .j) (TREE ' * ' (NUMBER .i) .do)) .e0/.ex
	:- member (1, 2, 4, 8,) .i
	, ic-compile-register .do .e0/.e1
	, .e1 = (_ LEA ($0, `$0 * .i + .j`), .ex)
#
ic-compile-register-better-option (TREE ' * ' (NUMBER .i) .do) .e0/.ex
	:- member (1, 2, 4, 8,) .i
	, ic-compile-register .do .e0/.e1
	, .e1 = (_ LEA ($0, `$0 * .i`), .ex)
#
ic-compile-register-better-option (TREE ' + ' (NUMBER .i) THIS) .e0/.ex
	:- .e0 = (_ R+, _ LEA ($0, `EBP + .i`), .ex)
#
ic-compile-register-better-option (TREE ' + ' (NUMBER .i) .do) .e0/.ex
	:- ic-compile-register .do .e0/.e1
	, .e1 = (_ ADDI ($0, .i), .ex)
#
ic-compile-register-better-option (TREE .shift .do (NUMBER .i)) .e0/.ex
	:- ic-operator-shift .shift .insn
	, ic-compile-register .do .e0/.e1
	, .e1 = (_ .insn ($0, .i), .ex)
#

ic-compile-operand-better-option (MEMORY 4 (TREE ' * ' (NUMBER .i) .pointer)) .e0/.ex .op
	:- member (1, 2, 4, 8,) .i
	, ic-compile-register .pointer .e0/.ex
	, .op = `$0 * .i`
#
ic-compile-operand-better-option (MEMORY 4 (TREE ' + ' (NUMBER .j) (TREE ' * ' (NUMBER .i) .pointer))) .e0/.ex .op
	:- member (1, 2, 4, 8,) .i
	, ic-compile-register .pointer .e0/.ex
	, .op = `$0 * .i + .j`
#
ic-compile-operand-better-option (MEMORY .size (TREE ' + ' (NUMBER .i0) (TREE ' + ' (NUMBER .i1) .pointer))) .e0/.ex .op
	:- .e0 = (_ LET (.i, .i0 + .i1), .e1)
	, ic-compile-operand-better-option (MEMORY .size (TREE ' + ' (NUMBER .i) .pointer)) .e1/.ex .op
#
ic-compile-operand-better-option (MEMORY 4 (TREE ' + ' (NUMBER .i) THIS)) .e0/.ex .op
	:- .e0 = (_ R+, .ex), .op = `EBP + .i`
#
ic-compile-operand-better-option (MEMORY 4 THIS) .e0/.ex .op
	:- .e0 = (_ R+, .ex), .op = `EBP`
#
ic-compile-operand-better-option (MEMORY 4 (TREE ' + ' (NUMBER .i) .pointer)) .e0/.ex .op
	:- ic-compile-register .pointer .e0/.ex, .op = `$0 + .i`
#
ic-compile-operand-better-option (MEMORY 4 .pointer) .e0/.ex .op
	:- ic-compile-register .pointer .e0/.ex, .op = `$0`
#
ic-compile-operand-better-option (NUMBER .i) (_ R+, .e)/.e (DWORD .i)
#
ic-compile-operand-better-option (REG .reg) (_ R+, .e)/.e .reg
#
ic-compile-operand-better-option (STRING .s) .e0/.ex .strLabel
	:- .e0 = (_ JMP (DWORD .label)
		, .strLabel .s
		, _ IMM (BYTE 0)
		, .label R+
		, .ex)
#
ic-compile-operand-better-option THIS (_ R+, .e)/.e EBP
#

ic-compile-jump-if-false .if .e0/.ex .elseLabel
	:- ic-compile-jump-if-false-better-option .if .e0/.ex .elseLabel, !
#
ic-compile-jump-if-false .if .e0/.ex .elseLabel
	:- ic-compile-register .if .e0/.e1
	, .e1 = (_ OR ($0, $0)
		, _ R-
		, _ JZ (DWORD .elseLabel)
		, .ex)
#

ic-compile-jump-if-false-better-option (IF .if0 .if1 (NUMBER 0)) .e0/.ex .elseLabel
	:- ic-compile-jump-if-false .if0 .e0/.e1 .elseLabel
	, ic-compile-jump-if-false .if1 .e1/.ex .elseLabel
#
ic-compile-jump-if-false-better-option (IF .if0 (NUMBER 1) .if1) .e0/.ex .elseLabel
	:- ic-compile-jump-if .if0 .e0/.e1 .thenLabel
	, ic-compile-jump-if .if1 .e1/.e2 .thenLabel
	, .e2 = (_ JMP (DWORD .elseLabel)
		, .thenLabel ()
		, .ex)
#
ic-compile-jump-if-false-better-option (TREE .operator .left .right) .e0/.ex .elseLabel
	:- once (ic-operator-negate .operator .negOp; ic-operator-negate .negOp .operator)
	, ic-compile-jump-if (TREE .negOp .left .right) .e0/.ex .elseLabel
#

ic-compile-jump-if (TREE .operator .left .right) .e0/.ex .elseLabel
	:- ic-operator-jmpcc .operator .jmp
	, !
	, ic-compile-register .left .e0/.e1
	, ic-compile-operand .right .e1/.e2 .rightOp
	, .e2 = (_ CMP ($1, .rightOp)
		, _ R-
		, _ R-
		, _ .jmp (DWORD .elseLabel)
		, .ex)
#
ic-compile-jump-if .if .e0/.ex .elseLabel
	:- ic-compile-register .if .e0/.e1
	, .e1 = (_ OR ($0, $0)
		, _ R-
		, _ JNZ (DWORD .elseLabel)
		, .ex)
#

ic-compile-then-else .then .else .elseLabel .e0/.ex
	:- ic-compile-register .then .e0/.e1
	, .e1 = (_ JMP (DWORD .endLabel)
		, .elseLabel R-
		, .e2)
	, ic-compile-register .else .e2/.e3
	, .e3 = (.endLabel ()
		, .ex)
#

ic-compile-let (ARRAYS .size .array) .memory .e0/.ex
	:- ic-compile-let-array (ARRAYS .size .array) 0 .memory .e0/.ex
#
ic-compile-let (METHOD .this .sub) .memory .e0/.ex
	:- ic-compile-memory .memory .e0/.e1 8 .pointer
	, ic-compile-register .pointer .e1/.e2
	, ic-compile-register .this .e2/.e3
	, .e3 = (_ MOV (`$1`, $0)
		, _ R-
		, .e4)
	, once (ic-compile-register .sub .e4/.e5
		; ic-error "Cannot assign value" .sub
	)
	, .e5 = (_ MOV (`$1 + 4`, $0)
		, _ R-
		, .ex)
#
ic-compile-let (NEW-STRUCTS ()) _ (_ R+, .e)/.e
#
ic-compile-let (NEW-STRUCTS (.size .offset .value, .sovs)) .memory .e0/.ex
	:- ic-compile-memory .memory .e0/.e1 _ .pointer
	, ic-compile-let .value (MEMORY .size (TREE ' + ' .pointer (NUMBER .offset))) .e1/.e2
	, .e2 = (_ R-, .e3)
	, ic-compile-let (NEW-STRUCTS .sovs) .memory .e3/.ex
#
ic-compile-let (NEW-TAGS .size .tag .value) .memory .e0/.ex
	:- ic-compile-memory .memory .e0/.e1 _ .pointer
	, ic-compile-let .tag (MEMORY 4 .pointer) .e1/.e2
	, .e2 = (_ R-, .e3)
	, ic-compile-let .value (MEMORY .size (TREE ' + ' .pointer (NUMBER 4))) .e3/.ex
#
ic-compile-let .memory0 .memory1 .e0/.ex
	:- ic-compile-memory .memory0 .e0/.e1 .size .pointer0
	, ic-compile-memory .memory1 .e1/.e2 .size .pointer1
	, ic-compile-register .pointer0 .e2/.e3
	, ic-compile-register .pointer1 .e3/.e4
	, ic-copy-memory 0 .size .e4/.e5
	, .e5 = (_ R-, .ex)
#
ic-compile-let .value .memory .e0/.ex
	:- ic-compile-register .value .e0/.e1
	, ic-compile-operand .memory .e1/.e2 .op
	, .e2 = (_ MOV (.op, $1), _ R-, .ex)
#
ic-compile-let .memory0 .memory1 _
	:- ic-error "Cannot assign from" .memory0 "to" .memory1
#

ic-compile-let-array (ARRAYS _ ()) _ _ (_ R+, .e)/.e
#
ic-compile-let-array (ARRAYS .size (.elem, .array)) .offset .memory .e0/.ex
	:- ic-compile-memory .memory .e0/.e1 _ .pointer
	, ic-compile-let .elem (MEMORY .size (TREE ' + ' (NUMBER .offset) .pointer)) .e1/.e2
	, .e2 = (_ R-, .e3)
	, let .offset1 (.offset + .size)
	, ic-compile-let-array (ARRAYS .size .array) .offset1 .memory .e3/.ex
#

ic-push-pop-invoke-parameters () .e/.e .f/.f
#
ic-push-pop-invoke-parameters (IP .io .param, .ips) .e0/.ex .f0/.fx
	:- ic-push-pop-invoke-parameters .ips .e0/.e1 .f3/.fx
	, once (
		.io = IN, (
			ic-compile-operand .param .e1/.e2 .op
			, .e2 = (_ FR-PUSH (.op), _ R-, .ex)
			, .f0 = (_ FR-POP (EDX), .f3)
			; .e1 = (_ FR-PUSHN (.size)
				, _ SUB (ESP, .size)
				, .e2)
			, ic-compile-let .param (MEMORY .size (REG ESP)) .e2/.e3
			, .e3 = (_ R-, .ex)
			, .f0 = (_ ADD (ESP, .size)
				, _ FR-POPN (.size)
				, .f3)
		)
		; .io = OUT, (
			.e1 = (_ FR-PUSHN (.size)
				, _ SUB (ESP, .size)
				, .ex)
			, ic-compile-let (MEMORY .size (REG ESP)) .param .f0/.f1
			, .f1 = (_ R-, .f2)
			, .f2 = (_ ADD (ESP, .size)
				, _ FR-POPN (.size)
				, .f3)
		)
	)
#

ic-right-associative ' + ' #
ic-right-associative ' * ' #
ic-right-associative and #
ic-right-associative or #
ic-right-associative xor #

ic-operator .operator .op
	(_ .insn ($1, .op)
	, _ R-
	, .e
)/.e
	:- ic-operator-insn .operator .insn
#
ic-operator .operator .op
	(_ CMP ($1, .op)
	, _ R-
	, _ .setcc (DL)
	, _ MOVSX ($0, DL)
	, .e
)/.e
	:- ic-operator-setcc .operator .setcc
#
ic-operator ' * ' .op -- iMUL cannot accept immediate operands
	(_ MOV ($0, .op)
	, _ IMUL ($1, $0)
	, _ R-
	, .e
)/.e
#
ic-operator ' / ' .op .e :- ic-divide .op EAX .e
#
ic-operator ' %% ' .op .e :- ic-divide .op EDX .e
#
ic-operator .shift .op
	(_ MOV (ECX, .op)
	, _ R-
	, _ .insn ($0, CL)
	, .e
)/.e
	:- ic-operator-shift .shift .insn
#

ic-divide .op .reg
	(_ MOV (ECX, .op)
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
	:- 16 < .size
	, let .div4 (.size / 4)
	, let .mod4 (.size % 4)
	, .e0 = (_ CLD ()
		, _ MOV (EDX, ESI)
		, _ MOV (EDI, $0)
		, _ MOV (ESI, $1)
		, _ MOV (ECX, .div4)
		, _ REP ()
		, _ MOVSD ()
		, .e1)
	, once (1 <= .mod4, .e1 = (_ MOVSB (), .e2); .e1 = .e2)
	, once (2 <= .mod4, .e2 = (_ MOVSB (), .e3); .e2 = .e3)
	, once (3 <= .mod4, .e3 = (_ MOVSB (), .e4); .e3 = .e4)
	, .e4 = (_ MOV (ESI, EDX)
		, .ex)
#
ic-copy-memory .o .size .e0/.ex
  :- 4 < .size
  , ic-copy-memory .o 4 .e0/.e1
  , let .size1 (.size - 4)
  , let .o1 (.o + 4)
  , ic-copy-memory .o1 .size1 .e1/.ex
#

ic-operator-negate ' = ' ' != ' #
ic-operator-negate ' > ' ' <= ' #
ic-operator-negate ' < ' ' >= ' #

ic-operator-insn ' + ' ADD #
ic-operator-insn ' - ' SUB #
ic-operator-insn and AND #
ic-operator-insn or OR #
ic-operator-insn xor XOR #

ic-operator-jmpcc ' = ' JE #
ic-operator-jmpcc ' != ' JNE #
ic-operator-jmpcc ' <= ' JLE #
ic-operator-jmpcc ' < ' JL #
ic-operator-jmpcc ' >= ' JGE #
ic-operator-jmpcc ' > ' JG #

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
