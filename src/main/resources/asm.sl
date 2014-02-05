-- Assembler

-- Assumes 32-bit mode

asm-assemble AAA (+x37, .e)/.e #
asm-assemble (ADD AL .imm) (+x04, .imm, .e)/.e #
asm-assemble (ADD EAX .imm) (+x05, .e1)/.ex :- asm-emit32 .imm .e1/.ex #
asm-assemble (ADD .rm .imm8) (+x83, .e1)/.ex :- asm-imm8 .imm8, asm-assemble-rm32 .rm 0 .e1/.e2, asm-emit8 .imm8 .e2/.ex #
asm-assemble (ADD .rm .imm) .e0/.ex :- asm-assemble-rm-imm +x80 .rm 0 .imm .e1/.e2 #
asm-assemble (ADD .rm0 .rm1) .e0/.ex :- asm-assemble-rm-reg2 +x00 .rm0 .rm1 .e0/.ex #
asm-assemble (DEC .reg) .e0/.ex :- asm-assemble-r32 +x48 .reg .e0/.ex #
asm-assemble (DEC .rm) .e0/.ex :- asm-assemble-rm +xFE .rm 1 .e0/.ex #
asm-assemble (INC .reg) .e0/.ex :- asm-assemble-r32 +x40 .reg .e0/.ex #
asm-assemble (INC .rm) .e0/.ex :- asm-assemble-rm +xFE .rm 0 .e0/.ex #
asm-assemble (MOV .reg .imm) .e0/.ex :- asm-assemble-reg-imm +xB0 .reg .imm .e0/.ex #
asm-assemble (MOV .rm .imm) .e0/.ex :- asm-assemble-rm-imm +xC6 .rm 0 .imm .e0/.ex #
asm-assemble (MOV .rm0 .rm1) .e0/.ex :- asm-assemble-rm-reg +x88 .rm0 .rm1 .e0/.ex #
asm-assemble (MOV .rm .sreg) (+x8C, .e1)/.ex :- asm-segment-reg .sreg .sr, asm-assemble-rm16 .rm .sr .e1/.ex #
asm-assemble (MOV .sreg .rm) (+x8D, .e1)/.ex :- asm-segment-reg .sreg .sr, asm-assemble-rm16 .rm .sr .e1/.ex #

asm-assemble-rm-imm .b .rm .num .imm (.b, .e1)/.ex
	:- asm-assemble-rm8 .rm .num .e1/.e2, asm-emit8 .imm .e2/.ex
#
asm-assemble-rm-imm .b0 .rm .num .imm (.b1, .e1)/.ex
	:- asm-assemble-rm32 .rm .num .e1/.e2, asm-emit32 .imm .e2/.ex
	, let .b1 (.b0 + 1)
#

asm-assemble-reg-imm .b0 .reg .imm .e0/.ex
	:- asm-assemble-r8 .b0 .reg .e0/.e1, asm-emit8 .imm .e1/.ex
#
asm-assemble-reg-imm .b0 .reg .imm .e0/.ex
	:- let .b1 (.b0 + 8)
	, asm-assemble-r32 .b1 .reg .e0/.e1, asm-emit32 .imm .e1/.ex
#

asm-assemble-rm-reg2 .b .rm .reg .e0/.ex
	:- asm-assemble-rm-reg .b .rm .reg .e0/.ex
#
asm-assemble-rm-reg2 .b0 .reg .rm .e0/.ex
	:- let .b1 (.b0 + 2)
	, asm-assemble-rm-reg .b1 .rm .reg .e0/.ex
#

asm-assemble-rm-reg .b .rm .reg (.b, .e0)/.ex
	:- asm-reg8 .reg .r, asm-assemble-rm8 .rm .r .e0/.ex
#
asm-assemble-rm-reg .b0 .rm .reg (.b1, .e0)/.ex
	:- asm-reg32 .reg .r, asm-assemble-rm32 .rm .r .e0/.ex
	, let .b1 (.b0 + 1)
#

asm-assemble-rm .b .rm .num (.b, .e1)/.ex
	:- asm-rm8 .rm, asm-mod-num-rm .rm .num .e1/.ex
#
asm-assemble-rm .b0 .rm .num (.b1, .e1)/.ex
	:- asm-rm32 .rm, asm-mod-num-rm .rm .num .e1/.ex
	, let .b1 (.b0 + 1)
#

asm-assemble-r8 .b0 .reg (.b1, .e)/.e :- asm-reg8 .reg .r, let .b1 (.b0 + .r) #
asm-assemble-r32 .b0 .reg (.b1, .e)/.e :- asm-reg32 .reg .r, let .b1 (.b0 + .r) #

asm-assemble-rm8 .rm .num .e0/.ex :- asm-rm8 .rm, asm-mod-num-rm .rm .num .e0/.ex #
asm-assemble-rm16 .rm .num .e0/.ex :- asm-rm16 .rm, asm-mod-num-rm .rm .num .e0/.ex #
asm-assemble-rm32 .rm .num .e0/.ex :- asm-rm32 .rm, asm-mod-num-rm .rm .num .e0/.ex #

asm-mod-num-rm .rm .num (.modregrm, .e1)/.ex
	:- is.int .num
	, asm-mod-rm .rm (.modb .rmb) .e1/.ex
	, let .modregrm (.modb * 64 + .num * 8 + .rmb)
#

asm-mod-rm .reg (3 .r) .e/.e
	:- asm-reg32 .reg .r
#
asm-mod-rm (`.disp`) (0 5) .e0/.ex
	:- asm-emit32 .disp .e0/.ex
#
asm-mod-rm (`.reg + .disp`) (.mod .r) .e0/.ex
	:- asm-reg32 .reg .r
	, asm-disp-mod .disp .mod .e0/.ex
	, not (.r = 4; .mod = 0, .r = 5; .r = 12; .mod = 0, .r = 13)
#
asm-mod-rm (`.indexReg * .scale + .baseReg + .disp`) (.mod 4) (.sib, .e1)/.ex
	:- asm-sib (`.indexReg * .scale + .baseReg`) .sib
	, asm-disp-mod .disp .mod .e1/.ex
#
asm-mod-rm (byte `.ptr`) (.mod .rm) .e :- asm-mod-rm `.ptr` (.mod .rm) .e #
asm-mod-rm (word `.ptr`) (.mod .rm) .e :- asm-mod-rm `.ptr` (.mod .rm) .e #
asm-mod-rm (dword `.ptr`) (.mod .rm) .e :- asm-mod-rm `.ptr` (.mod .rm) .e #
asm-mod-rm (qword `.ptr`) (.mod .rm) .e :- asm-mod-rm `.ptr` (.mod .rm) .e #
asm-mod-rm (`.reg`) (.mod .rm) .e
	:- asm-reg32 .reg _
	, asm-mod-rm (`.reg + 0`) (.mod .rm) .e
#
asm-mod-rm (`.indexReg * .scale + .baseReg`) (.mod .rm) .e
	:- asm-mod-rm (`.indexReg * .scale + .baseReg + 0`) (.mod .rm) .e
#

asm-disp-mod .disp .mod .e0/.ex
	:- once (.disp = 0, .mod = 0, .e0 = .ex
		; asm-imm8 .disp, .mod = 1, asm-emit8 .disp .e0/.ex
		; .mod = 2, asm-emit32 .disp .e0/.ex
	)
#

asm-sib (`.indexReg * .scale + .baseReg`) .sib
	, asm-reg32 .indexReg .ir
	, asm-sib-scale .scale .s
	, asm-reg32 .baseReg .br
	, let .sib (.s * 64 + .ir * 8 + .br)
#

asm-sib-scale 1 0 #
asm-sib-scale 2 1 #
asm-sib-scale 4 2 #
asm-sib-scale 8 3 #

asm-rm8 .rm :- asm-rm-size .rm BYTE #
asm-rm16 .rm :- asm-rm-size .rm WORD #
asm-rm32 .rm :- asm-rm-size .rm DWORD #

asm-reg8 .reg .r :- asm-general-reg .reg BYTE .r #
asm-reg32 .reg .r :- asm-general-reg .reg DWORD .r #

asm-imm8 .imm :- is.int .imm, -128 <= .imm, .imm < 128 #

asm-rm-size .reg .size :- asm-general-reg .reg .size _ #
asm-rm-size (byte `_`) BYTE #
asm-rm-size (word `_`) WORD #
asm-rm-size (dword `_`) DWORD #
asm-rm-size (qword `_`) QWORD #
asm-rm-size `_` _ #

asm-emit32 .d32 .e0/.ex
	:- is.int .d32
	, let .w0 (.d32 % 65536)
	, let .w1 (.d32 / 65536)
	, asm-emit16 .w0 .e0/.e1
	, asm-emit16 .w1 .e1/.ex
#

asm-emit16 .w16 .e0/.ex
	:- is.int .w16
	, let .b0 (.w16 % 256)
	, let .b1 (.w16 / 256)
	, asm-emit8 .b0 .e0/.e1
	, asm-emit8 .b1 .e1/.ex
#

asm-emit8 .b8 (.b8, .e)/.e :- is.int .b8 #

asm-general-reg AL BYTE 0 #
asm-general-reg CL BYTE 1 #
asm-general-reg DL BYTE 2 #
asm-general-reg BL BYTE 3 #
asm-general-reg AH BYTE 4 :- asm-mode-amd64; asm-rex-prefix #
asm-general-reg CH BYTE 5 :- asm-mode-amd64; asm-rex-prefix #
asm-general-reg DH BYTE 6 :- asm-mode-amd64; asm-rex-prefix #
asm-general-reg BH BYTE 7 :- asm-mode-amd64; asm-rex-prefix #
asm-general-reg R8B BYTE 8 :- asm-mode-amd64 #
asm-general-reg R9B BYTE 9 :- asm-mode-amd64 #
asm-general-reg R10B BYTE 10 :- asm-mode-amd64 #
asm-general-reg R11B BYTE 11 :- asm-mode-amd64 #
asm-general-reg R12B BYTE 12 :- asm-mode-amd64 #
asm-general-reg R13B BYTE 13 :- asm-mode-amd64 #
asm-general-reg R14B BYTE 14 :- asm-mode-amd64 #
asm-general-reg R15B BYTE 15 :- asm-mode-amd64 #
asm-general-reg AX WORD 0 #
asm-general-reg CX WORD 1 #
asm-general-reg DX WORD 2 #
asm-general-reg BX WORD 3 #
asm-general-reg SP WORD 4 #
asm-general-reg BP WORD 5 #
asm-general-reg SI WORD 6 #
asm-general-reg DI WORD 7 #
asm-general-reg R8W WORD 8 :- asm-mode-amd64 #
asm-general-reg R9W WORD 9 :- asm-mode-amd64 #
asm-general-reg R10W WORD 10 :- asm-mode-amd64 #
asm-general-reg R11W WORD 11 :- asm-mode-amd64 #
asm-general-reg R12W WORD 12 :- asm-mode-amd64 #
asm-general-reg R13W WORD 13 :- asm-mode-amd64 #
asm-general-reg R14W WORD 14 :- asm-mode-amd64 #
asm-general-reg R15W WORD 15 :- asm-mode-amd64 #
asm-general-reg EAX DWORD 0 #
asm-general-reg ECX DWORD 1 #
asm-general-reg EDX DWORD 2 #
asm-general-reg EBX DWORD 3 #
asm-general-reg ESP DWORD 4 #
asm-general-reg EBP DWORD 5 #
asm-general-reg ESI DWORD 6 #
asm-general-reg EDI DWORD 7 #
asm-general-reg R8D DWORD 8 :- asm-mode-amd64 #
asm-general-reg R9D DWORD 9 :- asm-mode-amd64 #
asm-general-reg R10D DWORD 10 :- asm-mode-amd64 #
asm-general-reg R11D DWORD 11 :- asm-mode-amd64 #
asm-general-reg R12D DWORD 12 :- asm-mode-amd64 #
asm-general-reg R13D DWORD 13 :- asm-mode-amd64 #
asm-general-reg R14D DWORD 14 :- asm-mode-amd64 #
asm-general-reg R15D DWORD 15 :- asm-mode-amd64 #

asm-segment-reg ES 0 #
asm-segment-reg CS 1 #
asm-segment-reg SS 2 #
asm-segment-reg DS 3 #
asm-segment-reg FS 4 #
asm-segment-reg GS 5 #

asm-control-reg CR0 0 #
asm-control-reg CR2 2 #
asm-control-reg CR3 3 #
asm-control-reg CR4 4 #
