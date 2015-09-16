declare irq = this;
declare-as irq-sinks/([int,] * 16);

declare dump-sink = [irq-number,] (
	invoke cs-puts ["IRQ = ",];
	invoke cs-put-hex1 [irq-number,];
	invoke cs-nl [];
);

declare irq-get-isr = function [] (
	invoke outb [+x20, +x0B,];
	invoke outb [+xA0, +x0B,];
	(invoke inb [+xA0,]) shl 8 + invoke inb [+x20,];
);

declare irq-get-sink = function [irq-number,] (
	irq-sinks:irq-number;
);

declare irq-set-sink = function [irq-number, irq-sink/[int,],] (
	let irq-sinks:irq-number = irq-sink;
);

declare irq-setup = function [] (
	for (declare irq-number = 0; irq-number < 16; irq-number =+ 1) (
		invoke irq-set-sink [irq-number, dump-sink,];
	);

	--invoke irq-set-sink [7, 0,]; -- Ignore spurious interrupts
);
