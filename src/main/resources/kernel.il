asm/(
	_ MOV (ESP, +x2FFF0),
	_ MOV (EBP, ESP),
	_ MOV (`+x40000`, EBP),
);

(
	declare idtAddress := +x20000 >>

	declare in := [port,] asm/(
		_ MOV (EDX, `EBP + 8`),
		_ XOR (EAX, EAX),
		_ IN (AL, DX),
	) >>

	declare out := [port, value,] asm/(
		_ MOV (EDX, `EBP + 8`),
		_ MOV (EAX, `EBP + 12`),
		_ OUT (DX, AL),
	) >>

	declare generalInterruptHandler := [dummy,]
		asm/(
			_ PUSHA (),
			_ PUSH (`+x40000`),
			_ MOV (EBP, ESP),
		);

		-- Sends end of interrupt signal
		out [+x20, +x20,];

		let `+xB8000` = `+xB8000` + 1;

		asm/(
			_ MOV (ESP, EBP),
			_ POP (EAX),
			_ POPA (),
			_ MOV (ESP, EBP),
			_ POP (EBP),
			_ IRET (),
		);
	>>

	-- Sets up and loads IDT
	(
		declare offset := 0 >>
		while (offset < 2048) do (
			let `idtAddress + offset` = 8 shl 16 + generalInterruptHandler and +xFFFF;
			let offset = offset + 4;
			let `idtAddress + offset` = generalInterruptHandler and +xFFFF0000 + +x8E00;
			let offset = offset + 4;
		);
	);

	let `+x7C00` = +x7FF;
	let `+x7C02` = idtAddress;

	asm/(
		_ LIDT `+x7C00`,
	);

	-- Sets the 8253 to enable 100 timer ticks per second, and enables keyboard
	out [+x43, +x36,];
	out [+x40, 11932 % 256,];
	out [+x40, 11932 / 256,];
	out [+x21, +xFC,];

	-- Reprograms the PIC to relocate IRQs to interrupt 20h-2Fh
	(
		declare d0 := in [+x21,] >>
		declare d1 := in [+xA1,] >>
		out [+x20, +x11,]; -- Initializes and disables ICW4
		out [+xA0, +x11,];
		out [+x21, +x20,]; -- Remaps IRQ0-7
		out [+xA1, +x28,]; -- Remaps IRQ8-F
		out [+x21, 4,];
		out [+xA1, 2,];
		out [+x21, 1,];
		out [+xA1, 1,];
		out [+x21, d0,];
		out [+xA1, d1,];
	);

	let `+xB8000` = +x704B704F;

	asm/(
		_ STI (),
	);

	asm/(
		.loop HLT (),
		_ JMP (.loop),
	);
);
