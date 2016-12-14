declare pm-sync = 0;
declare pm-ref-counts;
declare pm-page0;
declare pm-pagex;
declare pm-stackx;
declare pm-stack;

declare pm-get-number = function [page,] (
	(page - memory-region-start) ushr 12;
);

declare pm-ref = function [page,] (
	if (pm-page0 <= page && page < pm-pagex) then (
		synchronized (& pm-sync) (
			pm-ref-counts/*/:(pm-get-number [page,]) += 1;
		);
	) else -1;
);

declare pm-unref = function [page,] (
	if (pm-page0 <= page && page < pm-pagex) then (
		synchronized (& pm-sync) (
			pm-ref-counts/*/:(pm-get-number [page,]) += -1;
		);
	) else -1;
);

declare pm-allocate = function [] (
	synchronized (& pm-sync) (
		stk-pop [& pm-stack,];
	);
);

declare pm-deallocate = function [page,] (
	synchronized (& pm-sync) (
		stk-push [& pm-stack, page,];
	);
);

declare pm-setup = function [] (
	declare pagex = memory-region-start;

	-- detects memory size
	while (
		declare t = (dw-get [pagex,]) xor +xFFFFFFFF;
		dw-set [pagex, t,];
		dw-get [pagex =+ 4096,] = t;
	) do ();

	-- sets up page allocator
	declare nPages = pm-get-number [pagex,];
	declare p0 = memory-region-start;
	declare p1 = p0 + nPages * size-of int;
	declare px = p1 + nPages * size-of int;

	{pm-ref-counts} = p0 as pointer:(int * _);
	{pm-stackx} = px;
	declare page0 = (px + 4095) and +xFFFFF000;

	{pm-page0} = page0;
	{pm-pagex} = pagex;
	stk-setup [& pm-stack, p1 as pointer:(int * _),];

	while (page0 < pagex) do (
		{pm-ref-counts/*/:(pm-get-number [pagex += -4096,])} = 0;
		stk-push [& pm-stack, pagex,];
	);
);

declare pm-teardown = function [] (
);
