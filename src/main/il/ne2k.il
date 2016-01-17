-- Reference http://www.jbox.dk/sanos/source/sys/dev/ne2000.c.html

constant ne2k-io-base = +x300;
constant ne2k-irq-number = 3;

constant ne2k-mem-base = 16384;
constant ne2k-mem-size = 16384;

declare ne2k-mac-address as (byte * 8);
declare ne2k-rx-page-start;
declare ne2k-rx-page-stop;
declare ne2k-is-arrived;
declare ne2k-is-transmitted;
declare ne2k-is-dma-completed;

declare ne2k-irq-sink = baseless [irq-number as int,] (
	declare isr;
	outb [ne2k-io-base + +x00, +x22,];
	while (let isr = inb [ne2k-io-base + +x07,]) do (
		outb [ne2k-io-base + +x07, isr,];
		(isr and +x01 && let ne2k-is-arrived = 1);
		(isr and +x02 && let ne2k-is-transmitted = 1);
		(isr and +x40 && let ne2k-is-dma-completed = 1);
	);
	outb [ne2k-io-base + +x00, +x22,];
);

declare ne2k-read = function [src, dst, len,] (
	let len = (len + 1) and +xFFFFFFFE;
	outb [ne2k-io-base + +x00, +x22,];
	outb [ne2k-io-base + +x0A, len,];
	outb [ne2k-io-base + +x0B, len ushr 8,];
	outb [ne2k-io-base + +x08, src,];
	outb [ne2k-io-base + +x09, src ushr 8,];
	outb [ne2k-io-base + +x00, +x0A,];

	for (declare p = 0; p < len; p =+ 2) do (
		declare w = inw [ne2k-io-base + +x10,];
		copy [dst + p, address w, 2,];
	);
);

declare ne2k-send = function [buffer, len,] (
	let ne2k-is-dma-completed = 0;

	let len = (len + 1) and +xFFFFFFFE;
	outb [ne2k-io-base + +x00, +x22,];
	outb [ne2k-io-base + +x04, +x40,];
	outb [ne2k-io-base + +x0A, len,];
	outb [ne2k-io-base + +x0B, len ushr 8,];
	outb [ne2k-io-base + +x08, 0,];
	outb [ne2k-io-base + +x09, ne2k-rx-page-stop,];
	outb [ne2k-io-base + +x00, +x12,];

	for (declare p = 0; p < len; p =+ 2) do (
		declare b as int;
		copy [address b, buffer + p, 2,];
		outw [ne2k-io-base + +x10, b,];
	);

	while (ne2k-is-dma-completed = 0) do ( -- Wait for DMA to complete
		sch-yield [];
	);

	let ne2k-is-transmitted = 0;

	declare padded-len = if (len >= 64) then len else 64;
	outb [ne2k-io-base + +x05, padded-len,];
	outb [ne2k-io-base + +x06, padded-len ushr 8,];
	outb [ne2k-io-base + +x00, +x26,];

	while (ne2k-is-transmitted = 0) do ( -- Wait for packet to transmit
		sch-yield [];
	);
);

declare ne2k-probe = function [] (
	outb [ne2k-io-base + +x1F, inb [ne2k-io-base + +x1F,],];
	outb [ne2k-io-base + +x00, +x21,];

	time-sleep [100,];

	-- Tests for a generic DP8390 NIC
	(+x27 and inb [ne2k-io-base + +x00,] = +x21
		&& +x80 and inb [ne2k-io-base + +x07,] = +x80
	);
);

declare ne2k-setup = function [] (
	(ne2k-probe [] && (
		outb [ne2k-io-base + +x00, +x21,];
		outb [ne2k-io-base + +x0E, +x49,];

		declare romdata as (byte * 16);
		ne2k-read [0, address romdata, 16,];
		for p in (0, 6) do (
			copy [address ne2k-mac-address + p, address romdata + p shl 1, 1,];
		);

		ne2k-rx-page-start = ne2k-mem-base ushr 8;
		ne2k-rx-page-stop = ne2k-rx-page-start + ne2k-mem-size ushr 8 - 12;

		cs-puts ["MAC address = ",];

		for p in (0, 6) do (
			cs-put-hex1 [extend ne2k-mac-address:p,];
		);

		cs-nl [];

		irq-set-sink [ne2k-irq-number, ne2k-irq-sink,];
	));
);
