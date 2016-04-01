constant gc-free = 0;
constant gc-allocated = 1;
constant gc-marked = 2;

constant gc-node_ .gc-class = ll-node struct (
| int +flag
| pointer:.gc-class +class
);

constant gc-class = fix :gc-class struct (
| int +size
| int +number-of-pointers
| (int * 0) +pointer-offsets
| (function [pointer:(stack pointer:(gc-node_ :gc-class)), pointer:(gc-node_ :gc-class),] int) +get-pointers
);

constant gc-node = gc-node_ gc-class;
declare gc-node-size = size-of gc-node;

declare gc-sync = 0;
declare pointer:gc-node gc-current;
declare pointer:gc-node head;

declare gc-create = function [pointer:gc-node node, class,] (
	ll-create [node,];
	{node/*/+current/+flag} = gc-free;
	{node/*/+current/+class} = class;
);

declare gc-prepare = function [] (
	declare node = head;
	while ({node} = ll-next [node,]) do (
		if (node/*/+current/+flag != gc-free) then (
			{node/*/+current/+flag} = gc-allocated
		) else ();
	);
);

declare gc-mark = function [root,] (
	declare (pointer:gc-node * 1024) nodes;
	declare (stack pointer:gc-node) stack;
	stk-setup [& stack, & nodes,];
	stk-push [& stack, root,];

	while (not (stk-is-empty [& stack,])) do (
		declare node = stk-pop [& stack,];

		if (node/*/+current/+flag = gc-allocated) then (
			{node/*/+current/+flag} = gc-marked;
			declare class = node/*/+current/+class;
			if class then (
				class/*/+get-pointers [& stack, node,];
			) else ();
		) else ();
	);
);

declare gc-sweep = function [] (
	declare node = head;
	while ({node} = ll-next [node,]) do (
		if (node/*/+current/+flag != gc-marked) then (
			{node/*/+current/+flag} = gc-free;
		) else ();
	);
);

declare gc-merge = function [] (
	declare node = head;
	while node do (
		declare node1 = ll-next [node,];
		if (node1 && node/*/+current/+flag = gc-free && node1/*/+current/+flag = gc-free) then (
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
	declare get-size = function [pointer:gc-node node,] (
		declare next = node/*/+next;
		if next then (next as int - node as int - gc-node-size) else 0;
	);

	declare size = class/*/+size;
	declare node = null;
	declare diff;
	declare ok = false;

	while (not ok) do (
		declare next;
		while ({ok} = (({diff} = get-size [gc-current,] - size) < 0) && ({next} = ll-next [gc-current,]) != null) do (
			{gc-current} = next;
		);
		if (not ok) then (
			gc-gc [null,]; -- TODO how to find root?
		) else ();
	);

	if (gc-node-size + 4 <= diff) then (
		declare node1 = (gc-current +offset (size + gc-node-size)) as pointer:gc-node;
		gc-create [node1, class,];
		ll-insert-next [gc-current, node1,];
		node1;
	) else null;
);

declare gc-allocate = function [pointer:gc-class class,] (
	synchronized (& gc-sync) (
		declare p = (gc-allocate0 [class,]) as int + gc-node-size;
		zero [p, class/*/+size,];
		p;
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
	declare p = (mm-allocate pool) as int;
	gc-setup0 [p, size-of pool,];
);
