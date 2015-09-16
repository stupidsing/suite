declare keyboard-sink = baseless [irq-number,] (
	declare status = inb [+x64,];
	declare scan-code = inb [+x60,];
	cs-puts ["SCAN CODE = ",];
	cs-put-hex1 [scan-code,];
	cs-puts [", STATUS = ",];
	cs-put-hex1 [status,];
	cs-nl [];
);

declare kb-setup = function [] (
	irq-set-sink [1, keyboard-sink,];
);

declare kb-teardown = function [] (
	irq-set-sink [1, 0 as baseless [int,],];
);
