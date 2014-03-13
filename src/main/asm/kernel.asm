	ORG  40000
	
	-- Show some fancy stuff on screen	
	MOV  DWORD `+xB8000`, +x70417041
.loop
	HLT
	JMP  .loop
	
	ADVANCE +x40200
	