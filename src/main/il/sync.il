declare sync = this;

declare cmpxchg = function [loc, v0, v1,] (
	asm _ MOV (EDI, `EBP + 8`);
	asm _ MOV (EAX, `EBP + 12`);
	asm _ MOV (EBX, `EBP + 16`);
	asm _ CMPXCHG (`EDI`, EBX);
	asm _ SETE (AL);
	asm _ MOVZX (EAX, AL);
);

declare sync-yield = function [] (
	asm _ PUSHF ();
	asm _ STI ();
	asm _ HLT ();
	asm _ POPF ();
);

declare sync-try-enter = function [p,] (
	invoke cmpxchg [p, 0, 1,];
);

declare sync-enter = function [p,] (
	while (invoke sync-try-enter [p,] = 0) do (
		invoke sync-yield [];
	);
);

declare sync-leave = function [p,] (
	let `p` = 0;
);

declare sync-wait = function [p, w,] (
	let `w` = 0;
	while (`w` = 0) do (
		invoke sync-leave [p,];
		invoke sync-yield [];
		invoke sync-enter [p,];
	);
);

declare sync-notify = function [p, w,] (
	let `w` = 1;
);

constant synchronized .p .block = (
	invoke sync-enter [.p,];
	declare result = .block;
	invoke sync-leave [.p,];
	result;
);
