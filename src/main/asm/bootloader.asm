	ORG  7C00
	JMP  BYTE .boot
.boot
	CLI
	
	-- Enables A20 gate by BIOS
	MOV  AX, +x2401
	INT  +x15

	-- Loads kernel
	AOP	
	MOV  `.bootDrive`, DL
	
	-- Resets disk drive
	XOR  AH, AH
	INT  +x13
	
	-- Load 128 sectors, i.e. 64K data
	MOV  AX, +x4000
	MOV  ES, AX
.readNextSector
	PUSHA
	MOV  AH, +x42
	MOV  SI, .dap
	AOP
	MOV  DL, `.bootDrive`
	INT  +x13
--	JC   .diskError
	POPA
	AOP
	ADD  DWORD `.dapLba`, 16
	AOP
	ADD  WORD `.dapMemAddress`, 8192
	JNZ  .readNextSector
	
	-- Kernel loaded to ES:[0]
	
	-- Show some fancy stuff on screen	
	MOV  AX, +xB800
	MOV  DS, AX
	AOP
	MOV  DWORD `0`, +x70417041
	AOP
	MOV  BYTE `4`, BL
.loop
	HLT
	JMP  .loop
	
.bootDrive
	D8   0
	
	ADVANCE +x7DE0
.dap -- Disk address packet for LBA BIOS
	D16  16
.dapNumOfSectors
	D16  16 -- Number of sectors to transfer
.dapMemAddress
	D16  0 -- Memory address
	D16  +x4000 -- Memory segment
.dapLba
	D32  1 -- Starting LBA
	D32  0
	
	ADVANCE +x7DFE
	D8   +x55
	D8   +xAA
