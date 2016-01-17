declare irq = this;
declare irq-sinks as ((baseless [int,] int) * 16);

declare dump-sink = baseless [irq-number,] (
	log-byte "IRQ = " irq-number;
);

declare irq-get-isr = function [] (
	outb [+x20, +x0B,];
	outb [+xA0, +x0B,];
	(inb [+xA0,]) shl 8 + inb [+x20,];
);

declare irq-get-sink = function [irq-number,] (
	irq-sinks:irq-number;
);

declare irq-set-sink = function [irq-number, irq-sink,] (
	let irq-sinks:irq-number = irq-sink;
);

declare irq-setup = function [] (
	for irq-number in (0, 16) do (
		irq-set-sink [irq-number, dump-sink,];
	);

	--irq-set-sink [7, 0,]; -- Ignore spurious interrupts
);
