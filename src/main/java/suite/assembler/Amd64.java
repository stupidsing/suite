package suite.assembler;

import primal.adt.map.BiHashMap;
import primal.adt.map.BiMap;
import suite.node.Atom;
import suite.object.ToStringObject;
import suite.util.To;

public class Amd64 {

	public static final Amd64 me = new Amd64();

	public enum Insn {
		AAA, //
		ADC, //
		ADD, //
		ADDPS, //
		ADVANCE, //
		ALIGN, //
		AND, //
		AOP, //
		CALL, //
		CLD, //
		CLI, //
		CMP, //
		CMPSB, //
		CMPSD, //
		CMPSQ, //
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
		MOVSQ, //
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
		STOSQ, //
		STOSW, //
		SUB, //
		SUBPS, //
		SYSCALL, //
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

	public class Operand extends ToStringObject<Operand> {
		public int size;
	}

	public class OpImm extends Operand {
		public long imm;

		public boolean isBound() {
			var opImmLabel = cast(OpImmLabel.class);
			return opImmLabel == null || opImmLabel.assigned;
		}
	}

	public class OpImmLabel extends OpImm {
		public boolean assigned;

		public OpImmLabel(int size) {
			this.size = size;
		}

		public void adjustImm(long imm) {
			assigned = true;
			this.imm += imm;
		}
	}

	public class OpMem extends Operand {
		public int baseReg, indexReg, scale;
		public OpImm disp;
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

	public class OpRegXmm extends Operand {
		public int reg;
	}

	public class OpRegYmm extends Operand {
		public int reg;
	}

	public class Instruction extends ToStringObject<Instruction> {
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

	public int axReg = 0;
	public int cxReg = 1;
	public int dxReg = 2;
	public int bxReg = 3;
	public int spReg = 4;
	public int bpReg = 5;
	public int siReg = 6;
	public int diReg = 7;

	public OpReg al = reg8[axReg];
	public OpReg cl = reg8[cxReg];
	public OpReg dl = reg8[dxReg];
	public OpReg bl = reg8[bxReg];
	public OpReg eax = reg32[axReg];
	public OpReg ecx = reg32[cxReg];
	public OpReg edx = reg32[dxReg];
	public OpReg ebx = reg32[bxReg];
	public OpReg esp = reg32[spReg];
	public OpReg ebp = reg32[bpReg];
	public OpReg esi = reg32[siReg];
	public OpReg edi = reg32[diReg];
	public OpReg rax = reg64[axReg];
	public OpReg rcx = reg64[cxReg];
	public OpReg rdx = reg64[dxReg];
	public OpReg rbx = reg64[bxReg];
	public OpReg rsp = reg64[spReg];
	public OpReg rbp = reg64[bpReg];
	public OpReg rsi = reg64[siReg];
	public OpReg rdi = reg64[diReg];

	public BiMap<Atom, OpReg> regByName = new BiHashMap<>() {
		{
			String[] rbs = { "AL", "CL", "DL", "BL", "SPL", "BPL", "SIL", "DIL", };
			String[] rws = { "AX", "CX", "DX", "BX", "SP", "BP", "SI", "DI", };
			String[] rds = { "EAX", "ECX", "EDX", "EBX", "ESP", "EBP", "ESI", "EDI", };
			String[] rqs = { "RAX", "RCX", "RDX", "RBX", "RSP", "RBP", "RSI", "RDI", };

			for (var i = 0; i < 8; i++) {
				put(Atom.of(rbs[i]), reg8[i]);
				put(Atom.of(rws[i]), reg16[i]);
				put(Atom.of(rds[i]), reg32[i]);
				put(Atom.of(rqs[i]), reg64[i]);
			}

			for (var i = 8; i < 16; i++) {
				put(Atom.of("R" + i + "B"), reg8[i]);
				put(Atom.of("R" + i + "W"), reg16[i]);
				put(Atom.of("R" + i + "D"), reg32[i]);
				put(Atom.of("R" + i), reg64[i]);
			}
		}
	};

	public BiMap<Atom, OpRegControl> cregByName = new BiHashMap<>() {
		{
			for (var i : new int[] { 0, 2, 3, 4, })
				put(Atom.of("CR" + i), newRegControl(i));
		}
	};

	public BiMap<Atom, OpRegSegment> sregByName = new BiHashMap<>() {
		{
			String[] srs = { "ES", "CS", "SS", "DS", "FS", "GS", };

			for (var i = 0; i < srs.length; i++)
				put(Atom.of(srs[i]), newRegSegment(i));
		}
	};

	public BiMap<Atom, Operand> registerByName = new BiHashMap<>() {
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

	public Operand imm16(long imm) {
		return imm(imm, 2);
	}

	public Operand imm32(long imm) {
		return imm(imm, 4);
	}

	public Operand imm64(long imm) {
		return imm(imm, 8);
	}

	public OpImm imm(long imm, int size) {
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

	public OpMem mem(OpImm opDisp, int size) {
		return mem(null, null, 0, opDisp, size);
	}

	public OpMem mem(OpReg baseReg, long disp, int size) {
		return mem(baseReg, null, 0, disp, size);
	}

	public OpMem mem(OpReg baseReg, OpReg indexReg, int scale, long disp, int size) {
		var opDisp = new OpImm();
		opDisp.imm = disp;
		opDisp.size = size(disp);
		return mem(baseReg, indexReg, scale, opDisp, size);
	}

	public OpMem mem(OpReg baseReg, OpReg indexReg, int scale, OpImm opDisp, int size) {
		var op = new OpMem();
		op.baseReg = baseReg != null ? baseReg.reg : -1;
		op.indexReg = indexReg != null ? indexReg.reg : -1;
		op.scale = scale;
		op.size = size;
		op.disp = opDisp;
		return op;
	}

	public OpReg[] regs(int size) {
		if (size == 1)
			return reg8;
		else if (size == 2)
			return reg16;
		else if (size == 4)
			return reg32;
		else if (size == 8)
			return reg64;
		else
			return null;
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
			return 0;
		else if (v == (byte) v)
			return 1;
		else if (v == (int) v)
			return 4;
		else
			return 8;
	}

}
