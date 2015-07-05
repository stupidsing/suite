-- TODO use constant to encapsulate code fragments
-- TODO better compiler type checking
-- TODO better compiler structure handling
-- TODO better compiler method reference (with this pointer embedded)
-- TODO compiler declare should return pointers
-- TODO fork
-- TODO garbage collection
-- TODO exit

-- Physical memory layout
-- 00000000 -> 00100000 refer logical memory layout below
-- 00100000 -> stackx   physical page stack
-- stackx   -> pagex    allocatable physical pages

-- Logical memory layout
-- 00007C00 -> 00007E00 boot sector
-- 00020000 -> 00020800 interrupt descriptor table (IDT)
-- 00020800 -> 00020868 task state segment (TSS)
-- 00020868 -> 00022000 kernel interrupt stack
-- 00022000 -> 00024000 kernel system call stack
-- 00024000 -> 00026000 kernel initialization stack
-- 00040000 -> 00040004 kernel this frame pointer
-- 00040000 -> 00050000 kernel code
-- 00050000 -> 000A0000 kernel buddy allocation pool
-- 000A0000 -> 00100000 BIOS / video memory
-- 00800000 -> FFC00000 allocatable logical pages
-- FFC00000 -> FFFFEFFF second level page tables, for the current address space
-- FFFFF000 -> end      first level page table, for the current address space

constant idt-address = +x20000;
constant tss-address = +x20800;
constant interrupt-stack-address = +x22000;
constant syscall-stack-address = +x24000;
constant memory-region-start = 1048576;

asm _ MOV (ESP, +x26000);
asm _ MOV (EBP, ESP);
asm _ MOV (`+x40000`, EBP);

declare kernel = this;
declare ticks = 0;

declare copy = [dst, src, len,] (
	asm _ CLD ();
	asm _ MOV (EDI, `EBP + 8`);
	asm _ MOV (ESI, `EBP + 12`);
	asm _ MOV (ECX, `EBP + 16`);
	asm _ SHR (ECX, 2);
	asm _ REP MOVSD ();
	asm _ MOV (ECX, `EBP + 16`);
	asm _ AND (ECX, 3);
	asm _ REP MOVSB ();
);

declare zero = [dst, len,] (
	asm _ XOR (EAX, EAX);
	asm _ CLD ();
	asm _ MOV (EDI, `EBP + 8`);
	asm _ MOV (ECX, `EBP + 12`);
	asm _ SHR (ECX, 2);
	asm _ REP STOSD ();
	asm _ MOV (ECX, `EBP + 12`);
	asm _ AND (ECX, 3);
	asm _ REP STOSB ();
);

declare inb = [port,] (
	asm _ MOV (EDX, `EBP + 8`);
	asm _ XOR (EAX, EAX);
	asm _ IN (AL, DX);
);

declare inw = [port,] (
	asm _ MOV (EDX, `EBP + 8`);
	asm _ XOR (EAX, EAX);
	asm _ IN (AX, DX);
);

declare outb = [port, value,] (
	asm _ MOV (EDX, `EBP + 8`);
	asm _ MOV (EAX, `EBP + 12`);
	asm _ OUT (DX, AL);
);

declare outw = [port, value,] (
	asm _ MOV (EDX, `EBP + 8`);
	asm _ MOV (EAX, `EBP + 12`);
	asm _ OUT (DX, AX);
);

declare wrmsr = [msr, value,] (
	asm _ MOV (ECX, `EBP + 8`);
	asm _ MOV (EAX, `EBP + 12`);
	asm _ XOR (EDX, EDX);
	asm _ WRMSR ();
);

#include(doubly-ll.il);
#include(sync.il);
#include(console.il);
#include(buddy-allocator.il);
#include(first-fit-allocator.il);

declare mm-allocate = ba-allocate;
declare mm-deallocate = ba-deallocate;

#include(irq.il);
#include(scheduler.il);
#include(idt.il);
#include(phy-mem.il);
#include(paging.il);
#include(service.il);
#include(time.il);
#include(keyboard.il);
#include(ne2k.il);

-- Reprograms the PIC to relocate IRQs to interrupt 20h-2Fh
(
	declare d0 = kernel:inb [+x21,];
	declare d1 = kernel:inb [+xA1,];
	kernel:outb [+x20, +x11,]; -- Initializes and disables ICW4
	kernel:outb [+xA0, +x11,];
	kernel:outb [+x21, +x20,]; -- Remaps IRQ0-7
	kernel:outb [+xA1, +x28,]; -- Remaps IRQ8-F
	kernel:outb [+x21, +x04,];
	kernel:outb [+xA1, +x02,];
	kernel:outb [+x21, +x01,]; -- 8086/88 (MCS-80/85) mode
	kernel:outb [+xA1, +x01,];
	kernel:outb [+x21, d0,];
	kernel:outb [+xA1, d1,];
);

-- Sets the 8253 to 100 timer ticks per second, and enables keyboard
kernel:outb [+x43, +x36,];
kernel:outb [+x40, 11932 % 256,];
kernel:outb [+x40, 11932 / 256,];
kernel:outb [+x21, +xFC,];

ba:ba-setup [];
irq:irq-setup [];
sch:sch-setup [];
idt:idt-setup [];
pm:pm-setup [];
pg:pg-setup []; -- Sets up paging
svc:svc-setup [];
kb:kb-setup [];

cs:cs-puts [if (ne2k:ne2k-probe []) then "ne2000 detected" else "ne2k not found",];
cs:cs-nl [];

ne2k:ne2k-setup [];

asm _ STI ();

-- Enters user mode
(
	declare user-stack-address = pg:pg-allocate [1,];

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
	svc:svc-service [svc-putc_, +x0A, 0,];

	asm .loop ();
	time:time-yield [];
	asm _ JMP (DWORD .loop);
);

kb:kb-teardown [];
sch:sch-teardown [];
