	.org = +x7C00
	.kernelAddress = +x40000

	JMP  (BYTE .boot)
.boot
	CLI  ()

	-- enables A20 gate by BIOS
	MOV  (AX, +x2401)
	INT  (+x15)

	-- loads kernel
	AOP  ()
	MOV  (`.bootDrive`, DL)

	-- resets disk drive
	XOR  (AH, AH)
	INT  (+x13)

	-- load 128 sectors, i.e. 64K data
	MOV  (EAX, .kernelAddress)
	SHR  (EAX, 4)
	MOV  (ES, AX)
.readNextSector
	PUSHA ()
	MOV  (AH, +x42)
	MOV  (SI, .dap)
	AOP  ()
	MOV  (DL, `.bootDrive`)
	INT  (+x13)
--	JC   (.diskError)
	POPA ()
	AOP  ()
	ADD  (DWORD `.dapLba`, 16)
	AOP  ()
	ADD  (WORD `.dapMemAddress`, 8192)
	JNZ  (.readNextSector)

	-- kernel loaded to ES:[0]

	-- enters protected mode
	AOP  ()
	LGDT (`.gdtr`)

	MOV  (EAX, CR0)
	OR   (EAX, 1)
	MOV  (CR0, EAX)
	JMP  (.flush)
.flush
	MOV  (AX, 16)
	MOV  (DS, AX)
	MOV  (ES, AX)
	MOV  (FS, AX)
	MOV  (GS, AX)
	MOV  (SS, AX)

	-- jumps to the kernel
	D8   (+x66)
	D8   (+x67)
	D8   (+xEA)
	D32  (.kernelAddress)
	D16  (+x8)

.bootDrive
	D8   (0)

	ADVANCE (+x7D00)
.dap -- disk address packet for LBA BIOS
	D16  (+x0010) -- size if this structure
	D16  (+x0010) -- number of sectors to transfer
.dapMemAddress
	D16  (+x0000) -- memory address
	D16  (+x4000) -- memory segment
.dapLba
	D32  (+x00000001) -- starting LBA, low 32-bits
	D32  (+x00000000) -- starting LBA, high 32-bits

.gdt
	D32  (0)
	D32  (0)
	D32  (+x0000FFFF) -- supervisor code descriptor
	D32  (+x00CF9A00)
	D32  (+x0000FFFF) -- supervisor data descriptor
	D32  (+x00CF9200)
	D32  (+x0000FFFF) -- user code descriptor
	D32  (+x00CFFA00)
	D32  (+x0000FFFF) -- user data descriptor
	D32  (+x00CFF200)
	D32  (+x08000867) -- task state segment
	D32  (+x00408902)
.gdtr
	D16  (+x2F)
	D32  (.gdt)

	ADVANCE (+x7DFE)
	D8   (+x55)
	D8   (+xAA)
