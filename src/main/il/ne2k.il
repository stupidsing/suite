-- Reference http://www.jbox.dk/sanos/source/sys/dev/ne2000.c.html

declare ne2k = this;

expand iobase = +x300;

declare ne2k-probe = [] (
	kernel:out [iobase + +x1F, kernel:in [iobase + +x1F,],];
	kernel:out [iobase + +x00, +x21,];

	kernel:sleep [100,];

	-- Tests for a generic DP8390 NIC
	(+x27 and kernel:in [iobase + +x00,] = +x21
		&& +x80 and kernel:in [iobase + +x07,] = +x80
	);
);

declare ne2k-read = [src, dst, len,] (
	let len = (len + 1) and +xFFFFFFFE;
	kernel:out [iobase + +x00, +x22,];
	kernel:out [iobase + +x0A, len,];
	kernel:out [iobase + +x0B, len shr 8,];
	kernel:out [iobase + +x08, src,];
	kernel:out [iobase + +x09, src shr 8,];
	kernel:out [iobase + +x00, +x0A,];
	for (declare p = 0; p < len; p =+ 2) (
		copy `dst + p`:2 = kernel:in [iobase + +x10,];
	);
);

declare ne2k-setup = [] (
	(ne2k:ne2k-probe [] && (
		kernel:out [iobase + +x00, +x21,];
		kernel:out [iobase + +x0E, +x49,];

		allocate romdata/16;
		ne2k:ne2k-read [0, romdata, 16,];
	));
);
