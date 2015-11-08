data sch-node = struct (
	dl as dl-node,
	eax as int,
	eip as int,
	esp as int,
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
	asm _ MOV (EBX, CR3);
	asm _ PUSH (EBX);
	let (`current-process`^eax) = dw-get [user-mode-eax,];
	let (`current-process`^eip) = dw-get [user-mode-eip,];
	let (`current-process`^esp) = dw-get [user-mode-esp,];

	sch-switch [];

	dw-set [user-mode-esp, `current-process`^esp,];
	dw-set [user-mode-eip, `current-process`^eip,];
	dw-set [user-mode-eax, `current-process`^eax,];
	asm _ POP (EBX);
	asm _ MOV (CR3, EBX);
);

declare sch-enqueue-process = function [] (
	declare process = sch-allocate [];
	sch-insert-next [processes, process,];
	process;
);

declare sch-dequeue-process = function [process,] (
	sch-remove [process,];
	sch-deallocate [process,];
);

declare sch-forker = function [] (
	declare a as int;
	asm _ MOV (`EBP + -4`, EAX);
);

declare sch-fork = function [] (
	synchronized (& sch-sync) (
		declare process1 = sch-enqueue-process [];

		-- TODO allocate stack
		-- TODO how to put in ESP? mark fork flag?
		mm-allocate sch-node;
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
