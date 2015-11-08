data sch-node = struct (
	dl as dl-node,
	esp as int,
	interrupt-stack-address as (int * 256),
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
	mm-allocate sch-node;
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

	sch-switch [];

	asm _ MOV (EAX, `EBP + -4`);
	asm _ MOV (ESP, `EAX`);
	asm _ POP (EBX);
	asm _ MOV (CR3, EBX);
);

declare sch-forker = baseless [] (
	declare a as (int * 2);
	asm _ MOV (EAX, `EBP + 4`);
	asm _ MOV (`EBP + -8`, EAX);
	asm _ MOV (EAX, `EBP + 8`);
	asm _ MOV (`EBP + -4`, EAX);

	(a as (function [] int)) [];

	-- TODO
	sch-remove [current-process,];
	--sch-deallocate [current-process,]; -- Concurrency issue?
	asm _ HLT ();
);

-=
declare sch-fork = function [f as function [],] (
	synchronized (& sch-sync) (
		declare stack1 = mm-allocate (int * 256);
		declare esp1 = stack1 as int + 4 * 256 - 8;
		copy [esp1, (& f) as int, 8,];

		declare process1 = sch-allocate [];
		`process1`^esp = esp1;
		sch-insert-next [processes, process1,];
		0;
	);
);
=-

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
