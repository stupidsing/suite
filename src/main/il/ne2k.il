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
