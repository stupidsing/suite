asm/(
	_ MOV (ESP, +x2FFF0),
	_ MOV (EBP, ESP),
);

(
	declare in := port => asm/(
		_ MOV (EDX, `EBP + 8`),
		_ XOR (EAX, EAX),
		_ IN (AL, DX),
	) >>

	in {+x20};

	let `+xB8000` = +x70417041;

	asm/(
		.loop HLT (),
		_ JMP .loop,
	);
);
