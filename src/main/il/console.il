declare cs = this;
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
	copy `p`:2 = +x0700 + ch;
	if (cs-column-offset += 2 = 160) then (cs:cs-nl []) else ();
);

declare cs-puts = [s,] (
	declare ch = 0;
	for (declare s1 = s; copy ch:1 = `s1` and +xFF; s1 += 1) (
		cs:cs-put [ch,];
	);
);

declare cs-put-hex = [h,] (
	let h = h and +xF;
	cs:cs-put [h + if (h < 10) then 48 else 55,];
);

declare cs-put-hex1 = [h,] (
	cs:cs-put-hex [h shr 4,];
	cs:cs-put-hex [h,];
);

declare cs-put-hex2 = [h,] (
	cs:cs-put-hex1 [h shr 8,];
	cs:cs-put-hex1 [h,];
);

declare cs-put-hex4 = [h,] (
	cs:cs-put-hex2 [h shr 16,];
	cs:cs-put-hex2 [h,];
);
