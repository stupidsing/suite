constant ffa-node = dl-node struct (
	+size as int,
);
declare ffa-node-size = size-of ffa-node;

declare ffa-sync = 0;
declare ffa-region0 = +x50000;
declare ffa-regionx = +xA0000;
declare ffa-current as pointer:ffa-node;

declare-pointer head to ffa-node;

declare ffa-prev = dl-prev;
declare ffa-next = dl-next;

declare ffa-allocate0 = function [size,] (
	declare next-node = function [n,] (
		declare n1 = ffa-next [n,];
		if (n1 != null) then n1 else head;
	);

	declare start = ffa-current;
	declare next;
	while (ffa-current/*/+current/+size < size && (let next = next-node [ffa-current,]) != start) do (
		let ffa-current = next;
	);

	declare diff = ffa-current/*/+current/+size - size;

	if (diff >= 0) then (
		if (diff >= ffa-node-size + 4) then (
			dl-insert-next [ffa-current, ffa-current +offset (size + ffa-node-size),];
			let next = ffa-next [ffa-current,];
			let next/*/+current/+size = diff - ffa-node-size;
		) else ();

		let ffa-current/*/+current/+size = 0;
		ffa-current;
	) else null;
);

declare ffa-deallocate0 = function [node,] (
	declare prev = ffa-prev [node,];
	declare next = ffa-next [node,];

	if (prev != null && prev/*/+current/+size != 0) then (
		dl-remove [node,];
		let node = prev;
	) else ();

	if (next != null && next/*/+current/+size != 0) then (
		dl-remove [next,];
	) else ();

	let node/*/+current/+size = (ffa-next [node,]) as int - node as int - ffa-node-size;
);

declare ffa-allocate = function [size,] (
	synchronized (& ffa-sync) (
		(ffa-allocate0 [size,]) as int + ffa-node-size;
	);
);

declare ffa-deallocate = function [p, size as int,] (
	synchronized (& ffa-sync) (
		ffa-deallocate0 [(p - ffa-node-size) as pointer:ffa-node,];
	);
);

declare ffa-setup = function [] (
	declare first = ffa-region0 as pointer:ffa-node;
	declare last = (ffa-regionx - ffa-node-size) as pointer:ffa-node; -- Stopper record

	let first/*/+current/+size = ffa-regionx - ffa-region0 - 24;
	let last/*/+current/+size = 0;

	dl-new [head,];
	dl-insert-next [head, first,];
	dl-insert-next [first, last,];
	let ffa-current = head;
);
