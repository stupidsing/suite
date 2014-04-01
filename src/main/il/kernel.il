asm _ MOV (ESP, +x2FFF0);
asm _ MOV (EBP, ESP);
asm _ MOV (`+x40000`, EBP);

declare kernel-this = this;
declare idt-address = +x20000;
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

declare general-interrupt-handler = [dummy,] (
	asm _ PUSHA ();
	asm _ PUSH (`+x40000`);
	asm _ MOV (EBP, ESP);

	-- Sends end of interrupt signal
	out [+x20, +x20,];

	`+xB8000` =+ 1;

	asm _ MOV (ESP, EBP);
	asm _ POP (EAX);
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
	set-idt-entry [i, general-interrupt-handler,];
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
	declare d0 = in [+x21,];
	declare d1 = in [+xA1,];
	out [+x20, +x11,]; -- Initializes and disables ICW4
	out [+xA0, +x11,];
	out [+x21, +x20,]; -- Remaps IRQ0-7
	out [+xA1, +x28,]; -- Remaps IRQ8-F
	out [+x21, +x04,];
	out [+xA1, +x02,];
	out [+x21, +x01,]; -- 8086/88 (MCS-80/85) mode
	out [+xA1, +x01,];
	out [+x21, d0,];
	out [+xA1, d1,];
);

-- Sets the 8253 to enable 100 timer ticks per second, and enables keyboard
out [+x43, +x36,];
out [+x40, 11932 % 256,];
out [+x40, 11932 / 256,];
out [+x21, +xFC,];

asm _ STI ();

pm-init [];
pg-setup []; -- Sets up paging

-- Shows OK
let `+xB8000` = +x704B704F;

asm .loop HLT ();
asm _ JMP (.loop);
