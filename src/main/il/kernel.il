-- Memory layout
-- 07C00 -> 07E00 boot sector
-- 20000 -> 20800 interrupt descriptor table (IDT)
-- 20800 -> 20868 task state segment (TSS)
-- 20868 -> 22000 kernel interrupt stack
-- 22000 -> 24000 kernel system call stack
-- 24000 -> 30000 kernel initialization stack
-- 40000 -> 40004 kernel this frame pointer
-- 40000 -> 50000 kernel code

asm _ MOV (ESP, +x30000);
asm _ MOV (EBP, ESP);
asm _ MOV (`+x40000`, EBP);

declare kernel = this;
declare idt-address = +x20000;
declare tss-address = +x20800;
declare syscall-stack-address = +x22000;
declare interrupt-stack-address = +x24000;
declare memory-region-start = 1048576;

#include(console.il);
#include(phy-mem.il);
#include(paging.il);

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

declare handle-general-interrupt = [] (
	asm _ PUSHA ();
	asm _ PUSH (DS);
	asm _ MOV (AX, 16);
	asm _ MOV (DS, AX);
	asm _ PUSH (`+x40000`);
	asm _ MOV (EBP, ESP);

	`+xB8000` =+ 1;

	-- Sends end of interrupt signal
	kernel:out [+x20, +x20,];

	asm _ MOV (ESP, EBP);
	asm _ POP (EAX);
	asm _ POP (DS);
	asm _ POPA ();
	asm _ MOV (ESP, EBP);
	asm _ POP (EBP);
	asm _ IRET ();
);

-- Sets up and loads interrupt descriptor table
declare set-idt-entry = [interrupt, handler,] (
	declare p = idt-address + interrupt shl 3;
	let `p + 0` = handler and +x0000FFFF + +x00080000;
	let `p + 4` = +x00008E00 + handler and +xFFFF0000;
);

for (declare i = 0; i < 256; i =+ 1) (
	kernel:set-idt-entry [i, handle-general-interrupt,];
);

(
	allocate idtr/8;
	let `idtr` = +x7FF;
	let `idtr + 2` = idt-address;
	idtr;

	asm _ LIDT (`EAX`);
);

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

pm:pm-init [];
pg:pg-setup []; -- Sets up paging

-- Enters user mode
(
	declare user-stack-address = +xF0000000;
	declare physical = pm:pm-allocate [];
	pg:pg-map [user-stack-address, physical,];

	for (declare i = 0; i < 104; i =+ 4) (
		let `tss-address + i` = 0;
	);
	`tss-address + 4` = interrupt-stack-address;
	`tss-address + 8` = 16;

	user-stack-address + 4096 - 8; -- 4k stack but minus 8 bytes for SS:ESP

	asm _ MOV (EBX, EAX);
	asm _ MOV (AX, +x2B);
	asm _ LTR (AX);
	asm _ MOV (AX, +x23);
	asm _ MOV (DS, AX);
	asm _ MOV (ES, AX);
	asm _ MOV (FS, AX);
	asm _ MOV (GS, AX);
	asm _ PUSH (+x23); -- Would be SS
	asm _ PUSH (EBX); -- Would be ESP
	asm _ PUSHF ();
	asm _ PUSH (+x1B); -- Would be CS
	asm _ D8 (+x68);
	asm _ D32 (.user); -- Would be IP
	asm _ STI ();
	--asm _ IRET ();
	--asm .user PUSH (`+x40000`);
	--asm _ MOV (EBP, ESP);
);

asm .loop JMP (DWORD .loop);
