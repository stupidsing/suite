declare kb = this;

declare keyboard-sink = [irq-number,] (
	declare status = invoke inb [+x64,];
	declare scan-code = invoke inb [+x60,];
	invoke cs-puts ["SCAN CODE = ",];
	invoke cs-put-hex1 [scan-code,];
	invoke cs-puts [", STATUS = ",];
	invoke cs-put-hex1 [status,];
	invoke cs-nl [];
);

declare kb-setup = [] (
	invoke irq-set-sink [1, keyboard-sink,];
);

declare kb-teardown = [] (
	invoke irq-set-sink [1, 0 as [int,],];
);
