constant ba-node = dl-node struct (
	+size as int,
);
declare ba-node-size = size-of ba-node;

declare ba-sync = 0;
declare lists as (ba-node * 32);

declare ba-region0 = +x50000;
declare ba-regionx = +xA0000;
declare total = ba-regionx - ba-region0;

declare exp2rel = function [l2,] (
	16 shl l2;
);

declare log2rel = function [size,] (
	declare result = 0;
	while (size >= 16) do (
		{size} = size ushr 1;
		result =+ 1;
	);
	result;
);

declare max-l2 = log2rel [total,];

declare ba-parent-of = function [block as pointer:ba-node, l2,] (
	(ba-region0 + (block as int - ba-region0) and (+xFFFFFFFF xor (exp2rel [l2,]))) as pointer:ba-node;
);

declare ba-buddy-of = function [block as pointer:ba-node, l2,] (
	(ba-region0 + (block as int - ba-region0) xor (exp2rel [l2,])) as pointer:ba-node;
);

declare ba-allocate0 = function [l2,] (
	if (l2 < max-l2) then (
		declare result = dl-next [& lists/:l2,];
		if (result = null) then (
			declare block0 = (ba-allocate0 [l2 + 1,]) as pointer:ba-node;
			if (block0 != null) then (
				declare block1 = block0 +offset (exp2rel [l2,]);
				{block0/*/+current/+size} = -1; -- Allocated
				{block1/*/+current/+size} = l2;
				dl-insert-next [& lists/:l2, block1,];
				block0;
			) else null;
		) else (
			dl-remove [result,];
			result;
		);
	) else null;
);

declare ba-deallocate0 = function [block, l2,] (
	declare buddy-block = ba-buddy-of [block, l2,];
	if (buddy-block/*/+current/+size = l2) then (
		dl-remove [buddy-block,];
		ba-deallocate0 [ba-parent-of [block, l2,], l2 + 1,];
	) else (
		{block/*/+current/+size} = l2;
		dl-insert-next [& lists/:l2, block,];
	);
);

declare ba-allocate = function [size,] (
	synchronized (& ba-sync) (
		(ba-allocate0 [log2rel [size + ba-node-size - 1,],]) as int + ba-node-size;
	);
);

declare ba-deallocate = function [block, size,] (
	synchronized (& ba-sync) (
		ba-deallocate0 [(block - ba-node-size) as pointer:ba-node, log2rel [size + ba-node-size,],];
	);
);

declare ba-setup = function [] (
	for i in (0, 32) do (
		dl-new [& lists/:i,];
		lists/:i/+current/+size = 0;
	);

	declare largest-l2 = max-l2 - 1;
	declare first = ba-region0 as pointer:ba-node;
	{first/*/+current/+size} = largest-l2; -- Granularity
	dl-insert-next [& lists/:largest-l2, first,];
);
