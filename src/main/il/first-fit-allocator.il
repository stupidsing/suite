declare ffa = this;
declare ffa-sync = 0;
allocate head/12;
declare current;

declare ffa-region0 = +x50000;
declare ffa-regionx = +xA0000;

declare ffa-allocate0 = [size,] (
	declare next-block = [block,] (
		declare n = dl:dl-next [block,];
		if (n = 0) then head else n;
	);

	declare start = current;
	declare next;
	while (((let next = this:next-block [current,]) != start) and (`current` < size)) do (
		let current = next;
	);

	declare diff = `current` - size;

	if (diff >= 0) then (
		if (diff >= 16) then (
			dl:dl-insert-next [current, current + size + 12,];
			let next = dl:dl-next [current,];
			let `next` = diff - 12;
		) else ();

		let `current` = 0;
		current + 8;
	) else 0;
);

declare ffa-deallocate0 = [block,] (
	declare prev = dl:dl-prev [block,];
	declare next = dl:dl-next [block,];

	if ((prev != 0) and (`prev` != 0)) then (
		dl:dl-remove [block,];
		let block = prev;
	) else ();

	if ((next != 0) and (`next` != 0)) then (
		dl:dl-remove [next,];
	) else ();

	let `block` = dl:dl-next [block,] - block - 12;
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

	let `ffa-region0` = ffa-regionx - ffa-region0 - 24;
	let `last` = 0;

	dl:dl-new [head,];
	dl:dl-insert-next [head, ffa-region0,];
	dl:dl-insert-next [ffa-region0, last,];
	let current = `head`;
);
