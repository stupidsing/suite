constant gc-free = 0;
constant gc-allocated = 1;
constant gc-marked = 2;

constant gc-class = struct (
| int +size
| int +number-of-pointers
| (int * 0) +pointer-offsets
| (function [gc-node, int, pointer:gc-node * 0,] int) +get-pointers
);

constant gc-node = ll-node struct (
| int +flag
| pointer:gc-class +class
);

declare gc-sync = 0;
declare pointer:gc-node gc-current;

declare-pointer head to gc-node;

declare gc-next = ll-next;

declare gc-node-size = size-of gc-node;

declare gc-create = function [node, class,] (
	sl-create [node,];
	{node/*/+flag} = gc-free;
	{node/*/+class} = class;
);

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

	while (0 < index) do (
		declare node = nodes/:(index += -1)/*;

		if (node/*/+flag = gc-allocated) then (
			{node/*/+flag} = gc-marked;
			declare class = node/*/+class;
			if class then (
				{index} = class/*/+get-pointers [node, index, nodes,];
			) else ();
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

declare gc-allocate0 = function [pointer:gc-class class,] (
	declare get-size = function [node,] (
		declare next = node/*/+next;
		if next then (next as int - node as int - gc-node-size) else 0;
	);

	declare size = class/*/+size;
	declare node = null;
	declare diff;

	while (not ok) do (
		declare next;
		while ({ok} = ((diff = get-size [gc-current,] - size) < 0) && {next} = gc-next [gc-current,]) do (
			{gc-current} = next;
		);
		ok || gc-gc [];
	);

	if (gc-node-size + 4 <= diff) then (
		declare node1 = (gc-current +offset (size + gc-node-size)) as pointer:gc-node;
		gc-create [node1, class,];
		ll-insert-next [gc-current, node1,];
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
	declare tail = (buffer + size - gc-node-size) as pointer:gc-node;
	gc-create [head, null,];
	gc-create [tail, null,];
	ll-insert-next [head, tail,];
);

declare gc-setup = function [] (
	constant pool = int * 1048576;
	declare p = (mm-allocate [pool,]) as int;
	gc-setup0 [p as pointer:gc-node, size-of pool,];
);
