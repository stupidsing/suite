data sch-node = struct (
	dl as dl-node,
	size as int,
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
	(mm-allocate [12,]) as pointer:sch-node;
);

declare sch-deallocate = function [e as pointer:sch-node,] (
	mm-deallocate [e as int, 12,];
);

declare sch-switch = function [] (-- Round robin in action
	sync-try-enter [& sch-sync,] && (
		let current-process = sch-next [current-process,];
		(current-process = null && let current-process = sch-next [processes,]);
	);
);

declare sch-interrupt-switch = function [] (
	current-process;
	asm _ MOV (EBX, CR3);
	asm _ PUSH (EBX);
	asm _ MOV (`EAX + 8`, ESP);

	sch-switch [];

	current-process;
	asm _ MOV (ESP, `EAX + 8`);
	asm _ POP (EBX);
	asm _ MOV (CR3, EBX);
);

declare sch-enqueue-process = function [] (
	declare process = sch-allocate [];
	sch-insert-next [processes, process,];
	process;
);

declare sch-dequeue-process = function [process as pointer:sch-node,] (
	sch-remove [process,];
	sch-deallocate [process,];
);

declare sch-fork = function [] (
	synchronized (& sch-sync) (
		declare process1 = sch-allocate [];
		sch-insert-next [processes, process1,];

		-- TODO allocate stack
		-- TODO how to put in ESP? mark fork flag?
	);
);

declare sch-exit = function [] (
	synchronized (& sch-sync) (
		declare process = current-process;
		declare process1 = sch-next [process,];
		sch-dequeue-process [process,];
		current-process = process1;

		-- TODO how to give up...
	);
);

declare sch-setup = function [] (
	sch-new [processes,];
	let current-process = sch-enqueue-process [];
);

declare sch-teardown = function [] (
	declare process = sch-next [processes,];
	sch-remove [process,];
	sch-deallocate [process,];
);
