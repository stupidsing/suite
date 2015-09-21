declare ba-sync = 0;
declare-pointer lists to (int * 64);

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

declare ba$size = function [e,] (
	e + 8;
);

declare ba-parent-of = function [block, l2,] (
	ba-region0 + (block - ba-region0) and (+xFFFFFFFF xor (exp2rel [l2,]));
);

declare ba-buddy-of = function [block, l2,] (
	ba-region0 + (block - ba-region0) xor (exp2rel [l2,]);
);

declare ba-list-entry = function [l2,] (
	lists + l2 shl 3;
);

declare ba-allocate0 = function [l2,] (
	if (l2 < max-l2) then (
		declare result = dl-next [ba-list-entry [l2,],];
		if (result = 0) then (
			declare block0 = ba-allocate0 [l2 + 1,];
			if (block0 != 0) then (
				declare block1 = block0 + exp2rel [l2,];
				let `ba$size [block0,]` = -1; -- Allocated
				let `ba$size [block1,]` = l2;
				dl-insert-next [ba-list-entry [l2,], block1,];
				block0;
			) else 0;
		) else (
			dl-remove [result,];
			result;
		);
	) else 0;
);

declare ba-deallocate0 = function [block, l2,] (
	declare buddy-block = ba-buddy-of [block, l2,];
	if (`ba$size [buddy-block,]` = l2) then (
		dl-remove [buddy-block,];
		ba-deallocate0 [ba-parent-of [block, l2,], l2 + 1,];
	) else (
		let `ba$size [block,]` = l2;
		dl-insert-next [ba-list-entry [l2,], block,];
	);
);

declare ba-allocate = function [size,] (
	synchronized (& ba-sync) (
		ba-allocate0 [log2rel [size + 12 - 1,],] + 12;
	);
);

declare ba-deallocate = function [block, size,] (
	synchronized (& ba-sync) (
		ba-deallocate0 [block - 12, log2rel [size + 12,],];
	);
);

declare ba-setup = function [] (
	zero [lists, 256,];
	declare largest-l2 = max-l2 - 1;
	let `ba$size [ba-region0,]` = largest-l2; -- Granularity
	dl-insert-next [ba-list-entry [largest-l2,], ba-region0,];
);
