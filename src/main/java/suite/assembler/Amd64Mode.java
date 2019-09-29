package suite.assembler;

/**
 * https://wiki.osdev.org/X86-64_Instruction_Encoding
 * 
 * section "Operand-size and address-size override prefix"
 *
 * @author ywsing
 */
public enum Amd64Mode {

	REAL16(2, 2, 2, 8), //
	PROT32(4, 4, 4, 8), //
	LONG64(4, 8, 8, 16), //
	;

	public final int opSize;
	public final int addrSize;
	public final int pushSize;
	public final int nRegisters;

	private Amd64Mode(int opSize, int addrSize, int pushSize, int nRegisters) {
		this.opSize = opSize;
		this.addrSize = addrSize;
		this.pushSize = pushSize;
		this.nRegisters = nRegisters;
	}

}
