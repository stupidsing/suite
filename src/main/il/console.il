declare cs-sync = 0;
declare cs-column-offset = 0;

declare cs-set-cursor = function [row, col,] (
	declare pos = row * 80 + col;
	outb [+x3D4, +x0F,];
	outb [+x3D5, pos and +xFF,];
	outb [+x3D4, +x0E,];
	outb [+x3D5, pos ushr 8,];
);

declare cs-nl0 = function [] (
	declare p0 = +xB8000;
	copy [p0, p0 + 160, 3840,]; -- Scrolls up
	zero [p0 + 3840, 160,];

	{cs-column-offset} = 0;
	cs-set-cursor [24, cs-column-offset ushr 1,];
);

declare cs-nl = function [] (
	synchronized (& cs-sync) (
		cs-nl0 [];
	);
);

declare cs-put = function [ch,] (
	synchronized (& cs-sync) (
		declare w = +x0700 + ch;
		copy [+xB8000 + 3840 + cs-column-offset, address w, 2,];
		if (cs-column-offset += 2 = 160) then (cs-nl0 []) else ();
	);
);

declare cs-puts = function [s,] (
	declare ch;
	for (declare s1 = s; ({ch} = extend (s1 as pointer:byte)/*) != 0; s1 += 1) do (
		cs-put [ch,];
	);
);

declare cs-put-hex = function [h,] (
	{h} = h and +xF;
	cs-put [h + if (h < 10) then 48 else 55,];
);

declare cs-put-hex1 = function [h,] (
	cs-put-hex [h ushr 4,];
	cs-put-hex [h,];
);

declare cs-put-hex2 = function [h,] (
	cs-put-hex1 [h ushr 8,];
	cs-put-hex1 [h,];
);

declare cs-put-hex4 = function [h,] (
	cs-put-hex2 [h ushr 16,];
	cs-put-hex2 [h,];
);
