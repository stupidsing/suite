data ba-node = struct (
	dl as dl-node,
	size as int,
);

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
		let size = size shr 1;
		result =+ 1;
	);
	result;
);

declare max-l2 = log2rel [total,];

declare ba-next = function [e as pointer:ba-node,] (
	(dl-next [e +f dl,]) as pointer:ba-node;
);

declare ba-new = function [e as pointer:ba-node,] (
	(dl-new [e +f dl,]) as pointer:ba-node;
);

declare ba-insert-next = function [e as pointer:ba-node, ne as pointer:ba-node,] (
	dl-insert-next [e +f dl, ne +f dl,];
);

declare ba-remove = function [e as pointer:ba-node,] (
	dl-remove [e +f dl,];
);

declare ba-parent-of = function [block as pointer:ba-node, l2,] (
	(ba-region0 + (block as int - ba-region0) and (+xFFFFFFFF xor (exp2rel [l2,]))) as pointer:ba-node;
);

declare ba-buddy-of = function [block as pointer:ba-node, l2,] (
	(ba-region0 + (block as int - ba-region0) xor (exp2rel [l2,])) as pointer:ba-node;
);

declare ba-allocate0 = function [l2,] (
	if (l2 < max-l2) then (
		declare result = ba-next [& lists:l2,];
		if (result = null) then (
			declare block0 = (ba-allocate0 [l2 + 1,]) as pointer:ba-node;
			if (block0 != null) then (
				declare block1 = block0 +offset (exp2rel [l2,]);
				let (`block0`^size) = -1; -- Allocated
				let (`block1`^size) = l2;
				ba-insert-next [& lists:l2, block1,];
				block0;
			) else null;
		) else (
			ba-remove [result,];
			result;
		);
	) else null;
);

declare ba-deallocate0 = function [block, l2,] (
	declare buddy-block = ba-buddy-of [block, l2,];
	if (`buddy-block`^size = l2) then (
		ba-remove [buddy-block,];
		ba-deallocate0 [ba-parent-of [block, l2,], l2 + 1,];
	) else (
		let (`block`^size) = l2;
		ba-insert-next [& lists:l2, block,];
	);
);

declare ba-allocate = function [size,] (
	synchronized (& ba-sync) (
		(ba-allocate0 [log2rel [size + 12 - 1,],]) as int + 12;
	);
);

declare ba-deallocate = function [block, size,] (
	synchronized (& ba-sync) (
		ba-deallocate0 [(block - 12) as pointer:ba-node, log2rel [size + 12,],];
	);
);

declare ba-setup = function [] (
	for i in (0, 32) (
		ba-new [& lists:i,];
		lists:i^size = 0;
	);

	declare largest-l2 = max-l2 - 1;
	declare first = ba-region0 as pointer:ba-node;
	let (`first`^size) = largest-l2; -- Granularity
	ba-insert-next [& lists:largest-l2, first,];
);
