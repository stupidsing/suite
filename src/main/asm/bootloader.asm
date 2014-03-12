	ORG  7C00
	JMP  BYTE .boot
.boot
	CLI
	
	-- Enables A20 gate by BIOS
	MOV  AX, +x2401
	INT  +x15
	
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
	
	ADVANCE +x7DFE
	D8   +x55
	D8   +xAA
