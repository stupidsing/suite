	JMP  BYTE .boot
.boot
	CLI
	
	-- Enables A20 gate
	--MOV  AX, +x2401
	--INT  +x15
	IN   AL, +x92
	OR   AL, 2
	OUT  +x92, AL
	
	MOV  AX, +xFFFF
	MOV  DS, AX
	ADP
	MOV  BX, WORD `+x7E0E`
	
	MOV  AX, +xB800
	MOV  DS, AX
	ADP
	MOV  DWORD `0`, +x70417041
	ADP
	MOV  BYTE `4`, BL
.loop
	HLT
	JMP  .loop
	
.enableA20
	ADP
	CALL .waitA20
	MOV  AL, +xAD
	OUT  +x64, AL
	
	ADP
	CALL .waitA20
	MOV  AL, +xD0
	OUT  +x64, AL
	
	ADP
	CALL .waitA202
	IN   AL, +x60
	PUSH EAX
	
	ADP
	CALL .waitA20
	MOV  AL, +xD1
	OUT  +x64, AL
	
	ADP
	CALL .waitA20
	POP  EAX
	OR   AL, 2
	OUT  +x60, AL
	
	ADP
	CALL .waitA20
	MOV  AL, +xAE
	OUT  +x64, AL
	
	ADP
	CALL .waitA20
	RET
	
.waitA20
	IN   AL, +x64
	TEST AL, 2
	JNZ  .waitA20
	RET
	
.waitA202
	IN   AL, +x64
	TEST AL, 1
	JZ   .waitA202
	RET
	
	ADVANCE 510
	D8   +x55
	D8   +xAA

