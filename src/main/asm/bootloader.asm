	JMP  BYTE .boot
.boot
	CLI
	MOV  AX, +xB800
	MOV  DS, AX
	D8   +x67
	MOV  DWORD `0`, +x70417041
.loop
	HLT
	JMP  .loop

	ADVANCE 510
	D8   +x55
	D8   +xAA
