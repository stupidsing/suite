-- Reference http://www.jbox.dk/sanos/source/sys/dev/ne2000.c.html

declare ne2k = this;

expand iobase = +x300;

allocate ne2k-mac-address/8;

declare ne2k-probe = [] (
	kernel:outb [iobase + +x1F, kernel:inb [iobase + +x1F,],];
	kernel:outb [iobase + +x00, +x21,];

	kernel:sleep [100,];

	-- Tests for a generic DP8390 NIC
	(+x27 and kernel:inb [iobase + +x00,] = +x21
		&& +x80 and kernel:inb [iobase + +x07,] = +x80
	);
);

declare ne2k-read = [src, dst, len,] (
	let len = (len + 1) and +xFFFFFFFE;
	kernel:outb [iobase + +x00, +x22,];
	kernel:outb [iobase + +x0A, len,];
	kernel:outb [iobase + +x0B, len shr 8,];
	kernel:outb [iobase + +x08, src,];
	kernel:outb [iobase + +x09, src shr 8,];
	kernel:outb [iobase + +x00, +x0A,];
	for (declare p = 0; p < len; p =+ 1) (
		declare b = kernel:inw [iobase + +x10,];
		kernel:copy [dst + p, & b, 1,];
	);
);

declare ne2k-setup = [] (
	(ne2k:ne2k-probe [] && (
		kernel:outb [iobase + +x00, +x21,];
		kernel:outb [iobase + +x0E, +x49,];

		allocate romdata/16;
		ne2k:ne2k-read [0, & romdata, 16,];
		kernel:copy [& ne2k-mac-address, & romdata, 6,];

		cs:cs-puts ["MAC ADDRESS = ",];

		for (declare p = 0; p < 6; p =+ 1) (
			declare b;
			kernel:copy [& b, & ne2k-mac-address + p, 1,];
			cs:cs-put-hex1 [b,];
		);

		cs:cs-nl [];
	));
);
