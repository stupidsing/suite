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
	AOP
	MOV  BX, WORD `+x7E0E`
	
	MOV  AX, +xB800
	MOV  DS, AX
	AOP
	MOV  DWORD `0`, +x70417041
	AOP
	MOV  BYTE `4`, BL
.loop
	HLT
	JMP  .loop
	
.enableA20
	AOP
	CALL .waitA20a
	MOV  AL, +xAD
	OUT  +x64, AL
	
	AOP
	CALL .waitA20a
	MOV  AL, +xD0
	OUT  +x64, AL
	
	AOP
	CALL .waitA202b
	IN   AL, +x60
	PUSH EAX
	
	AOP
	CALL .waitA20a
	MOV  AL, +xD1
	OUT  +x64, AL
	
	AOP
	CALL .waitA20a
	POP  EAX
	OR   AL, 2
	OUT  +x60, AL
	
	AOP
	CALL .waitA20a
	MOV  AL, +xAE
	OUT  +x64, AL
	
	AOP
	CALL .waitA20a
	RET
	
.waitA20a
	IN   AL, +x64
	TEST AL, 2
	JNZ  .waitA20
	RET
	
.waitA202b
	IN   AL, +x64
	TEST AL, 1
	JZ   .waitA202b
	RET
	
	ADVANCE 510
	D8   +x55
	D8   +xAA

