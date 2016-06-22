declare idt-set-entry = function [vector, handler, is-interrupt-gate,] (
	declare p = idt-address + vector shl 3;
	dw-set [p + 0, handler and +x0000FFFF + +x00080000,];
	dw-set [p + 4, (if is-interrupt-gate then +x00008E00 else +x00008F00) + handler and +xFFFF0000,];
);

declare idt-setup = function [] (
	constant isr-prolog = (
		asm _ PUSHA ();
		asm _ PUSH (DS);
		asm _ PUSH (ES);
		asm _ MOV (AX, 16);
		asm _ MOV (DS, AX);
		asm _ MOV (ES, AX);
		asm _ PUSH (`+x40000`);
		asm _ MOV (EBP, ESP);
	);

	constant isr-epilog = (
		asm _ POP (EAX);
		asm _ POP (ES);
		asm _ POP (DS);
		asm _ POPA ();
	);

	declare handle-general-interrupt = snippet (
		isr-prolog;
		();
		isr-epilog;
		asm _ IRET ();
	);

	declare handle-timer-interrupt = snippet (
		isr-prolog;
		ticks =+ 1;
		(+xB8000 as pointer:int)/* =+ 1;

		-- Switch processes
		{sch-current-process/*/+current/+busy} = true;
		sch-yield [];

		-- Sends end of interrupt signal
		outb [+x20, +x20,];

		isr-epilog;
		asm _ IRET ();
	);

	snippet (
		asm .irq-handler ();
		isr-prolog;

		(
			declare vector;
			asm _ ADD (ESP, 4);
			asm _ PUSH (`EBP + 44`);

			declare irq-number = vector - +x20;
			declare isr = irq-get-isr [];

			if (isr and 1 shl irq-number != 0) then (
				declare irq-sink;
				irq-get-sink [out irq-sink, irq-number,];
				if (((& irq-sink) as pointer:int)/* != 0) then (irq-sink [irq-number,]) else ();

				-- Sends end of interrupt signal
				if (8 <= irq-number) then (outb [+xA0, +x20,]) else ();
				outb [+x20, +x20,];
			) else (
				log-byte "SPURIOUS INTERRUPT = " irq-number;
			);
		);

		isr-epilog;
		asm _ ADD (ESP, 4);
		asm _ IRET ();

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

			log-byte "FAULT INTERRUPT = " vector;
			log-dword "ERROR CODE = " code;
			log-word "CS = " cs0;
			log-dword "EIP = " eip0;
			log-dword "CR2 = " cr2;
		);

		sch-exit [];
	);

	for i in (0, 256) do (
		idt-set-entry [i, handle-general-interrupt, true,];
	);

	(
		constant set-fault-handler .i .h0 = (
			idt-set-entry [.i, snippet (
				asm _ PUSH (.i);
				asm _ JMP (DWORD .h0);
			), false,]
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
		constant set-irq-handler .i .h0 = (
			idt-set-entry [.i, snippet (
				asm _ PUSH (.i);
				asm _ JMP (DWORD .h0);
			), true,]
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

	idt-set-entry [+x20, handle-timer-interrupt, true,];

	(
		declare (int * 2) idtr;
		dw-set [address idtr + 0, +x7FF,];
		dw-set [address idtr + 2, idt-address,];
		address idtr;

		asm _ LIDT (`EAX`);
	);
);
