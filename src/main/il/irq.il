declare irq = this;
declare-as irq-sinks/(baseless [int,] * 16);

declare dump-sink = baseless [irq-number,] (
	cs-puts ["IRQ = ",];
	cs-put-hex1 [irq-number,];
	cs-nl [];
);

declare irq-get-isr = function [] (
	outb [+x20, +x0B,];
	outb [+xA0, +x0B,];
	(inb [+xA0,]) shl 8 + inb [+x20,];
);

declare irq-get-sink = function [irq-number,] (
	irq-sinks:irq-number;
);

declare irq-set-sink = function [irq-number, irq-sink as baseless [int,],] (
	let irq-sinks:irq-number = irq-sink;
);

declare irq-setup = function [] (
	for (declare irq-number = 0; irq-number < 16; irq-number =+ 1) (
		irq-set-sink [irq-number, dump-sink,];
	);

	--irq-set-sink [7, 0,]; -- Ignore spurious interrupts
);
