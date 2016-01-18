constant gc-free = 0;
constant gc-allocated = 1;
constant gc-marked = 2;

constant gc-class = struct (
	size as int,
	number-of-pointers as int,
	pointer-offsets as (int * 0),
);

constant gc-node = dl-node struct (
	flag as int,
	class as pointer:gc-class,
);

declare gc-sync = 0;
declare gc-current;

declare-pointer head to gc-node;

declare gc-prev = dl-prev;
declare gc-next = dl-next;

declare gc-mark = function [root,] (
	(
		declare node = gc-current;

		while (let node = gc-next [node,]) do (
			`node`/flag = gc-marked && let `node`/flag = gc-allocated
		);
	);

	declare nodes = pointer:gc-node * 1024;
	declare n = 0;
	let `nodes:(n =+ 1)` = root;

	while (n > 0) do (
		declare node = `nodes:(n += -1)`;

		(`node`/flag = gc-allocated && (
			let `node`/flag = gc-marked;

			let class = `node`/class;
			let p = address node + size-of gc-node;
			for i in (0, `class`/number-of-pointers) (
				let `nodes:(n =+ 1)` = (p + i) as pointer:gc-node;
			);
		));
	);
);

declare gc-sweep = function [root,] (
	declare node = gc-current;

	while (let node = gc-next [node,]) do (
		(`node`/flag = gc-allocated && let `node`/flag = gc-free);
	);
);

declare gc-merge = function [root,] (
	declare node = gc-current;

	while (let node = gc-next [node,]) do (
		declare node1 = gc-next [node,];
		(node1
			&& `node`/flag = gc-allocated
			&& `node1`/flag = gc-allocated
			&& dl-remove [node1,]);
	);
);

declare gc-gc = function [root,] (
	gc-mark [root,];
	gc-sweep [];
	gc-merge [];
);

declare gc-allocate = function [class,] (
	0;
);

declare gc-deallocate = function [class, object,] (
	0;
);
