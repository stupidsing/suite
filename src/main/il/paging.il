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
	(dw-get [pe0,] && dw-get [pe1,]);
);

declare pg-get-physical = function [logical,] (
	declare pe1 = +xFFC00000 + (logical ushr 12) shl 2;
	(dw-get [pe1,]) and +xFFFFF000;
);

declare pg-set-pt-entry = function [pe0, pt1, pm-alloc,] (
	declare physical = dw-get [pe0,];
	(physical = 0 && (
		dw-set [pe0, (let physical = pm-alloc []) + 7,];
		pg-invalidate-page [pt1,];
		zero [pt1, 4096,];
	));
	pm-ref [physical,];
);

declare pg-unset-pt-entry = function [pe0, pt1,] (
	declare physical = (dw-get [pe0,]) and +xFFFFF000;
	(pm-unref [physical,] || (
		dw-set [pe0, 0,];
		pg-invalidate-page [pt1,];
		pm-deallocate [physical,];
	));
);

declare pg-map = function [logical, physical,] (
	declare pe0 = +xFFFFF000 + (logical ushr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical ushr 12) shl 2;
	declare pt1 = pe1 and +xFFFFF000;

	pg-set-pt-entry [pe0, pt1, pm-allocate,];
	pg-set-pt-entry [pe1, logical, function [] physical,];
);

declare pg-unmap = function [logical,] (
	declare pe0 = +xFFFFF000 + (logical ushr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical ushr 12) shl 2;
	declare pt1 = pe1 and +xFFFFF000;

	pg-unset-pt-entry [pe1, logical,];
	pg-unset-pt-entry [pe0, pt1,];
);

declare pg-allocate = function [nPages,] (
	declare next-page = function [page,] (
		declare n = page + 4096;
		if (n != pg-regionx) then n else pg-region0;
	);

	synchronized (& pg-sync) (
		declare initial = pg-current;
		declare r0 = 0;

		while (r0 = 0 && ((let pg-current = next-page [pg-current,]) != initial)) do (
			declare r1 = 1;
			for (declare i = 0; (r1 && i < nPages); i =+ 1) (
				let r1 = (r1 && not pg-is-mapped [pg-current + i shl 12,]);
			);
			(r1 && let r0 = pg-current);
		);

		(r0 && (
			for i in (0, nPages) (
				declare logical = r0 + i shl 12;
				declare physical = pm-allocate [];
				pm-ref [physical,];
				pg-map [logical, physical,];
			);
		));

		r0;
	);
);

declare pg-deallocate = function [p, nPages,] (
	synchronized (& pg-sync) (
		for i in (0, nPages) (
			declare logical = p + i shl 12;
			declare physical = pg-get-physical [logical,];
			pg-unmap [logical,];
			pm-unref [physical,];
		);
	);
);

declare pg-setup = function [] (
	declare pt0 = pm-allocate [];
	declare pt1 = pm-allocate [];

	pm-ref [pt0,];
	pm-ref [pt1,];

	zero [pt0, 4096,];
	dw-set [pt0, pt1 + 5,]; -- Maps kernel memory
	dw-set [pt0 + 4092, pt0 + 1,]; -- Maps itself

	zero [pt1, 4096,];
	declare offset = 0;

	for (declare physical = 0; physical < pm-stackx; physical =+ 4096) (
		dw-set [pt1 + offset =+ 4, physical + 261,]; -- User, read/write
	);

	dw-set [pt1, 0,]; -- Trap zero pointers

	& pt0;

	asm _ MOV (EAX, `EAX`);
	asm _ MOV (CR3, EAX); -- Set page table address
	asm _ MOV (EAX, CR0); -- Enable paging
	asm _ OR (EAX, +x80000000);
	asm _ MOV (CR0, EAX);
	asm _ D8 (+xEB); -- JMP $ + 2
	asm _ D8 (+x00);
);

declare pg-teardown = function [] (
	declare pt0 as int;
	& pt0;

	asm _ MOV (EBX, CR3); -- Get page table address
	asm _ MOV (`EAX`, EBX);
	asm _ MOV (EAX, CR0); -- Disable paging
	asm _ AND (EAX, +x7FFFFFFF);
	asm _ MOV (CR0, EAX);
	asm _ D8 (+xEB); -- JMP $ + 2
	asm _ D8 (+x00);

	declare pt1 = (dw-get [pt0,]) and +xFFFFF000;

	(pm-unref [pt1,] || pm-deallocate [pt1,]);
	(pm-unref [pt0,] || pm-deallocate [pt0,]);
);
