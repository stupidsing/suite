constant ap-header = struct (
| byte type
| byte length
);

constant ap-local-apic = struct (
| ap-header header
| byte acpi-processor-id
| byte apic-id
| int flags
);

constant ap-io-apic = struct (
| ap-header header
| byte io-apic-id
| byte reserved
| int io-apic-address
| int global-system-interrupt-base
);

constant ap-interrupt-override = struct (
| ap-header header
| byte bus
| byte source
| int interrupt
| (byte * 2) flags
);

constant ap-acpi-header = struct (
| int signature
| int length
| byte revision
| byte checksum
| (byte * 6) oem
| (byte * 8) oem-table-id
| int oem-revision
| int creator-id
| int creator-revision
);

constant ap-acpi-fadt = struct (
| ap-acpi-header header
| int firmware-control
| int dsdt
| byte reserved
| byte preferred-pm-profile
| (byte * 2) sci-interrupt
| int smi-command-port
| byte acpi-enable
| byte acpi-disable
);

constant ap-acpi-madt = struct (
| ap-acpi-header header
| int local-apic-address
| int flags
);

declare ap-io-apic-address;
declare ap-local-apic-address;

declare ap-parse-madt = function [pointer:ap-acpi-madt madt,] (
	declare p = madt as int + size-of ap-acpi-madt;
	declare px = madt as int + madt/*/header/length;

	{ap-local-apic-address} = madt/*/local-apic-address;

	while (p < px) do (
		declare header = p as pointer:ap-header;
		declare type = extend header/*/type;

		if (type = 0) then ( -- local APIC
			declare local-apic = p as pointer:ap-local-apic;
		) else if (type = 1) then ( -- IO APIC
			declare io-apic = p as pointer:ap-io-apic;
			{ap-io-apic-address} = io-apic/*/io-apic-address;
		) else if (type = 2) then ( -- interrupt override
			declare intr-override = p as pointer:ap-interrupt-override;
		) else ();

		p += extend header/*/length;
	);
);

declare ap-parse-dt = function [pointer:ap-acpi-header header,] (
	if (header/*/signature = +x50434146) then (
	) else if (header/*/signature = +x43495041) then (
		ap-parse-madt [header as pointer:ap-acpi-madt,];
	) else ();
);

declare ap-parse-rsdt = function [pointer:ap-acpi-header rsdt,] (
	declare p = rsdt as int + size-of ap-acpi-header;
	declare px = rsdt as int + rsdt/*/length;
	while (p < px) do (
		ap-parse-dt [(dw-get [p =+ 4,]) as pointer:ap-acpi-header,];
	);
);

declare ap-parse-xsdt = function [pointer:ap-acpi-header rsdt,] (
	0;
);

declare ap-local-apic-get = function [r,] (
	dw-get [ap-local-apic-address + r,];
);

declare ap-local-apic-set = function [r, value,] (
	dw-set [ap-local-apic-address + r, value,];
);

declare ap-ioapic-get = function [r,] (
	dw-set [ap-io-apic-address, r,];
	dw-get [ap-io-apic-address + +x10,];
);

declare ap-ioapic-set = function [r, value,] (
	dw-set [ap-io-apic-address, r,];
	dw-set [ap-io-apic-address + +x10, value,];
);

declare ap-setup = function [] (
	declare p1;

	-- https://github.com/pdoane/osdev/blob/master/acpi/acpi.c
	for (declare p = +xE0000; p < +x100000; p += 16) do (
		if (dw-get [p,] = +x20445352 && dw-get [p + 4,] = +x20525450) then ( -- 'RSD PTR '
			{p1} = p
		) else ();
	);

	declare revision = extend ((p1 + 15) as pointer:byte)/*;

	if (revision = 0) then (
		declare rsdt = dw-get [p1 + 16,];
		ap-parse-rsdt [rsdt as pointer:ap-acpi-header,];
	) else if (revision = 2) then (
		declare rsdt = dw-get [p1 + 16,];
		declare xsdt = dw-get [p1 + 24,];
		if (xsdt != 0) then (
			ap-parse-xsdt [xsdt as pointer:ap-acpi-header,];
		) else (
			ap-parse-rsdt [rsdt as pointer:ap-acpi-header,];
		);
	) else (
		log-byte "Unsupported ACPI version " revision;
	);

	declare ap-local-apic-address0;
	declare ap-local-apic-address1;
	rdmsr [+x1B, out ap-local-apic-address0, out ap-local-apic-address1,];

	log-dword "IO APIC address = " ap-io-apic-address;
	log-dword "Local APIC page address = " ap-local-apic-address;
	log-dword "Local APIC base address 0 = " ap-local-apic-address0;
	log-dword "Local APIC base address 1 = " ap-local-apic-address1;

	-- disable PIC
	outb [+x21, +xFF,];
	outb [+xA1, +xFF,];

	-- enable APIC
	wrmsr [+x1B, ap-local-apic-address0 or +x800, ap-local-apic-address1,];
	ap-local-apic-set [+xE0, +xFFFFFFFF,]; -- DFR
	ap-local-apic-set [+xD0, +x01000000 or +xFFFFFF and dw-get [ap-local-apic-address + +xD0,],]; -- LDR
	ap-local-apic-set [+x340, +x400,]; -- perf
	ap-local-apic-set [+x350, +x10000,]; -- disable LINT0
	ap-local-apic-set [+x360, +x10000,]; -- disable LINT1
	ap-local-apic-set [+x80, 0,]; -- task prior
	ap-local-apic-set [+xF0, +x107,]; -- spurious IRQ

	-- enable APIC timer
	declare ticksIn10ms = 400000;
	ap-local-apic-set [+x3E0, +x3,]; -- timer divisor
	ap-local-apic-set [+x380, ticksIn10ms,]; -- timer initial count
	ap-local-apic-set [+x320, +x20020,]; -- timer IRQ (periodic)

	-- enable IO APIC IRQ 1 - keyboard
	--declare io-apic-id = (ap-ioapic-get [0,]) shr 24 and +x0F;

	declare irq = 1;
	declare ioapic-r0 = irq * 2 + +x10;
	declare ioapic-r1 = irq * 2 + +x11;
	ap-ioapic-set [ioapic-r0, +x00000921 or +xFFFEF000 and ap-ioapic-get [ioapic-r0,],];
	ap-ioapic-set [ioapic-r1, +xFF000000 or +x00FFFFFF and ap-ioapic-get [ioapic-r1,],];
);

declare ap-teardown = function [] (
);
