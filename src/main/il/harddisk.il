declare hd-set-lba = function [lba, count,] (
	outb [+x1F6, (lba ushr 24) or +xE0,];
	outb [+x1F2, count,];
	outb [+x1F3, (lba ushr 0) and +xFF,];
	outb [+x1F4, (lba ushr 8) and +xFF,];
	outb [+x1F5, (lba ushr 16) and +xFF,];
);

-- http://wiki.osdev.org/ATA_read/write_sectors
declare hd-read = function [lba, count, int buffer,] (
	hd-set-lba [lba, count,];
	outb [+x1F7, +x20,];

	while ((inb [+x1F7,]) and 8 = 0) do (
		sch-yield [];
	);

	declare end = buffer + count * 512;

	while (buffer < end) do (
		declare d = 0;
		{d} = d shl 16 + inw [+x1F0,];
		{d} = d shl 16 + inw [+x1F0,];
		dw-set [buffer =+ 4, d,];
	);
);

declare hd-write = function [lba, count, int buffer,] (
	hd-set-lba [lba, count,];
	outb [+x1F7, +x30,];

	while ((inb [+x1F7,]) and 8 = 0) do (
		sch-yield [];
	);

	declare end = buffer + count * 512;

	while (buffer < end) do (
		declare d = dw-get [buffer =+ 4,];
		outw [+x1F0, (d ushr 0) and +xFFFF,];
		outw [+x1F0, (d ushr 16) and +xFFFF,];
	);
);
