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
	asm _ PUSHF ();
	asm _ STI ();
	asm _ HLT ();
	asm _ POPF ();
);

declare sync-try-enter = [p,] (
	sync:cmpxchg [p, 0, 1,];
);

declare sync-enter = [p,] (
	while (sync:sync-try-enter [p,] = 0) do (
		sync:sync-yield [];
	);
);

declare sync-leave = [p,] (
	let `p` = 0;
);

declare sync-wait = [p, w,] (
	let `w` = 0;
	while (`w` = 0) do (
		sync:sync-leave [p,];
		sync:sync-yield [];
		sync:sync-enter [p,];
	);
);

declare sync-notify = [p, w,] (
	let `w` = 1;
);

constant synchronized .p .block = (
	sync:sync-enter [.p,];
	declare result = .block;
	sync:sync-leave [.p,];
	result;
);
