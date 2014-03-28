declare cs-this = this;
declare cs-column-offset = 0;

declare cs-nl = [] (
	declare p0 = +xB8000;
	declare p1 = +xB8000 + 3840;
	declare px = +xB8000 + 4000;

	for (declare p = p0; p < p1; p =+ 4) ( -- Scrolls up
		let `p` = `p + 160`;
	);

	for (declare p = p1; p < px; p =+ 4) (
		let `p` = 0;
	);

	let cs-column-offset = 0;
);

declare cs-put = [ch,] (
	declare p = +xB8000 + 3840 + cs-column-offset;
	let `p` = `p` and +xFFFF0000 + +x0700 + ch;
	if (cs-column-offset += 2 = 160) then (invoke cs-this cs-nl []) else ();
);

declare cs-put-hex = [h,] (
	invoke cs-this cs-put [h + if (h < 10) then 48 else 55,];
);

declare cs-put-hex1 = [h,] (
	invoke cs-this cs-put-hex [h shr 4,];
	invoke cs-this cs-put-hex [h and +xF,];
);

declare cs-put-hex2 = [h,] (
	invoke cs-this cs-put-hex1 [h shr 8,];
	invoke cs-this cs-put-hex1 [h and +xFF,];
);

declare cs-put-hex4 = [h,] (
	invoke cs-this cs-put-hex2 [h shr 16,];
	invoke cs-this cs-put-hex2 [h and +xFFFF,];
);
