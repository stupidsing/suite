declare pg = this;

declare pg-setup = [] (
	declare pt1 = pm:pm-allocate [];
	pm:pm-fill-zeroes [pt1,];

	declare offset = 0;
	for (declare physical = 0; physical < stackx; physical =+ 4096) (
		let `pt1 + offset =+ 4` = physical + 261; -- User, read/write
	);

	let `pt1` = 0; -- Trap zero pointers

	declare pt0 = pm:pm-allocate [];
	pm:pm-fill-zeroes [pt0,];

	let `pt0` = pt1 + 5; -- Maps kernel memory
	let `pt0 + 4092` = pt0 + 1; -- Maps itself
	pt0;

	asm _ MOV (CR3, EAX);
	asm _ MOV (EAX, CR0);
	asm _ OR (EAX, +x80000000);
	asm _ MOV (CR0, EAX);
	asm _ D8 (+xEB); -- JMP $ + 2
	asm _ D8 (+x00);
);

declare pg-invalidate-all = [] (
	asm _ MOV (EAX, CR3);
	asm _ MOV (CR3, EAX);
);

declare pg-invalidate-page = [logical,] (
	logical;
	asm _ INVLPG `EAX`;
);

declare pg-map = [logical, physical,] (
	declare pe0 = +xFFFFF000 + (logical ushr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical ushr 12) shl 2;
	declare pt1 = pe1 and +xFFFFF000;

	(`pe0` = 0 && (
		let `pe0` = pm:pm-allocate [] + 7;
		pg:pg-invalidate-page [pt1,];
		pm:pm-fill-zeroes [pt1,];
	));

	let `pe1` = physical + 7;

	pg:pg-invalidate-page [logical,];
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
		pm:pm-free [`pe0` and +xFFFFF000,];
		let `pe0` = 0;
	));

	pg:pg-invalidate-page [logical,];
);
