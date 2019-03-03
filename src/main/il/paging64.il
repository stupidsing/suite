declare pg-sync = 0;

declare pg-region0 = +x0000000000800000;
declare pg-regionx = +xFFFFFFFFFFC00000;
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
	declare pe0 = +xFFFFFFFFFFFFF000 + (logical ushr 39) shl 3;
	declare pe1 = +xFFFFFFFFFFE00000 + (logical ushr 30) shl 3;
	declare pe2 = +xFFFFFFFFC0000000 + (logical ushr 21) shl 3;
	declare pe3 = +xFFFFFF8000000000 + (logical ushr 12) shl 3;
	(qw-get [pe0,] != 0 && qw-get [pe1,] != 0 && qw-get [pe2,] != 0 && qw-get [pe3,] != 0);
);

declare pg-get-physical = function [logical,] (
	declare pe3 = +xFFFFFF8000000000 + (logical ushr 12) shl 3;
	(qw-get [pe3,]) and +xFFFFFFFFFFFFF000;
);

declare pg-set-pt-entry = function [pe0, pt1, pm-alloc,] (
	declare physical = qw-get [pe0,];
	if (physical = 0) then (
		qw-set [pe0, ({physical} = pm-alloc []) + 7,];
		pg-invalidate-page [pt1,];
		zero [pt1, 4096,];
	) else ();
	pm-ref [physical,];
);

declare pg-unset-pt-entry = function [pe0, pt1,] (
	declare physical = (qw-get [pe0,]) and +xFFFFFFFFFFFFF000;
	if (pm-unref [physical,] = 0) then (
		qw-set [pe0, 0,];
		pg-invalidate-page [pt1,];
		pm-deallocate [physical,];
	) else ();
);

declare pg-map = function [logical, physical,] (
	declare pe0 = +xFFFFFFFFFFFFF000 + (logical ushr 39) shl 3;
	declare pe1 = +xFFFFFFFFFFE00000 + (logical ushr 30) shl 3;
	declare pe2 = +xFFFFFFFFC0000000 + (logical ushr 21) shl 3;
	declare pe3 = +xFFFFFF8000000000 + (logical ushr 12) shl 3;
	declare pt1 = pe1 and +xFFFFFFFFFFFFF000;
	declare pt2 = pe2 and +xFFFFFFFFFFFFF000;
	declare pt3 = pe3 and +xFFFFFFFFFFFFF000;

	pg-set-pt-entry [pe0, pt1, pm-allocate,];
	pg-set-pt-entry [pe1, pt2, pm-allocate,];
	pg-set-pt-entry [pe2, pt3, pm-allocate,];
	pg-set-pt-entry [pe3, logical, function [] physical,];
);

declare pg-unmap = function [logical,] (
	declare pe0 = +xFFFFFFFFFFFFF000 + (logical ushr 39) shl 3;
	declare pe1 = +xFFFFFFFFFFE00000 + (logical ushr 30) shl 3;
	declare pe2 = +xFFFFFFFFC0000000 + (logical ushr 21) shl 3;
	declare pe3 = +xFFFFFF8000000000 + (logical ushr 12) shl 3;
	declare pt1 = pe1 and +xFFFFFFFFFFFFF000;
	declare pt2 = pe2 and +xFFFFFFFFFFFFF000;
	declare pt3 = pe3 and +xFFFFFFFFFFFFF000;

	pg-unset-pt-entry [pe3, logical,];
	pg-unset-pt-entry [pe2, pt3,];
	pg-unset-pt-entry [pe1, pt2,];
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
	declare phyx0 = (pm-stackx + +x0000007FFFFFFFFF) and +xFFFFFF8000000000;
	declare phyx1 = (pm-stackx + +x000000003FFFFFFF) and +xFFFFFFFFC0000000;
	declare phyx2 = (pm-stackx + +x00000000001FFFFF) and +xFFFFFFFFFFE00000;
	declare phyx3 = (pm-stackx + +x0000000000000FFF) and +xFFFFFFFFFFFFF000;
	declare physical = 0;

	declare pt0 = pg-acquire-pt [];
	for i0 in (0, 1024) do (
		declare entry0 = if (physical < phyx0) then (
			declare pt1 = pg-acquire-pt [];
			for i1 in (0, 1024) do (
				declare entry1 = if (physical < phyx1) then (
					declare pt2 = pg-acquire-pt [];
					for i2 in (0, 1024) do (
						declare entry2 = if (physical < phyx2) then (
							declare pt3 = pg-acquire-pt [];
							for i3 in (0, 1024) do (
								declare entry3 = if (0 < physical && physical < phyx3) then (physical + 261) else 0;
								physical += 4096;
								qw-set [pt3 + i3 shl 3, entry3,]; -- global, user, present
							);
							pt3 + 7; -- maps kernel memory, user, read/write, present
						) else 0;
						qw-set [pt1 + i2 shl 3, entry2,];
					);
					pt2 + 7; -- maps kernel memory, user, read/write, present
				) else 0;
				qw-set [pt0 + i1 shl 3, entry1,];
			);
			pt1 + 7; -- maps kernel memory, user, read/write, present
		) else if (i0 = 1023) then (
			pt0 + 1; -- maps itself
		) else 0;
		qw-set [pt0 + i0 shl 3, entry0,];
	);

	& pt0;

	asm _ MOV (EAX, `EAX`);
	asm _ MOV (CR3, EAX); -- set page table address
	asm _ MOV (EAX, CR4); -- enable PAE
	asm _ OR (EAX, 32);
	asm _ MOV (CR4, EAX);
	asm _ MOV (ECX, +xC0000080); -- set long mode bit in EFER MSR
	asm _ RDMSR ();
	asm _ OR (EAX, 256);
	asm _ WRMSR ();
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
		declare pt1 = (qw-get [pt0 + i0 shl 3,]) and +xFFFFFFFFFFFFF000;
		if (pt1 != 0) then (
			for i1 in (0, 1024) do (
				declare pt2 = (qw-get [pt1 + i1 shl 3,]) and +xFFFFFFFFFFFFF000;
				if (pt2 != 0) then (
					for i2 in (0, 1024) do (
						declare pt3 = (qw-get [pt2 + i2 shl 3,]) and +xFFFFFFFFFFFFF000;
						if (pt3 != 0) then (
							for i3 in (0, 1024) do (
								declare pt4 = (qw-get [pt3 + i3 shl 3,]) and +xFFFFFFFFFFFFF000;
								if (pt4 != 0) then (pg-release-pt [pt4,]) else ();
							);
							pg-release-pt [pt3,];
						) else ();
					);
					pg-release-pt [pt2,];
				) else ();
			);
			pg-release-pt [pt1,];
		) else ();
	);
	pg-release-pt [pt0,];
);
