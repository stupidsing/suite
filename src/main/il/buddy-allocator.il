declare ba = this;
declare ba-sync = 0;
allocate lists/256;

declare ba-region0 = +x50000;
declare ba-regionx = +xA0000;
declare total = ba-regionx - ba-region0;

declare exp2rel = [l2,] (
	16 shl l2;
);

declare log2rel = [size,] (
	size =+ -1;
	declare result = 0;
	while (size > 16) do (
		let size = size shr 1;
		result =+ 1;
	);
	result;
);

declare max-l2 = ba:log2rel [total,] + 1;

declare ba-parent-of = [block, l2,] (
	ba-region0 + (block - ba-region0) and (+xFFFFFFFF xor ba:exp2rel [l2,]);
);

declare ba-buddy-of = [block, l2,] (
	ba-region0 + (block - ba-region0) xor (ba:exp2rel [l2,]);
);

declare ba-list-entry = [l2,] (
	& lists + l2 shl 3;
);

declare ba-allocate0 = [l2,] (
	if (l2 < max-l2) then (
		declare result = dl:dl-next [ba:ba-list-entry [l2,],];
		if (result = 0) then (
			declare block0 = ba:ba-allocate0 [l2 + 1,];
			if (block0 != 0) then (
				declare block1 = block0 + ba:exp2rel [l2,];
				let `block0` = -1; -- Allocated
				let `block1` = l2;
				dl:dl-add-next [ba:ba-list-entry [l2,], block1,];
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
	if (`buddy-block` = l2) then (
		dl:dl-remove [buddy-block,];
		ba:ba-deallocate0 [ba:ba-parent-of [block, l2,], l2 + 1,];
	) else (
		let `block` = l2;
		dl:dl-add-next [ba:ba-list-entry [l2,], block,];
	);
);

declare ba-allocate = [size,] (
	synchronized (& ba-sync) (
		ba:ba-allocate0 [ba:log2rel [size + 4,],] + 4;
	);
);

declare ba-deallocate = [block, size,] (
	synchronized (& ba-sync) (
		ba:ba-deallocate0 [block - 4, ba:log2rel [size + 4,],];
	);
);

declare ba-setup = [] (
	kernel:zero [& lists, 256,];
	let `ba-region0` = ba:log2rel [total,]; -- Granularity
	dl:dl-add-next [ba:ba-list-entry [max-l2,], ba-region0,];
);
