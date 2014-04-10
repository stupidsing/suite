-- Reference http://www.jbox.dk/sanos/source/sys/dev/ne2000.c.html

declare ne2k = this;

expand io-base = +x300;
expand mem-base = 16384;
expand mem-size = 16384;

allocate ne2k-mac-address/8;
declare ne2k-rx-page-start;
declare ne2k-rx-page-stop;

declare ne2k-read = [src, dst, len,] (
	let len = (len + 1) and +xFFFFFFFE;
	kernel:outb [io-base + +x00, +x22,];
	kernel:outb [io-base + +x0A, len,];
	kernel:outb [io-base + +x0B, len shr 8,];
	kernel:outb [io-base + +x08, src,];
	kernel:outb [io-base + +x09, src shr 8,];
	kernel:outb [io-base + +x00, +x0A,];

	for (declare p = 0; p < len; p =+ 1) (
		declare b = kernel:inw [io-base + +x10,];
		kernel:copy [dst + p, & b, 1,];
	);
);

declare ne2k-send = [buffer, len,] (
	let len = (len + 1) and +xFFFFFFFE;
	kernel:outb [io-base + +x00, +x22,];
	kernel:outb [io-base + +x04, +x40,];
	kernel:outb [io-base + +x0A, len,];
	kernel:outb [io-base + +x0B, len shr 8,];
	--kernel:outb [io-base + +x08, ne2k-rx-page-stop,];
	--kernel:outb [io-base + +x09, dst,];
);

declare ne2k-probe = [] (
	kernel:outb [io-base + +x1F, kernel:inb [io-base + +x1F,],];
	kernel:outb [io-base + +x00, +x21,];

	kernel:sleep [100,];

	-- Tests for a generic DP8390 NIC
	(+x27 and kernel:inb [io-base + +x00,] = +x21
		&& +x80 and kernel:inb [io-base + +x07,] = +x80
	);
);

declare ne2k-setup = [] (
	(ne2k:ne2k-probe [] && (
		kernel:outb [io-base + +x00, +x21,];
		kernel:outb [io-base + +x0E, +x49,];

		allocate romdata/16;
		ne2k:ne2k-read [0, & romdata, 16,];
		kernel:copy [& ne2k-mac-address, & romdata, 6,];

		ne2k-rx-page-start = mem-base shr 8;
		ne2k-rx-page-stop = ne2k-rx-page-start + mem-size shr 8 - 12;

		cs:cs-puts ["MAC ADDRESS = ",];

		for (declare p = 0; p < 6; p =+ 1) (
			declare b;
			kernel:copy [& b, & ne2k-mac-address + p, 1,];
			cs:cs-put-hex1 [b,];
		);

		cs:cs-nl [];
	));
);
