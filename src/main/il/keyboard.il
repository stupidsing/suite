declare keyboard-sink = function [irq-number,] (
	declare status = inb [+x64,];
	declare scan-code = inb [+x60,];
	log-byte "SCAN CODE = " scan-code;
	log-byte "STATUS = " status;
);

declare kb-setup = function [] (
	irq-set-sink [1, keyboard-sink,];
);

declare kb-teardown = function [] (
	irq-set-sink [1, 0 as (function [int,] int),];
);
