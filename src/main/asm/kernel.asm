	.org = +x40000
	.idtAddress = +x20000
	.stackTopAddress = +x2FFF0
	
	MOV  ESP, .stackTopAddress
	
	-- Sets up IDT
	MOV  EBX, .idtAddress
	ADD  EBX, 2048
.nextIdtEntry
	SUB  EBX, 8
	MOV  ECX, .generalInterruptHandler
	MOV  EDX, ECX
	SHR  EDX, 16
	MOV  WORD `EBX`, CX
	MOV  WORD `EBX + 2`, CS
	MOV  WORD `EBX + 4`, +x8E00
	MOV  WORD `EBX + 6`, DX
	CMP  EBX, .idtAddress
	JNE  .nextIdtEntry
	LIDT `.idtr`
	
	-- Sets the 8253 to enable 100 timer ticks per second, and enable keyboard
	MOV  AL, +x36
	OUT  +x43, AL
	MOV  AX, 11932
	OUT  +x40, AL
	MOV  AL, AH
	OUT  +x40, AL
	MOV  AL, +xFC
	OUT  +x21, AL
	
	-- Show some fancy stuff on screen
	MOV  DWORD `+xB8000`, +x70417041
	STI
	
.loop
	HLT
	JMP  .loop
	
.generalInterruptHandler
	INC  BYTE `+xB8000`
	
	-- Sends end of interrupt signal
	MOV  AL, +x20
	OUT  +x20, AL
	IRET
	
.idtr
	D16  +x7FF
	D32  .idtAddress
	