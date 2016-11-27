constant stack-size = 240;
constant sch-action-none = 0;
constant sch-action-exit = 1;
constant sch-action-fork = 2;

constant sch-node = dl-node struct (
| int esp
| int action
| boolean +busy
| (int * stack-size) +kernel-stack
);

declare pointer:sch-node sch-current-process = null;
declare sch-head-process; -- Cyclic doubly-linked list

declare sch-allocate = function [] (
	declare process = mm-allocate sch-node;
	{process/*/prev} = process;
	{process/*/next} = process;
	{process/*/current/action} = sch-action-none;
	{process/*/current/+busy} = true;
	process;
);

declare sch-deallocate = function [e,] (
	mm-deallocate sch-node e;
);

declare sch-switch = function [] ( -- Round robin in action
	{sch-current-process} = dl-next [sch-current-process,];
);

declare sch-yield = function [] (
	asm _ FR-PUSHN 4;
	asm _ PUSHF ();
	asm _ CLI ();

	(sch-current-process +f current) +f esp;
	asm _ MOV (EBX, CR3);
	asm _ FR-PUSH (EBX);
	asm _ MOV (`EAX`, ESP);

	if (sch-current-process/*/current/action = sch-action-exit) then (
		declare process0 = sch-current-process;
		{sch-current-process} = dl-prev [sch-current-process,];
		dl-remove [process0,];
		sch-deallocate [process0,];
	) else ();

	if (sch-current-process/*/current/action = sch-action-fork) then (
		declare process1 = sch-allocate [];
		{process1/*/current/+kernel-stack} = sch-current-process/*/current/+kernel-stack;
		{process1/*/current/esp} = sch-current-process/*/current/esp - sch-current-process as int + process1 as int;
		dl-insert-next [sch-head-process, process1,];
		{sch-current-process/*/current/action} = sch-action-none;
	) else ();

	sch-switch [];
	dw-set [tss-address + 4, sch-current-process as int + size-of sch-node,]; -- TSS ESP0
	wrmsr [+x175, sch-current-process as int + size-of sch-node, 0,]; -- SYSENTER ESP

	(sch-current-process +f current) +f esp;
	asm _ MOV (ESP, `EAX`);
	asm _ FR-POP (EBX);
	asm _ MOV (CR3, EBX);

	asm _ POPF ();
	asm _ FR-POPN 4;
);

declare sch-fork = function [] (
	declare process0 = sch-current-process;
	{process0/*/current/action} = sch-action-fork;
	sch-yield [];
	sch-current-process != process0;
);

declare sch-exit = function [] (
	{sch-current-process/*/current/action} = sch-action-exit;
	if (sch-current-process != sch-head-process) then (
		sch-yield [];
	) else ( -- Initial process should not exit. Halt if this happened.
		asm _ CLI ();
		asm _ HLT ();
	);
);

declare sch-setup = function [] (
	{sch-head-process} = sch-allocate [];
	{sch-current-process} = sch-head-process;

	-- Uses the process stack
	declare stack-end = sch-current-process as int + size-of sch-node;
	copy [stack-end - stack-size * 4, kernel-second-esp - stack-size * 4, stack-size * 4,];
	stack-end;
	asm _ SUB (EAX, kernel-second-esp);
	asm _ ADD (EBP, EAX);
	asm _ ADD (ESP, EAX);

	-- Sets up machine status register for SYSENTER
	sch-yield [];
);

declare sch-teardown = function [] (
	sch-deallocate [sch-head-process,];
);
