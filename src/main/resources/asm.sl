-- Assembler

-- Assumes 32-bit mode

as-assemble AAA (+x37, .e)/.e #
as-assemble (ADD .acc .imm) .e0/.ex :- as-assemble-acc-imm +x04 .acc .imm .e0/.ex #
as-assemble (ADD .rm .imm8) (+x83, .e1)/.ex :- as-imm8 .imm8, as-assemble-rm32 .rm 0 .e1/.e2, as-emit8 .imm8 .e2/.ex #
as-assemble (ADD .rm .imm) .e0/.ex :- as-assemble-rm-imm +x80 .rm 0 .imm .e0/.ex #
as-assemble (ADD .rm0 .rm1) .e0/.ex :- as-assemble-rm-reg2 +x00 .rm0 .rm1 .e0/.ex #
as-assemble (DEC .reg) .e0/.ex :- as-assemble-r32 +x48 .reg .e0/.ex #
as-assemble (DEC .rm) .e0/.ex :- as-assemble-rm +xFE .rm 1 .e0/.ex #
as-assemble (INC .reg) .e0/.ex :- as-assemble-r32 +x40 .reg .e0/.ex #
as-assemble (INC .rm) .e0/.ex :- as-assemble-rm +xFE .rm 0 .e0/.ex #
as-assemble (JMP .rel8) (+xEB, .e1)/.ex :- as-emit8 .rel8 .e1/.ex #
as-assemble (JMP .rel32) (+xE9, .e1)/.ex :- as-emit32 .rel32 .e1/.ex #
as-assemble (JMP .rm) .e0/.ex :- as-assemble-rm32 +xFF .rm 4 .e0/.ex #
as-assemble (MOV .reg .imm) .e0/.ex :- as-assemble-reg-imm +xB0 .reg .imm .e0/.ex #
as-assemble (MOV .rm .imm) .e0/.ex :- as-assemble-rm-imm +xC6 .rm 0 .imm .e0/.ex #
as-assemble (MOV .rm0 .rm1) .e0/.ex :- as-assemble-rm-reg +x88 .rm0 .rm1 .e0/.ex #
as-assemble (MOV .rm .sreg) (+x8C, .e1)/.ex :- as-segment-reg .sreg .sr, as-assemble-rm16 .rm .sr .e1/.ex #
as-assemble (MOV .sreg .rm) (+x8D, .e1)/.ex :- as-segment-reg .sreg .sr, as-assemble-rm16 .rm .sr .e1/.ex #
as-assemble RET (+xC3, .e)/.e #
as-assemble (RET .imm) (+xC2, .e1)/.ex :- as-emit16 .imm .e1/.ex #

as-assemble-rm-imm .b .rm .num .imm (.b, .e1)/.ex
	:- as-assemble-rm8 .rm .num .e1/.e2, as-emit8 .imm .e2/.ex
#
as-assemble-rm-imm .b0 .rm .num .imm (.b1, .e1)/.ex
	:- as-assemble-rm32 .rm .num .e1/.e2, as-emit32 .imm .e2/.ex
	, let .b1 (.b0 + 1)
#

as-assemble-acc-imm .b AL .imm (.b, .e1)/.ex
	:- as-emit8 .imm .e1/.ex
#
as-assemble-acc-imm .b0 EAX .imm (.b1, .e1)/.ex
	:- as-emit32 .imm .e1/.ex
	, let .b1 (.b0 + 8)
#

as-assemble-reg-imm .b0 .reg .imm .e0/.ex
	:- as-assemble-r8 .b0 .reg .e0/.e1, as-emit8 .imm .e1/.ex
#
as-assemble-reg-imm .b0 .reg .imm .e0/.ex
	:- let .b1 (.b0 + 8)
	, as-assemble-r32 .b1 .reg .e0/.e1, as-emit32 .imm .e1/.ex
#

as-assemble-rm-reg2 .b .rm .reg .e0/.ex
	:- as-assemble-rm-reg .b .rm .reg .e0/.ex
#
as-assemble-rm-reg2 .b0 .reg .rm .e0/.ex
	:- let .b1 (.b0 + 2)
	, as-assemble-rm-reg .b1 .rm .reg .e0/.ex
#

as-assemble-rm-reg .b .rm .reg (.b, .e0)/.ex
	:- as-reg8 .reg .r, as-assemble-rm8 .rm .r .e0/.ex
#
as-assemble-rm-reg .b0 .rm .reg (.b1, .e0)/.ex
	:- as-reg32 .reg .r, as-assemble-rm32 .rm .r .e0/.ex
	, let .b1 (.b0 + 1)
#

as-assemble-rm .b .rm .num (.b, .e1)/.ex
	:- as-rm8 .rm, as-mod-num-rm .rm .num .e1/.ex
#
as-assemble-rm .b0 .rm .num (.b1, .e1)/.ex
	:- as-rm32 .rm, as-mod-num-rm .rm .num .e1/.ex
	, let .b1 (.b0 + 1)
#

as-assemble-r8 .b0 .reg (.b1, .e)/.e :- as-reg8 .reg .r, let .b1 (.b0 + .r) #
as-assemble-r32 .b0 .reg (.b1, .e)/.e :- as-reg32 .reg .r, let .b1 (.b0 + .r) #

as-assemble-rm8 .rm .num .e0/.ex :- as-rm8 .rm, as-mod-num-rm .rm .num .e0/.ex #
as-assemble-rm16 .rm .num .e0/.ex :- as-rm16 .rm, as-mod-num-rm .rm .num .e0/.ex #
as-assemble-rm32 .rm .num .e0/.ex :- as-rm32 .rm, as-mod-num-rm .rm .num .e0/.ex #

as-mod-num-rm .rm .num (.modregrm, .e1)/.ex
	:- is.int .num
	, as-mod-rm .rm (.modb .rmb) .e1/.ex
	, let .modregrm (.modb * 64 + .num * 8 + .rmb)
#

as-mod-rm .reg (3 .r) .e/.e
	:- as-reg32 .reg .r
#
as-mod-rm (`.disp`) (0 5) .e0/.ex
	:- as-emit32 .disp .e0/.ex
#
as-mod-rm (`.reg + .disp`) (.mod .r) .e0/.ex
	:- as-reg32 .reg .r
	, as-disp-mod .disp .mod .e0/.ex
	, not (.r = 4; .mod = 0, .r = 5; .r = 12; .mod = 0, .r = 13)
#
as-mod-rm (`.indexReg * .scale + .baseReg + .disp`) (.mod 4) (.sib, .e1)/.ex
	:- as-sib (`.indexReg * .scale + .baseReg`) .sib
	, as-disp-mod .disp .mod .e1/.ex
#
as-mod-rm (byte `.ptr`) (.mod .rm) .e :- as-mod-rm `.ptr` (.mod .rm) .e #
as-mod-rm (word `.ptr`) (.mod .rm) .e :- as-mod-rm `.ptr` (.mod .rm) .e #
as-mod-rm (dword `.ptr`) (.mod .rm) .e :- as-mod-rm `.ptr` (.mod .rm) .e #
as-mod-rm (qword `.ptr`) (.mod .rm) .e :- as-mod-rm `.ptr` (.mod .rm) .e #
as-mod-rm (`.reg`) (.mod .rm) .e
	:- as-reg32 .reg _
	, as-mod-rm (`.reg + 0`) (.mod .rm) .e
#
as-mod-rm (`.indexReg * .scale + .baseReg`) (.mod .rm) .e
	:- as-mod-rm (`.indexReg * .scale + .baseReg + 0`) (.mod .rm) .e
#

as-disp-mod .disp .mod .e0/.ex
	:- once (.disp = 0, .mod = 0, .e0 = .ex
		; as-imm8 .disp, .mod = 1, as-emit8 .disp .e0/.ex
		; .mod = 2, as-emit32 .disp .e0/.ex
	)
#

as-sib (`.indexReg * .scale + .baseReg`) .sib
	, as-reg32 .indexReg .ir
	, as-sib-scale .scale .s
	, as-reg32 .baseReg .br
	, let .sib (.s * 64 + .ir * 8 + .br)
#

as-sib-scale 1 0 #
as-sib-scale 2 1 #
as-sib-scale 4 2 #
as-sib-scale 8 3 #

as-rm8 .rm :- as-rm-size .rm BYTE #
as-rm16 .rm :- as-rm-size .rm WORD #
as-rm32 .rm :- as-rm-size .rm DWORD #

as-reg8 .reg .r :- as-general-reg .reg BYTE .r #
as-reg32 .reg .r :- as-general-reg .reg DWORD .r #

as-rm-size .reg .size :- as-general-reg .reg .size _ #
as-rm-size (byte `_`) BYTE #
as-rm-size (word `_`) WORD #
as-rm-size (dword `_`) DWORD #
as-rm-size (qword `_`) QWORD #
as-rm-size `_` _ #

as-emit32 .d32 .e0/.ex
	:- as-imm32 .d32
	, let .w0 (.d32 % 65536)
	, let .w1 (.d32 / 65536)
	, as-emit16 .w0 .e0/.e1
	, as-emit16 .w1 .e1/.ex
#

as-emit16 .w16 .e0/.ex
	:- as-imm16 .w16
	, let .b0 (.w16 % 256)
	, let .b1 (.w16 / 256)
	, as-emit8 .b0 .e0/.e1
	, as-emit8 .b1 .e1/.ex
#

as-emit8 .b8 (.b8, .e)/.e :- as-imm8 .b8 #

as-imm8 .imm :- is.int .imm, -128 <= .imm, .imm < 128 #
as-imm16 .imm :- is.int .imm, -32768 <= .imm, .imm < 32768 #
as-imm32 .imm :- is.int .imm #

as-general-reg AL BYTE 0 #
as-general-reg CL BYTE 1 #
as-general-reg DL BYTE 2 #
as-general-reg BL BYTE 3 #
as-general-reg AH BYTE 4 :- as-mode-amd64; as-rex-prefix #
as-general-reg CH BYTE 5 :- as-mode-amd64; as-rex-prefix #
as-general-reg DH BYTE 6 :- as-mode-amd64; as-rex-prefix #
as-general-reg BH BYTE 7 :- as-mode-amd64; as-rex-prefix #
as-general-reg R8B BYTE 8 :- as-mode-amd64 #
as-general-reg R9B BYTE 9 :- as-mode-amd64 #
as-general-reg R10B BYTE 10 :- as-mode-amd64 #
as-general-reg R11B BYTE 11 :- as-mode-amd64 #
as-general-reg R12B BYTE 12 :- as-mode-amd64 #
as-general-reg R13B BYTE 13 :- as-mode-amd64 #
as-general-reg R14B BYTE 14 :- as-mode-amd64 #
as-general-reg R15B BYTE 15 :- as-mode-amd64 #
as-general-reg AX WORD 0 #
as-general-reg CX WORD 1 #
as-general-reg DX WORD 2 #
as-general-reg BX WORD 3 #
as-general-reg SP WORD 4 #
as-general-reg BP WORD 5 #
as-general-reg SI WORD 6 #
as-general-reg DI WORD 7 #
as-general-reg R8W WORD 8 :- as-mode-amd64 #
as-general-reg R9W WORD 9 :- as-mode-amd64 #
as-general-reg R10W WORD 10 :- as-mode-amd64 #
as-general-reg R11W WORD 11 :- as-mode-amd64 #
as-general-reg R12W WORD 12 :- as-mode-amd64 #
as-general-reg R13W WORD 13 :- as-mode-amd64 #
as-general-reg R14W WORD 14 :- as-mode-amd64 #
as-general-reg R15W WORD 15 :- as-mode-amd64 #
as-general-reg EAX DWORD 0 #
as-general-reg ECX DWORD 1 #
as-general-reg EDX DWORD 2 #
as-general-reg EBX DWORD 3 #
as-general-reg ESP DWORD 4 #
as-general-reg EBP DWORD 5 #
as-general-reg ESI DWORD 6 #
as-general-reg EDI DWORD 7 #
as-general-reg R8D DWORD 8 :- as-mode-amd64 #
as-general-reg R9D DWORD 9 :- as-mode-amd64 #
as-general-reg R10D DWORD 10 :- as-mode-amd64 #
as-general-reg R11D DWORD 11 :- as-mode-amd64 #
as-general-reg R12D DWORD 12 :- as-mode-amd64 #
as-general-reg R13D DWORD 13 :- as-mode-amd64 #
as-general-reg R14D DWORD 14 :- as-mode-amd64 #
as-general-reg R15D DWORD 15 :- as-mode-amd64 #

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
