declare cs-sync = 0;
declare cs-column-offset = 0;

declare cs-set-cursor = function [row, col,] (
	declare pos = row * 80 + col;
	outb [+x3D4, +x0F,];
	outb [+x3D5, pos and +xFF,];
	outb [+x3D4, +x0E,];
	outb [+x3D5, pos shr 8,];
);

declare cs-nl = function [] (
	synchronized (& cs-sync) (
		declare p0 = +xB8000;
		declare p1 = p0 + 3840;
		declare px = p0 + 4000;

		for (declare p = p0; p < p1; p =+ 4) ( -- Scrolls up
			let `p` = `p + 160`;
		);

		for (declare p = p1; p < px; p =+ 4) (
			let `p` = 0;
		);

		let cs-column-offset = 0;
		cs-set-cursor [24, cs-column-offset shr 1,];
	);
);

declare cs-put = function [ch,] (
	synchronized (& cs-sync) (
		declare w = +x0700 + ch;
		copy [+xB8000 + 3840 + cs-column-offset, & w, 2,];
		(cs-column-offset += 2 = 160 && cs-nl []);
	);
);

declare cs-puts = function [s,] (
	declare ch = 0;
	for (declare s1 = s; (copy [& ch, s1, 1,]; ch); s1 += 1) (
		cs-put [ch,];
	);
);

declare cs-put-hex = function [h,] (
	let h = h and +xF;
	cs-put [h + if (h < 10) then 48 else 55,];
);

declare cs-put-hex1 = function [h,] (
	cs-put-hex [h shr 4,];
	cs-put-hex [h,];
);

declare cs-put-hex2 = function [h,] (
	cs-put-hex1 [h shr 8,];
	cs-put-hex1 [h,];
);

declare cs-put-hex4 = function [h,] (
	cs-put-hex2 [h shr 16,];
	cs-put-hex2 [h,];
);
