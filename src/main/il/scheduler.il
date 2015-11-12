constant stack-size = 250;
constant sch-action-none = 0;
constant sch-action-exit = 1;
constant sch-action-fork = 2;

data sch-node = struct (
	dl as dl-node,
	esp as int,
	action as int,
	kernel-stack as (int * stack-size),
);

declare current-process = null;
declare head-process as pointer:sch-node;

declare sch-prev = function [e as pointer:sch-node,] (
	(dl-prev [e +f dl,]) as pointer:sch-node;
);

declare sch-next = function [e as pointer:sch-node,] (
	(dl-next [e +f dl,]) as pointer:sch-node;
);

declare sch-insert-next = function [e as pointer:sch-node, ne as pointer:sch-node,] (
	dl-insert-next [e +f dl, ne +f dl,];
);

declare sch-remove = function [e as pointer:sch-node,] (
	dl-remove [e +f dl,];
);

declare sch-allocate = function [] (
	declare process = mm-allocate sch-node;
	dl-new [process +f dl,];
	let `process`/action = sch-action-none;
	process;
);

declare sch-deallocate = function [e,] (
	mm-deallocate sch-node e;
);

declare sch-switch = function [] (-- Round robin in action
	let current-process = sch-next [current-process,];
	(current-process = null && let current-process = head-process);
);

declare sch-interrupt-switch = function [] (
	asm _ FR-PUSHN 4;
	asm _ PUSHF ();
	asm _ CLI ();

	current-process +f esp;
	asm _ MOV (EBX, CR3);
	asm _ FR-PUSH (EBX);
	asm _ MOV (`EAX`, ESP);

	(`current-process`/action = sch-action-exit && (
		declare process0 = current-process;
		let current-process = sch-prev [current-process,];
		sch-remove [process0,];
		sch-deallocate [process0,];
	));

	(`current-process`/action = sch-action-fork && (
		declare process1 = sch-allocate [];
		let `process1`/kernel-stack = `current-process`/kernel-stack;
		let `process1`/esp = `current-process`/esp - current-process as int + process1 as int;
		sch-insert-next [head-process, process1,];
		let `current-process`/action = sch-action-none;
	));

	sch-switch [];
	wrmsr [+x175, current-process as int + size-of sch-node,]; -- SYSENTER ESP

	current-process +f esp;
	asm _ MOV (ESP, `EAX`);
	asm _ FR-POP (EBX);
	asm _ MOV (CR3, EBX);

	asm _ POPF ();
	asm _ FR-POPN 4;
);

declare sch-fork = function [] (
	declare process0 = current-process;
	let `process0`/action = sch-action-fork;
	sch-interrupt-switch [];
	current-process != process0;
);

declare sch-exit = function [] (
	let `current-process`/action = sch-action-exit;
	sch-interrupt-switch [];
);

declare sch-setup = function [] (
	let head-process = sch-allocate [];
	let current-process = head-process;

	-- Uses the process stack
	declare stack-end = current-process as int + size-of sch-node;
	copy [stack-end - stack-size * 4, kernel-second-esp - stack-size * 4, stack-size * 4,];
	stack-end;
	asm _ SUB (EAX, kernel-second-esp);
	asm _ ADD (EBP, EAX);
	asm _ ADD (ESP, EAX);

	-- Sets up machine status register for SYSENTER
	sch-interrupt-switch [];
);

declare sch-teardown = function [] (
	sch-deallocate [head-process,];
);
