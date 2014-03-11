-- Assembler. Assumes 32-bit mode and 32-bit addressing.
--
-- as	= assembler
-- asi	= assemble instruction

asis:_s .a (ADVANCE .a) .e/.e #
asis:.s .a0 (ADVANCE .a1) (0, .e1)/.ex
	:- let .a (.a0 + 1), asis:.s .a (ADVANCE .a1) .e1/.ex, !
#
asi:.s .a .i .e0/.ex :- asis:.s .a .i .e0/.ex, ! #
asi:16 .a .i (+x66, .e1)/.ex :- asis:32 .a .i .e1/.ex, ! #
asi:32 .a .i (+x66, .e1)/.ex :- asis:16 .a .i .e1/.ex, ! #

asis:_s _a (() _) .e/.e #
asis:_s _a (AAA) (+x37, .e)/.e #
asis:.s _a (ADD .acc .imm) .e0/.ex :- asi-acc-imm:.s +x04 .acc .imm .e0/.ex #
asis:.s _a (ADD .rm .imm8) (+x83, .e1)/.ex :- as-imm:8 .imm8, as-mod-num-rm:.s .rm 0 .e1/.e2, as-emit:8 .imm8 .e2/.ex #
asis:.s _a (ADD .rm .imm) .e0/.ex :- asi-rm-imm:.s +x80 .rm 0 .imm .e0/.ex #
asis:.s _a (ADD .rm0 .rm1) .e0/.ex :- asi-rm-reg2:.s +x00 .rm0 .rm1 .e0/.ex #
asis:_s _a (CLI) (+xFA, .e)/.e #
asis:_s _a (D8 .imm) .e0/.ex :- as-emit:8 .imm .e0/.ex #
asis:_s _a (D32 .imm) .e0/.ex :- as-emit:32 .imm .e0/.ex #
asis:.s _a (DEC .reg) .e0/.ex :- asi-reg:.s +x48 .reg .e0/.ex, .s != 8 #
asis:.s _a (DEC .rm) .e0/.ex :- asi-rm:.s +xFE .rm 1 .e0/.ex #
asis:.s _a (INC .reg) .e0/.ex :- asi-reg:.s +x40 .reg .e0/.ex, .s != 8 #
asis:.s _a (INC .rm) .e0/.ex :- asi-rm:.s +xFE .rm 0 .e0/.ex #
asis:_s _a (INT 3) (+x37, .e)/.e #
asis:_s _a (INT .imm) (+xCD, .e0)/.ex :- as-emit:8 .imm .e0/.ex #
asis:_s _a (INTO) (+xCE, .e)/.e #
asis:_s .a (JE .rel) .e0/.ex :- asi-jump .a .rel +x74 +x0F +x84 .e0/.ex #
asis:_s .a (JMP .rel) .e0/.ex :- asi-jump .a .rel +xEB () +xE9 .e0/.ex #
asis:_s _a (JMP .rm) .e0/.ex :- as-mod-num-rm:32 +xFF .rm 4 .e0/.ex #
asis:_s _a (LABEL _) .e/.e #
asis:_s _a (LEA .reg .rm) .e0/.ex :- asi-rm-reg:_ +x8D .rm .reg .e0/.ex #
asis:.s _a (MOV .reg .imm) .e0/.ex :- asi-reg-imm:.s +xB0 .reg .imm .e0/.ex #
asis:.s _a (MOV .rm .imm) .e0/.ex :- asi-rm-imm:.s +xC6 .rm 0 .imm .e0/.ex #
asis:.s _a (MOV .rm0 .rm1) .e0/.ex :- asi-rm-reg2:.s +x88 .rm0 .rm1 .e0/.ex #
asis:_s _a (MOV .rm .sreg) (+x8C, .e1)/.ex :- as-segment-reg .sreg .sr, as-mod-num-rm:16 .rm .sr .e1/.ex #
asis:_s _a (MOV .sreg .rm) (+x8D, .e1)/.ex :- as-segment-reg .sreg .sr, as-mod-num-rm:16 .rm .sr .e1/.ex #
asis:_s _a (RET) (+xC3, .e)/.e #
asis:_s _a (RET .imm) (+xC2, .e1)/.ex :- as-emit:16 .imm .e1/.ex #
asis:_s _a (STI) (+xFB, .e)/.e #

asi-jump .a (byte .rel8) .b _ _ (.b, .e1)/.ex
	:- let .rel (.rel8 - .a - 2), as-emit:8 .rel .e1/.ex, !
#
asi-jump .a (dword .rel32) _ () .b (.b, .e1)/.ex
	:- let .rel (.rel32 - .a - 5), as-emit:32 .rel .e1/.ex, !
#
asi-jump .a (dword .rel32) _ .b0 .b1 (.b0, .b1, .e1)/.ex
	:- let .rel (.rel32 - .a - 6), as-emit:32 .rel .e1/.ex, !
#
asi-jump .a .rel8 .b _ _ (.b, .e1)/.ex
	:- asi-jump .a (byte .rel8) .b _ _ (.b, .e1)/.ex
#

asi-rm-imm:.size .b0 .rm .num .imm (.b1, .e1)/.ex
	:- as-mod-num-rm:.size .rm .num .e1/.e2, as-emit:.size .imm .e2/.ex
	, (.size = 8, .b0 = .b1; let .b1 (.b0 + 1))
#

asi-acc-imm:.size .b0 .acc .imm (.b1, .e1)/.ex
	:- as-reg:.size .acc 0
	, as-emit:.size .imm .e1/.ex
	, (.size = 8, .b0 = .b1; let .b1 (.b0 + 8))
#

asi-reg-imm:.size .b0 .reg .imm .e0/.ex
	:- (.size = 8, .b0 = .b1; let .b1 (.b0 + 8))
	, asi-reg:.size .b1 .reg .e0/.e1, as-emit:.size .imm .e1/.ex
#

asi-rm-reg2:.size .b .rm .reg .e0/.ex
	:- asi-rm-reg:.size .b .rm .reg .e0/.ex
#
asi-rm-reg2:.size .b0 .reg .rm .e0/.ex
	:- let .b1 (.b0 + 2)
	, asi-rm-reg:.size .b1 .rm .reg .e0/.ex
#

asi-rm-reg:.size .b0 .rm .reg (.b1, .e0)/.ex
	:- as-reg:.size .reg .r
	, as-mod-num-rm:.size .rm .r .e0/.ex
	, (.size = 8, .b0 = .b1; let .b1 (.b0 + 1))
#

asi-rm:.size .b0 .rm .num (.b1, .e1)/.ex
	:- as-mod-num-rm:.size .rm .num .e1/.ex
	, (.size = 8, .b0 = .b1; let .b1 (.b0 + 1))
#

asi-reg:.size .b0 .reg (.b1, .e)/.e
	:- as-reg:.size .reg .r
	, let .b1 (.b0 + .r)
#

as-mod-num-rm:.size .rm .num (.modregrm, .e1)/.ex
	:- is.int .num
	, as-mod-rm:.size .rm (.modb .rmb) .e1/.ex
	, let .modregrm (.modb * 64 + .num * 8 + .rmb)
#

as-mod-rm:.size .reg (3 .r) .e/.e
	:- as-reg:.size .reg .r
#
as-mod-rm:.size (`.disp`) (0 5) .e0/.ex
	:- as-emit:.size .disp .e0/.ex
#
as-mod-rm:_ (`.reg + .disp`) (.mod .r) .e0/.ex
	:- as-reg:32 .reg .r
	, as-disp-mod .disp .mod .e0/.ex
	, not (.r = 4; .mod = 0, .r = 5; .r = 12; .mod = 0, .r = 13)
#
as-mod-rm:_ (`.indexReg * .scale + .baseReg + .disp`) (.mod 4) (.sib, .e1)/.ex
	:- as-sib (`.indexReg * .scale + .baseReg`) .sib
	, as-disp-mod .disp .mod .e1/.ex
#
as-mod-rm:8 (byte `.ptr`) (.mod .rm) .e :- as-mod-rm:_ `.ptr` (.mod .rm) .e #
as-mod-rm:16 (word `.ptr`) (.mod .rm) .e :- as-mod-rm:_ `.ptr` (.mod .rm) .e #
as-mod-rm:32 (dword `.ptr`) (.mod .rm) .e :- as-mod-rm:_ `.ptr` (.mod .rm) .e #
as-mod-rm:64 (qword `.ptr`) (.mod .rm) .e :- as-mod-rm:_ `.ptr` (.mod .rm) .e #
as-mod-rm:_ (`.reg`) (.mod .rm) .e
	:- as-reg:32 .reg _
	, as-mod-rm:_ (`.reg + 0`) (.mod .rm) .e
#
as-mod-rm:_ (`.indexReg * .scale + .baseReg`) (.mod .rm) .e
	:- as-mod-rm:_ (`.indexReg * .scale + .baseReg + 0`) (.mod .rm) .e
#

as-disp-mod .disp .mod .e0/.ex
	:- once (.disp = 0, .mod = 0, .e0 = .ex
		; as-imm:8 .disp, .mod = 1, as-emit:8 .disp .e0/.ex
		; .mod = 2, as-emit:32 .disp .e0/.ex
	)
#

as-sib (`.indexReg * .scale + .baseReg`) .sib
	, as-reg:32 .indexReg .ir
	, as-sib-scale .scale .s
	, as-reg:32 .baseReg .br
	, let .sib (.s * 64 + .ir * 8 + .br)
#

as-sib-scale 1 0 #
as-sib-scale 2 1 #
as-sib-scale 4 2 #
as-sib-scale 8 3 #

as-emit:32 .d32 .e0/.ex
	:- as-imm:32 .d32
	, let .w0 (.d32 % 65536)
	, let .w1 (.d32 / 65536)
	, as-emit:16 .w0 .e0/.e1
	, as-emit:16 .w1 .e1/.ex
#

as-emit:16 .w16 .e0/.ex
	:- as-imm:16 .w16
	, let .b0 (.w16 % 256)
	, let .b1 (.w16 / 256)
	, as-emit:8 .b0 .e0/.e1
	, as-emit:8 .b1 .e1/.ex
#

as-emit:8 .b8 (.b8, .e)/.e :- as-imm:8 .b8 #

as-imm:8 .imm :- is.int .imm, -128 <= .imm, .imm < 128 #
as-imm:16 .imm :- is.int .imm, -32768 <= .imm, .imm < 32768 #
as-imm:32 .imm :- is.int .imm #

as-reg:8 AL 0 #
as-reg:8 CL 1 #
as-reg:8 DL 2 #
as-reg:8 BL 3 #
as-reg:8 AH 4 :- as-mode-amd64; as-rex-prefix #
as-reg:8 CH 5 :- as-mode-amd64; as-rex-prefix #
as-reg:8 DH 6 :- as-mode-amd64; as-rex-prefix #
as-reg:8 BH 7 :- as-mode-amd64; as-rex-prefix #
as-reg:8 R8B 8 :- as-mode-amd64 #
as-reg:8 R9B 9 :- as-mode-amd64 #
as-reg:8 R10B 10 :- as-mode-amd64 #
as-reg:8 R11B 11 :- as-mode-amd64 #
as-reg:8 R12B 12 :- as-mode-amd64 #
as-reg:8 R13B 13 :- as-mode-amd64 #
as-reg:8 R14B 14 :- as-mode-amd64 #
as-reg:8 R15B 15 :- as-mode-amd64 #
as-reg:16 AX 0 #
as-reg:16 CX 1 #
as-reg:16 DX 2 #
as-reg:16 BX 3 #
as-reg:16 SP 4 #
as-reg:16 BP 5 #
as-reg:16 SI 6 #
as-reg:16 DI 7 #
as-reg:16 R8W 8 :- as-mode-amd64 #
as-reg:16 R9W 9 :- as-mode-amd64 #
as-reg:16 R10W 10 :- as-mode-amd64 #
as-reg:16 R11W 11 :- as-mode-amd64 #
as-reg:16 R12W 12 :- as-mode-amd64 #
as-reg:16 R13W 13 :- as-mode-amd64 #
as-reg:16 R14W 14 :- as-mode-amd64 #
as-reg:16 R15W 15 :- as-mode-amd64 #
as-reg:32 EAX 0 #
as-reg:32 ECX 1 #
as-reg:32 EDX 2 #
as-reg:32 EBX 3 #
as-reg:32 ESP 4 #
as-reg:32 EBP 5 #
as-reg:32 ESI 6 #
as-reg:32 EDI 7 #
as-reg:32 R8D 8 :- as-mode-amd64 #
as-reg:32 R9D 9 :- as-mode-amd64 #
as-reg:32 R10D 10 :- as-mode-amd64 #
as-reg:32 R11D 11 :- as-mode-amd64 #
as-reg:32 R12D 12 :- as-mode-amd64 #
as-reg:32 R13D 13 :- as-mode-amd64 #
as-reg:32 R14D 14 :- as-mode-amd64 #
as-reg:32 R15D 15 :- as-mode-amd64 #
as-reg:64 RAX 0 #
as-reg:64 RCX 1 #
as-reg:64 RDX 2 #
as-reg:64 RBX 3 #
as-reg:64 RSP 4 #
as-reg:64 RBP 5 #
as-reg:64 RSI 6 #
as-reg:64 RDI 7 #
as-reg:64 R8 8 :- as-mode-amd64 #
as-reg:64 R9 9 :- as-mode-amd64 #
as-reg:64 R10 10 :- as-mode-amd64 #
as-reg:64 R11 11 :- as-mode-amd64 #
as-reg:64 R12 12 :- as-mode-amd64 #
as-reg:64 R13 13 :- as-mode-amd64 #
as-reg:64 R14 14 :- as-mode-amd64 #
as-reg:64 R15 15 :- as-mode-amd64 #

as-segment-reg ES 0 #
as-segment-reg CS 1 #
as-segment-reg SS 2 #
as-segment-reg DS 3 #
as-segment-reg FS 4 #
as-segment-reg GS 5 #

as-control-reg CR0 0 #
as-control-reg CR2 2 #
as-control-reg CR3 3 #
as-control-reg CR4 4 #
