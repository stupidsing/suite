package suite.assembler;

public class Amd64 {

	public enum Insn {
		AAA, ADC, ADD, AND, AOP, CALL, CLD, CLI, CMP, CMPXCHG, CPUID, DEC, DIV, HLT, IDIV, IMM, IMUL, IN, INC, INT, INTO, INVLPG, IRET, JMP, LEA, MOV, MUL, OUT, SHL, TEST, XCHG,
	};

	public class Operand {
		public int size;
	}

	public class OpImm extends Operand {
		public long imm;
	}

	public class OpMem extends Operand {
		public int scale, indexReg, baseReg, dispSize;
		public long disp;
	}

	public class OpNone extends Operand {
	}

	public class OpReg extends Operand {
		public int reg;
	}

	public class OpRegSegment extends Operand {
		public int sreg;
	}

	public class Instruction {
		public Insn insn;
		public Operand op0, op1, op2;
	}

}
