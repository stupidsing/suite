constant gc-free = 0;
constant gc-allocated = 1;
constant gc-marked = 2;

constant gc-node_ .gc-class = fix :gc-node struct (
| pointer::gc-node next
| int flag
| pointer:.gc-class class
);

constant gc-class = fix :gc-class struct (
| int size
| (function [pointer:(stack pointer:(gc-node_ :gc-class)), pointer:(gc-node_ :gc-class),] int) +get-pointers
);

declare gc-class-class = new gc-class (
	size = size-of gc-class,
	+get-pointers = function [stack, p,] ( 0; ),
);

constant gc-node = gc-node_ gc-class;
declare gc-node-size = size-of gc-node;

declare gc-sync = 0;
signature gc-current = pointer:gc-node;
signature gc-head = pointer:gc-node;
signature gc-last-allocate = pointer:gc-node;

declare gc-prepare = function [] (
	declare node = gc-head;
	declare node1;
	while ({node1} = node/*/next) do (
		if (node/*/flag != gc-free) then (
			{node/*/flag} = gc-allocated;
		) else ();
		{node} = node1;
	);
);

declare gc-mark = function [root,] (
	signature nodes = pointer:gc-node * 1024;
	signature stack = stack pointer:gc-node;
	stk-setup [& stack, & nodes,];
	stk-push [& stack, root,];

	while (not (stk-is-empty [& stack,])) do (
		declare node = stk-pop [& stack,];

		if (node/*/flag = gc-allocated) then (
			{node/*/flag} = gc-marked;
			declare class = node/*/class;
			if class then (
				class/*/+get-pointers [& stack, node,];
			) else ();
		) else ();
	);
);

declare gc-sweep = function [] (
	declare node = gc-head;
	declare node1;

	while ({node1} = node/*/next) do (
		if (node/*/flag != gc-marked) then (
			{node/*/flag} = gc-free;
			while ({node1} = node/*/next && node1/*/flag != gc-marked) do (
				{node/*/next} = node1/*/next;
			);
		) else ();
		{node} = node1;
	);
);

declare gc-gc = function [root,] (
	gc-prepare [];
	gc-mark [root,];
	gc-sweep [];
	{gc-current} = gc-head;
);

declare gc-allocate0 = function [pointer:gc-class class,] (
	declare get-size = function [pointer:gc-node node,] (
		declare next = node/*/next;
		if (next && node/*/flag = gc-free) then (next as int - node as int - gc-node-size) else 0;
	);

	declare size = class/*/size;
	declare node = null;
	declare ok = false;
	declare diff;

	while (not ok) do (
		declare next;
		while (not ({ok} = 0 < ({diff} = get-size [gc-current,] - size)) && ({next} = gc-current/*/next) != null) do (
			{gc-current} = next;
		);
		if (not ok) then (
			gc-gc [null,]; -- TODO how to find root?
		) else ();
	);

	if (gc-node-size + 4 <= diff) then (
		declare node1 = (gc-current +offset (size + gc-node-size)) as pointer:gc-node;
		{node1/*} = new gc-node (next = gc-current/*/next, flag = gc-free, class = null,);
		{gc-current/*/next} = node1;
		node1;
	) else gc-current;
);

declare gc-allocate = function [pointer:gc-class class,] (
	synchronized (& gc-sync) (
		declare p = ({gc-last-allocate} = gc-allocate0 [class,]) as int + gc-node-size;
		zero [p, class/*/size,];
		p;
	);
);

declare gc-setup0 = function [buffer, size,] (
	{gc-current} = {gc-head} = buffer as pointer:gc-node;
	declare tail = (buffer + size - gc-node-size) as pointer:gc-node;
	{gc-current/*} = new gc-node (next = tail, flag = gc-free, class = null,);
	{tail/*} = new gc-node (next = null, flag = gc-marked, class = null,);
);

declare gc-setup = function [] (
	constant pool = int * 16000;
	declare p = (mm-allocate pool) as int;
	gc-setup0 [p, size-of pool,];
);

declare gc-teardown = function [] (
);
