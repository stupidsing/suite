data ffa-node = struct (
	dl as dl-node,
	size as int,
);

declare ffa-sync = 0;

declare ffa-region0 = +x50000;
declare ffa-regionx = +xA0000;
declare ffa-current;

declare-pointer head to ffa-node;

declare ffa-prev = function [e as pointer:ffa-node,] (
	(dl-prev [e +f dl,]) as pointer:ffa-node;
);

declare ffa-next = function [e as pointer:ffa-node,] (
	(dl-next [e +f dl,]) as pointer:ffa-node;
);

declare ffa-new = function [e as pointer:ffa-node,] (
	(dl-new [e +f dl,]) as pointer:ffa-node;
);

declare ffa-insert-next = function [e as pointer:ffa-node, ne as pointer:ffa-node,] (
	dl-insert-next [e +f dl, ne +f dl,];
);

declare ffa-remove = function [e as pointer:ffa-node,] (
	dl-remove [e +f dl,];
);

declare ffa-allocate0 = function [size,] (
	declare next-block = function [block,] (
		declare n = ffa-next [block,];
		if (n != null) then n else head;
	);

	declare start = ffa-current;
	declare next;
	while ((let next = next-block [ffa-current,]) != start && `ffa-current`^size < size) do (
		let ffa-current = next;
	);

	declare diff = `ffa-current`^size - size;

	if (diff >= 0) then (
		if (diff >= 16) then (
			ffa-insert-next [ffa-current, ffa-current +offset (size + 12),];
			let next = ffa-next [ffa-current,];
			let (`next`^size) = diff - 12;
		) else ();

		let (`ffa-current`^size) = 0;
		ffa-current +offset 12;
	) else null;
);

declare ffa-deallocate0 = function [block,] (
	declare prev = ffa-prev [block,];
	declare next = ffa-next [block,];

	if (prev != null && `prev`^size != 0) then (
		ffa-remove [block,];
		let block = prev;
	) else ();

	if (next != null && `next`^size != 0) then (
		ffa-remove [next,];
	) else ();

	let (`block`^size) = (ffa-next [block,]) as int - block as int - 12;
);

declare ffa-allocate = function [size,] (
	synchronized (& ffa-sync) (
		(ffa-allocate0 [size,]) as int + 12;
	);
);

declare ffa-deallocate = function [p, size,] (
	synchronized (& ffa-sync) (
		ffa-deallocate0 [(p - 12) as pointer:ffa-node,];
	);
);

declare ffa-setup = function [] (
	declare first = ffa-region0 as pointer:ffa-node;
	declare last = (ffa-regionx - 12) as pointer:ffa-node; -- Stopper record

	let (`first`^size) = ffa-regionx - ffa-region0 - 24;
	let (`last`^size) = 0;

	ffa-new [head,];
	ffa-insert-next [head, first,];
	ffa-insert-next [first, last,];
	let ffa-current = head;
);
