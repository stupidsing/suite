declare pg-sync = 0;

declare pg-region0 = +x00800000;
declare pg-regionx = +xFFC00000;
declare pg-current = pg-region0;

declare pg-invalidate-all = function [] (
	asm _ MOV (EAX, CR3);
	asm _ MOV (CR3, EAX);
);

declare pg-invalidate-page = function [logical,] (
	logical;
	asm _ INVLPG `EAX`;
);

declare pg-is-mapped = function [logical,] (
	declare pe0 = +xFFFFF000 + (logical ushr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical ushr 12) shl 2;
	(dw-get [pe0,] != 0 && dw-get [pe1,] != 0);
);

declare pg-get-physical = function [logical,] (
	declare pe1 = +xFFC00000 + (logical ushr 12) shl 2;
	(dw-get [pe1,]) and +xFFFFF000;
);

declare pg-set-pt-entry = function [pe0, pt1, pm-alloc, is-zero,] (
	declare physical = dw-get [pe0,];
	if (physical = 0) then (
		dw-set [pe0, ({physical} = pm-alloc []) + 7,];
		pg-invalidate-page [pt1,];
		if is-zero then (zero [pt1, 4096,]) else ();
	) else ();
	pm-ref [physical,];
);

declare pg-unset-pt-entry = function [pe0, pt1,] (
	declare physical = (dw-get [pe0,]) and +xFFFFF000;
	if (pm-unref [physical,] = 0) then (
		dw-set [pe0, 0,];
		pg-invalidate-page [pt1,];
		pm-deallocate [physical,];
	) else ();
);

declare pg-map = function [logical, physical,] (
	declare pe0 = +xFFFFF000 + (logical ushr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical ushr 12) shl 2;
	declare pt1 = pe1 and +xFFFFF000;

	pg-set-pt-entry [pe0, pt1, pm-allocate, true,];
	pg-set-pt-entry [pe1, logical, function [] physical, false,];
);

declare pg-unmap = function [logical,] (
	declare pe0 = +xFFFFF000 + (logical ushr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical ushr 12) shl 2;
	declare pt1 = pe1 and +xFFFFF000;

	pg-unset-pt-entry [pe1, logical,];
	pg-unset-pt-entry [pe0, pt1,];
);

declare pg-acquire-pt = function [] (
	declare pt = pm-allocate [];
	pm-ref [pt,];
	pt;
);

declare pg-release-pt = function [pt,] (
	if (pm-unref [pt,] = 0) then (pm-deallocate [pt,]) else ();
);

declare pg-allocate = function [nPages,] (
	declare next-page = function [page,] (
		declare n = page + 4096;
		if (n != pg-regionx) then n else pg-region0;
	);

	synchronized (& pg-sync) (
		declare initial = pg-current;
		declare page = 0;

		while (page = 0 && ({pg-current} = next-page [pg-current,]) != initial) do (
			declare b = true;
			for (declare i = 0; (b && i < nPages); i =+ 1) do (
				{b} = (b && not pg-is-mapped [pg-current + i shl 12,]);
			);
			if b then ({page} = pg-current) else ();
		);

		if (page != 0) then (
			for i in (0, nPages) do (
				declare logical = page + i shl 12;
				declare physical = pg-acquire-pt [];
				pg-map [logical, physical,];
			);
		) else ();

		page;
	);
);

declare pg-deallocate = function [p, nPages,] (
	synchronized (& pg-sync) (
		for i in (0, nPages) do (
			declare logical = p + i shl 12;
			declare physical = pg-get-physical [logical,];
			pg-unmap [logical,];
			pg-release-pt [physical,];
		);
	);
);

declare pg-setup = function [] (
	declare phyx0 = (pm-stackx + +x003FFFFF) and +xFFC00000;
	declare phyx1 = (pm-stackx + +x00000FFF) and +xFFFFE000;
	declare physical = 0;

	declare pt0 = pg-acquire-pt [];
	for i0 in (0, 1024) do (
		declare entry0 = if (physical < phyx0) then (
			declare pt1 = pg-acquire-pt [];
			for i1 in (0, 1024) do (
				declare entry1 = if (0 < physical && physical < phyx1) then (physical + 261) else 0;
				physical += 4096;
				dw-set [pt1 + i1 shl 2, entry1,]; -- global, user, present
			);
			pt1 + 7; -- maps kernel memory, user, read/write, present
		) else if (i0 = 1023) then (
			pt0 + 1; -- maps itself
		) else 0;
		dw-set [pt0 + i0 shl 2, entry0,];
	);

	& pt0;

	asm _ MOV (EAX, `EAX`);
	asm _ MOV (CR3, EAX); -- set page table address
	asm _ MOV (EAX, CR0); -- enable paging
	asm _ OR (EAX, +x80000000);
	asm _ MOV (CR0, EAX);
	asm _ IMM (BYTE +xEB); -- JMP $ + 2
	asm _ IMM (BYTE +x00);

	pg-map [ap-local-apic-address, ap-local-apic-address,];
);

declare pg-teardown = function [] (
	pg-unmap [ap-local-apic-address,];

	signature pt0 = int;
	& pt0;

	asm _ MOV (EBX, CR3); -- get page table address
	asm _ MOV (`EAX`, EBX);
	asm _ MOV (EAX, CR0); -- disable paging
	asm _ AND (EAX, +x7FFFFFFF);
	asm _ MOV (CR0, EAX);
	asm _ IMM (BYTE +xEB); -- JMP $ + 2
	asm _ IMM (BYTE +x00);

	for i0 in (0, 1024) do (
		declare pt1 = (dw-get [pt0 + i0 shl 2,]) and +xFFFFF000;
		if (pt1 != 0) then (
			for i1 in (0, 1024) do (
				declare pt2 = (dw-get [pt1 + i1 shl 2,]) and +xFFFFF000;
				if (pt2 != 0) then (pg-release-pt [pt2,]) else ();
			);
			pg-release-pt [pt1,];
		) else ();
	);
	pg-release-pt [pt0,];
);

