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
	MOV  (BYTE `.bootDrive`, DL)

	-- resets disk drive
	XOR  (AX, AX)
	INT  (+x13)

	-- load 128 sectors, i.e. 64K data
	MOV  (EAX, .kernelAddress)
	SHR  (EAX, 4)
	MOV  (ES, AX)
.readNextSector
	PUSHA ()
	MOV  (AX, +x4200)
	MOV  (SI, .dap)
	AOP  ()
	MOV  (DL, BYTE `.bootDrive`)
	INT  (+x13)
--	JC   (.diskError)
	POPA ()
	AOP  ()
	ADD  (DWORD `.dapLba`, 16)
	AOP  ()
	ADD  (WORD `.dapMemAddress`, WORD 8192)
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
	IMM  (BYTE +x66)
	IMM  (BYTE +x67)
	IMM  (BYTE +xEA)
	IMM  (DWORD .kernelAddress)
	IMM  (WORD +x8)

.bootDrive
	IMM  (BYTE 0)

	ADVANCE (+x7D00)
.dap -- disk address packet for LBA BIOS
	IMM  (WORD +x0010) -- size if this structure
	IMM  (WORD +x0010) -- number of sectors to transfer
.dapMemAddress
	IMM  (WORD +x0000) -- memory address
	IMM  (WORD +x4000) -- memory segment
.dapLba
	IMM  (DWORD +x00000001) -- starting LBA, low 32-bits
	IMM  (DWORD +x00000000) -- starting LBA, high 32-bits

.gdt
	IMM  (DWORD 0)
	IMM  (DWORD 0)
	IMM  (DWORD +x0000FFFF) -- supervisor code descriptor
	IMM  (DWORD +x00CF9A00)
	IMM  (DWORD +x0000FFFF) -- supervisor data descriptor
	IMM  (DWORD +x00CF9200)
	IMM  (DWORD +x0000FFFF) -- user code descriptor
	IMM  (DWORD +x00CFFA00)
	IMM  (DWORD +x0000FFFF) -- user data descriptor
	IMM  (DWORD +x00CFF200)
	IMM  (DWORD +x08000867) -- task state segment
	IMM  (DWORD +x00408902)
.gdtr
	IMM  (WORD +x2F)
	IMM  (DWORD .gdt)

	ADVANCE (+x7DFE)
	IMM  (BYTE +x55)
	IMM  (BYTE +xAA)
