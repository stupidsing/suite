package org.asm;

public class x86 {

	public interface Operand {
	}

	public interface Register extends Operand {
	}

	public static enum Size {
		D8, D16, D32, D64
	}

	public static enum Reg8 implements Register {
		AL, AH, BL, BH, CL, CH, DL, DH
	}

	public static enum Reg16 implements Register {
		AX, BX, ECX, DX, SI, DI, BP, SP
	}

	public static enum Reg32 implements Register {
		EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP
	}

	public static enum Reg64 implements Register {
		RAX, RBX, RCX, RDX, RSI, RDI, RBP, RSP, R8, R9, R10, R11, R12, R13, R14, R15
	}

	public static enum SegmentRegister implements Operand {
		CS, DS, ES, FS, GS
	}

	public static class Immediate implements Operand {
		public int imm;
	}

	public static class Mem implements Operand {
		public static enum Scale {
			x1, x2, x4, x8
		}

		public int abs;
		public Reg32 base;
		public Scale scale;
		public Reg32 index;
	}

	public static class Instruction {
		public String name;
		public Operand operands[];
	}

}
