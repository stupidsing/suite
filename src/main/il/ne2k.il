declare ne2k = this;

expand iobase = +x300;

declare ne2k-probe = [] (
	declare byte = kernel:in [iobase + 0x1F,];
	kernel:out [iobase + 0x1F, byte,];
	kernel:out [iobase + 0x00, +x21,];

	declare ticks1 = ticks + 100;
	while (ticks < ticks1) do (
		asm _ HLT ();
	);
);
