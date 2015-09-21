declare sch-sync = 0;
declare current-process = 0;
declare-pointer processes to (int * 3);

declare sch-switch = function [] (-- Round robin in action
	sync-try-enter [& sch-sync,] && (
		let current-process = dl-next [current-process,];
		(current-process = 0 && let current-process = dl-next [processes,]);
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
	declare process = mm-allocate [12,];
	dl-insert-next [processes, process,];
	process;
);

declare sch-dequeue-process = function [process,] (
	dl-remove [process,];
	mm-deallocate [process, 4,];
);

declare sch-fork = function [] (
	synchronized (& sch-sync) (
		declare process1 = mm-allocate [12,];
		dl-insert-next [processes, process1,];

		-- TODO allocate stack
		-- TODO how to put in ESP? mark fork flag?
	);
);

declare sch-exit = function [] (
	synchronized (& sch-sync) (
		declare process = current-process;
		declare process1 = dl-next [process,];
		sch-dequeue-process [process,];
		current-process = process1;

		-- TODO how to give up...
	);
);

declare sch-setup = function [] (
	dl-new [processes,];
	let current-process = sch-enqueue-process [];
);

declare sch-teardown = function [] (
	declare process = dl-next [processes,];
	dl-remove [process,];
	mm-deallocate [process, 4,];
);
