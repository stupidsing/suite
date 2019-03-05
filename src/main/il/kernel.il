constant idt-address = +x20000;
constant tss-address = +x20800;
constant kernel-first-esp = +x23000;
constant kernel-second-esp = +x24000;
constant memory-region-start = 1048576;

asm _ MOV (ESP, kernel-first-esp);
asm _ MOV (EBP, ESP);
asm _ MOV (`+x40000`, EBP);

declare ticks = 0;

declare copy = function [dst, src, len,] (
	asm _ CLD ();
	asm _ MOV (EDI, `EBP + 12`);
	asm _ MOV (ESI, `EBP + 16`);
	asm _ MOV (ECX, `EBP + 20`);
	asm _ SHR (ECX, 2);
	asm _ REP ();
	asm _ MOVSD ();
	asm _ MOV (ECX, `EBP + 20`);
	asm _ AND (ECX, 3);
	asm _ REP ();
	asm _ MOVSB ();
);

declare zero = function [dst, len,] (
	asm _ XOR (EAX, EAX);
	asm _ CLD ();
	asm _ MOV (EDI, `EBP + 12`);
	asm _ MOV (ECX, `EBP + 16`);
	asm _ SHR (ECX, 2);
	asm _ REP ();
	asm _  STOSD ();
	asm _ MOV (ECX, `EBP + 16`);
	asm _ AND (ECX, 3);
	asm _ REP ();
	asm _  STOSB ();
);

declare dw-get = function [p,] (
	(p as pointer:int)/*;
);

declare dw-set = function [p, i,] (
	{(p as pointer:int)/*} = i;
);

declare inb = function [port,] (
	asm _ MOV (EDX, `EBP + 12`);
	asm _ XOR (EAX, EAX);
	asm _ IN (AL, DX);
);

declare inw = function [port,] (
	asm _ MOV (EDX, `EBP + 12`);
	asm _ XOR (EAX, EAX);
	asm _ IN (AX, DX);
);

declare outb = function [port, value,] (
	asm _ MOV (EDX, `EBP + 12`);
	asm _ MOV (EAX, `EBP + 16`);
	asm _ OUT (DX, AL);
);

declare outw = function [port, value,] (
	asm _ MOV (EDX, `EBP + 12`);
	asm _ MOV (EAX, `EBP + 16`);
	asm _ OUT (DX, AX);
);

declare rdmsr = function [msr, out value0, out value1,] (
	asm _ MOV (ECX, `EBP + 12`);
	asm _ RDMSR ();
	asm _ MOV (`EBP + 16`, EAX);
	asm _ MOV (`EBP + 20`, EDX);
);

declare wrmsr = function [msr, value0, value1,] (
	asm _ MOV (ECX, `EBP + 12`);
	asm _ MOV (EAX, `EBP + 16`);
	asm _ MOV (EDX, `EBP + 20`);
	asm _ WRMSR ();
);

#include(bitmap.il);
#include(doubly-ll.il);
#include(stack.il);
#include(sync.il);

#include(buddy-allocator.il);
#include(console.il);
#include(first-fit-allocator.il);

constant mm-allocate .type = (
	(ba-allocate [size-of .type,]) as pointer:.type
);
constant mm-deallocate .type .pointer = (
	ba-deallocate [address .pointer/*, size-of .type,]
);

constant log .s = ( cs-puts [.s,]; cs-nl []; );
constant log-byte .s .i = ( cs-puts [.s,]; cs-put-hex1 [.i,]; cs-nl []; );
constant log-word .s .i = ( cs-puts [.s,]; cs-put-hex2 [.i,]; cs-nl []; );
constant log-dword .s .i = ( cs-puts [.s,]; cs-put-hex4 [.i,]; cs-nl []; );

#include(apic.il);
#include(gc.il);
#include(irq.il);
#include(phy-mem.il);
#include(scheduler.il);

#include(idt.il);
#include(harddisk.il);
#include(keyboard.il);
#include(paging.il);
#include(service.il);

#include(time.il);

#include(ne2k.il);

#include(tests.il);

-- reprograms the PIC to relocate IRQs to interrupt 20h-2Fh
(
	declare d0 = inb [+x21,];
	declare d1 = inb [+xA1,];
	outb [+x20, +x11,]; -- initializes and disables ICW4
	outb [+xA0, +x11,];
	outb [+x21, +x20,]; -- remaps IRQ0-7
	outb [+xA1, +x28,]; -- remaps IRQ8-F
	outb [+x21, +x04,];
	outb [+xA1, +x02,];
	outb [+x21, +x01,]; -- 8086/88 (MCS-80/85) mode
	outb [+xA1, +x01,];
	outb [+x21, d0,];
	outb [+xA1, d1,];
);

-- sets the 8253 to 100 timer ticks per second, and enables keyboard
outb [+x43, +x36,];
outb [+x40, 11932 % 256,];
outb [+x40, 11932 / 256,];
outb [+x21, +xFC,];

-- initializes task state segment (TSS)
for (declare i = 0; i < 104; i =+ 4) do (
	dw-set [tss-address + i, 0,];
);
--dw-set [tss-address + 4, current-process as int + size-of sch-node,]; -- set by scheduler instead
dw-set [tss-address + 8, +x10,];

asm _ MOV (AX, WORD +x2B);
asm _ LTR (AX);

declare user-mode = function [] (
	asm .user PUSH (`+x40000`);
	asm _ MOV (EBP, ESP);

	-- shows OK
	svc-service [svc-putc_, +x4F, 0,];
	svc-service [svc-putc_, +x4B, 0,];
	svc-service [svc-putc_, +x0A, 0,];

	while true do (time-yield []);
);

-- switch to a smaller stack so as to fit in the process stack space.
-- global scope variables, defined before this point, are kept in the first stack.
asm _ MOV (ESP, kernel-second-esp);

(function [] (
	asm _ MOV (EAX, `+x40000`);
	asm _ MOV (`EBP`, EAX);

	ba-setup [];
	ap-setup [];
	irq-setup [];
	sch-setup [];
	idt-setup [];
	pm-setup [];
	pg-setup []; -- sets up paging
	gc-setup [];
	svc-setup [];
	kb-setup [];

	test [];

	asm _ STI ();

	log (if (ne2k-probe []) then "ne2k detected" else "ne2k not found");

	ne2k-setup [];

	if (sch-fork []) then (
		log "From another thread";
	) else (
		log "From first thread";
		while true do ( -- halt CPU if none of the process is busy
			asm _ CLI ();
			declare process = sch-head-process;
			declare busy = false;
			while (({process} = dl-next [process,]) != sch-head-process) do (
				{busy} = (busy || process/*/current/+busy);
			);
			asm _ STI ();
			if (not busy) then (asm _ HLT ()) else ();
		);
	);

	-- enters user mode
	declare user-stack-address = pg-allocate [1,];
	user-stack-address + 4096;
	asm _ MOV (EBX, EAX);
	asm _ MOV (EAX, +x23);
	asm _ MOV (DS, AX);
	asm _ MOV (ES, AX);
	asm _ MOV (FS, AX);
	asm _ MOV (GS, AX);
	asm _ PUSH (EAX); -- would be SS
	asm _ PUSH (EBX); -- would be ESP
	asm _ PUSHF ();
	asm _ PUSH (+x1B); -- would be CS
	asm _ IMM (BYTE +x68);
	asm _ IMM (DWORD .user); -- would be IP
	asm _ IRET ();

	pg-deallocate [user-stack-address, 1,];
	ne2k-teardown [];
	kb-teardown [];
	svc-teardown [];
	gc-teardown [];
	pg-teardown [];
	pm-teardown [];
	idt-teardown [];
	sch-teardown [];
	irq-teardown [];
	ap-teardown [];
	ba-teardown [];

	log "System halted";
	asm _ CLI ();
	asm _ HLT ();
)) [];
