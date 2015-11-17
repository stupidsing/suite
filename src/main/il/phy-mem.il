declare pm-sync = 0;
declare pm-stackx;

declare pm-stack = memory-region-start as pointer:(int * 0);
declare pm-stack-pointer = 0;

declare pm-allocate = function [] (
	synchronized (& pm-sync) (
		if (pm-stack-pointer != 0) then (
			pm-stack-pointer += -1;
			`pm-stack`:pm-stack-pointer;
		) else 0;
	);
);

declare pm-deallocate = function [page,] (
	synchronized (& pm-sync) (
		let `pm-stack`:pm-stack-pointer = page;
		pm-stack-pointer =+ 1;
	);
);

declare pm-setup = function [] (
	declare pagex = memory-region-start;

	-- Detects memory size
	while (
		declare t = (dw-get [pagex,]) xor +xFFFFFFFF;
		dw-set [pagex, t,];
		dw-get [pagex =+ 4096,] = t;
	) do ();

	-- Sets up page allocator
	declare nPages = (pagex - memory-region-start) shr 12;
	let pm-stackx = address `pm-stack`:nPages;
	while (pagex > memory-region-start) do (
		let `pm-stack`:pm-stack-pointer = pagex += -4096;
		pm-stack-pointer =+ 1;
	);
);
