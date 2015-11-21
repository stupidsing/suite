declare pm-sync = 0;
declare pm-ref-counts;
declare pm-stack;
declare pm-stackx;
declare pm-stack-pointer = 0;

declare pm-get-number = function [page,] (
	(page - memory-region-start) shr 12;
);

declare pm-ref = function [page,] (
	synchronized (& pm-sync) (
		`pm-ref-counts`:(pm-get-number [page,]) += 1;
	);
);

declare pm-unref = function [page,] (
	synchronized (& pm-sync) (
		`pm-ref-counts`:(pm-get-number [page,]) += -1;
	);
);

declare pm-allocate = function [] (
	synchronized (& pm-sync) (
		if (pm-stack-pointer != 0) then (
			`pm-stack`:(pm-stack-pointer += -1);
		) else 0;
	);
);

declare pm-deallocate = function [page,] (
	synchronized (& pm-sync) (
		let `pm-stack`:(pm-stack-pointer =+ 1) = page;
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

	declare nPages = pm-get-number [pagex,];
	let pm-ref-counts = memory-region-start as pointer:(int * 0);
	let pm-stack = (address `pm-ref-counts`:nPages) as pointer:(int * 0);
	let pm-stackx = address `pm-stack`:nPages;

	-- Sets up page allocator
	while (pagex > memory-region-start) do (
		let `pm-ref-counts`:(pm-get-number [pagex,]) = 0;
		let `pm-stack`:(pm-stack-pointer =+ 1) = pagex += -4096;
	);
);
