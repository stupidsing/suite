constant ne2k-io-base = +x300;
constant ne2k-irq-number = 3;

constant ne2k-mem-base = 16384;
constant ne2k-mem-size = 16384;

signature ne2k-mac-address = byte * 8;
declare ne2k-rx-page-start;
declare ne2k-rx-page-stop;
declare ne2k-is-arrived;
declare ne2k-is-transmitted;
declare ne2k-is-dma-completed;

declare ne2k-irq-sink = function [int irq-number,] (
	declare isr;
	outb [ne2k-io-base + +x00, +x22,];
	while (({isr} = inb [ne2k-io-base + +x07,]) != 0) do (
		outb [ne2k-io-base + +x07, isr,];
		if (isr and +x01 != 0) then ({ne2k-is-arrived} = true) else ();
		if (isr and +x02 != 0) then ({ne2k-is-transmitted} = true) else ();
		if (isr and +x40 != 0) then ({ne2k-is-dma-completed} = true) else ();
	);
	outb [ne2k-io-base + +x00, +x22,];
);

declare ne2k-read = function [src, dst, len,] (
	{len} = (len + 1) and +xFFFFFFFE;
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
	{ne2k-is-dma-completed} = false;

	{len} = (len + 1) and +xFFFFFFFE;
	outb [ne2k-io-base + +x00, +x22,];
	outb [ne2k-io-base + +x04, +x40,];
	outb [ne2k-io-base + +x0A, len,];
	outb [ne2k-io-base + +x0B, len ushr 8,];
	outb [ne2k-io-base + +x08, 0,];
	outb [ne2k-io-base + +x09, ne2k-rx-page-stop,];
	outb [ne2k-io-base + +x00, +x12,];

	for (declare p = 0; p < len; p =+ 2) do (
		signature b = int;
		copy [address b, buffer + p, 2,];
		outw [ne2k-io-base + +x10, b,];
	);

	while (not ne2k-is-dma-completed) do ( -- Wait for DMA to complete
		sch-yield [];
	);

	{ne2k-is-transmitted} = false;

	declare padded-len = if (64 <= len) then len else 64;
	outb [ne2k-io-base + +x05, padded-len,];
	outb [ne2k-io-base + +x06, padded-len ushr 8,];
	outb [ne2k-io-base + +x00, +x26,];

	while (not ne2k-is-transmitted) do ( -- Wait for packet to transmit
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
	if (ne2k-probe []) then (
		outb [ne2k-io-base + +x00, +x21,];
		outb [ne2k-io-base + +x0E, +x49,];

		signature romdata = byte * 16;
		ne2k-read [0, address romdata, 16,];
		for p in (0, 6) do (
			copy [address ne2k-mac-address + p, address romdata + p shl 1, 1,];
		);

		ne2k-rx-page-start = ne2k-mem-base ushr 8;
		ne2k-rx-page-stop = ne2k-rx-page-start + ne2k-mem-size ushr 8 - 12;

		cs-puts ["MAC address = ",];

		for p in (0, 6) do (
			cs-put-hex1 [extend ne2k-mac-address/:p,];
		);

		cs-nl [];

		irq-set-sink [ne2k-irq-number, ne2k-irq-sink,];
	) else ();
);
