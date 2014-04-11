declare cs = this;
declare cs-sync = 0;
declare cs-column-offset = 0;

declare cs-set-cursor = [row, col,] (
	declare pos = row * 80 + col;
	kernel:outb [+x3D4, +x0F,];
	kernel:outb [+x3D5, pos and +xFF,];
	kernel:outb [+x3D4, +x0E,];
	kernel:outb [+x3D5, pos shr 8,];
);

declare cs-nl = [] (
	synchronized (& cs-sync) (
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
		cs:cs-set-cursor [24, cs-column-offset shr 1,];
	);
);

declare cs-put = [ch,] (
	synchronized (& cs-sync) (
		declare w = +x0700 + ch;
		kernel:copy [+xB8000 + 3840 + cs-column-offset, & w, 2,];
		(cs-column-offset += 2 = 160 && cs:cs-nl []);
	);
);

declare cs-puts = [s,] (
	declare ch = 0;
	for (declare s1 = s; (kernel:copy [& ch, s1, 1,]; ch); s1 += 1) (
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
