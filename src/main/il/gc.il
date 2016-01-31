constant gc-free = 0;
constant gc-allocated = 1;
constant gc-marked = 2;

constant gc-class = struct (
	+number-of-pointers as int,
	+pointer-offsets as (int * 0),
	+get-pointers as function [gc-node, int, pointer:gc-node * 0,] int,
);

constant gc-node = ll-node struct (
	+flag as int,
	+size as int,
	+class as pointer:gc-class,
);
declare gc-node-size = size-of gc-node;

declare gc-sync = 0;
declare gc-current as pointer:gc-node;

declare-pointer head to gc-node;

declare gc-next = ll-next;

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
			{index} = node/*/+class/*/+get-pointers [node, index, nodes,];
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
			ll-remove-next [node,];
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
		ll-insert-next [gc-current, gc-current +offset (size + gc-node-size),];
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

declare gc-setup0 = function [buffer, size,] (
	head = buffer as pointer:gc-node;
	declare tail = (buffer + size - size-of gc-node) as pointer:gc-node;
	ll-create [head,];
	ll-create [tail,];
	ll-insert-next [head, tail,];
);

declare gc-setup = function [] (
	constant pool = int * 1048576;
	declare p = (mm-allocate [int * 1048576,]) as int;
	gc-setup0 [p as pointer:gc-node, size-of pool,];
);
