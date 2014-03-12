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
	MOV  SI, 128
	
	XOR  BX, BX
	MOV  AX, 1
.readNextSector
	PUSHA
	XOR  DX, DX
	MOV  SI, +x12 -- nSectorsPerTrack
	DIV  SI
	MOV  CL, DL
	INC  CL
	XOR  DX, DX
	MOV  SI, 2 -- nHeads
	DIV  SI
	MOV  DH, DL
	MOV  CH, AL
	AOP
	MOV  DL, `.bootDrive`
	MOV  AX, +x0201
	INT  +x13
--	JC   .diskError
	POPA
	ADD  BX, 512
	INC  AX
	DEC  SI
	JNZ  .readNextSector
	
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
		
	ADVANCE +x7DFE
	D8   +x55
	D8   +xAA
