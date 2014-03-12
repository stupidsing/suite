.method0
	MOV  AX, +x2401
	INT  +x15
	RET
	
.method1	
	IN   AL, +x92
	OR   AL, 2
	OUT  +x92, AL
	RET
	
.method2
	CALL .waitA20a
	MOV  AL, +xAD
	OUT  +x64, AL
	
	CALL .waitA20a
	MOV  AL, +xD0
	OUT  +x64, AL
	
	CALL .waitA202b
	IN   AL, +x60
	PUSH EAX
	
	CALL .waitA20a
	MOV  AL, +xD1
	OUT  +x64, AL
	
	CALL .waitA20a
	POP  EAX
	OR   AL, 2
	OUT  +x60, AL
	
	CALL .waitA20a
	MOV  AL, +xAE
	OUT  +x64, AL
	
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
