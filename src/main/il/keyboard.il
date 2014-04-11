declare kb = this;

declare keyboard-sink = [irq-number,] (
	declare status = kernel:inb [+x64,];
	declare scan-code = kernel:inb [+x60,];
	cs:cs-puts ["SCAN CODE = ",];
	cs:cs-put-hex1 [scan-code,];
	cs:cs-puts [", STATUS = ",];
	cs:cs-put-hex1 [status,];
	cs:cs-nl [];
);

declare kb-setup = [] (
	irq:irq-set-sink [1, keyboard-sink,];
);
