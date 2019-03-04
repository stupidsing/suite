declare cmpxchg = function [loc, v0, v1,] (
	asm _ MOV (EDI, `EBP + 12`);
	asm _ MOV (EAX, `EBP + 16`);
	asm _ MOV (EBX, `EBP + 20`);
	asm _ LOCK ();
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
	cmpxchg [p, 0, 1,];
);

declare sync-enter = function [p,] (
	while (sync-try-enter [p,] = 0) do (
		sync-yield [];
	);
);

declare sync-leave = function [p,] (
	{p/*} = 0;
);

declare sync-wait = function [p, w,] ( -- TODO
	{w/*} = 0;
	while (w/* = 0) do (
		sync-leave [p,];
		sync-yield [];
		sync-enter [p,];
	);
);

declare sync-notify = function [pointer:int p, w,] ( -- TODO
	{w/*} = 1;
);

constant synchronized .p .block = (
	sync-enter [.p,];
	declare result = .block;
	sync-leave [.p,];
	result;
);
