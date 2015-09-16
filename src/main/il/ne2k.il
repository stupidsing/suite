-- Reference http://www.jbox.dk/sanos/source/sys/dev/ne2000.c.html

constant io-base = +x300;
constant mem-base = 16384;
constant mem-size = 16384;

declare-as-pointer ne2k-mac-address/(int * 2);
declare ne2k-rx-page-start;
declare ne2k-rx-page-stop;

declare ne2k-irq-sink = [irq-number,] (
	declare isr;
	invoke outb [io-base + +x00, +x22,];
	while (let isr = invoke inb [io-base + +x07,]) do (
		invoke outb [io-base + +x07, isr,];
		(isr and +x01 && ( -- Arrived
		));
		(isr and +x02 && ( -- Transmitted
		));
		(isr and +x40 && ( -- DMA completed
		));
	);
	invoke outb [io-base + +x00, +x22,];
);

declare ne2k-read = function [src, dst, len,] (
	let len = (len + 1) and +xFFFFFFFE;
	invoke outb [io-base + +x00, +x22,];
	invoke outb [io-base + +x0A, len,];
	invoke outb [io-base + +x0B, len shr 8,];
	invoke outb [io-base + +x08, src,];
	invoke outb [io-base + +x09, src shr 8,];
	invoke outb [io-base + +x00, +x0A,];

	for (declare p = 0; p < len; p =+ 2) (
		declare w = invoke inw [io-base + +x10,];
		invoke copy [dst + p, & w, 2,];
	);
);

declare ne2k-send = function [buffer, len,] (
	let len = (len + 1) and +xFFFFFFFE;
	invoke outb [io-base + +x00, +x22,];
	invoke outb [io-base + +x04, +x40,];
	invoke outb [io-base + +x0A, len,];
	invoke outb [io-base + +x0B, len shr 8,];
	invoke outb [io-base + +x08, 0,];
	invoke outb [io-base + +x09, ne2k-rx-page-stop,];
	invoke outb [io-base + +x00, +x12,];

	for (declare p = 0; p < len; p =+ 2) (
		declare b;
		invoke copy [& b, buffer + p, 2,];
		invoke outw [io-base + +x10, b,];
	);

	-- TODO wait IRQ for DMA complete

	declare padded-len = if (len >= 64) then len else 64;
	invoke outb [io-base + +x05, padded-len,];
	invoke outb [io-base + +x06, padded-len shr 8,];
	invoke outb [io-base + +x00, +x26,];

	-- TODO wait IRQ for packet transmitted
);

declare ne2k-probe = function [] (
	invoke outb [io-base + +x1F, invoke inb [io-base + +x1F,],];
	invoke outb [io-base + +x00, +x21,];

	invoke time-sleep [100,];

	-- Tests for a generic DP8390 NIC
	(+x27 and invoke inb [io-base + +x00,] = +x21
		&& +x80 and invoke inb [io-base + +x07,] = +x80
	);
);

declare ne2k-setup = function [] (
	(invoke ne2k-probe [] && (
		invoke outb [io-base + +x00, +x21,];
		invoke outb [io-base + +x0E, +x49,];

		declare-as-pointer romdata/(int * 4);
		invoke ne2k-read [0, romdata, 16,];
		for (declare p = 0; p < 6; p =+ 1) (
			invoke copy [ne2k-mac-address + p, romdata + p shl 1, 1,];
		);

		ne2k-rx-page-start = mem-base shr 8;
		ne2k-rx-page-stop = ne2k-rx-page-start + mem-size shr 8 - 12;

		invoke cs-puts ["MAC ADDRESS = ",];

		for (declare p = 0; p < 6; p =+ 1) (
			declare b;
			invoke copy [& b, ne2k-mac-address + p, 1,];
			invoke cs-put-hex1 [b,];
		);

		invoke cs-nl [];
	));
);
