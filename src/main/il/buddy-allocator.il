declare ba = this;
allocate lists/256;

declare region0 = +x50000;
declare regionx = +xA0000;
declare total = regionx - region0;

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
	region0 + (block - region0) and (+xFFFFFFFF xor ba:exp2rel [l2,]);
);

declare ba-buddy-of = [block, l2,] (
	region0 + (block - region0) xor (ba:exp2rel [l2,]);
);

declare ba-allocate0 = [l2,] (
	if (l2 < max-l2) then (
		declare result = dl:dl-next [`& lists + l2 shl 3` + 4,];
		if (result = 0) then (
			declare block0 = ba:ba-allocate0 [l2 + 1,];
			if (block0 != 0) then (
				declare block1 = block0 + ba:exp2rel [l2,];
				let `block0` = -1; -- Allocated
				let `block1` = l2;
				dl:dl-add-next [`& lists + l2 shl 3` + 4, block1,];
				block0;
			) else 0;
		) else (
			dl:dl-remove [result,];
			result;
		);
	) else 0;
);

declare ba-deallocate0 = [block, l2,] (
	declare buddy = ba:ba-buddy-of [block, l2,];
	if (`buddy` = l2) then (
		dl:dl-remove [buddy + 4,];
		ba:ba-deallocate0 [ba:ba-parent-of [block, l2,], l2 + 1,];
	) else (
		let `block` = l2;
		dl:dl-remove [block + 4,];
	);
);

declare ba-allocate = [size,] (
	ba:ba-allocate0 [ba:log2rel [size + 4,],] + 4;
);

declare ba-deallocate = [block, size,] (
	ba:ba-deallocate0 [block - 4, ba:log2rel [size + 4,],];
);

declare ba-setup = [] (
	let `region0` = ba:log2rel [total,]; -- Granularity
	dl:dl-add-prev [& lists + max-l2 shl 3, region0,];
);
