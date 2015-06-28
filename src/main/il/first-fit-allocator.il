declare ffa = this;
declare ffa-sync = 0;

declare ffa-region0 = +x50000;
declare ffa-regionx = +xA0000;
declare ffa-current;

allocate head/12;

declare ffa-size0 = [e,] (
	e + 8;
);

declare ffa-allocate0 = [size,] (
	declare next-block = [block,] (
		declare n = dl:dl-next [block,];
		if (n != 0) then n else head;
	);

	declare start = ffa-current;
	declare next;
	while (((let next = this:next-block [ffa-current,]) != start) and (`ffa:ffa-size0 [ffa-current,]` < size)) do (
		let ffa-current = next;
	);

	declare diff = `ffa:ffa-size0 [ffa-current,]` - size;

	if (diff >= 0) then (
		if (diff >= 16) then (
			dl:dl-insert-next [ffa-current, ffa-current + size + 12,];
			let next = dl:dl-next [ffa-current,];
			let `ffa:ffa-size0 [next,]` = diff - 12;
		) else ();

		let `ffa:ffa-size0 [ffa-current,]` = 0;
		ffa-current + 12;
	) else 0;
);

declare ffa-deallocate0 = [block,] (
	declare prev = dl:dl-prev [block,];
	declare next = dl:dl-next [block,];

	if ((prev != 0) and (`ffa:ffa-size0 [prev,]` != 0)) then (
		dl:dl-remove [block,];
		let block = prev;
	) else ();

	if ((next != 0) and (`ffa:ffa-size0 [next,]` != 0)) then (
		dl:dl-remove [next,];
	) else ();

	let `ffa:ffa-size0 [block,]` = dl:dl-next [block,] - block - 12;
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

	let `ffa:ffa-size0 [ffa-region0,]` = ffa-regionx - ffa-region0 - 24;
	let `ffa:ffa-size0 [last,]` = 0;

	dl:dl-new [head,];
	dl:dl-insert-next [head, ffa-region0,];
	dl:dl-insert-next [ffa-region0, last,];
	let ffa-current = `head`;
);
