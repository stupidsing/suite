	JMP  BYTE .boot
.boot
	CLI
	MOV  AX, +xB800
	MOV  DS, AX
	MOV  DWORD `0`, +x41704170
.loop
	HLT
	JMP  .loop

	ADVANCE 510
	D8   +x55
	D8   +xAA
