declare pm = this;

declare page0 = memory-region-start;
declare pagex = page0;
declare stack0 = memory-region-start;
declare stackx = stack0;

-- Detects memory size
declare pm-init = [] (
	while (
		declare t = `pagex` xor +xFFFFFFFF;
		let `pagex` = t;
		`pagex =+ 4096` = t
	) do ();

	-- Sets up page allocator
	declare stack-size = 4 * (pagex - page0) shr 12;
	declare first-page = page0 + (stack-size + 4096) and +xFFFFF000;
	for (declare page = first-page; page < pagex; page =+ 4096) (
		let `stackx =+ 4` = page;
	);
);

declare pm-allocate = [] (
	if (stack0 != stackx) then `stack0 =+ 4` else 0;
);

declare pm-free = [page,] (
	`stack0 += 4` = page;
);

declare pm-fill-zeroes = [page,] (
	for (declare offset = 0; offset < 4096; offset =+ 4) (
		let `page + offset` = 0;
	);
);
