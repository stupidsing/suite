declare ffa-sync = 0;

declare ffa-region0 = +x50000;
declare ffa-regionx = +xA0000;
declare ffa-current;

declare-pointer head to (int * 3);

declare ffa$size = function [e,] (
	e + 8;
);

declare ffa-allocate0 = function [size,] (
	declare next-block = function [block,] (
		declare n = dl-next [block,];
		if (n != 0) then n else head;
	);

	declare start = ffa-current;
	declare next;
	while ((let next = next-block [ffa-current,]) != start && `ffa$size [ffa-current,]` < size) do (
		let ffa-current = next;
	);

	declare diff = `ffa$size [ffa-current,]` - size;

	if (diff >= 0) then (
		if (diff >= 16) then (
			dl-insert-next [ffa-current, ffa-current + size + 12,];
			let next = dl-next [ffa-current,];
			let `ffa$size [next,]` = diff - 12;
		) else ();

		let `ffa$size [ffa-current,]` = 0;
		ffa-current + 12;
	) else 0;
);

declare ffa-deallocate0 = function [block,] (
	declare prev = dl-prev [block,];
	declare next = dl-next [block,];

	if (prev != 0 && `ffa$size [prev,]` != 0) then (
		dl-remove [block,];
		let block = prev;
	) else ();

	if (next != 0 && `ffa$size [next,]` != 0) then (
		dl-remove [next,];
	) else ();

	let `ffa$size [block,]` = dl-next [block,] - block - 12;
);

declare ffa-allocate = function [size,] (
	synchronized (& ffa-sync) (
		ffa-allocate0 [size,] + 12;
	);
);

declare ffa-deallocate = function [p, size,] (
	synchronized (& ffa-sync) (
		ffa-deallocate0 [p - 12,];
	);
);

declare ffa-setup = function [] (
	declare last = ffa-regionx - 12; -- Stopper record

	let `ffa$size [ffa-region0,]` = ffa-regionx - ffa-region0 - 24;
	let `ffa$size [last,]` = 0;

	dl-new [head,];
	dl-insert-next [head, ffa-region0,];
	dl-insert-next [ffa-region0, last,];
	let ffa-current = `head`;
);
