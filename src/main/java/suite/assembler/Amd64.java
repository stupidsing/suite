package suite.assembler;

public class Amd64 {

	public enum Insn {
		AAA, //
		ADC, //
		ADD, //
		AND, //
		AOP, //
		CALL, //
		CLD, //
		CLI, //
		CMP, //
		CMPXCHG, //
		CPUID, //
		DEC, //
		DIV, //
		HLT, //
		IDIV, //
		IMM, //
		IMUL, //
		IN, //
		INC, //
		INT, //
		INTO, //
		INVLPG, //
		IRET, //
		JA, //
		JAE, //
		JB, //
		JBE, //
		JE, //
		JG, //
		JGE, //
		JL, //
		JLE, //
		JMP, //
		JNE, //
		JNO, //
		JNP, //
		JNS, //
		JNZ, //
		JO, //
		JP, //
		JS, //
		JZ, //
		LEA, //
		LOCK, //
		LOOP, //
		LOOPE, //
		LOOPNE, //
		LOOPNZ, //
		LOOPZ, //
		LGDT, //
		LIDT, //
		LTR, //
		MOV, //
		MOVSB, //
		MOVSD, //
		MOVSW, //
		MUL, //
		OUT, //
		SHL, //
		TEST, //
		XCHG, //
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

	public class OpRegControl extends Operand {
		public int creg;
	}

	public class OpRegSegment extends Operand {
		public int sreg;
	}

	public class Instruction {
		public Insn insn;
		public Operand op0, op1, op2;
	}

}
