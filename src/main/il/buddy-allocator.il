declare ba = this;
declare ba-sync = 0;
declare-as-pointer lists/(int * 64);

declare ba-region0 = +x50000;
declare ba-regionx = +xA0000;
declare total = ba-regionx - ba-region0;

declare exp2rel = [l2,] (
	16 shl l2;
);

declare log2rel = [size,] (
	declare result = 0;
	while (size >= 16) do (
		let size = size shr 1;
		result =+ 1;
	);
	result;
);

declare max-l2 = ba:log2rel [total,];

declare ba$size = [e,] (
	e + 8;
);

declare ba-parent-of = [block, l2,] (
	ba-region0 + (block - ba-region0) and (+xFFFFFFFF xor ba:exp2rel [l2,]);
);

declare ba-buddy-of = [block, l2,] (
	ba-region0 + (block - ba-region0) xor (ba:exp2rel [l2,]);
);

declare ba-list-entry = [l2,] (
	lists + l2 shl 3;
);

declare ba-allocate0 = [l2,] (
	if (l2 < max-l2) then (
		declare result = dl:dl-next [ba:ba-list-entry [l2,],];
		if (result = 0) then (
			declare block0 = ba:ba-allocate0 [l2 + 1,];
			if (block0 != 0) then (
				declare block1 = block0 + ba:exp2rel [l2,];
				let `ba:ba$size [block0,]` = -1; -- Allocated
				let `ba:ba$size [block1,]` = l2;
				dl:dl-insert-next [ba:ba-list-entry [l2,], block1,];
				block0;
			) else 0;
		) else (
			dl:dl-remove [result,];
			result;
		);
	) else 0;
);

declare ba-deallocate0 = [block, l2,] (
	declare buddy-block = ba:ba-buddy-of [block, l2,];
	if (`ba:ba$size [buddy-block,]` = l2) then (
		dl:dl-remove [buddy-block,];
		ba:ba-deallocate0 [ba:ba-parent-of [block, l2,], l2 + 1,];
	) else (
		let `ba:ba$size [block,]` = l2;
		dl:dl-insert-next [ba:ba-list-entry [l2,], block,];
	);
);

declare ba-allocate = [size,] (
	synchronized (& ba-sync) (
		ba:ba-allocate0 [ba:log2rel [size + 12 - 1,],] + 12;
	);
);

declare ba-deallocate = [block, size,] (
	synchronized (& ba-sync) (
		ba:ba-deallocate0 [block - 12, ba:log2rel [size + 12,],];
	);
);

declare ba-setup = [] (
	kernel:zero [lists, 256,];
	declare largest-l2 = max-l2 - 1;
	let `ba:ba$size [ba-region0,]` = largest-l2; -- Granularity
	dl:dl-insert-next [ba:ba-list-entry [largest-l2,], ba-region0,];
);
