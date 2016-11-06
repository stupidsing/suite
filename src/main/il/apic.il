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
		) else if (type = 2) then ( -- Interrupt override
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
	--outb [+x21, +xFF,];
	--outb [+xA1, +xFF,];

	-- enable APIC
	--wrmsr [+x1B, ap-local-apic-address0 or 1, 0,];
	--declare reg = ap-local-apic-address + +xF0;
	--dw-set [reg, (dw-get [reg,]) or +x100,];
);

declare ap-teardown = function [] (
);
