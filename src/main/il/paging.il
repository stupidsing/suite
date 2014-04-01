declare pg-this = this;

declare pg-setup = [] (
	declare spt = invoke pg-this pm-allocate [];
	invoke pg-this pm-fill-zeroes [spt,];

	declare offset = 0;
	for (declare physical = 0; physical < stackx; physical =+ 4096) (
		let `spt + offset =+ 4` = physical + 261; -- Supervisor, read-only
	);

	let `spt` = 0; -- Trap zero pointers

	declare rpt = invoke pg-this pm-allocate [];
	invoke pg-this pm-fill-zeroes [rpt,];

	let `rpt` = spt + 5; -- Maps kernel memory
	let `rpt + 4092` = rpt + 1; -- Maps itself
	rpt;

	asm _ D8 (+x0F); -- MOV CR3, EAX
	asm _ D8 (+x22);
	asm _ D8 (+xD8);
	asm _ D8 (+x0F); -- MOV EAX, CR0
	asm _ D8 (+x20);
	asm _ D8 (+xC0);
	asm _ OR (EAX, +x80000000);
	asm _ D8 (+x0F); -- MOV CR0, EAX
	asm _ D8 (+x22);
	asm _ D8 (+xC0);
	asm _ D8 (+xEB); -- JMP $ + 2
	asm _ D8 (+x00);
);

declare invalidate-page = [logical,] (
	logical;
	asm _ INVLPG `EAX`;
);

declare pg-map = [logical, physical,] (
	declare pe0 = +xFFFFF000 + (logical shr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical shr 12) shl 2;
	declare p1 = pe1 and +xFFFFF000;

	if (`pe0` = 0) then (
		let `pe0` = pm-allocate [] + 7;
		pm-fill-zeroes [p1,];
	) else ();

	let `pe1` = physical + 7;

	invoke pg-this invalidate-page [logical,];
);

declare pg-unmap = [logical,] (
	declare pe0 = +xFFFFF000 + (logical shr 22) shl 2;
	declare pe1 = +xFFC00000 + (logical shr 12) shl 2;
	declare p1 = pe1 and +xFFFFF000;
	declare isEmpty = 1;

	let `pe1` = 0;

	for (declare offset = 0; offset < 4096; offset =+ 4) (
		let isEmpty = isEmpty and (`p1 + offset` = 0);
	);

	if (isEmpty) then (
		pm-free [`pe0` and +xFFFFFFFC,];
		let `pe0` = 0;
	) else ();

	invoke pg-this invalidate-page [logical,];
);
