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
	declare pe0 = +xFFFFFFFFFFFFF000 + (logical ushr 39) shl 3;
	declare pe1 = +xFFFFFFFFFFE00000 + (logical ushr 30) shl 3;
	declare pe2 = +xFFFFFFFFC0000000 + (logical ushr 21) shl 3;
	declare pe3 = +xFFFFFF8000000000 + (logical ushr 12) shl 3;
	(`pe0` && `pe1` && `pe2` && `pe3`);
);

declare pg-get-physical = [logical,] (
	declare pe3 = +xFFFFFF8000000000 + (logical ushr 12) shl 3;
	`pe3` and +xFFFFFFFFFFFFF000;
);

declare pg-set-pt-entry = [pe0, pt1,] (
	(`pe0` = 0 && (
		let `pe0` = invoke pm-allocate [] + 7;
		invoke pg-invalidate-page [pt1,];
		invoke zero [pt1, 4096,];
	));
);

declare pg-unset-pt-entry = [pe0, pt1,] (
	declare isEmpty = 1;

	for (declare offset = 0; offset < 4096; offset =+ 4) (
		let isEmpty = isEmpty and (`pt1 + offset` = 0);
	);

	(isEmpty && (
		invoke pm-deallocate [`pe0` and +xFFFFF000,];
		let `pe0` = 0;
		1;
	));
);

declare pg-map = [logical, physical,] (
	declare pe0 = +xFFFFFFFFFFFFF000 + (logical ushr 39) shl 3;
	declare pe1 = +xFFFFFFFFFFE00000 + (logical ushr 30) shl 3;
	declare pe2 = +xFFFFFFFFC0000000 + (logical ushr 21) shl 3;
	declare pe3 = +xFFFFFF8000000000 + (logical ushr 12) shl 3;
	declare pt1 = pe1 and +xFFFFFFFFFFFFF000;
	declare pt2 = pe2 and +xFFFFFFFFFFFFF000;
	declare pt3 = pe3 and +xFFFFFFFFFFFFF000;

	pg:pg-set-pt-entry [pe0, pt1,];
	pg:pg-set-pt-entry [pe1, pt2,];
	pg:pg-set-pt-entry [pe2, pt3,];
	let `pe3` = physical + 7;

	invoke pg-invalidate-page [logical,];
);

declare pg-unmap = [logical,] (
	declare pe0 = +xFFFFFFFFFFFFF000 + (logical ushr 39) shl 3;
	declare pe1 = +xFFFFFFFFFFE00000 + (logical ushr 30) shl 3;
	declare pe2 = +xFFFFFFFFC0000000 + (logical ushr 21) shl 3;
	declare pe3 = +xFFFFFF8000000000 + (logical ushr 12) shl 3;
	declare pt1 = pe1 and +xFFFFFFFFFFFFF000;
	declare pt2 = pe2 and +xFFFFFFFFFFFFF000;
	declare pt3 = pe3 and +xFFFFFFFFFFFFF000;

	let `pe3` = 0;
	pg:pg-unset-pt-entry [pe2, pt3,]
	&& pg:pg-unset-pt-entry [pe1, pt2,]
	&& pg:pg-unset-pt-entry [pe0, pt1,];

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
	declare pt2 = invoke pm-allocate [];
	declare pt3 = invoke pm-allocate [];

	invoke zero [pt0, 4096,];
	invoke zero [pt1, 4096,];
	invoke zero [pt2, 4096,];
	invoke zero [pt3, 4096,];

	let `pt0 + 4092` = pt0 + 1; -- Maps itself
	let `pt0` = pt1 + 5; -- Maps kernel memory
	let `pt1` = pt2 + 5;
	let `pt2` = pt3 + 5;

	declare offset = 0;

	for (declare physical = 0; physical < stackx; physical =+ 4096) (
		let `pt3 + offset =+ 4` = 0;
		let `pt3 + offset =+ 4` = physical + 261; -- User, read/write
	);

	let `pt3` = 0; -- Trap zero pointers

	pt0;

	asm _ MOV (CR3, EAX); -- Set page table address
	asm _ MOV (EAX, CR4); -- Enable PAE
	asm _ OR (EAX, 32);
	asm _ MOV (CR4, EAX);
	asm _ MOV (ECX, +xC0000080); -- Set long mode bit in EFER MSR
	asm _ RDMSR;
	asm _ OR (EAX, 256);
	asm _ WRMSR;
	asm _ MOV (EAX, CR0); -- Enable paging
	asm _ OR (EAX, +x80000000);
	asm _ MOV (CR0, EAX);
	asm _ D8 (+xEB); -- JMP $ + 2
	asm _ D8 (+x00);
);
