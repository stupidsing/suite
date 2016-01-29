constant gc-free = 0;
constant gc-allocated = 1;
constant gc-marked = 2;

constant gc-class = struct (
	+number-of-pointers as int,
	+pointer-offsets as (int * 0),
);

constant gc-node = dl-node struct (
	+flag as int,
	+size as int,
	+class as pointer:gc-class,
);
declare gc-node-size = size-of gc-node;

declare gc-sync = 0;
declare gc-current as pointer:gc-node;

declare-pointer head to gc-node;

declare gc-prev = dl-prev;
declare gc-next = dl-next;

declare gc-prepare = function [] (
	declare node = head;
	while ({node} = gc-next [node,]) do (
		node/*/+flag = gc-free || {node/*/+flag} = gc-allocated
	);
);

declare gc-mark = function [root,] (
	declare nodes = pointer:gc-node * 1024;
	declare index = 0;
	{nodes/:(index =+ 1)/*} = root;

	while (index > 0) do (
		declare node = nodes/:(index += -1)/*;

		if (node/*/+flag = gc-allocated) then (
			{node/*/+flag} = gc-marked;

			{class} = node/*/+class;
			{p} = address node + size-of gc-node;
			for i in (0, class/*/+number-of-pointers) (
				{nodes/:(index =+ 1)/*} = (p + i) as pointer:gc-node;
			);
		) else ();
	);
);

declare gc-sweep = function [] (
	declare node = head;
	while ({node} = gc-next [node,]) do (
		node/*/+flag = gc-marked || {node/*/+flag} = gc-free;
	);
);

declare gc-merge = function [] (
	declare node = head;
	while node do (
		declare node1 = gc-next [node,];
		if (node1 && node/*/+flag = gc-free && node1/*/+flag = gc-free) then (
			dl-remove [node1,];
			{node/*/+current/+size} = (gc-next [node,]) as int - node as int - gc-node-size;
		) else (
			{node} = node1;
		);
	);
);

declare gc-gc = function [root,] (
	gc-prepare [];
	gc-mark [root,];
	gc-sweep [];
	gc-merge [];
	gc-current = head;
);

declare gc-allocate0 = function [class as pointer:gc-class,] (
	declare size = class/*/+size;
	declare node = null;
	declare ok = 0;

	while (not ok) do (
		declare next;
		while ({ok} = (gc-current/*/+current/+size < size) && {next} = gc-next [gc-current,]) do (
			{gc-current} = next;
		);
		ok || gc-gc [];
	);

	declare diff = gc-current/*/+current/+size - size;
	if (diff >= gc-node-size + 4) then (
		dl-insert-next [gc-current, gc-current +offset (size + gc-node-size),];
		declare node1 = gc-next [gc-current,];
		{node1/*/+current/+size} = diff - gc-node-size;
	) else ();

	node;
);

declare gc-allocate = function [class,] (
	synchronized (& gc-sync) (
		(gc-allocate0 [class,]) as int + gc-node-size;
		zero [p, size,];
	);
);

declare gc-setup = function [] (
	constant pool = int * 1048576;
	declare p = (mm-allocate [int * 1048576,]) as int;
	head = p as pointer:gc-node;
	{head/*/+size} = p + size-of pool - size-of gc-node;
);
