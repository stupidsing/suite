data sch-node = struct (
	dl as dl-node,
	esp as int,
	is-fork as int,
	interrupt-stack as (int * 256),
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
	asm _ CLI ();
	dl-insert-next [e +f dl, ne +f dl,];
	asm _ STI ();
);

declare sch-remove = function [e as pointer:sch-node,] (
	asm _ CLI ();
	dl-remove [e +f dl,];
	asm _ STI ();
);

declare sch-allocate = function [] (
	declare process = mm-allocate sch-node;
	let (`process`^is-fork) = 0;
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
	declare esp = current-process +f esp;
	asm _ MOV (EBX, CR3);
	asm _ PUSH (EBX);
	asm _ MOV (EAX, `EBP + -4`);
	asm _ MOV (`EAX`, ESP);

	if (`current-process`^is-fork) then (
		declare process1 = sch-allocate [];
		let (`process1`^interrupt-stack) = `current-process`^interrupt-stack;
		declare diff = process1 as int - current-process as int;
		let (`process1`^esp) = `current-process`^esp + diff;
		sch-insert-next [processes, current-process,];
		let (`current-process`^is-fork) = 0;
	) else ();

	sch-switch [];

	asm _ MOV (EAX, `EBP + -4`);
	asm _ MOV (ESP, `EAX`);
	asm _ POP (EBX);
	asm _ MOV (CR3, EBX);
);

declare sch-fork = function [f as function [] int,] (
	declare process0 = current-process;
	let (`process0`^is-fork) = 1;
	while (`process0`^is-fork) do (
		asm _ HLT ();
	);
	current-process != process0;
);

declare sch-exit = function [] (
	synchronized (& sch-sync) (
		declare process = current-process;
		declare process1 = sch-next [process,];
		sch-remove [process,];
		sch-deallocate [process,];
		current-process = process1;

		-- TODO how to give up...
	);
);

declare sch-setup = function [] (
	sch-new [processes,];
	let current-process = sch-allocate [];
	sch-insert-next [processes, current-process,];
);

declare sch-teardown = function [] (
	declare process = sch-next [processes,];
	sch-remove [process,];
	sch-deallocate [process,];
);
