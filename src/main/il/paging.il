declare pg = this;
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

declare pg-is-mapped = [logical,] (
	declare pe0 = +xFFFFF000 + (logical ushr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical ushr 12) shl 2;
	(`pe0` && `pe1`);
);

declare pg-get-physical = [logical,] (
	declare pe1 = +xFFC00000 + (logical ushr 12) shl 2;
	`pe1` and +xFFFFF000;
);

declare pg-map = [logical, physical,] (
	declare pe0 = +xFFFFF000 + (logical ushr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical ushr 12) shl 2;
	declare pt1 = pe1 and +xFFFFF000;

	(`pe0` = 0 && (
		let `pe0` = invoke pm-allocate [] + 7;
		invoke pg-invalidate-page [pt1,];
		invoke zero [pt1, 4096,];
	));

	let `pe1` = physical + 7;

	invoke pg-invalidate-page [logical,];
);

declare pg-unmap = [logical,] (
	declare pe0 = +xFFFFF000 + (logical ushr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical ushr 12) shl 2;
	declare pt1 = pe1 and +xFFFFF000;
	declare isEmpty = 1;

	let `pe1` = 0;

	for (declare offset = 0; offset < 4096; offset =+ 4) (
		let isEmpty = isEmpty and (`pt1 + offset` = 0);
	);

	(isEmpty && (
		invoke pm-deallocate [`pe0` and +xFFFFF000,];
		let `pe0` = 0;
	));

	invoke pg-invalidate-page [logical,];
);

declare pg-allocate = [nPages,] (
	declare next-page = [page,] (
		declare n = page + 4096;
		if (n != pg-regionx) then n else pg-region0;
	);

	synchronized (& pg-sync) (
		declare initial = pg-current;
		declare r0 = 0;

		while (r0 = 0 && ((let pg-current = this:next-page [pg-current,]) != initial)) do (
			declare r1 = 1;
			for (declare i = 0; (r1 && i < nPages); i =+ 1) (
				let r1 = (r1 && not pg:pg-is-mapped [pg-current + i shl 12,]);
			);
			(r1 && let r0 = pg-current);
		);

		(r0 && (
			for (declare i = 0; i < nPages; i =+ 1) (
				pg:pg-map [r0 + i shl 12, invoke pm-allocate [],];
			);
		));

		r0;
	);
);

declare pg-deallocate = [p, nPages,] (
	synchronized (& pg-sync) (
		for (declare i = 0; i < nPages; i =+ 1) (
			declare logical = p + i shl 12;
			invoke pm-deallocate [pg:pg-get-physical [logical,],];
			pg:pg-unmap [logical,];
		);
	);
);

declare pg-setup = function [] (
	declare pt0 = invoke pm-allocate [];
	declare pt1 = invoke pm-allocate [];

	invoke zero [pt0, 4096,];
	let `pt0` = pt1 + 5; -- Maps kernel memory
	let `pt0 + 4092` = pt0 + 1; -- Maps itself

	invoke zero [pt1, 4096,];
	declare offset = 0;

	for (declare physical = 0; physical < stackx; physical =+ 4096) (
		let `pt1 + offset =+ 4` = physical + 261; -- User, read/write
	);

	let `pt1` = 0; -- Trap zero pointers

	pt0;

	asm _ MOV (CR3, EAX);
	asm _ MOV (EAX, CR0);
	asm _ OR (EAX, +x80000000);
	asm _ MOV (CR0, EAX);
	asm _ D8 (+xEB); -- JMP $ + 2
	asm _ D8 (+x00);
);
