constant svc-exit_ = 0;
constant svc-fork_ = 1;
constant svc-nl___ = 2;
constant svc-putc_ = 3;
constant svc-ticks = 4;
constant svc-yield = 5;

declare svc-handle-service = function [p0, p1, p2,] (
	if (p0 = svc-exit_) then (
		sch-exit [];
	) else if (p0 = svc-fork_) then (
		if (sch-fork []) then 1 else 0;
	) else if (p0 = svc-nl___) then (
		cs-nl [];
	) else if (p0 = svc-putc_) then (
		if (p1 != 10) then (cs-put [p1,]) else (cs-nl []);
	) else if (p0 = svc-ticks) then (
		ticks;
	) else if (p0 = svc-yield) then (
		{sch-current-process/*/current/+busy} = false;
		sch-yield [];
	) else 1;
);

declare sysenter-sink = baseless [p0, p1, p2,] (
	asm _ PUSH (EDI); -- return IP
	asm _ PUSH (EDX); -- return ESP
	asm _ PUSH (EAX); -- p0
	asm _ PUSH (EBX); -- p1
	asm _ PUSH (ECX); -- p2
	asm _ PUSH (EBP);
	asm _ MOV (AX, +x10);
	asm _ MOV (DS, AX);
	asm _ PUSH (0);
	asm _ PUSH (`+x40000`);
	asm _ MOV (EBP, ESP);

	svc-handle-service [p0, p1, p2,];

	asm _ ADD (ESP, 24);
	asm _ MOV (DX, +x23);
	asm _ MOV (DS, DX);
	asm _ POP (ECX);
	asm _ POP (EDX);
	asm _ POP (EBP);
	asm _ SYSEXIT ();
);

declare svc-service = function [p0, p1, p2,] (
	declare cs0;
	asm _ ADD (ESP, 4);
	asm _ PUSH (CS);

	if (cs0 and 3 != 0) then (
		asm _ MOV (EAX, `EBP + 20`);
		asm _ MOV (EBX, `EBP + 16`);
		asm _ MOV (ECX, `EBP + 12`);
		asm _ MOV (EDX, ESP);
		asm _ IMM (BYTE +x68);
		asm _ IMM (DWORD .sysexitPoint);
		asm _ POP (EDI);
		asm _ SYSENTER ();
		asm .sysexitPoint ();
	) else (
		svc-handle-service [p0, p1, p2,];
	);
);

declare svc-setup = function [] (
	wrmsr [+x174, +x8, 0,];
	--wrmsr [+x175, syscall-stack-address, 0,]; -- set by scheduler instead
	wrmsr [+x176, sysenter-sink as int, 0,];
);

declare svc-teardown = function [] (
);
