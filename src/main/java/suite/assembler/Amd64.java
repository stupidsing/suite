package suite.assembler;

import suite.adt.map.BiMap;
import suite.adt.map.HashBiMap;
import suite.node.Atom;
import suite.util.To;

public class Amd64 {

	public static final Amd64 me = new Amd64();

	public enum Insn {
		AAA, //
		ADC, //
		ADD, //
		ADDPS, //
		AND, //
		AOP, //
		CALL, //
		CLD, //
		CLI, //
		CMP, //
		CMPSB, //
		CMPSD, //
		CMPSW, //
		CMPXCHG, //
		CPUID, //
		D, //
		DEC, //
		DIV, //
		DS, // define space
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
		LOG, //
		LOOP, //
		LOOPE, //
		LOOPNE, //
		LOOPNZ, //
		LOOPZ, //
		LGDT, //
		LIDT, //
		LTR, //
		MOV, //
		MOVAPS, //
		MOVD, //
		MOVQ, //
		MOVSB, //
		MOVSD, //
		MOVSW, //
		MOVSX, //
		MOVZX, //
		MUL, //
		MULPS, //
		NEG, //
		NOP, //
		NOT, //
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
		SUBPS, //
		SYSENTER, //
		SYSEXIT, //
		WRMSR, //
		TEST, //
		VADDPS, //
		VMOVAPS, //
		VMOVD, //
		VMOVQ, //
		VMULPS, //
		VSUBPS, //
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

	public class OpRegXmm extends OpReg {
		public int reg;
	}

	public class OpRegYmm extends OpReg {
		public int reg;
	}

	public class Instruction {
		public Insn insn;
		public Operand op0, op1, op2;
	}

	public Operand none = new OpNone();

	public OpReg[] reg8 = To.array(16, OpReg.class, r -> newReg(1, r));
	public OpReg[] reg16 = To.array(16, OpReg.class, r -> newReg(2, r));
	public OpReg[] reg32 = To.array(16, OpReg.class, r -> newReg(4, r));
	public OpReg[] reg64 = To.array(16, OpReg.class, r -> newReg(8, r));
	public OpRegXmm[] regXmm = To.array(16, OpRegXmm.class, this::newRegXmm);
	public OpRegYmm[] regYmm = To.array(16, OpRegYmm.class, this::newRegYmm);

	public OpReg al = reg8[0];
	public OpReg cl = reg8[1];
	public OpReg dl = reg8[2];
	public OpReg bl = reg8[3];
	public OpReg eax = reg32[0];
	public OpReg ecx = reg32[1];
	public OpReg edx = reg32[2];
	public OpReg ebx = reg32[3];
	public OpReg esp = reg32[4];
	public OpReg ebp = reg32[5];
	public OpReg esi = reg32[6];
	public OpReg edi = reg32[7];

	public BiMap<Atom, OpReg> regByName = new HashBiMap<>() {
		{
			put(Atom.of("AL"), al);
			put(Atom.of("CL"), cl);
			put(Atom.of("DL"), dl);
			put(Atom.of("BL"), bl);
			put(Atom.of("SPL"), reg8[4]);
			put(Atom.of("BPL"), reg8[5]);
			put(Atom.of("SIL"), reg8[6]);
			put(Atom.of("DIL"), reg8[7]);
			put(Atom.of("R8B"), reg8[8]);
			put(Atom.of("R9B"), reg8[9]);
			put(Atom.of("R10B"), reg8[10]);
			put(Atom.of("R11B"), reg8[11]);
			put(Atom.of("R12B"), reg8[12]);
			put(Atom.of("R13B"), reg8[13]);
			put(Atom.of("R14B"), reg8[14]);
			put(Atom.of("R15B"), reg8[15]);
			put(Atom.of("AX"), reg16[0]);
			put(Atom.of("CX"), reg16[1]);
			put(Atom.of("DX"), reg16[2]);
			put(Atom.of("BX"), reg16[3]);
			put(Atom.of("SP"), reg16[4]);
			put(Atom.of("BP"), reg16[5]);
			put(Atom.of("SI"), reg16[6]);
			put(Atom.of("DI"), reg16[7]);
			put(Atom.of("R8W"), reg16[8]);
			put(Atom.of("R9W"), reg16[9]);
			put(Atom.of("R10W"), reg16[10]);
			put(Atom.of("R11W"), reg16[11]);
			put(Atom.of("R12W"), reg16[12]);
			put(Atom.of("R13W"), reg16[13]);
			put(Atom.of("R14W"), reg16[14]);
			put(Atom.of("R15W"), reg16[15]);
			put(Atom.of("EAX"), eax);
			put(Atom.of("ECX"), ecx);
			put(Atom.of("EDX"), edx);
			put(Atom.of("EBX"), ebx);
			put(Atom.of("ESP"), esp);
			put(Atom.of("EBP"), ebp);
			put(Atom.of("ESI"), esi);
			put(Atom.of("EDI"), edi);
			put(Atom.of("R8D"), reg32[8]);
			put(Atom.of("R9D"), reg32[9]);
			put(Atom.of("R10D"), reg32[10]);
			put(Atom.of("R11D"), reg32[11]);
			put(Atom.of("R12D"), reg32[12]);
			put(Atom.of("R13D"), reg32[13]);
			put(Atom.of("R14D"), reg32[14]);
			put(Atom.of("R15D"), reg32[15]);
			put(Atom.of("RAX"), reg64[0]);
			put(Atom.of("RCX"), reg64[1]);
			put(Atom.of("RDX"), reg64[2]);
			put(Atom.of("RBX"), reg64[3]);
			put(Atom.of("RSP"), reg64[4]);
			put(Atom.of("RBP"), reg64[5]);
			put(Atom.of("RSI"), reg64[6]);
			put(Atom.of("RDI"), reg64[7]);
			put(Atom.of("R8"), reg64[8]);
			put(Atom.of("R9"), reg64[9]);
			put(Atom.of("R10"), reg64[10]);
			put(Atom.of("R11"), reg64[11]);
			put(Atom.of("R12"), reg64[12]);
			put(Atom.of("R13"), reg64[13]);
			put(Atom.of("R14"), reg64[14]);
			put(Atom.of("R15"), reg64[15]);

		}
	};

	public BiMap<Atom, OpRegControl> cregByName = new HashBiMap<>() {
		{
			put(Atom.of("CR0"), newRegControl(0));
			put(Atom.of("CR2"), newRegControl(2));
			put(Atom.of("CR3"), newRegControl(3));
			put(Atom.of("CR4"), newRegControl(4));
		}
	};

	public BiMap<Atom, OpRegSegment> sregByName = new HashBiMap<>() {
		{
			put(Atom.of("ES"), newRegSegment(0));
			put(Atom.of("CS"), newRegSegment(1));
			put(Atom.of("SS"), newRegSegment(2));
			put(Atom.of("DS"), newRegSegment(3));
			put(Atom.of("FS"), newRegSegment(4));
			put(Atom.of("GS"), newRegSegment(5));
		}
	};

	public BiMap<Atom, Operand> registerByName = new HashBiMap<>() {
		{
			putAll(regByName);
			putAll(cregByName);
			putAll(sregByName);
			for (var i = 0; i < 16; i++) {
				put(Atom.of("XMM" + i), regXmm[i]);
				put(Atom.of("YMM" + i), regYmm[i]);
			}
		}
	};

	public Operand imm8(long imm) {
		return imm(imm, 1);
	}

	public Operand imm32(long imm) {
		return imm(imm, 4);
	}

	public Operand imm(long imm, int size) {
		var op = new OpImm();
		op.imm = imm;
		op.size = size;
		return op;
	}

	public Instruction instruction(Insn insn, Operand... ops) {
		var instruction = new Instruction();
		instruction.insn = insn;
		instruction.op0 = 0 < ops.length ? ops[0] : none;
		instruction.op1 = 1 < ops.length ? ops[1] : none;
		instruction.op2 = 2 < ops.length ? ops[2] : none;
		return instruction;
	}

	public OpMem mem(OpReg baseReg, long disp, int size) {
		return mem(baseReg, null, 1, disp, size);
	}

	public OpMem mem(OpReg baseReg, OpReg indexReg, int scale, long disp, int size) {
		var op = new OpMem();
		op.baseReg = baseReg != null ? baseReg.reg : -1;
		op.indexReg = indexReg != null ? indexReg.reg : -1;
		op.scale = scale;
		op.size = size;
		op.disp = disp;
		op.dispSize = disp != 0 ? size(disp) : 0;
		return op;
	}

	public OpReg reg(String name) {
		return regByName.get(Atom.of(name));
	}

	private Amd64() {
	}

	private OpReg newReg(int size, int reg) {
		var opReg = new OpReg();
		opReg.size = size;
		opReg.reg = reg;
		return opReg;
	}

	private OpRegControl newRegControl(int creg) {
		var opRegControl = new OpRegControl();
		opRegControl.size = 4;
		opRegControl.creg = creg;
		return opRegControl;
	}

	private OpRegSegment newRegSegment(int sreg) {
		var opRegSegment = new OpRegSegment();
		opRegSegment.size = 2;
		opRegSegment.sreg = sreg;
		return opRegSegment;
	}

	private OpRegXmm newRegXmm(int xreg) {
		var opRegXmm = new OpRegXmm();
		opRegXmm.size = 16;
		opRegXmm.reg = xreg;
		return opRegXmm;
	}

	private OpRegYmm newRegYmm(int yreg) {
		var opRegYmm = new OpRegYmm();
		opRegYmm.size = 32;
		opRegYmm.reg = yreg;
		return opRegYmm;
	}

	private int size(long v) {
		if (v == 0)
			return -1;
		else if (v == (byte) v)
			return 1;
		else if (v == (int) v)
			return 4;
		else
			return 8;
	}

}
