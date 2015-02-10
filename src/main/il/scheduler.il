declare sch = this;
declare current-process = 0;
allocate processes/8;

declare sch-setup = [] (
	let current-process = ba:ba-allocate [4,];
	dl:dl-new [& processes,];
	dl:dl-insert-next [& processes, current-process,];
);

declare sch-teardown = [] (
	declare process = dl:dl-next [& processes,];
	dl:dl-remove [process,];
	ba:ba-deallocate [process, 4,];
);

declare sch-switch = [] (
	current-process;
	asm _ MOV (EBX, CR3);
	asm _ PUSH (EBX);
	asm _ MOV (`EAX`, ESP);

	-- Round robin in action
	let current-process = dl:dl-next [current-process,];
	(current-process = 0 && let current-process = dl:dl-next [& processes,]);

	current-process;
	asm _ MOV (ESP, `EAX`);
	asm _ POP (EBX);
	asm _ MOV (CR3, EBX);
);
