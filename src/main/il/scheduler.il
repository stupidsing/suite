declare sch = this;
declare sch-sync = 0;
declare current-process = 0;
allocate processes/12;

declare sch-switch = [] (-- Round robin in action
	sync:sync-try-enter [& sch-sync,] && (
		let current-process = dl:dl-next [current-process,];
		(current-process = 0 && let current-process = dl:dl-next [& processes,]);
	);
);

declare sch-interrupt-switch = [] (
	current-process;
	asm _ MOV (EBX, CR3);
	asm _ PUSH (EBX);
	asm _ MOV (`EAX + 8`, ESP);

	sch:sch-switch [];

	current-process;
	asm _ MOV (ESP, `EAX + 8`);
	asm _ POP (EBX);
	asm _ MOV (CR3, EBX);
);

declare sch-enqueue-process = [] (
	declare process = kernel:mm-allocate [12,];
	dl:dl-insert-next [& processes, process,];
	process;
);

declare sch-dequeue-process = [process,] (
	dl:dl-remove [process,];
	kernel:mm-deallocate [process, 4,];
);

declare sch-fork = [] (
	synchronized (& sch-sync) (
		declare process1 = kernel:mm-allocate [12,];
		dl:dl-insert-next [& processes, process1,];

		-- TODO allocate stack
		-- TODO how to put in ESP? mark fork flag?
	);
);

declare sch-exit = [] (
	synchronized (& sch-sync) (
		declare process = current-process;
		declare process1 = dl:dl-next [process,];
		sch:sch-dequeue-process [process,];
		current-process = process1;

		-- TODO how to give up...
	);
);

declare sch-setup = [] (
	dl:dl-new [& processes,];
	let current-process = sch:sch-enqueue-process [];
);

declare sch-teardown = [] (
	declare process = dl:dl-next [& processes,];
	dl:dl-remove [process,];
	kernel:mm-deallocate [process, 4,];
);
