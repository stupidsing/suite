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

	let `+xB8000` = +x704B704F;

	asm/(
		_ STI (),
	);

	asm/(
		.loop HLT (),
		_ JMP (.loop),
	);
);
