declare ba-sync = 0;
declare-as-pointer lists/(int * 64);

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

declare max-l2 = invoke log2rel [total,];

declare ba$size = function [e,] (
	e + 8;
);

declare ba-parent-of = function [block, l2,] (
	ba-region0 + (block - ba-region0) and (+xFFFFFFFF xor (invoke exp2rel [l2,]));
);

declare ba-buddy-of = function [block, l2,] (
	ba-region0 + (block - ba-region0) xor (invoke exp2rel [l2,]);
);

declare ba-list-entry = function [l2,] (
	lists + l2 shl 3;
);

declare ba-allocate0 = function [l2,] (
	if (l2 < max-l2) then (
		declare result = invoke dl-next [invoke ba-list-entry [l2,],];
		if (result = 0) then (
			declare block0 = invoke ba-allocate0 [l2 + 1,];
			if (block0 != 0) then (
				declare block1 = block0 + invoke exp2rel [l2,];
				let `invoke ba$size [block0,]` = -1; -- Allocated
				let `invoke ba$size [block1,]` = l2;
				invoke dl-insert-next [invoke ba-list-entry [l2,], block1,];
				block0;
			) else 0;
		) else (
			invoke dl-remove [result,];
			result;
		);
	) else 0;
);

declare ba-deallocate0 = function [block, l2,] (
	declare buddy-block = invoke ba-buddy-of [block, l2,];
	if (`invoke ba$size [buddy-block,]` = l2) then (
		invoke dl-remove [buddy-block,];
		invoke ba-deallocate0 [invoke ba-parent-of [block, l2,], l2 + 1,];
	) else (
		let `invoke ba$size [block,]` = l2;
		invoke dl-insert-next [invoke ba-list-entry [l2,], block,];
	);
);

declare ba-allocate = function [size,] (
	synchronized (& ba-sync) (
		invoke ba-allocate0 [invoke log2rel [size + 12 - 1,],] + 12;
	);
);

declare ba-deallocate = function [block, size,] (
	synchronized (& ba-sync) (
		invoke ba-deallocate0 [block - 12, invoke log2rel [size + 12,],];
	);
);

declare ba-setup = function [] (
	invoke zero [lists, 256,];
	declare largest-l2 = max-l2 - 1;
	let `invoke ba$size [ba-region0,]` = largest-l2; -- Granularity
	invoke dl-insert-next [invoke ba-list-entry [largest-l2,], ba-region0,];
);
