-- Mark-and-sweep garbage collection
constant gc-free = 0;
constant gc-allocated = 1;
constant gc-marked = 2;

constant gc-class = struct (
	number-of-pointers as int,
	pointer-offsets as (int * 0),
);

constant gc-node = dl-node struct (
	flag as int,
	size as int,
	class as pointer:gc-class,
);
declare gc-node-size = size-of gc-node;

declare gc-sync = 0;
declare gc-current;

declare-pointer head to gc-node;

declare gc-prev = dl-prev;
declare gc-next = dl-next;

declare gc-prepare = function [] (
	declare node = head;
	while (let node = gc-next [node,]) do (
		`node`/flag = gc-free || let `node`/flag = gc-allocated
	);
);

declare gc-mark = function [root,] (
	declare nodes = pointer:gc-node * 1024;
	declare index = 0;
	let `nodes:(index =+ 1)` = root;

	while (index > 0) do (
		declare node = `nodes:(index += -1)`;

		if (`node`/flag = gc-allocated) then (
			let `node`/flag = gc-marked;

			let class = `node`/class;
			let p = address node + size-of gc-node;
			for i in (0, `class`/number-of-pointers) (
				let `nodes:(index =+ 1)` = (p + i) as pointer:gc-node;
			);
		) else ();
	);
);

declare gc-sweep = function [] (
	declare node = head;
	while (let node = gc-next [node,]) do (
		`node`/flag = gc-marked || let `node`/flag = gc-free;
	);
);

declare gc-merge = function [] (
	declare node = head;
	while (let node = gc-next [node,]) do (
		declare node1 = gc-next [node,];
		if (node1 && `node`/flag = gc-free && `node1`/flag = gc-free) then (
			dl-remove [node1,];
			let `node`/current/size = (gc-next [node,]) as int - node as int - gc-node-size;
		) else ();
	);
);

declare gc-gc = function [root,] (
	synchronized (& gc-sync) (
		gc-prepare [];
		gc-mark [root,];
		gc-sweep [];
		gc-merge [];
		gc-current = head;
	);
);

declare gc-allocate = function [class as pointer:gc-class,] (
	declare size = `class`/size;
	declare next = null;

	while (next = null) do (
		declare size = `class`/size;
		while (let next = gc-next [gc-current,]) do (let gc-current = next);
		next || gc-gc [];
	);

	declare p = next +offset ffa-node-size;
	zero [p, size,];
	p;
);

declare gc-setup = function [] (
);
