constant ffa-node = dl-node struct (
| boolean +is-occupied
);
declare ffa-node-size = size-of ffa-node;

declare ffa-sync = 0;
declare ffa-region0 = +x50000;
declare ffa-regionx = +xA0000;
declare pointer:ffa-node ffa-current;

declare ffa-node head-node;
declare head = & head-node;

declare ffa-create = function [pointer:ffa-node node,] (
	dl-create [node,];
	{node/*/+current/+is-occupied} = false;
);

declare ffa-allocate0 = function [size,] (
	declare next-node = function [n,] (
		declare n1 = dl-next [n,];
		if n1 then n1 else head;
	);

	declare get-size = function [pointer:ffa-node node,] (
		declare next = node/*/+next;
		if (not node/*/+current/+is-occupied && next != null) then (next as int - node as int - ffa-node-size) else 0;
	);

	declare start = ffa-current;
	declare next;
	declare diff;
	while (({diff} = get-size [ffa-current,] - size) < 0 && ({next} = next-node [ffa-current,]) != start) do (
		{ffa-current} = next;
	);

	if (0 <= diff) then (
		if (ffa-node-size + 4 <= diff) then (
			dl-insert-next [ffa-current, ffa-current +offset (size + ffa-node-size),];
			{next} = dl-next [ffa-current,];
		) else ();

		{ffa-current/*/+current/+is-occupied} = true;
		ffa-current;
	) else null;
);

declare ffa-deallocate0 = function [pointer:ffa-node node,] (
	{node/*/+current/+is-occupied} = false;
	declare prev = dl-prev [node,];
	declare next = dl-next [node,];

	if (prev && not prev/*/+current/+is-occupied) then (
		dl-remove [node,];
		{node} = prev;
	) else ();

	if (next && not next/*/+current/+is-occupied) then (
		dl-remove [next,];
	) else ();
);

declare ffa-allocate = function [size,] (
	synchronized (& ffa-sync) (
		(ffa-allocate0 [size,]) as int + ffa-node-size;
	);
);

declare ffa-deallocate = function [p, int size,] (
	synchronized (& ffa-sync) (
		ffa-deallocate0 [(p - ffa-node-size) as pointer:ffa-node,];
	);
);

declare ffa-setup = function [] (
	declare first = ffa-region0 as pointer:ffa-node;
	declare last = (ffa-regionx - ffa-node-size) as pointer:ffa-node; -- Stopper record
	ffa-create [head,];
	ffa-create [first,];
	ffa-create [last,];
	dl-insert-next [head, first,];
	dl-insert-next [first, last,];
	{ffa-current} = head;
);
