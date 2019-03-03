-- assembler. Only supports 32-bit addressing.
--
-- as	= assembler
-- asi	= assemble instruction

asi:.s:.a .i .e1 :- asi0:.s:.a .i .e0/(), as-lets .e0 .e1 #

as-lets () () #
as-lets (.i, .is) .os0 :- once (as-let .i .os0 .os1), as-lets .is .os1 #

as-let (E8 .i) .os0 .osx :- builtin:suite.asm.AssemblePredicates:emit8 .i .os0 .osx #
as-let (E16 .i) .os0 .osx :- builtin:suite.asm.AssemblePredicates:emit16 .i .os0 .osx #
as-let (E32 .i) .os0 .osx :- builtin:suite.asm.AssemblePredicates:emit32 .i .os0 .osx #
as-let .v (.v, .os) .os #

asi0:.s0:.a .i .e0/.ex
	:- asis:.s1:.a .i .e1/.ex
	, (
		.s0 = .s1, .e0 = .e1
		; .s1 = 8, .e0 = .e1
		; .s0/.s1 = 16/32, .e0 = (+x66, .e1)
		; .s0/.s1 = 32/16, .e0 = (+x66, .e1)
	)
#

asis:_s:_a () .e/.e #
asis:_s:_a (AAA ()) (+x37, .e)/.e #
asis:.s:.a (ADC (.op0, .op1)) .e :- asi-2op:.s:.a .op0 .op1 +x10 +x80 2 .e #
asis:.s:.a (ADD (.op0, .op1)) .e :- asi-2op:.s:.a .op0 .op1 +x00 +x80 0 .e #
asis:_s:.a (ADVANCE .a) .e/.e #
asis:.s:.a0 (ADVANCE .a1) (0, .e1)/.ex :- .a0 < .a1, let .a (.a0 + 1), asis:.s:.a (ADVANCE .a1) .e1/.ex #
asis:.s:.a (AND (.op0, .op1)) .e :- asi-2op:.s:.a .op0 .op1 +x20 +x80 4 .e #
asis:_s:_a (AOP ()) (+x67, .e)/.e #
asis:.s:.a (CALL .target) (+xE8, .e1)/.ex :- .s = 32, asi-jump-rel:.s .target .a 1 .rel, as-verify-emit:.s .rel .e1/.ex #
asis:_s:.a (CALL .rm) .e0/.ex :- as-mod-num-rm:32:.a .rm (0 2) .e0/(+xFF, .e1)/.e1/.ex #
asis:_s:_a (CLD ()) (+xFC, .e)/.e #
asis:_s:_a (CLI ()) (+xFA, .e)/.e #
asis:.s:.a (CMP (.op0, .op1)) .e :- asi-2op:.s:.a .op0 .op1 +x38 +x80 07 .e #
asis:.s:.a (CMPXCHG (.rm, .reg)) (+x0F, .e1)/.ex :- asi-rm-reg:.s:.a .rm .reg +xB0 .e1/.ex #
asis:_s:_a (CPUID) (+x0F, +xA2, .e)/.e #
asis:.s:.a (DEC .op) .e :- asi-1op:.s:.a .op +x48 +xFE 1 .e #
asis:.s:.a (DIV .rm) .e :- asi-rm:.s:.a +xF6 .rm 6 .e #
asis:_s:_a (DS "") .e/.e #
asis:_s:_a (DS .s) (.b, .e1)/.ex :- .s != "", substring .s 0 1 .h, substring .s 1 0 .t, char.ascii .h .b, asis:_:_ (DS .t) .e1/.ex #
asis:_s:_a (HLT ()) (+xF4, .e)/.e #
asis:.s:.a (IDIV .rm) .e :- asi-rm:.s:.a +xF6 .rm 7 .e #
asis:_s:_a (IMM (BYTE .imm)) .e :- as-emit:8 .imm .e #
asis:_s:_a (IMM (DWORD .imm)) .e :- as-emit:32 .imm .e #
asis:_s:_a (IMM (QWORD .imm)) .e :- as-emit:64 .imm .e #
asis:_s:_a (IMM (WORD .imm)) .e :- as-emit:16 .imm .e #
asis:.s:.a (IMUL .rm) .e :- asi-rm:.s:.a +xF6 .rm 5 .e #
asis:.s:.a (IMUL (.reg, .rm)) .e0/.ex :- as-rm-regwd:.s:.a .rm .reg .e0/(+x0F, +xAF, .e1)/.e1/.ex #
asis:.s:.a (IMUL (.reg, .rm, .imm)) .e0/.ex :- as-rm-regwd:.s:.a .rm .reg .e0/(+x6B, .e1)/.e1/.e2, as-verify-emit:8 .imm .e2/.ex #
asis:.s:.a (IMUL (.reg, .rm, .imm)) .e0/.ex :- as-rm-regwd:.s:.a .rm .reg .e0/(+x69, .e1)/.e1/.e2, as-verify-emit:.s .imm .e2/.ex #
asis:.s:_a (IN (.val, .port)) .e :- asi-in-out:.s .val .port +xE4 .e #
asis:.s:.a (INC .op) .e :- asi-1op:.s:.a .op +x40 +xFE 0 .e #
asis:_s:_a (INT 3) (+xCC, .e)/.e #
asis:_s:_a (INT .imm) (+xCD, .e0)/.ex :- as-verify-emit:8 .imm .e0/.ex #
asis:_s:_a (INTO ()) (+xCE, .e)/.e #
asis:_s:.a (INVLPG .m) .e0/.ex :- as-mod-num-rm:_:.a .m (0 7) .e0/(+x0F, +x01, .e1)/.e1/.ex #
asis:_s:_a (IRET ()) (+xCF, .e)/.e #
asis:_s:.a (JA .target) .e :- asi-jump .a .target +x77 +x0F +x87 .e #
asis:_s:.a (JAE .target) .e :- asi-jump .a .target +x73 +x0F +x83 .e #
asis:_s:.a (JB .target) .e :- asi-jump .a .target +x72 +x0F +x82 .e #
asis:_s:.a (JBE .target) .e :- asi-jump .a .target +x76 +x0F +x86 .e #
asis:_s:.a (JE .target) .e :- asi-jump .a .target +x74 +x0F +x84 .e #
asis:_s:.a (JG .target) .e :- asi-jump .a .target +x7F +x0F +x8F .e #
asis:_s:.a (JGE .target) .e :- asi-jump .a .target +x7D +x0F +x8D .e #
asis:_s:.a (JL .target) .e :- asi-jump .a .target +x7C +x0F +x8C .e #
asis:_s:.a (JLE .target) .e :- asi-jump .a .target +x7E +x0F +x8E .e #
asis:_s:.a (JMP .target) .e :- asi-jump .a .target +xEB () +xE9 .e #
asis:_s:.a (JMP .rm) .e0/.ex :- as-mod-num-rm:32:.a .rm (0 4) .e0/(+xFF, .e1)/.e1/.ex #
asis:_s:.a (JNE .target) .e :- asi-jump .a .target +x75 +x0F +x85 .e #
asis:_s:.a (JNO .target) .e :- asi-jump .a .target +x71 +x0F +x81 .e #
asis:_s:.a (JNP .target) .e :- asi-jump .a .target +x7B +x0F +x8B .e #
asis:_s:.a (JNS .target) .e :- asi-jump .a .target +x79 +x0F +x89 .e #
asis:_s:.a (JNZ .target) .e :- asi-jump .a .target +x75 +x0F +x85 .e #
asis:_s:.a (JO .target) .e :- asi-jump .a .target +x70 +x0F +x80 .e #
asis:_s:.a (JP .target) .e :- asi-jump .a .target +x7A +x0F +x8A .e #
asis:_s:.a (JS .target) .e :- asi-jump .a .target +x78 +x0F +x88 .e #
asis:_s:.a (JZ .target) .e :- asi-jump .a .target +x74 +x0F +x84 .e #
asis:_s:.a (LEA (.reg, .rm)) .e0/.ex :- as-rm-regwd:_:.a .rm .reg .e0/(+x8D, .e1)/.e1/.ex #
asis:.s:_a (LOCK .a) (+xF0, .e1)/.ex :- asis:.s:_a .a .e1/.ex #
asis:_s:.a (LOOP .target) (+xE2, .e1)/.ex :- asi-jump8 .a .target .e1/.ex #
asis:_s:.a (LOOPE .target) (+xE1, .e1)/.ex :- asi-jump8 .a .target .e1/.ex #
asis:_s:.a (LOOPNE .target) (+xE0, .e1)/.ex :- asi-jump8 .a .target .e1/.ex #
asis:_s:.a (LOOPNZ .target) (+xE0, .e1)/.ex :- asi-jump8 .a .target .e1/.ex #
asis:_s:.a (LOOPZ .target) (+xE1, .e1)/.ex :- asi-jump8 .a .target .e1/.ex #
asis:.s:.a (LGDT .rm) .e0/.ex :- as-mod-num-rm:.s:.a .rm (0 2) .e0/(+x0F, +x01, .e1)/.e1/.ex #
asis:.s:.a (LIDT .rm) .e0/.ex :- as-mod-num-rm:.s:.a .rm (0 3) .e0/(+x0F, +x01, .e1)/.e1/.ex #
asis:_s:.a (LTR .rm) .e0/.ex :- as-mod-num-rm:16:.a .rm (0 3) .e0/(+x0F, +x00, .e1)/.e1/.ex #
asis:.s:_a (MOV (.reg, .imm)) .e :- asi-reg-imm:.s .reg .imm +xB0 .e #
asis:.s:.a (MOV (.rm, .imm)) .e :- asi-rm-imm:.s:.a .rm .imm +xC6 0 .e #
asis:.s:.a (MOV (.rm0, .rm1)) .e :- asi-rm-reg2:.s:.a .rm0 .rm1 +x88 .e #
asis:16:.a (MOV (.rm, .sreg)) .e0/.ex :- as-segment-reg .sreg .sr, as-mod-num-rm:16:.a .rm (0 .sr) .e0/(+x8C, .e1)/.e1/.ex #
asis:16:.a (MOV (.sreg, .rm)) .e0/.ex :- as-segment-reg .sreg .sr, as-mod-num-rm:16:.a .rm (0 .sr) .e0/(+x8E, .e1)/.e1/.ex #
asis:.s:_a (MOV (.reg, .cr)) (+x0F, +x20, .b, .e)/.e :- as-control-reg .cr .n, as-reg:.s .reg .r, let .b (+xC0 + .n * 8 + .r), .s = 32 #
asis:.s:_a (MOV (.cr, .reg)) (+x0F, +x22, .b, .e)/.e :- as-control-reg .cr .n, as-reg:.s .reg .r, let .b (+xC0 + .n * 8 + .r), .s = 32 #
asis:8:_a (MOVSB ()) (+xA4, .e)/.e #
asis:32:_a (MOVSD ()) (+xA5, .e)/.e #
asis:16:_a (MOVSW ()) (+xA5, .e)/.e #
asis:.s:.a (MOVSX (.reg, .rm)) .e :- asi-reg-rm-extended:.s:.a .reg .rm +xBE .e #
asis:.s:.a (MOVZX (.reg, .rm)) .e :- asi-reg-rm-extended:.s:.a .reg .rm +xB6 .e #
asis:.s:.a (MUL .rm) .e :- asi-rm:.s:.a +xF6 .rm 4 .e #
asis:_s:_a (NOP ()) (+x90, .e)/.e #
asis:_s:_a (OOP ()) (+x66, .e)/.e #
asis:.s:.a (OR (.op0, .op1)) .e :- asi-2op:.s:.a .op0 .op1 +x08 +x80 1 .e #
asis:.s:_a (OUT (.port, .val)) .e :- asi-in-out:.s .val .port +xE6 .e #
asis:.s:.a (POP .op) .e :- asi-1op:.s:.a .op +x58 +x8E 0 .e, member (16, 32, 64,) .s #
asis:_s:_a (POP DS) (+x1F, .e)/.e #
asis:_s:_a (POP ES) (+x07, .e)/.e #
asis:_s:_a (POP FS) (+x0F, +xA1, .e)/.e #
asis:_s:_a (POP GS) (+x0F, +xA9, .e)/.e #
asis:_s:_a (POP SS) (+x17, .e)/.e #
asis:_s:_a (POPA ()) (+x61, .e)/.e #
asis:_s:_a (POPF ()) (+x9D, .e)/.e #
asis:.s:_a (PUSH .imm) (.b, .e1)/.ex :- as-verify-emit:.s .imm .e1/.ex, if (.s = 8) (.b = +x6A) (.b = +x68) #
asis:.s:.a (PUSH .op) .e :- asi-1op:.s:.a .op +x50 +xFE 6 .e, member (16, 32, 64,) .s #
asis:_s:_a (PUSH CS) (+x0E, .e)/.e #
asis:_s:_a (PUSH DS) (+x1E, .e)/.e #
asis:_s:_a (PUSH ES) (+x06, .e)/.e #
asis:_s:_a (PUSH FS) (+x0F, +xA0, .e)/.e #
asis:_s:_a (PUSH GS) (+x0F, +xA8, .e)/.e #
asis:_s:_a (PUSH SS) (+x16, .e)/.e #
asis:_s:_a (PUSHA ()) (+x60, .e)/.e #
asis:_s:_a (PUSHF ()) (+x9C, .e)/.e #
asis:_s:_a (RDMSR ()) (+x0F, +x32, .e)/.e #
asis:.s:_a (REP .i) (+xF3, .e1)/.ex :- asis:.s:_a .i .e1/.ex #
asis:.s:_a (REPE .i) (+xF3, .e1)/.ex :- asis:.s:_a .i .e1/.ex #
asis:.s:_a (REPNE .i) (+xF2, .e1)/.ex :- asis:.s:_a .i .e1/.ex #
asis:_s:_a (RET ()) (+xC3, .e)/.e #
asis:_s:_a (RET .imm) (+xC2, .e1)/.ex :- as-verify-emit:16 .imm .e1/.ex #
asis:.s:.a (SAL (.rm, .op)) .e :- asi-shift:.s:.a .rm .op +xC0 4 .e #
asis:.s:.a (SAR (.rm, .op)) .e :- asi-shift:.s:.a .rm .op +xC0 7 .e #
asis:.s:.a (SBB (.op0, .op1)) .e :- asi-2op:.s:.a .op0 .op1 +x18 +x80 3 .e #
asis:.s:.a (SETA .rm) .e0/.ex :- as-mod-num-rm:.s:.a .rm (0 0) .e0/(+x0F, +x97, .e1)/.e1/.ex #
asis:.s:.a (SETAE .rm) .e0/.ex :- as-mod-num-rm:.s:.a .rm (0 0) .e0/(+x0F, +x93, .e1)/.e1/.ex #
asis:.s:.a (SETB .rm) .e0/.ex :- as-mod-num-rm:.s:.a .rm (0 0) .e0/(+x0F, +x92, .e1)/.e1/.ex #
asis:.s:.a (SETBE .rm) .e0/.ex :- as-mod-num-rm:.s:.a .rm (0 0) .e0/(+x0F, +x96, .e1)/.e1/.ex #
asis:.s:.a (SETE .rm) .e0/.ex :- as-mod-num-rm:.s:.a .rm (0 0) .e0/(+x0F, +x94, .e1)/.e1/.ex #
asis:.s:.a (SETG .rm) .e0/.ex :- as-mod-num-rm:.s:.a .rm (0 0) .e0/(+x0F, +x9F, .e1)/.e1/.ex #
asis:.s:.a (SETGE .rm) .e0/.ex :- as-mod-num-rm:.s:.a .rm (0 0) .e0/(+x0F, +x9D, .e1)/.e1/.ex #
asis:.s:.a (SETL .rm) .e0/.ex :- as-mod-num-rm:.s:.a .rm (0 0) .e0/(+x0F, +x9C, .e1)/.e1/.ex #
asis:.s:.a (SETLE .rm) .e0/.ex :- as-mod-num-rm:.s:.a .rm (0 0) .e0/(+x0F, +x9E, .e1)/.e1/.ex #
asis:.s:.a (SETNE .rm) .e0/.ex :- as-mod-num-rm:.s:.a .rm (0 0) .e0/(+x0F, +x95, .e1)/.e1/.ex #
asis:.s:.a (SHL (.rm, .op)) .e :- asi-shift:.s:.a .rm .op +xC0 4 .e #
asis:.s:.a (SHR (.rm, .op)) .e :- asi-shift:.s:.a .rm .op +xC0 5 .e #
asis:_s:_a (STI ()) (+xFB, .e)/.e #
asis:8:_a (STOSB ()) (+xAA, .e)/.e #
asis:32:_a (STOSD ()) (+xAB, .e)/.e #
asis:16:_a (STOSW ()) (+xAB, .e)/.e #
asis:.s:.a (SUB (.op0, .op1)) .e :- asi-2op:.s:.a .op0 .op1 +x28 +x80 5 .e #
asis:_s:_a (SYSENTER ()) (+x0F, +x34, .e)/.e #
asis:_s:_a (SYSEXIT ()) (+x0F, +x35, .e)/.e #
asis:.s:_a (TEST (.acc, .imm)) (.b, .e1)/.ex :- as-reg:.s .acc 0, as-verify-emit:.s .imm .e1/.ex, if (.s = 8) (.b = +xA8) (.b = +xA9) #
asis:.s:.a (TEST (.rm, .imm)) .e :- asi-rm-imm:.s:.a .rm .imm +xF6 0 .e #
asis:.s:.a (TEST (.rm, .reg)) .e :- asi-rm-reg:.s:.a .rm .reg +x84 .e #
asis:_s:_a (WRMSR ()) (+x0F, +x30, .e)/.e #
asis:.s:_a (XCHG (.acc, .reg)) (.b, .e)/.e :- as-reg:.s .acc 0, as-reg:.s .reg .r, let .b (+x90 + .r) #
asis:.s:.a (XCHG (.rm, .reg)) .e :- asi-rm-reg:.s:.a .rm .reg +x86 .e #
asis:.s:.a (XOR (.op0, .op1)) .e :- asi-2op:.s:.a .op0 .op1 +x30 +x80 6 .e #

asi-jump .a (BYTE .target) .b _ _ (.b, .e1)/.ex
	:- asi-jump8 .a .target .e1/.ex, !
#
asi-jump .a (DWORD .target) _ () .b (.b, .e1)/.ex
	:- asi-jump-rel:32 .target .a 1 .rel, as-verify-emit:32 .rel .e1/.ex, !
#
asi-jump .a (DWORD .target) _ .b0 .b1 (.b0, .b1, .e1)/.ex
	:- asi-jump-rel:32 .target .a 2 .rel, as-verify-emit:32 .rel .e1/.ex, !
#
asi-jump .a .target .b _ _ (.b, .e1)/.ex
	:- asi-jump .a (BYTE .target) .b _ _ (.b, .e1)/.ex
#

asi-jump8 .a .target .e
	:- asi-jump-rel:8 .target .a 1 .rel
	, if (as-imm:8 .rel) (as-verify-emit:8 .rel .e) (throw "Jumping too far")
#

asi-jump-rel:.size .target .a .f .rel
	:- if (bound .target) (is.int .target, let .rel (.target - .a - .f - .size / 8)) (.rel = 0)
#

asi-in-out:.size .acc .port .b0 (E8 .b2, .e1)/.ex
	:- as-reg:.size .acc 0
	, (.size = 8, .b1 = .b0; member (16, 32, 64,) .size, .b1 = .b0 + 1)
	, if (.port = DX) (.e1 = .ex, .b2 = .b1 + 8) (as-verify-emit:8 .port .e1/.ex, .b1 = .b2)
#

asi-shift:.size:.a .rm .op .b0 .n .e
	:- asi-rm:.size:.a .b1 .rm .n .e
	, (.op = 1, .b1 = .b0 + 16; .op = CL, .b1 = .b0 + 18)
#
asi-shift:.size:.a .rm .imm8 .b0 .n .e0/.ex
	:- asi-rm-imm8:.size:.a .rm .imm8 .n .e0/(E8 .b1, .e1)/.e1/.ex
	, (.size = 8, .b1 = .b0; member (16, 32, 64,) .size, .b1 = .b0 + 1)
#

-- common single-operand instructions, like DEC, NEG
asi-1op:.size:_a .reg .b _ _ .e :- asi-reg:.size .reg .b .e, member (16, 32, 64,) .size #
asi-1op:.size:.a .rm _ .b .n .e :- asi-rm:.size:.a .b .rm .n .e #

-- common two-operand instructions, like ADD, OR, XOR
asi-2op:.size:_a .acc .imm .b0 _ _ .e
	:- asi-acc-imm:.size .b1 .acc .imm .e
	, .b1 = .b0 + 4
#
asi-2op:.size:.a .rm0 .rm1 .b _ _ .e
	:- asi-rm-reg2:.size:.a .rm0 .rm1 .b .e
#
asi-2op:.size:.a .rm .imm8 _ .b0 .n .e0/.ex
	:- asi-rm-imm8:.size:.a .rm .imm8 .n .e0/(E8 .b1, .e1)/.e1/.ex
	, .b1 = .b0 + 3
#
asi-2op:.size:.a .rm .imm _ .b .n .e
	:- asi-rm-imm:.size:.a .rm .imm .b .n .e
#

asi-reg-rm-extended:.size:.a .reg .rm .b0 .e0/.ex
	:- as-rex-reg:.size .reg .rexr .r
	, as-mod-num-rm:.size1:.a .rm (.rexr .r) .e0/(+x0F, E8 .b1, .e1)/.e1/.ex
	, if (.size1 = 8) (.b1 = .b0) (.b1 = .b0 + 1)
#

asi-rm-imm8:.size:.a .rm .imm8 .n .e0/.e1/.e2/.ex
	:- as-mod-num-rm:.size:.a .rm (0 .n) .e0/.e1/.e2/.e3
	, as-verify-emit:8 .imm8 .e3/.ex
#

asi-rm-imm:.size:.a .rm .imm .b0 .num .e0/.ex
	:- as-mod-num-rm:.size:.a .rm (0 .num) .e0/(E8 .b1, .e1)/.e1/.e2
	, as-verify-emit:.size .imm .e2/.ex
	, (.size = 8, .b1 = .b0; member (16, 32, 64,) .size, .b1 = .b0 + 1)
#

asi-acc-imm:.size .b0 .acc .imm (E8 .b1, .e1)/.ex
	:- as-reg:.size .acc 0
	, as-verify-emit:.size .imm .e1/.ex
	, (.size = 8, .b1 = .b0; member (16, 32, 64,) .size, .b1 = .b0 + 1)
#

asi-reg-imm:.size .reg .imm .b0 .e0/.ex
	:- asi-reg:.size .reg .b1 .e0/.e1
	, as-verify-emit:.size .imm .e1/.ex
	, (.size = 8, .b1 = .b0; member (16, 32, 64,) .size, .b1 = .b0 + 8)
#

asi-rm-reg2:.size:.a .rm .reg .b .e
	:- asi-rm-reg:.size:.a .rm .reg .b .e
#
asi-rm-reg2:.size:.a .reg .rm .b0 .e
	:- asi-rm-reg:.size:.a .rm .reg .b1 .e
	, .b1 = .b0 + 2
#

asi-rm-reg:.size:.a .rm .reg .b0 .e0/.ex
	:- as-rm-regwd:.size:.a .rm .reg .e0/(E8 .b1, .e1)/.e1/.ex
	, (.size = 8, .b1 = .b0; member (16, 32, 64,) .size, .b1 = .b0 + 1)
#

asi-rm:.size:.a .b0 .rm .num .e0/.ex
	:- as-mod-num-rm:.size:.a .rm (0 .num) .e0/(E8 .b1, .e1)/.e1/.ex
	, (.size = 8, .b1 = .b0; member (16, 32, 64,) .size, .b1 = .b0 + 1)
#

asi-reg:.size .reg .b0 (E8 .b1, .e)/.e
	:- as-reg:.size .reg .r
	, .b1 = .b0 + .r
#

as-rm-regwd:.size:.a .rm .reg .e
	:- as-rex-reg:.size .reg .rexr .r
	, as-mod-num-rm:.size:.a .rm (.rexr .r) .e
#

as-mod-num-rm:.size:.a .rm (.rexr .num) .e0/.e1/.e2/.ex
	:- is.int .num
	, once (as-mod-rm:.size:.a .rm (.modb (.rexb .rmb)) .sib (.ds .disp)
		, .e2 = (E8 (.modb * 64 + .num * 8 + .rmb), .e3)
		, (
			.sib = (), .rexx = 0, .e3 = .e4
			; .sib = .s (.rexx .ir) .br, .e3 = (E8 (.s * 64 + .ir * 8 + .br), .e4)
		)
		, (.ds = 0, .e4 = .ex
			; as-emit:.ds .disp .e4/.ex
		)
		, .rexw = 0
		, let .rex (+x40 + .rexw * 8 + .rexr * 4 + .rexx * 2 + .rexb)
		, (.rex = +x40, .e0 = .e1; .e0 = (.rex, .e1))
	)
#

as-mod-rm:.size:_a .reg (3 (.rexr .r)) () (0 0)
	:- as-rex-reg:.size .reg .rexr .r
#
as-mod-rm:_:_a .op (1 (.rexr 5)) () (8 0)
	:- (.op = `.reg`; .op = `.reg + 0`)
	, as-rex-reg:_ .reg .rexr 5
	, !
#
as-mod-rm:.size:.a (.prefix `.ptr`) .modrm .sib .dsd
	:- as-prefix-size .prefix .size
	, as-mod-rm:_:.a `.ptr` .modrm .sib .dsd
#
as-mod-rm:_:_a (`.indexReg * .scale + .baseReg + .disp`) (.mod (.rexbr 4)) (.s (.rexir .ir) .br) (.ds .disp)
	:- as-sib-scale .scale .s
	, as-rex-reg:32 .indexReg .rexir .ir
	, as-rex-reg:32 .baseReg .rexbr .br
	, as-disp-mod:.ds .disp .mod
	, not (.ir = 4; .br = 5)
#
as-mod-rm:_:_a (`.indexReg * .scale + .baseReg`) .modrm .sib .dsd
	:- as-rex-reg:32 .baseReg _ _
	, as-mod-rm:_:_a (`.indexReg * .scale + .baseReg + 0`) .modrm .sib .dsd
#
as-mod-rm:_:_a (`.indexReg * .scale + .disp`) (0 (0 4)) (.s (.rexir .ir) 5) (32 .disp)
	:- as-sib-scale .scale .s
	, as-rex-reg:32 .indexReg .rexir .ir
	, not (.ir = 4)
#
as-mod-rm:_:_a (`ESP + .disp`) (.mod (0 4)) (0 (0 4) 4) (.ds .disp)
	:- once (.disp = 0, .mod = 1, .ds = 1; as-disp-mod:.ds .disp .mod)
#
as-mod-rm:_:_a (`.reg + .disp`) (.mod (.rexr .r)) () (.ds .disp)
	:- as-rex-reg:32 .reg .rexr .r
	, as-disp-mod:.ds .disp .mod
	, not (.r = 4; .mod = 0, .r = 5; .r = 12; .mod = 0, .r = 13)
#
as-mod-rm:_:_a (`.indexReg * .scale`) .modrm .sib .dsd
	:- as-mod-rm:_:_a (`.indexReg * .scale + 0`) .modrm .sib .dsd
#
as-mod-rm:_:_a (`.reg`) .modrm .sib .dsd
	:- as-rex-reg:32 .reg _ _
	, as-mod-rm:_ (`.reg + 0`) .modrm .sib .dsd
#
as-mod-rm:_:.a (`.abs`) (0 (0 5)) () (32 .disp)
	:- as-verify-imm:32 .abs .imm
	, once (as-long-mode, .disp = .imm - .a; .disp = .imm)
#

as-disp-mod:.ds .disp .mod
	:- .disp = 0, .mod = 0, .ds = 0
	; as-imm:8 .disp, .mod = 1, .ds = 8
	; .mod = 2, .ds = 32
#

as-verify-emit:.size .imm .e
	:- as-verify-imm:.size .imm .i
	, as-emit:.size .i .e
#

as-emit:8 .d (E8 .d, .e)/.e #
as-emit:16 .d (E16 .d, .e)/.e #
as-emit:32 .d (E32 .d, .e)/.e #

as-verify-imm:.size .imm .i
	:- once (not (bound .imm)
		; .imm = .prefix .i
		, as-prefix-size .prefix .size
		, as-imm:.size .i
		; as-imm:.size .imm
		, .imm = .i
	)
#

as-imm:8 .imm :- is.int .imm, -128 <= .imm, .imm < 128 #
as-imm:16 .imm :- is.int .imm, -32768 <= .imm, .imm < 32768 #
as-imm:32 .imm :- is.int .imm #

as-sib-scale 1 0 #
as-sib-scale 2 1 #
as-sib-scale 4 2 #
as-sib-scale 8 3 #

as-prefix-size BYTE 8 #
as-prefix-size WORD 16 #
as-prefix-size DWORD 32 #
as-prefix-size QWORD 64 #

as-reg:.s .reg .i :- as-rex-reg:.s .reg 0 .i #

as-rex-reg:.s .reg .rexr .r :- bound .reg, as-rex-reg0:.s .reg .rexr .r #

as-segment-reg .sreg .sr :- bound .sreg, as-segment-reg0 .sreg .sr #

as-control-reg .creg .cr :- bound .creg, as-control-reg0 .creg .cr #

as-rex-reg0:8 AL 0 0 #
as-rex-reg0:8 CL 0 1 #
as-rex-reg0:8 DL 0 2 #
as-rex-reg0:8 BL 0 3 #
as-rex-reg0:8 AH 0 4 :- not as-long-mode #
as-rex-reg0:8 CH 0 5 :- not as-long-mode #
as-rex-reg0:8 DH 0 6 :- not as-long-mode #
as-rex-reg0:8 BH 0 7 :- not as-long-mode #
as-rex-reg0:8 R4B 0 4 :- as-long-mode #
as-rex-reg0:8 R5B 0 5 :- as-long-mode #
as-rex-reg0:8 R6B 0 6 :- as-long-mode #
as-rex-reg0:8 R7B 0 7 :- as-long-mode #
as-rex-reg0:8 R8B 1 0 :- as-long-mode #
as-rex-reg0:8 R9B 1 1 :- as-long-mode #
as-rex-reg0:8 R10B 1 2 :- as-long-mode #
as-rex-reg0:8 R11B 1 3 :- as-long-mode #
as-rex-reg0:8 R12B 1 4 :- as-long-mode #
as-rex-reg0:8 R13B 1 5 :- as-long-mode #
as-rex-reg0:8 R14B 1 6 :- as-long-mode #
as-rex-reg0:8 R15B 1 7 :- as-long-mode #
as-rex-reg0:16 AX 0 0 #
as-rex-reg0:16 CX 0 1 #
as-rex-reg0:16 DX 0 2 #
as-rex-reg0:16 BX 0 3 #
as-rex-reg0:16 SP 0 4 #
as-rex-reg0:16 BP 0 5 #
as-rex-reg0:16 SI 0 6 #
as-rex-reg0:16 DI 0 7 #
as-rex-reg0:16 R8W 1 0 :- as-long-mode #
as-rex-reg0:16 R9W 1 1 :- as-long-mode #
as-rex-reg0:16 R10W 1 2 :- as-long-mode #
as-rex-reg0:16 R11W 1 3 :- as-long-mode #
as-rex-reg0:16 R12W 1 4 :- as-long-mode #
as-rex-reg0:16 R13W 1 5 :- as-long-mode #
as-rex-reg0:16 R14W 1 6 :- as-long-mode #
as-rex-reg0:16 R15W 1 7 :- as-long-mode #
as-rex-reg0:32 EAX 0 0 #
as-rex-reg0:32 ECX 0 1 #
as-rex-reg0:32 EDX 0 2 #
as-rex-reg0:32 EBX 0 3 #
as-rex-reg0:32 ESP 0 4 #
as-rex-reg0:32 EBP 0 5 #
as-rex-reg0:32 ESI 0 6 #
as-rex-reg0:32 EDI 0 7 #
as-rex-reg0:32 R8D 1 0 :- as-long-mode #
as-rex-reg0:32 R9D 1 1 :- as-long-mode #
as-rex-reg0:32 R10D 1 2 :- as-long-mode #
as-rex-reg0:32 R11D 1 3 :- as-long-mode #
as-rex-reg0:32 R12D 1 4 :- as-long-mode #
as-rex-reg0:32 R13D 1 5 :- as-long-mode #
as-rex-reg0:32 R14D 1 6 :- as-long-mode #
as-rex-reg0:32 R15D 1 7 :- as-long-mode #
as-rex-reg0:64 RAX 0 0 #
as-rex-reg0:64 RCX 0 1 #
as-rex-reg0:64 RDX 0 2 #
as-rex-reg0:64 RBX 0 3 #
as-rex-reg0:64 RSP 0 4 #
as-rex-reg0:64 RBP 0 5 #
as-rex-reg0:64 RSI 0 6 #
as-rex-reg0:64 RDI 0 7 #
as-rex-reg0:64 R8 1 0 :- as-long-mode #
as-rex-reg0:64 R9 1 1 :- as-long-mode #
as-rex-reg0:64 R10 1 2 :- as-long-mode #
as-rex-reg0:64 R11 1 3 :- as-long-mode #
as-rex-reg0:64 R12 1 4 :- as-long-mode #
as-rex-reg0:64 R13 1 5 :- as-long-mode #
as-rex-reg0:64 R14 1 6 :- as-long-mode #
as-rex-reg0:64 R15 1 7 :- as-long-mode #

as-xmm-reg0 XMM:.r 0 .r :- .r < 8 #
as-xmm-reg0 XMM:.r 0 .rb :- 8 <= .r, let .rb (.r - 8) #

as-ymm-reg0 YMM:.r 0 .r :- .r < 8 #
as-ymm-reg0 YMM:.r 0 .rb :- 8 <= .r, let .rb (.r - 8) #

as-zmm-reg0 ZMM:.r 0 .r :- .r < 8 #
as-zmm-reg0 ZMM:.r 0 .rb :- 8 <= .r, let .rb (.r - 8) #

as-segment-reg0 ES 0 #
as-segment-reg0 CS 1 #
as-segment-reg0 SS 2 #
as-segment-reg0 DS 3 #
as-segment-reg0 FS 4 #
as-segment-reg0 GS 5 #

as-control-reg0 CR0 0 #
as-control-reg0 CR2 2 #
as-control-reg0 CR3 3 #
as-control-reg0 CR4 4 #

as-long-mode :- fail #

as-error .m :- !, write.error .m, nl, fail #
