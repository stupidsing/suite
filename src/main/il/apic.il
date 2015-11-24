constant apic-header = struct (
	type as byte,
	length as byte,
);

constant apic-local-apic = struct (
	header as apic-header,
	acpi-processor-id as byte,
	apic-id as byte,
	flags as int, 
);

constant apic-io-apic = struct (
	header as apic-header,
	io-apic-id as byte,
	reserved as byte,
	io-apic-address as int,
	global-system-interrupt-base as int,
);

constant apic-interrupt-override = struct (
	header as apic-header,
	bus as byte,
	source as byte,
	interrupt as int,
	flags as (byte * 2),
);

constant apic-acpi-header = struct (
	signature as int,
	length as int,
	revision as byte,
	checksum as byte,
	oem as (byte * 6),
	oem-table-id as (byte * 8),
	oem-revision as int,
	creator-id as int,
	creator-revision as int,
);

constant apic-acpi-fadt = struct (
	header as apic-acpi-header,
	firmware-control as int,
	dsdt as int,
	reserved as byte,
	preferred-pm-profile as byte,
	sci-interrupt as (byte * 2),
	smi-command-port as int,
	acpi-enable as byte,
	acpi-disable as byte,
);

constant apic-acpi-madt = struct (
	header as apic-acpi-header,
	local-apic-address as int,
	flags as int,
);

declare io-apic-address;

declare apic-parse-madt = function [madt as pointer:apic-acpi-madt,] (
	declare io-apic-address;
	declare p = madt as int + size-of apic-acpi-madt;
	declare px = madt as int + `madt`/header/length;

	while (p < px) do (
		declare header = p as pointer:apic-header;
		declare type = extend `header`/type;

		if (type = 0) then ( -- local APIC
			declare local-apic = p as pointer:apic-local-apic;
		) else if (type = 1) then ( -- IO APIC
			declare io-apic = p as pointer:apic-io-apic;
			let io-apic-address = `io-apic`/io-apic-address;
		) else if (type = 2) then ( -- Interrupt override
			declare intr-override = p as pointer:apic-interrupt-override;
		) else ();

		p += extend `header`/length;
	);
);

declare apic-parse-dt = function [header as pointer:apic-acpi-header,] (
	if (`header`/signature = +x50434146) then (
	) else if (`header`/signature = +x43495041) then (
		apic-parse-madt [header as pointer:apic-acpi-madt,];
	) else ();
);

declare apic-parse-rsdt = function [rsdt as pointer:apic-acpi-header,] (
	declare p2 = rsdt as int + size-of apic-acpi-header;
	declare px = rsdt as int + `rsdt`/length;
	while (p2 < px) do (
		apic-parse-dt [(p2 += 4) as pointer:apic-acpi-header,];
	);
);

declare apic-parse-xsdt = function [rsdt as pointer:apic-acpi-header,] (
);

declare apic-setup = function [] (
	declare p = +xE0000;
	declare p1;

	for (declare p = +xE0000; p < +x100000; p += 16) (
		(dw-get [p,] = +x20445352 && dw-get [p + 4,] = +x20525450 -- 'RSD PTR '
			&& let p1 = p
		);
	);

	declare revision = extend `(p1 + 15) as pointer:byte`;

	if (revision = 1) then (
		declare rsdt = dw-get [p1 + 16,];
		apic-parse-rsdt [rsdt as pointer:apic-acpi-header,];
	) else if (revision = 2) then (
		declare rsdt = dw-get [p1 + 16,];
		declare xsdt = dw-get [p1 + 24,];
		if xsdt then (
			apic-parse-xsdt [xsdt as pointer:apic-acpi-header,];
		) else (
			apic-parse-rsdt [rsdt as pointer:apic-acpi-header,];
		);
	) else (
		cs-puts ["Unsupported ACPI version ",];
		cs-put-hex1 [revision,];
		cs-nl [];
	);

	cs-puts ["IO APIC address = ",];
	cs-put-hex4 [io-apic-address,];
	cs-nl [];
);
