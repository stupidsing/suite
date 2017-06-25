package suite.assembler;

import java.util.HashMap;
import java.util.Map;

import suite.node.Atom;
import suite.node.Node;

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
		LABEL, //
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
		MOVSX, //
		MOVZX, //
		MUL, //
		NOP, //
		OR, //
		OUT, //
		POP, //
		POPA, //
		POPF, //
		PUSH, //
		PUSHA, //
		PUSHF, //
		RDMSR, //
		REP, //
		REPE, //
		REPNE, //
		RET, //
		SAL, //
		SAR, //
		SBB, //
		SETA, //
		SETAE, //
		SETB, //
		SETBE, //
		SETE, //
		SETG, //
		SETGE, //
		SETL, //
		SETLE, //
		SETNE, //
		SHL, //
		SHR, //
		STI, //
		STOSB, //
		STOSD, //
		STOSW, //
		SUB, //
		SYSENTER, //
		SYSEXIT, //
		WRMSR, //
		TEST, //
		XCHG, //
		XOR, //
	};

	public class Operand {
		public int size;
	}

	public class OpImm extends Operand {
		public long imm;
	}

	public class OpMem extends Operand {
		public int baseReg, indexReg, scale, dispSize;
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

	public final Operand none = new OpNone();

	public Map<Node, OpReg> regsByName = new HashMap<Node, OpReg>() {
		private static final long serialVersionUID = 1l;

		{
			put(Atom.of("AL"), newReg(1, 0));
			put(Atom.of("CL"), newReg(1, 1));
			put(Atom.of("DL"), newReg(1, 2));
			put(Atom.of("BL"), newReg(1, 3));
			put(Atom.of("SPL"), newReg(1, 4));
			put(Atom.of("BPL"), newReg(1, 5));
			put(Atom.of("SIL"), newReg(1, 6));
			put(Atom.of("DIL"), newReg(1, 7));
			put(Atom.of("R8B"), newReg(1, 8));
			put(Atom.of("R9B"), newReg(1, 9));
			put(Atom.of("R10B"), newReg(1, 10));
			put(Atom.of("R11B"), newReg(1, 11));
			put(Atom.of("R12B"), newReg(1, 12));
			put(Atom.of("R13B"), newReg(1, 13));
			put(Atom.of("R14B"), newReg(1, 14));
			put(Atom.of("R15B"), newReg(1, 15));
			put(Atom.of("AX"), newReg(2, 0));
			put(Atom.of("CX"), newReg(2, 1));
			put(Atom.of("DX"), newReg(2, 2));
			put(Atom.of("BX"), newReg(2, 3));
			put(Atom.of("SP"), newReg(2, 4));
			put(Atom.of("BP"), newReg(2, 5));
			put(Atom.of("SI"), newReg(2, 6));
			put(Atom.of("DI"), newReg(2, 7));
			put(Atom.of("R8W"), newReg(2, 8));
			put(Atom.of("R9W"), newReg(2, 9));
			put(Atom.of("R10W"), newReg(2, 10));
			put(Atom.of("R11W"), newReg(2, 11));
			put(Atom.of("R12W"), newReg(2, 12));
			put(Atom.of("R13W"), newReg(2, 13));
			put(Atom.of("R14W"), newReg(2, 14));
			put(Atom.of("R15W"), newReg(2, 15));
			put(Atom.of("EAX"), newReg(4, 0));
			put(Atom.of("ECX"), newReg(4, 1));
			put(Atom.of("EDX"), newReg(4, 2));
			put(Atom.of("EBX"), newReg(4, 3));
			put(Atom.of("ESP"), newReg(4, 4));
			put(Atom.of("EBP"), newReg(4, 5));
			put(Atom.of("ESI"), newReg(4, 6));
			put(Atom.of("EDI"), newReg(4, 7));
			put(Atom.of("R8D"), newReg(4, 8));
			put(Atom.of("R9D"), newReg(4, 9));
			put(Atom.of("R10D"), newReg(4, 10));
			put(Atom.of("R11D"), newReg(4, 11));
			put(Atom.of("R12D"), newReg(4, 12));
			put(Atom.of("R13D"), newReg(4, 13));
			put(Atom.of("R14D"), newReg(4, 14));
			put(Atom.of("R15D"), newReg(4, 15));
			put(Atom.of("RAX"), newReg(8, 0));
			put(Atom.of("RCX"), newReg(8, 1));
			put(Atom.of("RDX"), newReg(8, 2));
			put(Atom.of("RBX"), newReg(8, 3));
			put(Atom.of("RSP"), newReg(8, 4));
			put(Atom.of("RBP"), newReg(8, 5));
			put(Atom.of("RSI"), newReg(8, 6));
			put(Atom.of("RDI"), newReg(8, 7));
			put(Atom.of("R8"), newReg(8, 8));
			put(Atom.of("R9"), newReg(8, 9));
			put(Atom.of("R10"), newReg(8, 10));
			put(Atom.of("R11"), newReg(8, 11));
			put(Atom.of("R12"), newReg(8, 12));
			put(Atom.of("R13"), newReg(8, 13));
			put(Atom.of("R14"), newReg(8, 14));
			put(Atom.of("R15"), newReg(8, 15));

		}
	};

	public Map<Node, OpRegControl> cregsByName = new HashMap<Node, OpRegControl>() {
		private static final long serialVersionUID = 1l;

		{
			put(Atom.of("CR0"), newRegControl(0));
			put(Atom.of("CR2"), newRegControl(2));
			put(Atom.of("CR3"), newRegControl(3));
			put(Atom.of("CR4"), newRegControl(4));
		}
	};

	public Map<Node, OpRegSegment> sregsByName = new HashMap<Node, OpRegSegment>() {
		private static final long serialVersionUID = 1l;

		{
			put(Atom.of("ES"), newRegSegment(0));
			put(Atom.of("CS"), newRegSegment(1));
			put(Atom.of("SS"), newRegSegment(2));
			put(Atom.of("DS"), newRegSegment(3));
			put(Atom.of("FS"), newRegSegment(4));
			put(Atom.of("GS"), newRegSegment(5));
		}
	};

	public Operand imm(long imm) {
		return imm(imm, size(imm));
	}

	public Operand imm(long imm, int size) {
		OpImm op = new OpImm();
		op.imm = imm;
		op.size = size;
		return op;
	}

	public Instruction instruction(Insn insn, Operand... ops) {
		Instruction instruction = new Instruction();
		instruction.insn = insn;
		instruction.op0 = 0 < ops.length ? ops[0] : none;
		instruction.op1 = 1 < ops.length ? ops[1] : none;
		instruction.op2 = 2 < ops.length ? ops[2] : none;
		return instruction;
	}

	public OpMem mem(OpReg reg, long disp, int size) {
		OpMem op = new OpMem();
		op.baseReg = reg.reg;
		op.indexReg = -1;
		op.size = size;
		op.disp = disp;
		op.dispSize = size(disp);
		return op;
	}

	public OpReg reg(String name) {
		return regsByName.get(Atom.of(name));
	}

	private OpReg newReg(int size, int reg) {
		OpReg opReg = new OpReg();
		opReg.size = size;
		opReg.reg = reg;
		return opReg;
	}

	private OpRegControl newRegControl(int creg) {
		OpRegControl opRegControl = new OpRegControl();
		opRegControl.size = 4;
		opRegControl.creg = creg;
		return opRegControl;
	}

	private OpRegSegment newRegSegment(int creg) {
		OpRegSegment opRegSegment = new OpRegSegment();
		opRegSegment.size = 2;
		opRegSegment.sreg = creg;
		return opRegSegment;
	}

	private int size(long v) {
		if (v == (byte) v)
			return 1;
		else if (v == (int) v)
			return 4;
		else
			return 8;
	}

}
