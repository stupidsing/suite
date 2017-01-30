signature irq-sinks = (function [int,] int) * 16;

declare dump-sink = function [irq-number,] (
	log-byte "IRQ = " irq-number;
);

declare irq-get-sink = fn [irq-number,] (
	return irq-sinks/:irq-number;
);

declare irq-set-sink = function [irq-number, irq-sink,] (
	{irq-sinks/:irq-number} = irq-sink;
);

declare irq-setup = function [] (
	for irq-number in (0, 16) do (
		irq-set-sink [irq-number, dump-sink,];
	);

	--irq-set-sink [7, 0,]; -- ignore spurious interrupts
);

declare irq-teardown = function [] (
);
