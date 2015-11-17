declare pm-sync = 0;

declare pm-page0 = memory-region-start;
declare pm-pagex = pm-page0;
declare pm-stack0 = memory-region-start;
declare pm-stackx = pm-stack0;

declare pm-setup = function [] (

	-- Detects memory size
	while (
		declare t = (dw-get [pm-pagex,]) xor +xFFFFFFFF;
		dw-set [pm-pagex, t,];
		dw-get [pm-pagex =+ 4096,] = t;
	) do ();

	-- Sets up page allocator
	declare size = 4 * (pm-pagex - pm-page0) shr 12;
	declare first-page = pm-page0 + (size + 4096 - 1) and +xFFFFF000;
	for (declare page = first-page; page < pm-pagex; page =+ 4096) (
		dw-set [pm-stackx =+ 4, page,];
	);
);

declare pm-allocate = function [] (
	synchronized (& pm-sync) (
		if (pm-stack0 != pm-stackx) then (dw-get [pm-stack0 =+ 4,]) else 0;
	);
);

declare pm-deallocate = function [page,] (
	synchronized (& pm-sync) (
		dw-set [pm-stack0 += -4, page,];
	);
);
