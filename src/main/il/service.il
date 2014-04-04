declare svc = this;

declare handle-service = [r0, r1, r2,] (
	if (r0 = 0) then (
		asm _ STI ();
		asm _ HLT ();
	) else if (r0 = 1) then (
		cs:cs-put [r1,];
	) else ();
);

declare handle-sysenter = [] (
	asm _ PUSH (EDI); -- Return IP
	asm _ PUSH (EDX); -- Return ESP
	asm _ PUSH (EAX); -- r0
	asm _ PUSH (EBX); -- r1
	asm _ PUSH (ECX); -- r2
	asm _ MOV (AX, 16);
	asm _ MOV (DS, AX);
	asm _ PUSH (`+x40000`);
	asm _ MOV (EBP, ESP);

	svc:handle-service [`this + 12`, `this + 8`, `this + 4`,];

	asm _ MOV (ESP, EBP);
	asm _ ADD (ESP, 16);
	asm _ POP (ECX);
	asm _ POP (EDX);

	asm _ MOV (ESP, EBP);
	asm _ POP (EBP);
	asm _ SYSEXIT ();
);

declare svc-service = [r0, r1, r2,] (
	asm _ PUSH (DS);
	asm _ PUSH (ECX);
	asm _ PUSH (EDX);
	asm _ PUSH (ESI);
	asm _ PUSH (EDI);
	asm _ PUSH (EBP);
	asm _ D8 (+x68);
	asm _ D32 (.sysexitPoint);
	asm _ POP (EDI);
	asm _ MOV (EDX, ESP);
	asm _ SYSENTER ();
	asm .sysexitPoint ();
	asm _ POP (EBP);
	asm _ POP (EDI);
	asm _ POP (ESI);
	asm _ POP (EDX);
	asm _ POP (ECX);
	asm _ POP (DS);
);

declare svc-setup = [] (
	kernel:wrmsr [+x174, +x8,];
	kernel:wrmsr [+x175, syscall-stack-address,];
	kernel:wrmsr [+x176, handle-sysenter,];
);
