declare idt = this;

-- Sets up and loads interrupt descriptor table
declare idt-set-entry = [vector, handler, is-interrupt-gate,] (
	declare p = idt-address + vector shl 3;
	let `p + 0` = handler and +x0000FFFF + +x00080000;
	let `p + 4` = (if is-interrupt-gate then +x00008E00 else +x00008F00) + handler and +xFFFF0000;
);

declare idt-setup = [] (
	declare handle-general-interrupt = snippet (
		asm _ PUSHA ();
		asm _ PUSH (DS);
		asm _ PUSH (ES);
		asm _ MOV (AX, 16);
		asm _ MOV (DS, AX);
		asm _ MOV (ES, AX);
		asm _ PUSH (`+x40000`);
		asm _ MOV (EBP, ESP);

		-- Sends end of interrupt signal
		kernel:outb [+x20, +x20,];

		asm _ MOV (ESP, EBP);
		asm _ POP (EAX);
		asm _ POP (ES);
		asm _ POP (DS);
		asm _ POPA ();
		asm _ IRET ();
	);

	declare handle-timer-interrupt = snippet (
		asm _ PUSHA ();
		asm _ PUSH (DS);
		asm _ PUSH (ES);
		asm _ MOV (AX, 16);
		asm _ MOV (DS, AX);
		asm _ MOV (ES, AX);
		asm _ PUSH (`+x40000`);
		asm _ MOV (EBP, ESP);

		ticks =+ 1;
		`+xB8000` =+ 1;

		-- Sends end of interrupt signal
		kernel:outb [+x20, +x20,];

		asm _ MOV (ESP, EBP);
		asm _ POP (EAX);
		asm _ POP (ES);
		asm _ POP (DS);
		asm _ POPA ();
		asm _ IRET ();
	);

	snippet (
		asm .fault-handler-0 ();
		asm _ POP (EAX);
		asm _ PUSH (0);
		asm _ PUSH (EAX);
		asm .fault-handler-e ();
		asm _ POP (EAX); -- Interrupt vector
		asm _ POP (EBX); -- Error code
		asm _ POP (ECX); -- Error EIP
		asm _ POP (EDX); -- Error CS
		asm _ MOV (DI, 16);
		asm _ MOV (DS, DI);
		asm _ PUSH (`+x40000`);
		asm _ MOV (EBP, ESP);

		(
			declare vector;
			declare code;
			declare eip0;
			declare cs0;
			declare cr2;
			asm _ ADD (ESP, 20);
			asm _ PUSH (EAX);
			asm _ PUSH (EBX);
			asm _ PUSH (ECX);
			asm _ PUSH (EDX);
			asm _ MOV (EAX, CR2);
			asm _ PUSH (EAX);

			cs:cs-puts ["FAULT INTERRUPT = ",];
			cs:cs-put-hex1 [vector,];
			cs:cs-puts [", ERROR CODE = ",];
			cs:cs-put-hex4 [code,];
			cs:cs-puts [", CS = ",];
			cs:cs-put-hex2 [cs0,];
			cs:cs-puts [", EIP = ",];
			cs:cs-put-hex4 [eip0,];
			cs:cs-puts [", CR2 = ",];
			cs:cs-put-hex4 [cr2,];
			cs:cs-nl [];
		);

		asm _ CLI ();
		asm _ HLT ();

		asm .irq-handler ();
		asm _ PUSHA ();
		asm _ PUSH (DS);
		asm _ PUSH (ES);
		asm _ MOV (DI, 16);
		asm _ MOV (DS, DI);
		asm _ MOV (ES, DI);
		asm _ PUSH (`+x40000`);
		asm _ MOV (EBP, ESP);
		asm _ MOV (EAX, `EBP + 44`);

		(
			declare vector;
			asm _ ADD (ESP, 4);
			asm _ PUSH (EAX);

			cs:cs-puts ["IRQ = ",];
			cs:cs-put-hex1 [vector,];
			cs:cs-nl [];

			-- Sends end of interrupt signal
			kernel:outb [+x20, +x20,];
		);

		asm _ MOV (ESP, EBP);
		asm _ POP (EAX);
		asm _ POP (ES);
		asm _ POP (DS);
		asm _ POPA ();

		asm _ ADD (ESP, 4);
		asm _ IRET ();
	);

	for (declare i = 0; i < 256; i =+ 1) (
		idt:idt-set-entry [i, handle-general-interrupt, 1,];
	);

	(
		expand set-fault-handler .i .h0 = (
			idt:idt-set-entry [.i, snippet (
				asm _ PUSH (.i);
				asm _ JMP (DWORD .h0);
			), 0,]
		);
		set-fault-handler +x00 .fault-handler-0;
		set-fault-handler +x01 .fault-handler-0;
		set-fault-handler +x02 .fault-handler-0;
		set-fault-handler +x03 .fault-handler-0;
		set-fault-handler +x04 .fault-handler-0;
		set-fault-handler +x05 .fault-handler-0;
		set-fault-handler +x06 .fault-handler-0;
		set-fault-handler +x07 .fault-handler-0;
		set-fault-handler +x08 .fault-handler-e;
		set-fault-handler +x09 .fault-handler-0;
		set-fault-handler +x0A .fault-handler-e;
		set-fault-handler +x0B .fault-handler-e;
		set-fault-handler +x0C .fault-handler-e;
		set-fault-handler +x0D .fault-handler-e;
		set-fault-handler +x0E .fault-handler-e;
		set-fault-handler +x0F .fault-handler-0;
		set-fault-handler +x10 .fault-handler-0;
		set-fault-handler +x11 .fault-handler-e;
		set-fault-handler +x12 .fault-handler-0;
		set-fault-handler +x13 .fault-handler-0;
		set-fault-handler +x14 .fault-handler-0;
		set-fault-handler +x15 .fault-handler-0;
		set-fault-handler +x16 .fault-handler-0;
		set-fault-handler +x17 .fault-handler-0;
		set-fault-handler +x18 .fault-handler-0;
		set-fault-handler +x19 .fault-handler-0;
		set-fault-handler +x1A .fault-handler-0;
		set-fault-handler +x1B .fault-handler-0;
		set-fault-handler +x1C .fault-handler-0;
		set-fault-handler +x1D .fault-handler-0;
		set-fault-handler +x1E .fault-handler-0;
		set-fault-handler +x1F .fault-handler-0;
	);

	(
		expand set-irq-handler .i .h0 = (
			idt:idt-set-entry [.i, snippet (
				asm _ PUSH (.i);
				asm _ JMP (DWORD .h0);
			), 1,]
		);
		set-irq-handler +x20 .irq-handler;
		set-irq-handler +x21 .irq-handler;
		set-irq-handler +x22 .irq-handler;
		set-irq-handler +x23 .irq-handler;
		set-irq-handler +x24 .irq-handler;
		set-irq-handler +x25 .irq-handler;
		set-irq-handler +x26 .irq-handler;
		set-irq-handler +x27 .irq-handler;
		set-irq-handler +x28 .irq-handler;
		set-irq-handler +x29 .irq-handler;
		set-irq-handler +x2A .irq-handler;
		set-irq-handler +x2B .irq-handler;
		set-irq-handler +x2C .irq-handler;
		set-irq-handler +x2D .irq-handler;
		set-irq-handler +x2E .irq-handler;
		set-irq-handler +x2F .irq-handler;
	);

	idt:idt-set-entry [+x20, handle-timer-interrupt, 1,];

	(
		allocate idtr/8;
		let `idtr` = +x7FF;
		let `idtr + 2` = idt-address;
		idtr;

		asm _ LIDT (`EAX`);
	);
);
