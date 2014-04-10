declare sync = this;

declare cmpxchg = [loc, v0, v1,] (
	asm _ MOV (EDI, `EBP + 8`);
	asm _ MOV (EAX, `EBP + 12`);
	asm _ MOV (EBX, `EBP + 16`);
	asm _ CMPXCHG (`EDI`, EBX);
	asm _ SETE (AL);
	asm _ MOVZX (EAX, AL);
);

declare sync-yield = [] (
	--svc:svc-service [svc-halt_, 0, 0,];
	asm _ PUSHF ();
	asm _ STI ();
	asm _ HLT ();
	asm _ POPF ();
);

declare sync-unwait = [p,] (
	let `p` = 0;
);

declare sync-wait = [p,] (
	while (`p` = 0) do (
		sync:sync-yield [];
	);
);

declare sync-notify = [p,] (
	let `p` = 1;
);

declare sync-enter = [p,] (
	while (sync:cmpxchg [p, 0, 1,] = 0) do (
		sync:sync-yield [];
	);
);

declare sync-leave = [p,] (
	let `p` = 0;
);
