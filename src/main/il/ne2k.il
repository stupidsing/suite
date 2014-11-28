-- Reference http://www.jbox.dk/sanos/source/sys/dev/ne2000.c.html

declare ne2k = this;

constant io-base = +x300;
constant mem-base = 16384;
constant mem-size = 16384;

allocate ne2k-mac-address/8;
declare ne2k-rx-page-start;
declare ne2k-rx-page-stop;

declare ne2k-irq-sink = [irq-number,] (
	declare isr;
	kernel:outb [io-base + +x00, +x22,];
	while (let isr = kernel:inb [io-base + +x07,]) do (
		kernel:outb [io-base + +x07, isr,];
		(isr and +x01 && ( -- Arrived
		));
		(isr and +x02 && ( -- Transmitted
		));
		(isr and +x40 && ( -- DMA completed
		));
	);
	kernel:outb [io-base + +x00, +x22,];
);

declare ne2k-read = [src, dst, len,] (
	let len = (len + 1) and +xFFFFFFFE;
	kernel:outb [io-base + +x00, +x22,];
	kernel:outb [io-base + +x0A, len,];
	kernel:outb [io-base + +x0B, len shr 8,];
	kernel:outb [io-base + +x08, src,];
	kernel:outb [io-base + +x09, src shr 8,];
	kernel:outb [io-base + +x00, +x0A,];

	for (declare p = 0; p < len; p =+ 2) (
		declare w = kernel:inw [io-base + +x10,];
		kernel:copy [dst + p, & w, 2,];
	);
);

declare ne2k-send = [buffer, len,] (
	let len = (len + 1) and +xFFFFFFFE;
	kernel:outb [io-base + +x00, +x22,];
	kernel:outb [io-base + +x04, +x40,];
	kernel:outb [io-base + +x0A, len,];
	kernel:outb [io-base + +x0B, len shr 8,];
	kernel:outb [io-base + +x08, 0,];
	kernel:outb [io-base + +x09, ne2k-rx-page-stop,];
	kernel:outb [io-base + +x00, +x12,];

	for (declare p = 0; p < len; p =+ 2) (
		declare b;
		kernel:copy [& b, buffer + p, 2,];
		kernel:outw [io-base + +x10, b,];
	);

	-- TODO wait IRQ for DMA complete

	declare padded-len = if (len >= 64) then len else 64;
	kernel:outb [io-base + +x05, padded-len,];
	kernel:outb [io-base + +x06, padded-len shr 8,];
	kernel:outb [io-base + +x00, +x26,];

	-- TODO wait IRQ for packet transmitted
);

declare ne2k-probe = [] (
	kernel:outb [io-base + +x1F, kernel:inb [io-base + +x1F,],];
	kernel:outb [io-base + +x00, +x21,];

	time:time-sleep [100,];

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
		for (declare p = 0; p < 6; p =+ 1) (
			kernel:copy [& ne2k-mac-address + p, & romdata + p shl 1, 1,];
		);

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
