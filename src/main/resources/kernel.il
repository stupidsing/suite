
asm/(
	_ MOV (ESP, +x2FFF0),
	_ MOV (EBP, ESP),
	_ MOV (`+x40000`, EBP),
);

declare idt-address = +x20000;
declare memory-region-start = 1048576;

declare in = [port,] asm/(
	_ MOV (EDX, `EBP + 8`),
	_ XOR (EAX, EAX),
	_ IN (AL, DX),
);

declare out = [port, value,] asm/(
	_ MOV (EDX, `EBP + 8`),
	_ MOV (EAX, `EBP + 12`),
	_ OUT (DX, AL),
);

declare general-interrupt-handler = [dummy,]
	asm/(
		_ PUSHA (),
		_ PUSH (`+x40000`),
		_ MOV (EBP, ESP),
	);

	-- Sends end of interrupt signal
	out [+x20, +x20,];

	`+xB8000` =+ 1;

	asm/(
		_ MOV (ESP, EBP),
		_ POP (EAX),
		_ POPA (),
		_ MOV (ESP, EBP),
		_ POP (EBP),
		_ IRET (),
	);
;

-- Sets up and loads IDT
for (declare offset = 0; offset < 2048;)
	let `idt-address + offset =+ 4` = 8 shl 16 + general-interrupt-handler and +xFFFF;
	let `idt-address + offset =+ 4` = general-interrupt-handler and +xFFFF0000 + +x8E00;
;

let `+x7C00` = +x7FF;
let `+x7C02` = idt-address;

asm/(
	_ LIDT (`+x7C00`),
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

asm/(
	_ STI (),
);

-- Detects memory size
declare page0 = memory-region-start;
declare pagex = page0;
(
	while (
		declare t = `pagex` xor +xFFFFFFFF;
		let `pagex` = t;
		`pagex =+ 4096` = t
	) do ();
);

-- Sets up page allocate-physical-pageator
declare stack0 = memory-region-start;
declare stackx = stack0;
(
	declare stack-size = 4 * (pagex - page0) shr 12;
	declare first-page = page0 + (stack-size + 4096) and +xFFFFF000;
	for (declare page = first-page; page < pagex; page =+ 4096)
		let `stackx =+ 4` = page
	;
);

declare allocate-physical-page = [] (
	if (stack0 != stackx) then `stack0 =+ 4` else 0
);

declare free-physical-page = [page,] (
	`stack0 += 4` = page
);

declare fill-zeroes = [page,] (
	for (declare offset = 0; offset < 4096; offset =+ 4)
		let `offset` = 0
	;
);

-- Sets up paging
(
	declare spt = allocate-physical-page [];
	fill-zeroes [spt,];

	declare offset = 0;
	for (declare physical = 0; physical < stackx; physical =+ 4096)
		let `spt + offset =+ 4` = physical + 261 -- Supervisor, read-only
	;

	let `spt` = 0; -- Trap zero pointers

	declare rpt = allocate-physical-page [];
	fill-zeroes [rpt,];

	let `rpt` = spt + 5; -- Maps kernel memory
	let `rpt + 4092` = rpt + 1; -- Maps itself
	rpt;

	asm/(
		_ D8 (+x0F), -- MOV CR3, EAX
		_ D8 (+x22),
		_ D8 (+xD8),
		_ D8 (+x0F), -- MOV EAX, CR0
		_ D8 (+x20),
		_ D8 (+xC0),
		_ OR (EAX, +x80000000),
		_ D8 (+x0F), -- MOV CR0, EAX
		_ D8 (+x22),
		_ D8 (+xC0),
		_ D8 (+xEB), -- JMP $ + 2
		_ D8 (+x00),
	);
);

declare refresh-paging = [] asm/(
	_ D8 (+x0F), -- MOV EAX, CR3
	_ D8 (+x20),
	_ D8 (+xD8),
	_ D8 (+x0F), -- MOV CR3, EAX
	_ D8 (+x22),
	_ D8 (+xD8),
);

declare map-page = [logical, physical,] (
	declare pe0 = +xFFFFF000 + (logical shr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical shr 12) shl 2;
	declare p1 = pe1 and +xFFFFF000;

	if (`pe0` = 0) then (
		let `pe0` = allocate-physical-page [] + 7;
		fill-zeroes [p1,];
	) else ();

	let `pe1` = physical + 7;

	refresh-paging [];
);

declare unmap-page = [logical,] (
	declare pe0 = +xFFFFF000 + (logical shr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical shr 12) shl 2;
	declare p1 = pe1 and +xFFFFF000;

	let `pe1` = 0;

	declare isEmpty = 1;

	for (declare offset = 0; offset < 4096; offset =+ 4)
		let isEmpty = isEmpty and (`p1 + offset` = 0)
	;

	if (isEmpty) then (
		free-physical-page [`pe0` and +xFFFFFFFC,];
		let `pe0` = 0;
	) else ();

	refresh-paging [];
);

-- Shows OK
let `+xB8000` = +x704B704F;

asm/(
	.loop HLT (),
	_ JMP (.loop),
);
