constant stack-size = 256;
constant sch-action-none = 0;
constant sch-action-exit = 1;
constant sch-action-fork = 2;

data sch-node = struct (
	dl as dl-node,
	esp as int,
	action as int,
	interrupt-stack as (int * stack-size),
);

declare sch-sync = 0;
declare current-process = null;
declare-pointer processes to sch-node;

declare sch-prev = function [e as pointer:sch-node,] (
	(dl-prev [e +f dl,]) as pointer:sch-node;
);

declare sch-next = function [e as pointer:sch-node,] (
	(dl-next [e +f dl,]) as pointer:sch-node;
);

declare sch-new = function [e as pointer:sch-node,] (
	(dl-new [e +f dl,]) as pointer:sch-node;
);

declare sch-insert-next = function [e as pointer:sch-node, ne as pointer:sch-node,] (
	dl-insert-next [e +f dl, ne +f dl,];
);

declare sch-remove = function [e as pointer:sch-node,] (
	dl-remove [e +f dl,];
);

declare sch-allocate = function [] (
	declare process = mm-allocate sch-node;
	let (`process`^action) = sch-action-none;
	process;
);

declare sch-deallocate = function [e,] (
	mm-deallocate sch-node e;
);

declare sch-switch = function [] (-- Round robin in action
	sync-try-enter [& sch-sync,] && (
		let current-process = sch-next [current-process,];
		(current-process = null && let current-process = sch-next [processes,]);
	);
);

declare sch-interrupt-switch = function [] (
	current-process +f esp;
	asm _ MOV (EBX, CR3);
	asm _ PUSH (EBX);
	asm _ MOV (`EAX`, ESP);

	(`current-process`^action = sch-action-exit && (
		declare process0 = current-process;
		let current-process = sch-prev [current-process,];
		sch-remove [process0,];
		sch-deallocate [process0,];
	));

	(`current-process`^action = sch-action-fork && (
		declare process1 = sch-allocate [];
		let (`process1`^interrupt-stack) = `current-process`^interrupt-stack;
		let (`process1`^esp) = `current-process`^esp - current-process as int + process1 as int;
		sch-insert-next [processes, current-process,];
		let (`current-process`^action) = sch-action-none;
	));

	sch-switch [];

	current-process +f esp;
	asm _ MOV (ESP, `EAX`);
	asm _ POP (EBX);
	asm _ MOV (CR3, EBX);
);

declare sch-yield = function [] (
	asm _ HLT ();
);

declare sch-fork = function [] (
	declare process0 = current-process;
	let (`process0`^action) = sch-action-fork;
	while (`process0`^action = sch-action-fork) do (
		sch-yield [];
	);
	current-process != process0;
);

declare sch-exit = function [] (
	let (`current-process`^action) = sch-action-exit;
	while 1 do (
		sch-yield [];
	);
);

declare sch-setup = function [] (
	sch-new [processes,];
	let current-process = sch-allocate [];

	-- Uses the process stack
	declare stack-end = current-process as int + size-of sch-node;
	copy [stack-end - stack-size * 4, kernel-second-esp - stack-size * 4, stack-size * 4,];
	stack-end;
	asm _ SUB (EAX, kernel-second-esp);
	asm _ ADD (EBP, EAX);
	asm _ ADD (ESP, EAX);

	sch-insert-next [processes, current-process,];
);

declare sch-teardown = function [] (
	declare process = sch-next [processes,];
	sch-remove [process,];
	sch-deallocate [process,];
);
