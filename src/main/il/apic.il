constant ap-header = struct (
	+type as byte,
	+length as byte,
);

constant ap-local-apic = struct (
	+header as ap-header,
	+acpi-processor-id as byte,
	+apic-id as byte,
	+flags as int, 
);

constant ap-io-apic = struct (
	+header as ap-header,
	+io-apic-id as byte,
	+reserved as byte,
	+io-apic-address as int,
	+global-system-interrupt-base as int,
);

constant ap-interrupt-override = struct (
	+header as ap-header,
	+bus as byte,
	+source as byte,
	+interrupt as int,
	+flags as (byte * 2),
);

constant ap-acpi-header = struct (
	+signature as int,
	+length as int,
	+revision as byte,
	+checksum as byte,
	+oem as (byte * 6),
	+oem-table-id as (byte * 8),
	+oem-revision as int,
	+creator-id as int,
	+creator-revision as int,
);

constant ap-acpi-fadt = struct (
	+header as ap-acpi-header,
	+firmware-control as int,
	+dsdt as int,
	+reserved as byte,
	+preferred-pm-profile as byte,
	+sci-interrupt as (byte * 2),
	+smi-command-port as int,
	+acpi-enable as byte,
	+acpi-disable as byte,
);

constant ap-acpi-madt = struct (
	+header as ap-acpi-header,
	+local-apic-address as int,
	+flags as int,
);

declare ap-io-apic-address;
declare ap-local-apic-address;

declare ap-parse-madt = function [madt as pointer:ap-acpi-madt,] (
	declare p = madt as int + size-of ap-acpi-madt;
	declare px = madt as int + madt/*/+header/+length;

	{ap-local-apic-address} = madt/*/+local-apic-address;

	while (p < px) do (
		declare header = p as pointer:ap-header;
		declare type = extend header/*/+type;

		if (type = 0) then ( -- local APIC
			declare local-apic = p as pointer:ap-local-apic;
		) else if (type = 1) then ( -- IO APIC
			declare io-apic = p as pointer:ap-io-apic;
			{ap-io-apic-address} = io-apic/*/+io-apic-address;
		) else if (type = 2) then ( -- Interrupt override
			declare intr-override = p as pointer:ap-interrupt-override;
		) else ();

		p += extend header/*/+length;
	);
);

declare ap-parse-dt = function [header as pointer:ap-acpi-header,] (
	if (header/*/+signature = +x50434146) then (
	) else if (header/*/+signature = +x43495041) then (
		ap-parse-madt [header as pointer:ap-acpi-madt,];
	) else ();
);

declare ap-parse-rsdt = function [rsdt as pointer:ap-acpi-header,] (
	declare p = rsdt as int + size-of ap-acpi-header;
	declare px = rsdt as int + rsdt/*/+length;
	while (p < px) do (
		ap-parse-dt [(dw-get [p =+ 4,]) as pointer:ap-acpi-header,];
	);
);

declare ap-parse-xsdt = function [rsdt as pointer:ap-acpi-header,] (
	0;
);

declare ap-setup = function [] (
	declare p1;

	-- https://github.com/pdoane/osdev/blob/master/acpi/acpi.c
	for (declare p = +xE0000; p < +x100000; p += 16) do (
		(dw-get [p,] = +x20445352 && dw-get [p + 4,] = +x20525450 -- 'RSD PTR '
			&& {p1} = p
		);
	);

	declare revision = extend ((p1 + 15) as pointer:byte)/*;

	if (revision = 0) then (
		declare rsdt = dw-get [p1 + 16,];
		ap-parse-rsdt [rsdt as pointer:ap-acpi-header,];
	) else if (revision = 2) then (
		declare rsdt = dw-get [p1 + 16,];
		declare xsdt = dw-get [p1 + 24,];
		if xsdt then (
			ap-parse-xsdt [xsdt as pointer:ap-acpi-header,];
		) else (
			ap-parse-rsdt [rsdt as pointer:ap-acpi-header,];
		);
	) else (
		log-byte "Unsupported ACPI version " revision;
	);

	log-dword "IO APIC address = " ap-io-apic-address;
	log-dword "Local APIC address = " ap-local-apic-address;
);
