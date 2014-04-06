declare svc = this;

declare handle-service = [p0, p1, p2,] (
	if (p0 = 0) then (
		asm _ STI ();
		asm _ HLT ();
	) else if (p0 = 1) then (
		cs:cs-put [p1,];
	) else ();
);

declare handle-sysenter = [p0, p1, p2,] (
	asm _ PUSH (EDI); -- Return IP
	asm _ PUSH (EDX); -- Return ESP
	asm _ PUSH (EAX); -- p0
	asm _ PUSH (EBX); -- p1
	asm _ PUSH (ECX); -- p2
	asm _ MOV (AX, +x10);
	asm _ MOV (DS, AX);
	asm _ PUSH (0);
	asm _ PUSH (`+x40000`);
	asm _ MOV (EBP, ESP);

	svc:handle-service [p0, p1, p2,];

	asm _ MOV (ESP, EBP);
	asm _ ADD (ESP, 20);
	asm _ MOV (AX, +x23);
	asm _ MOV (DS, AX);
	asm _ POP (ECX);
	asm _ POP (EDX);
	asm _ POP (EBP);
	asm _ SYSEXIT ();
);

declare svc-service = [p0, p1, p2,] (
	asm _ MOV (EAX, `EBP + 16`);
	asm _ MOV (EBX, `EBP + 12`);
	asm _ MOV (ECX, `EBP + 8`);
	asm _ MOV (EDX, ESP);
	asm _ D8 (+x68);
	asm _ D32 (.sysexitPoint);
	asm _ POP (EDI);
	asm _ SYSENTER ();
	asm .sysexitPoint ();
);

declare svc-setup = [] (
	kernel:wrmsr [+x174, +x8,];
	kernel:wrmsr [+x175, syscall-stack-address,];
	kernel:wrmsr [+x176, handle-sysenter,];
);
