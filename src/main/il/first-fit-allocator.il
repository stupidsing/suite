declare ffa = this;
declare ffa-sync = 0;

declare ffa-region0 = +x50000;
declare ffa-regionx = +xA0000;
declare ffa-current;

declare-as-pointer head/(int * 3);

declare ffa$size = [e,] (
	e + 8;
);

declare ffa-allocate0 = [size,] (
	declare next-block = [block,] (
		declare n = invoke dl-next [block,];
		if (n != 0) then n else head;
	);

	declare start = ffa-current;
	declare next;
	while (((let next = this:next-block [ffa-current,]) != start) and (`ffa:ffa$size [ffa-current,]` < size)) do (
		let ffa-current = next;
	);

	declare diff = `ffa:ffa$size [ffa-current,]` - size;

	if (diff >= 0) then (
		if (diff >= 16) then (
			invoke dl-insert-next [ffa-current, ffa-current + size + 12,];
			let next = invoke dl-next [ffa-current,];
			let `ffa:ffa$size [next,]` = diff - 12;
		) else ();

		let `ffa:ffa$size [ffa-current,]` = 0;
		ffa-current + 12;
	) else 0;
);

declare ffa-deallocate0 = [block,] (
	declare prev = invoke dl-prev [block,];
	declare next = invoke dl-next [block,];

	if ((prev != 0) and (`ffa:ffa$size [prev,]` != 0)) then (
		invoke dl-remove [block,];
		let block = prev;
	) else ();

	if ((next != 0) and (`ffa:ffa$size [next,]` != 0)) then (
		invoke dl-remove [next,];
	) else ();

	let `ffa:ffa$size [block,]` = invoke dl-next [block,] - block - 12;
);

declare ffa-allocate = [size,] (
	synchronized (& ffa-sync) (
		ffa:ffa-allocate0 [size,] + 12;
	);
);

declare ffa-deallocate = [p, size,] (
	synchronized (& ffa-sync) (
		ffa:ffa-deallocate0 [p - 12,];
	);
);

declare ffa-setup = [] (
	declare last = ffa-regionx - 12; -- Stopper record

	let `ffa:ffa$size [ffa-region0,]` = ffa-regionx - ffa-region0 - 24;
	let `ffa:ffa$size [last,]` = 0;

	invoke dl-new [head,];
	invoke dl-insert-next [head, ffa-region0,];
	invoke dl-insert-next [ffa-region0, last,];
	let ffa-current = `head`;
);
