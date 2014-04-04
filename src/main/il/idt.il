declare idt = this;

declare handle-general-interrupt = [] (
	asm _ PUSHA ();
	asm _ PUSH (DS);
	asm _ MOV (AX, 16);
	asm _ MOV (DS, AX);
	asm _ PUSH (`+x40000`);
	asm _ MOV (EBP, ESP);

	`+xB8000` =+ 1;

	-- Sends end of interrupt signal
	kernel:out [+x20, +x20,];

	asm _ MOV (ESP, EBP);
	asm _ POP (EAX);
	asm _ POP (DS);
	asm _ POPA ();
	asm _ MOV (ESP, EBP);
	asm _ POP (EBP);
	asm _ IRET ();
);

declare handle-gp-fault = [] (
	asm _ PUSHA ();
	asm _ PUSH (DS);
	asm _ MOV (AX, 16);
	asm _ MOV (DS, AX);
	asm _ PUSH (`+x40000`);
	asm _ MOV (EBP, ESP);

	cs:cs-puts ["GP-FAULT",];
	cs:cs-nl [];
	asm _ HLT ();
);

declare handle-page-fault = [] (
	asm _ PUSHA ();
	asm _ PUSH (DS);
	asm _ MOV (AX, 16);
	asm _ MOV (DS, AX);
	asm _ PUSH (`+x40000`);
	asm _ MOV (EBP, ESP);

	cs:cs-puts ["PAGE-FAULT",];
	cs:cs-nl [];
	asm _ HLT ();
);

-- Sets up and loads interrupt descriptor table
declare set-idt-entry = [interrupt, handler,] (
	declare p = idt-address + interrupt shl 3;
	let `p + 0` = handler and +x0000FFFF + +x00080000;
	let `p + 4` = +x00008E00 + handler and +xFFFF0000;
);

declare idt-setup = [] (
	for (declare i = 0; i < 256; i =+ 1) (
		idt:set-idt-entry [i, handle-general-interrupt,];
	);

	idt:set-idt-entry [+x0D, handle-gp-fault,];
	idt:set-idt-entry [+x0E, handle-page-fault,];

	allocate idtr/8;
	let `idtr` = +x7FF;
	let `idtr + 2` = idt-address;
	idtr;

	asm _ LIDT (`EAX`);
);
