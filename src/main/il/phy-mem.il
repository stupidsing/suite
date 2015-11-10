declare pm-sync = 0;

declare page0 = memory-region-start;
declare pagex = page0;
declare stack0 = memory-region-start;
declare stackx = stack0;

declare pm-setup = function [] (

	-- Detects memory size
	while (
		declare t = (dw-get [pagex,]) xor +xFFFFFFFF;
		dw-set [pagex, t,];
		dw-get [pagex =+ 4096,] = t;
	) do ();

	-- Sets up page allocator
	declare size = 4 * (pagex - page0) shr 12;
	declare first-page = page0 + (size + 4096) and +xFFFFF000;
	for (declare page = first-page; page < pagex; page =+ 4096) (
		dw-set [stackx =+ 4, page,];
	);
);

declare pm-allocate = function [] (
	synchronized (& pm-sync) (
		if (stack0 != stackx) then (dw-get [stack0 =+ 4,]) else 0;
	);
);

declare pm-deallocate = function [page,] (
	synchronized (& pm-sync) (
		dw-set [stack0 += 4, page,];
	);
);
