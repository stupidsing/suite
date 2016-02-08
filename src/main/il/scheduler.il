constant stack-size = 240;
constant sch-action-none = 0;
constant sch-action-exit = 1;
constant sch-action-fork = 2;

constant sch-node = dl-node struct (
| int +esp
| int +action
| boolean +busy
| (int * stack-size) +kernel-stack
);

declare pointer:sch-node current-process = null;
declare head-process; -- Cyclic doubly-linked list

declare sch-allocate = function [] (
	declare process = mm-allocate sch-node;
	{process/*/+prev} = process;
	{process/*/+next} = process;
	{process/*/+current/+action} = sch-action-none;
	{process/*/+current/+busy} = true;
	process;
);

declare sch-deallocate = function [e,] (
	mm-deallocate sch-node e;
);

declare sch-switch = function [] (-- Round robin in action
	{current-process} = dl-next [current-process,];
);

declare sch-yield = function [] (
	asm _ FR-PUSHN 4;
	asm _ PUSHF ();
	asm _ CLI ();

	(current-process +f +current) +f +esp;
	asm _ MOV (EBX, CR3);
	asm _ FR-PUSH (EBX);
	asm _ MOV (`EAX`, ESP);

	if (current-process/*/+current/+action = sch-action-exit) then (
		declare process0 = current-process;
		{current-process} = dl-prev [current-process,];
		dl-remove [process0,];
		sch-deallocate [process0,];
	) else ();

	if (current-process/*/+current/+action = sch-action-fork) then (
		declare process1 = sch-allocate [];
		{process1/*/+current/+kernel-stack} = current-process/*/+current/+kernel-stack;
		{process1/*/+current/+esp} = current-process/*/+current/+esp - current-process as int + process1 as int;
		dl-insert-next [head-process, process1,];
		{current-process/*/+current/+action} = sch-action-none;
	) else ();

	sch-switch [];
	dw-set [tss-address + 4, current-process as int + size-of sch-node,]; -- TSS ESP0
	wrmsr [+x175, current-process as int + size-of sch-node,]; -- SYSENTER ESP

	(current-process +f +current) +f +esp;
	asm _ MOV (ESP, `EAX`);
	asm _ FR-POP (EBX);
	asm _ MOV (CR3, EBX);

	asm _ POPF ();
	asm _ FR-POPN 4;
);

declare sch-fork = function [] (
	declare process0 = current-process;
	{process0/*/+current/+action} = sch-action-fork;
	sch-yield [];
	current-process != process0;
);

declare sch-exit = function [] (
	{current-process/*/+current/+action} = sch-action-exit;
	if (current-process != head-process) then (
		sch-yield [];
	) else ( -- Initial process should not exit. Halt if this happened.
		asm _ CLI ();
		asm _ HLT ();
	);
);

declare sch-setup = function [] (
	{head-process} = sch-allocate [];
	{current-process} = head-process;

	-- Uses the process stack
	declare stack-end = current-process as int + size-of sch-node;
	copy [stack-end - stack-size * 4, kernel-second-esp - stack-size * 4, stack-size * 4,];
	stack-end;
	asm _ SUB (EAX, kernel-second-esp);
	asm _ ADD (EBP, EAX);
	asm _ ADD (ESP, EAX);

	-- Sets up machine status register for SYSENTER
	sch-yield [];
);

declare sch-teardown = function [] (
	sch-deallocate [head-process,];
);
