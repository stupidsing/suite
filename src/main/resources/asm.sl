-- Assembler

-- Assumes 32-bit mode

asm-assemble AAA (+x37, .e)/.e #
asm-assemble (ADD AL .imm) (+x04, .imm, .e)/.e #
asm-assemble (ADD EAX .imm) (+x05, .e1)/.ex :- asm-emit32 .imm .e1/.ex #
asm-assemble (ADD .rm .imm8) (+x83, .e1)/.ex :- asm-imm8 .imm8, asm-assemble-rm32 .rm 0 .e1/.e2, asm-emit8 .imm8 .e2/.ex #
asm-assemble (ADD .rm .imm) (+x80, .e1)/.ex :- asm-assemble-rm8 .rm 0 .e1/.e2, asm-emit8 .imm .e2/.ex #
asm-assemble (ADD .rm .imm) (+x81, .e1)/.ex :- asm-assemble-rm32 .rm 0 .e1/.e2, asm-emit32 .imm .e2/.ex #
asm-assemble (ADD .rm .reg) (+x00, .e1)/.ex :- asm-assemble-rm8-r8 .rm .reg .e1/.ex #
asm-assemble (ADD .rm .reg) (+x01, .e1)/.ex :- asm-assemble-rm32-r32 .rm .reg .e1/.ex #
asm-assemble (ADD .reg .rm) (+x02, .e1)/.ex :- asm-assemble-rm8-r8 .rm .reg .e1/.ex #
asm-assemble (ADD .reg .rm) (+x03, .e1)/.ex :- asm-assemble-rm32-r32 .rm .reg .e1/.ex #
asm-assemble (DEC .reg) (.b, .e)/.e :- asm-assemble-r32 +x48 .reg .b #
asm-assemble (DEC .rm) (+xFE, .e1)/.ex :- asm-assemble-rm8 .rm 1 .e1/.ex #
asm-assemble (DEC .rm) (+xFF, .e1)/.ex :- asm-assemble-rm32 .rm 1 .e1/.ex #
asm-assemble (INC .reg) (.b, .e)/.e :- asm-assemble-r32 +x40 .reg .b #
asm-assemble (INC .rm) (+xFE, .e1)/.ex :- asm-assemble-rm8 .rm 0 .e1/.ex #
asm-assemble (INC .rm) (+xFF, .e1)/.ex :- asm-assemble-rm32 .rm 0 .e1/.ex #
asm-assemble (MOV .reg .imm) (.b, .e1)/.ex :- asm-assemble-r8 +xB0 .reg .b, asm-emit8 .imm .e1/.ex #
asm-assemble (MOV .reg .imm) (.b, .e1)/.ex :- asm-assemble-r32 +xB8 .reg .b, asm-emit32 .imm .e1/.ex #
asm-assemble (MOV .rm .imm) (+xC6, .e1)/.ex :- asm-assemble-rm8 .rm 0 .e1/.e2, asm-emit8 .imm .e2/.ex #
asm-assemble (MOV .rm .imm) (+xC7, .e1)/.ex :- asm-assemble-rm32 .rm 0 .e1/.e2, asm-emit32 .imm .e2/.ex #
asm-assemble (MOV .rm .reg) (+x88, .e1)/.ex :- asm-assemble-rm8-r8 .rm .reg .e1/.ex #
asm-assemble (MOV .rm .reg) (+x89, .e1)/.ex :- asm-assemble-rm32-r32 .rm .reg .e1/.ex #
asm-assemble (MOV .reg .rm) (+x8A, .e1)/.ex :- asm-assemble-rm8-r8 .rm .reg .e1/.ex #
asm-assemble (MOV .reg .rm) (+x8B, .e1)/.ex :- asm-assemble-rm32-r32 .rm .reg .e1/.ex #
asm-assemble (MOV .rm .sreg) (+x8C, .e1)/.ex :- asm-segment-register .sreg .sr, asm-assemble-rm16 .rm .sreg .e1/.ex #
asm-assemble (MOV .sreg .rm) (+x8D, .e1)/.ex :- asm-segment-register .sreg .sr, asm-assemble-rm16 .rm .sreg .e1/.ex #

asm-assemble-r8 .b .reg .b1 :- asm-reg8 .reg .r, let .b1 (.b + .r) #
asm-assemble-r32 .b .reg .b1 :- asm-reg32 .reg .r, let .b1 (.b + .r) #

asm-assemble-rm8-r8 .rm .reg .e0/.ex :- asm-reg8 .reg .r, asm-assemble-rm8 .rm .r .e0/.ex #
asm-assemble-rm32-r32 .rm .reg .e0/.ex :- asm-reg32 .reg .r, asm-assemble-rm32 .rm .r .e0/.ex #

asm-assemble-rm8 .rm .num .e0/.ex :- asm-rm8 .rm, asm-mod-num-rm .rm .num .e0/.ex #
asm-assemble-rm16 .rm .num .e0/.ex :- asm-rm16 .rm, asm-mod-num-rm .rm .num .e0/.ex #
asm-assemble-rm32 .rm .num .e0/.ex :- asm-rm32 .rm, asm-mod-num-rm .rm .num .e0/.ex #

asm-mod-num-rm .rm .num (.modregrm, .e1)/.ex
	:- asm-mod-rm .rm (.modb .rmb) .e1/.ex
	, let .modregrm (.modb * 64 + .num * .8 + .rmb)
#

asm-mod-rm .reg (3 .r) .e/.e
	:- asm-reg32 .reg .r
#
asm-mod-rm (`.reg + .disp`) (.mod .r) .e0/.ex
	:- asm-reg32 .reg .r
	, not (.r = 4; .r = 12)
	, asm-disp-mod .disp .mod .e0/.ex
#
asm-mod-rm (`.indexReg * .scale + .easeReg + .disp`) (.mod 4) (.sib, .e1)/.ex
	:- asm-sib (`.indexReg * .scale + .easeReg`) .sib
	, asm-disp-mod .disp .mod .e1/.ex
#
asm-mod-rm (byte `.ptr`) (.mod .rm) .e :- asm-mod-rm `.ptr` (.mod .rm) .e #
asm-mod-rm (word `.ptr`) (.mod .rm) .e :- asm-mod-rm `.ptr` (.mod .rm) .e #
asm-mod-rm (dword `.ptr`) (.mod .rm) .e :- asm-mod-rm `.ptr` (.mod .rm) .e #
asm-mod-rm (qword `.ptr`) (.mod .rm) .e :- asm-mod-rm `.ptr` (.mod .rm) .e #
asm-mod-rm (`.reg`) (.mod .rm) .e :- asm-mod-rm (`.reg + 0`) (.mod .rm) .e #
asm-mod-rm (`.indexReg * .scale + .easeReg`) (.mod .rm) .e
	:- asm-mod-rm (`.indexReg * .scale + .easeReg + 0`) (.mod .rm) .e
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
	, let .sib (.s * 64 + .ir * 8 + br)
#

asm-sib-scale 1 0 #
asm-sib-scale 2 1 #
asm-sib-scale 4 2 #
asm-sib-scale 8 3 #

asm-accum8 AL #
asm-accum16 AX #
asm-accum32 EAX #

asm-rm8 .rm :- asm-operand-size .rm BYTE #
asm-rm16 .rm :- asm-operand-size .rm WORD #
asm-rm32 .rm :- asm-operand-size .rm DWORD #

asm-reg8 .reg .r :- asm-general-register .reg BYTE .r #
asm-reg32 .reg .r :- asm-general-register .reg DWORD .r #

asm-imm8 .imm :- -128 <= .imm, .imm < 128 #

asm-operand-size .reg .size :- asm-general-register .reg .size _ #
asm-operand-size (byte `_`) BYTE #
asm-operand-size (word `_`) WORD #
asm-operand-size (dword `_`) DWORD #
asm-operand-size (qword `_`) QWORD #

asm-emit32 .d32 .e0/.ex
	:- let .w0 (.d16 mod 65536)
	, let .w1 (.d16 / 65536)
	, asm-emit16 .w0 .e0/.e1
	, asm-emit16 .w1 .e1/.ex
#

asm-emit16 .w16 .e0/.ex
	:- let .b0 (.w16 mod 256)
	, let .b1 (.w16 / 256)
	, asm-emit8 .b0 .e0/.e1
	, asm-emit8 .b1 .e1/.ex
#

asm-emit8 .b8 (.b8, .e)/.e #

asm-general-register AL BYTE 0 #
asm-general-register CL BYTE 1 #
asm-general-register DL BYTE 2 #
asm-general-register BL BYTE 3 #
asm-general-register AH BYTE 4 :- asm-mode-amd64; asm-rex-prefix #
asm-general-register CH BYTE 5 :- asm-mode-amd64; asm-rex-prefix #
asm-general-register DH BYTE 6 :- asm-mode-amd64; asm-rex-prefix #
asm-general-register BH BYTE 7 :- asm-mode-amd64; asm-rex-prefix #
asm-general-register R8B BYTE 8 :- asm-mode-amd64 #
asm-general-register R9B BYTE 9 :- asm-mode-amd64 #
asm-general-register R10B BYTE 10 :- asm-mode-amd64 #
asm-general-register R11B BYTE 11 :- asm-mode-amd64 #
asm-general-register R12B BYTE 12 :- asm-mode-amd64 #
asm-general-register R13B BYTE 13 :- asm-mode-amd64 #
asm-general-register R14B BYTE 14 :- asm-mode-amd64 #
asm-general-register R15B BYTE 15 :- asm-mode-amd64 #
asm-general-register AX WORD 0 #
asm-general-register CX WORD 1 #
asm-general-register DX WORD 2 #
asm-general-register BX WORD 3 #
asm-general-register SP WORD 4 #
asm-general-register BP WORD 5 #
asm-general-register SI WORD 6 #
asm-general-register DI WORD 7 #
asm-general-register R8W WORD 8 :- asm-mode-amd64 #
asm-general-register R9W WORD 9 :- asm-mode-amd64 #
asm-general-register R10W WORD 10 :- asm-mode-amd64 #
asm-general-register R11W WORD 11 :- asm-mode-amd64 #
asm-general-register R12W WORD 12 :- asm-mode-amd64 #
asm-general-register R13W WORD 13 :- asm-mode-amd64 #
asm-general-register R14W WORD 14 :- asm-mode-amd64 #
asm-general-register R15W WORD 15 :- asm-mode-amd64 #
asm-general-register EAX DWORD 0 #
asm-general-register ECX DWORD 1 #
asm-general-register EDX DWORD 2 #
asm-general-register EBX DWORD 3 #
asm-general-register ESP DWORD 4 #
asm-general-register EBP DWORD 5 #
asm-general-register ESI DWORD 6 #
asm-general-register EDI DWORD 7 #
asm-general-register R8D DWORD 8 :- asm-mode-amd64 #
asm-general-register R9D DWORD 9 :- asm-mode-amd64 #
asm-general-register R10D DWORD 10 :- asm-mode-amd64 #
asm-general-register R11D DWORD 11 :- asm-mode-amd64 #
asm-general-register R12D DWORD 12 :- asm-mode-amd64 #
asm-general-register R13D DWORD 13 :- asm-mode-amd64 #
asm-general-register R14D DWORD 14 :- asm-mode-amd64 #
asm-general-register R15D DWORD 15 :- asm-mode-amd64 #

asm-segment-register ES 0 #
asm-segment-register CS 1 #
asm-segment-register SS 2 #
asm-segment-register DS 3 #
asm-segment-register FS 4 #
asm-segment-register GS 5 #

asm-control-register CR0 0 #
asm-control-register CR2 2 #
asm-control-register CR3 3 #
asm-control-register CR4 4 #
