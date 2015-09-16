declare pm-sync = 0;

declare page0 = memory-region-start;
declare pagex = page0;
declare stack0 = memory-region-start;
declare stackx = stack0;

declare pm-setup = function [] (

	-- Detects memory size
	while (
		declare t = `pagex` xor +xFFFFFFFF;
		let `pagex` = t;
		`pagex =+ 4096` = t;
	) do ();

	-- Sets up page allocator
	declare stack-size = 4 * (pagex - page0) shr 12;
	declare first-page = page0 + (stack-size + 4096) and +xFFFFF000;
	for (declare page = first-page; page < pagex; page =+ 4096) (
		let `stackx =+ 4` = page;
	);
);

declare pm-allocate = function [] (
	synchronized (& pm-sync) (
		if (stack0 != stackx) then `stack0 =+ 4` else 0;
	);
);

declare pm-deallocate = function [page,] (
	synchronized (& pm-sync) (
		let `stack0 += 4` = page;
	);
);
