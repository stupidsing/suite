-- Memory layout
-- 00007C00 -> 00007E00 boot sector
-- 00020000 -> 00020800 interrupt descriptor table (IDT)
-- 00020800 -> 00020868 task state segment (TSS)
-- 00020868 -> 00022000 kernel interrupt stack
-- 00022000 -> 00024000 kernel system call stack
-- 00024000 -> 00030000 kernel initialization stack
-- 00040000 -> 00040004 kernel this frame pointer
-- 00040000 -> 00050000 kernel code
-- 00050000 -> 000A0000 unused
-- 000A0000 -> 00100000 BIOS / video memory
-- 00100000 -> stackx   physical page stack
-- stackx   -> pagex    allocatable physical pages

expand idt-address = +x20000;
expand tss-address = +x20800;
expand interrupt-stack-address = +x22000;
expand syscall-stack-address = +x24000;
expand memory-region-start = 1048576;

asm _ MOV (ESP, +x30000);
asm _ MOV (EBP, ESP);
asm _ MOV (`+x40000`, EBP);

declare kernel = this;
declare ticks = 0;

declare copy = [dst, src, len,] (
	asm _ MOV (EDI, `EBP + 8`);
	asm _ MOV (ESI, `EBP + 12`);
	asm _ MOV (ECX, `EBP + 16`);
	asm _ CLD ();
	asm _ REP MOVSB ();
);

declare in = [port,] (
	asm _ MOV (EDX, `EBP + 8`);
	asm _ XOR (EAX, EAX);
	asm _ IN (AL, DX);
);

declare out = [port, value,] (
	asm _ MOV (EDX, `EBP + 8`);
	asm _ MOV (EAX, `EBP + 12`);
	asm _ OUT (DX, AL);
);

declare wrmsr = [msr, value,] (
	asm _ MOV (ECX, `EBP + 8`);
	asm _ MOV (EAX, `EBP + 12`);
	asm _ XOR (EDX, EDX);
	asm _ WRMSR ();
);

#include(console.il);
#include(idt.il);
#include(phy-mem.il);
#include(paging.il);
#include(service.il);

declare sleep = [ms,] (
	declare ticks1 = svc:svc-service [svc-ticks, 0, 0,] + ms / 10;
	while (svc:svc-service [svc-ticks, 0, 0,] < ticks1) do (
		svc:svc-service [svc-halt_, 0, 0,];
	);
);

#include(ne2k.il);

-- Reprograms the PIC to relocate IRQs to interrupt 20h-2Fh
(
	declare d0 = kernel:in [+x21,];
	declare d1 = kernel:in [+xA1,];
	kernel:out [+x20, +x11,]; -- Initializes and disables ICW4
	kernel:out [+xA0, +x11,];
	kernel:out [+x21, +x20,]; -- Remaps IRQ0-7
	kernel:out [+xA1, +x28,]; -- Remaps IRQ8-F
	kernel:out [+x21, +x04,];
	kernel:out [+xA1, +x02,];
	kernel:out [+x21, +x01,]; -- 8086/88 (MCS-80/85) mode
	kernel:out [+xA1, +x01,];
	kernel:out [+x21, d0,];
	kernel:out [+xA1, d1,];
);

-- Sets the 8253 to enable 100 timer ticks per second, and enables keyboard
kernel:out [+x43, +x36,];
kernel:out [+x40, 11932 % 256,];
kernel:out [+x40, 11932 / 256,];
kernel:out [+x21, +xFC,];

idt:idt-setup [];
pm:pm-init [];
pg:pg-setup []; -- Sets up paging
svc:svc-setup [];

cs:cs-puts [if (ne2k:ne2k-probe []) then "ne2000 detected" else "ne2k not found",];
cs:cs-nl [];

ne2k:ne2k-setup [];

asm _ STI ();

-- Enters user mode
(
	declare user-stack-address = +xF0000000;
	declare physical = pm:pm-allocate [];
	pg:pg-map [user-stack-address, physical,];

	for (declare i = 0; i < 104; i =+ 4) (
		let `tss-address + i` = 0;
	);
	let `tss-address + 4` = interrupt-stack-address;
	let `tss-address + 8` = +x10;

	asm _ MOV (AX, +x2B);
	asm _ LTR (AX);

	user-stack-address + 4096;
);

asm _ MOV (EBX, EAX);
asm _ MOV (EAX, +x23);
asm _ MOV (DS, AX);
asm _ MOV (ES, AX);
asm _ MOV (FS, AX);
asm _ MOV (GS, AX);
asm _ PUSH (EAX); -- Would be SS
asm _ PUSH (EBX); -- Would be ESP
asm _ PUSHF ();
asm _ PUSH (+x1B); -- Would be CS
asm _ D8 (+x68);
asm _ D32 (.user); -- Would be IP
asm _ IRET ();

declare user-mode = [] (
	asm .user PUSH (`+x40000`);
	asm _ MOV (EBP, ESP);

	-- Shows OK
	svc:svc-service [svc-putc_, +x4F, 0,];
	svc:svc-service [svc-putc_, +x4B, 0,];

	asm .loop ();
	svc:svc-service [svc-halt_, 0, 0,];
	asm _ JMP (DWORD .loop);
);
