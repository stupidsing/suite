constant gc-none = 0;
constant gc-marked = 1;

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

declare ffa-prev = dl-prev;
declare ffa-next = dl-next;

declare gc-mark = function [root,] (
	declare start = gc-current;
	declare next = start;

	while ((let next = gc-next [next,]) != start) do (
		let `next`/flag = gc-none;
	);

	declare nodes = pointer:gc-node * 1024;
	declare n = 0;
	let `nodes:(n =+ 1)` = root;

	while (n > 0) do (
		let node = `nodes:(n += -1)`;

		(`node`/flag = gc-none && (
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
	declare start = gc-current;
	declare next = start;

	while ((let next = gc-next [next,]) != start) do (
		(`node`/flag = gc-none && dl-remove [next,]);
	);
);

declare gc-gc = function [root,] (
	gc-mark [root,];
	gc-sweep [];
);

declare gc-allocate = function [class,] (
	0;
);

declare gc-deallocate = function [class, object,] (
	0;
);
