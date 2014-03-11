	JMP  BYTE .boot
.boot
	CLI
	
	-- Enables A20 gate
	MOV  AX, +x2401
	INT  +x15
	-- IN   AL, +x92
	-- OR   AL, 2
	-- OUT  +x92, AL
	
	MOV  AX, +xFFFF
	MOV  DS, AX
	D8   +x67
	MOV  BX, WORD `+x7E0E`
	
	MOV  AX, +xB800
	MOV  DS, AX
	D8   +x67
	MOV  DWORD `0`, +x70417041
	D8   +x67
	MOV  BYTE `4`, BL
.loop
	HLT
	JMP  .loop
	
	ADVANCE 510
	D8   +x55
	D8   +xAA
