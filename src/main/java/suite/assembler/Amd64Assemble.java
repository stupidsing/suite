package suite.assembler;

import static java.lang.Math.min;
import static java.util.Map.entry;
import static primal.statics.Fail.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import primal.os.Log_;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.Bytes.BytesBuilder;
import primal.statics.Fail;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpImmLabel;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpNone;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.OpRegControl;
import suite.assembler.Amd64.OpRegSegment;
import suite.assembler.Amd64.OpRegXmm;
import suite.assembler.Amd64.OpRegYmm;
import suite.assembler.Amd64.Operand;
import suite.inspect.Dump;
import suite.primitive.Bytes_;

// TODO validate number of operands
// TODO validate size of operands
// do not use SPL, BPL, SIL, DIL in 32-bit mode
// do not use AH, BH, CH, DH in 64-bit long mode
public class Amd64Assemble {

	private InsnCode invalid = new InsnCode(-1, new byte[0]);
	private Amd64 amd64 = Amd64.me;
	private Amd64Mode mode;
	private boolean isLongMode;

	private enum Vexm {
		VM0F__, VM0F38, VM0F3A,
	};

	private enum Vexp {
		VP__, VP66, VPF3, VPF2,
	};

	private Map<Vexp, Integer> vexps = Map.ofEntries( //
			entry(Vexp.VP__, 0), //
			entry(Vexp.VP66, 1), //
			entry(Vexp.VPF3, 2), //
			entry(Vexp.VPF2, 3));

	private Map<Vexm, Integer> vexms = Map.ofEntries( //
			entry(Vexm.VM0F__, 1), //
			entry(Vexm.VM0F38, 2), //
			entry(Vexm.VM0F3A, 3));

	private interface Encode {
		public boolean isValid();

		public Bytes encode_(long offset, Instruction instruction);
	}

	private class VexCode implements Encode {
		public int m, w, p, v;
		public InsnCode code;

		public boolean isValid() {
			return code.isValid();
		}

		public Bytes encode_(long offset, Instruction instruction) {
			return encode(offset, code, false, vex(m, p, code.opSize, code.modrm, w, v));
		}
	}

	private class InsnCode implements Encode {
		public int opSize;
		public byte[] bs;
		public Modrm modrm;
		public long imm;
		public int immSize;

		private InsnCode(int opSize, OpImm imm) {
			this(opSize, imm.imm, imm.size);
		}

		private InsnCode(int opSize, long imm, int immSize) {
			this.opSize = opSize;
			this.imm = imm;
			this.immSize = immSize;
		}

		private InsnCode(byte[] bs) {
			this(mode.opSize, bs);
		}

		private InsnCode(int opSize, byte[] bs) {
			this.opSize = opSize;
			this.bs = bs;
		}

		private InsnCode imm(OpImm imm) {
			return imm(imm.imm, imm.size);
		}

		private InsnCode imm(long imm1, int size1) {
			return set(opSize, bs, imm1, size1);
		}

		private InsnCode pre(int pre) {
			return pre(bs(pre));
		}

		private InsnCode pre(byte[] pre) {
			var length0 = pre.length;
			var length1 = bs.length;
			var bs1 = Arrays.copyOf(pre, length0 + length1);
			Bytes_.copy(bs, 0, bs1, length0, length1);
			return set(opSize, bs1, imm, immSize);
		}

		private InsnCode setByte(int b) {
			return set(opSize, bs(b), imm, immSize);
		}

		private InsnCode size(int size1) {
			return set(size1, bs, imm, immSize);
		}

		private InsnCode set(int opSize1, byte[] bs1, long imm, int immSize) {
			var insnCode = new InsnCode(opSize1, bs1);
			insnCode.modrm = modrm;
			insnCode.immSize = immSize;
			insnCode.imm = imm;
			return insnCode;
		}

		private Encode vex(Vexp vexp, Operand op, Vexm vexm) {
			var opReg = (OpReg) op;
			if (opReg.size == opSize)
				return vex(vexp, opReg.reg, vexm, opSize == 8 ? 1 : 0);
			else
				return invalid;
		}

		private VexCode vex(Vexp vexp, int v, Vexm vexm) {
			return vex(vexp, v, vexm, opSize == 8 ? 1 : 0);
		}

		private VexCode vex(Vexp vexp, int v, Vexm vexm, int w) {
			var vex = new VexCode();
			vex.p = vexps.get(vexp);
			vex.v = v;
			vex.m = vexms.get(vexm);
			vex.w = w;
			return vex;
		}

		public boolean isValid() {
			return 0 < opSize;
		}

		public Bytes encode_(long offset, Instruction instruction) {
			var isValid = opSize == 1 || opSize == 2 || opSize == 4 || opSize == 8 ? this : invalid;

			var isEnforceRex = false //
					|| amd64.reg8nonHighs.contains(instruction.op0) //
					|| amd64.reg8nonHighs.contains(instruction.op1);

			var isNotEnforceRex = false //
					|| amd64.reg8highs.contains(instruction.op0) //
					|| amd64.reg8highs.contains(instruction.op1);

			if (!isLongMode)
				return encode(offset, isValid, false, null);
			else if (!isEnforceRex || !isNotEnforceRex)
				return encode(offset, isValid, isEnforceRex, null);
			else
				return fail("bad instruction");
		}
	}

	private class Modrm {
		private int mod, num, rm, s, i, b, dispSize;
		private long disp;
	}

	public Amd64Assemble(Amd64Mode mode) {
		this.mode = mode;
		this.isLongMode = mode == Amd64Mode.LONG64;
	}

	public Bytes assemble(long offset, List<Instruction> instructions, boolean dump) {
		assemblePass(false, offset, instructions);
		var bytes = assemblePass(true, offset, instructions);
		if (dump)
			Log_.info(new Amd64Dump().dump(instructions));
		return bytes;
	}

	private Bytes assemblePass(boolean isPass2, long offset, List<Instruction> instructions) {
		var bb = new BytesBuilder();
		for (var instruction : instructions)
			try {
				var bytes = assemble(isPass2, offset, instruction);
				bb.append(bytes);
				offset += bytes.size();
			} catch (Exception ex) {
				return fail("for " + Dump.toLine(instruction), ex);
			}
		return bb.toBytes();
	}

	public Bytes assemble(boolean isPass2, long offset, Instruction instruction) {
		Encode encode;
		OpImm opImm;
		OpReg opReg;
		OpRegControl opRegCtrl;
		OpRegSegment opRegSegment;
		byte[] bs;

		switch (instruction.insn) {
		case AAA:
			encode = assemble(0x37);
			break;
		case ADC:
			encode = assembleRmRegImm(instruction, 0x10, 0x80, 2);
			break;
		case ADD:
			encode = assembleRmRegImm(instruction, 0x00, 0x80, 0);
			break;
		case ADDPS:
			encode = assembleRegRm(instruction.op0, instruction.op1, 0x58).pre(0x0F);
			break;
		case ADVANCE:
			encode = new InsnCode(new byte[(int) (((OpImm) instruction.op0).imm - offset)]);
			break;
		case ALIGN:
			var align = instruction.op0.cast(OpImm.class).imm;
			var alignm1 = align - 1;
			bs = new byte[(int) (align - (offset & alignm1) & alignm1)];
			Arrays.fill(bs, (byte) 0x90);
			encode = new InsnCode(Bytes.of(bs).toArray());
			break;
		case AND:
			encode = assembleRmRegImm(instruction, 0x20, 0x80, 4);
			break;
		case AOP:
			encode = assemble(0x67);
			break;
		case CALL:
			if ((opImm = instruction.op0.cast(OpImm.class)) != null && 4 <= instruction.op0.size)
				encode = assembleJumpImm(opImm, offset, -1, bs(0xE8));
			else if (isRm.test(instruction.op0))
				encode = assemble(instruction.op0, 0xFF, 2);
			else
				encode = invalid;
			break;
		case CLD:
			encode = assemble(0xFC);
			break;
		case CLI:
			encode = assemble(0xFA);
			break;
		case CMP:
			encode = assembleRmRegImm(instruction, 0x38, 0x80, 7);
			break;
		case CMPSB:
			encode = new InsnCode(1, bs(0xA6));
			break;
		case CMPSD:
			encode = new InsnCode(4, bs(0xA7));
			break;
		case CMPSQ:
			encode = new InsnCode(8, bs(0xA7));
			break;
		case CMPSW:
			encode = new InsnCode(2, bs(0xA7));
			break;
		case CMPXCHG:
			encode = assembleRmReg(instruction, 0xB0, -1, isReg).pre(0x0F);
			break;
		case CPUID:
			encode = new InsnCode(bs(0x0F, 0xA2));
			break;
		case D:
			opImm = (OpImm) instruction.op0;
			var bb = new BytesBuilder();
			appendImm(bb, opImm.size, opImm.imm);
			encode = new InsnCode(bb.toBytes().toArray());
			break;
		case DEC:
			encode = assembleRm(instruction, isLongMode ? -1 : 0x48, 0xFE, 1);
			break;
		case DIV:
			encode = assembleByteFlag(instruction.op0, 0xF6, 6);
			break;
		case DS:
			bs = new byte[(int) instruction.op0.cast(OpImm.class).imm];
			var b = (opImm = instruction.op1.cast(OpImm.class)) != null ? opImm.imm : 0x90;
			Arrays.fill(bs, (byte) b);
			encode = new InsnCode(Bytes.of(bs).toArray());
			break;
		case HLT:
			encode = assemble(0xF4);
			break;
		case IDIV:
			encode = assembleByteFlag(instruction.op0, 0xF6, 7);
			break;
		case IMM:
			if ((opImm = instruction.op0.cast(OpImm.class)) != null) {
				var insnCode_ = new InsnCode(mode.opSize, opImm);
				insnCode_.bs = new byte[] {};
				encode = insnCode_;
			} else
				encode = invalid;
			break;
		case IMUL:
			if (instruction.op1 instanceof OpNone)
				encode = assembleByteFlag(instruction.op0, 0xF6, 5);
			else if (instruction.op2 instanceof OpNone)
				encode = assembleRegRm(instruction.op0, instruction.op1, 0xAF).pre(0x0F);
			else if ((opImm = instruction.op2.cast(OpImm.class)) != null) {
				if (opImm.size <= 1)
					encode = assembleRegRm(instruction.op0, instruction.op1, 0x6B).imm(opImm);
				else if (opImm.size == instruction.op0.size)
					encode = assembleRegRm(instruction.op0, instruction.op1, 0x69).imm(opImm);
				else
					encode = invalid;
			} else
				encode = invalid;
			break;
		case IN:
			encode = assembleInOut(instruction.op1, instruction.op0, 0xE4);
			break;
		case INC:
			encode = assembleRm(instruction, isLongMode ? -1 : 0x40, 0xFE, 0);
			break;
		case INT:
			if ((opImm = instruction.op0.cast(OpImm.class)) != null) {
				encode = opImm.imm != 3 ? assemble(0xCD).imm(opImm.imm, 1) : assemble(0xCC);
			} else
				encode = invalid;
			break;
		case INTO:
			encode = assemble(0xCE);
			break;
		case INVLPG:
			encode = assemble(instruction.op0, 0x01, 7).pre(0x0F);
			break;
		case IRET:
			encode = assemble(0xCF);
			break;
		case JA:
			encode = assembleJump(instruction, offset, 0x77, bs(0x0F, 0x87));
			break;
		case JAE:
			encode = assembleJump(instruction, offset, 0x73, bs(0x0F, 0x83));
			break;
		case JB:
			encode = assembleJump(instruction, offset, 0x72, bs(0x0F, 0x82));
			break;
		case JBE:
			encode = assembleJump(instruction, offset, 0x76, bs(0x0F, 0x86));
			break;
		case JE:
			encode = assembleJump(instruction, offset, 0x74, bs(0x0F, 0x84));
			break;
		case JG:
			encode = assembleJump(instruction, offset, 0x7F, bs(0x0F, 0x8F));
			break;
		case JGE:
			encode = assembleJump(instruction, offset, 0x7D, bs(0x0F, 0x8D));
			break;
		case JL:
			encode = assembleJump(instruction, offset, 0x7C, bs(0x0F, 0x8C));
			break;
		case JLE:
			encode = assembleJump(instruction, offset, 0x7E, bs(0x0F, 0x8E));
			break;
		case JMP:
			if (isRm.test(instruction.op0) && instruction.op0.size == mode.addrSize)
				encode = assemble(instruction.op0, 0xFF, 4);
			else
				encode = assembleJump(instruction, offset, 0xEB, bs(0xE9));
			break;
		case JNE:
			encode = assembleJump(instruction, offset, 0x75, bs(0x0F, 0x85));
			break;
		case JNO:
			encode = assembleJump(instruction, offset, 0x71, bs(0x0F, 0x81));
			break;
		case JNP:
			encode = assembleJump(instruction, offset, 0x7B, bs(0x0F, 0x8B));
			break;
		case JNS:
			encode = assembleJump(instruction, offset, 0x79, bs(0x0F, 0x89));
			break;
		case JNZ:
			encode = assembleJump(instruction, offset, 0x75, bs(0x0F, 0x85));
			break;
		case JO:
			encode = assembleJump(instruction, offset, 0x70, bs(0x0F, 0x80));
			break;
		case JP:
			encode = assembleJump(instruction, offset, 0x7A, bs(0x0F, 0x8A));
			break;
		case JS:
			encode = assembleJump(instruction, offset, 0x78, bs(0x0F, 0x88));
			break;
		case JZ:
			encode = assembleJump(instruction, offset, 0x74, bs(0x0F, 0x84));
			break;
		case LABEL:
			if (!isPass2)
				((OpImmLabel) instruction.op0).adjustImm(offset);
			encode = new InsnCode(new byte[0]);
			break;
		case LEA:
			encode = assembleRegRm_(instruction.op0, instruction.op1, 0x8D);
			break;
		case LOG:
			encode = new InsnCode(new byte[0]);
			break;
		case LOCK:
			encode = assemble(0xF0);
			break;
		case LOOP:
			encode = assembleJump(instruction, offset, 0xE2, null);
			break;
		case LOOPE:
			encode = assembleJump(instruction, offset, 0xE1, null);
			break;
		case LOOPNE:
			encode = assembleJump(instruction, offset, 0xE0, null);
			break;
		case LOOPNZ:
			encode = assembleJump(instruction, offset, 0xE0, null);
			break;
		case LOOPZ:
			encode = assembleJump(instruction, offset, 0xE1, null);
			break;
		case LGDT:
			encode = assemble(instruction.op0, 0x01, 2, mode.opSize).pre(0x0F);
			break;
		case LIDT:
			encode = assemble(instruction.op0, 0x01, 3, mode.opSize).pre(0x0F);
			break;
		case LTR:
			encode = assemble(instruction.op0, 0x00, 3, mode.opSize).pre(0x0F);
			break;
		case MOV:
			if ((opImm = instruction.op1.cast(OpImm.class)) != null //
					&& isRm.test(instruction.op0) //
					&& Integer.MIN_VALUE <= opImm.imm && opImm.imm <= Integer.MAX_VALUE //
					&& min(instruction.op0.size, 4) == opImm.size //
					&& (!isNonRexReg.test(instruction.op0) //
							|| instruction.op0 instanceof OpMem //
							|| instruction.op0.size == 8))
				// MOV r/m8, imm8
				// MOV r/m16, imm16
				// MOV r/m32, imm32
				// MOV r/m64, imm32 sign-extended
				encode = assembleByteFlag(instruction.op0, 0xC6, 0).imm(opImm.imm, min(opImm.size, 4));
			else if ((encode = assembleRmReg(instruction, 0x88)).isValid())
				;
			else if (instruction.op0.size == instruction.op1.size)
				if ((opImm = instruction.op1.cast(OpImm.class)) != null)
					if (instruction.op0 instanceof OpReg && isNonRexReg.test(instruction.op0))
						encode = assembleReg(instruction, 0xB0 + (opImm.size <= 1 ? 0 : 8)).imm(opImm);
					else
						encode = invalid;
				else if ((opRegSegment = instruction.op0.cast(OpRegSegment.class)) != null)
					encode = assemble(instruction.op1, 0x8E, opRegSegment.sreg);
				else if ((opRegSegment = instruction.op1.cast(OpRegSegment.class)) != null)
					encode = assemble(instruction.op0, 0x8C, opRegSegment.sreg);
				else if (instruction.op0.size == 4 //
						&& (opReg = instruction.op0.cast(OpReg.class)) != null //
						&& (opRegCtrl = instruction.op1.cast(OpRegControl.class)) != null)
					encode = new InsnCode(4, new byte[] { (byte) 0x0F, (byte) 0x20, b(opReg.reg, opRegCtrl.creg, 3), });
				else if (instruction.op0.size == 4 //
						&& (opRegCtrl = instruction.op0.cast(OpRegControl.class)) != null //
						&& (opReg = instruction.op1.cast(OpReg.class)) != null)
					encode = new InsnCode(4, new byte[] { (byte) 0x0F, (byte) 0x22, b(opReg.reg, opRegCtrl.creg, 3), });
				else
					encode = invalid;
			else
				encode = invalid;
			break;
		case MOVAPS:
			encode = assembleRmReg(instruction, 0x29, 0x28, isXmm).size(4).pre(0x0F);
			break;
		case MOVD:
			encode = assembleRmReg(instruction, 0x7E, 0x6E, isXmm).size(4).pre(bs(0x66, 0x0F));
			break;
		case MOVQ:
			encode = assembleRmReg(instruction, 0x7E, 0x6E, isXmm).size(8).pre(bs(0x66, 0x0F));
			break;
		case MOVSB:
			encode = new InsnCode(1, bs(0xA4));
			break;
		case MOVSD:
			encode = new InsnCode(4, bs(0xA5));
			break;
		case MOVSQ:
			encode = new InsnCode(8, bs(0xA5));
			break;
		case MOVSW:
			encode = new InsnCode(2, bs(0xA5));
			break;
		case MOVSX:
			if (instruction.op1.size < instruction.op0.size && (instruction.op1.size == 1 || instruction.op1.size == 2))
				encode = assembleRegRmExtended(instruction, 0xBE).pre(0x0F);
			else
				encode = fail();
			break;
		case MOVSXD:
			if (instruction.op1.size < instruction.op0.size && instruction.op1.size == 4) {
				var reg = (OpReg) instruction.op0;
				encode = assemble(instruction.op1, 0x63, reg.reg, reg.size);
			} else {
				encode = fail();
			}
			break;
		case MOVZX:
			encode = assembleRegRmExtended(instruction, 0xB6).pre(0x0F);
			break;
		case MUL:
			encode = assembleByteFlag(instruction.op0, 0xF6, 4);
			break;
		case MULPS:
			encode = assembleRegRm(instruction.op0, instruction.op1, 0x59).pre(0x0F);
			break;
		case NEG:
			encode = assembleByteFlag(instruction.op0, 0xF6, 3);
			break;
		case NOP:
			encode = assemble(0x90);
			break;
		case NOT:
			encode = assembleByteFlag(instruction.op0, 0xF6, 2);
			break;
		case OR:
			encode = assembleRmRegImm(instruction, 0x08, 0x80, 1);
			break;
		case OUT:
			encode = assembleInOut(instruction.op0, instruction.op1, 0xE6);
			break;
		case POP:
			if (isRm.test(instruction.op0))
				if (instruction.op0.size == 2 || instruction.op0.size == mode.addrSize)
					encode = assembleRm(instruction, 0x58, 0x8E, 0);
				else
					encode = invalid;
			else if (instruction.op0 instanceof OpRegSegment) {
				var sreg = (OpRegSegment) instruction.op0;
				switch (sreg.sreg) {
				case 0: // POP ES
					encode = isLongMode ? invalid : assemble(0x07);
					break;
				// case 1: // POP CS, no such thing
				case 2: // POP SS
					encode = isLongMode ? invalid : assemble(0x17);
					break;
				case 3: // POP DS
					encode = isLongMode ? invalid : assemble(0x1F);
					break;
				case 4: // POP FS
					encode = new InsnCode(sreg.size, bs(0x0F, 0xA1));
					break;
				case 5: // POP GS
					encode = new InsnCode(sreg.size, bs(0x0F, 0xA9));
					break;
				default:
					encode = invalid;
				}
			} else
				encode = invalid;
			break;
		case POPA:
			encode = assemble(0x61);
			break;
		case POPF:
			encode = assemble(0x9D);
			break;
		case PUSH:
			if ((opImm = instruction.op0.cast(OpImm.class)) != null) {
				var size = instruction.op0.size;
				encode = new InsnCode(size, opImm).setByte(0x68 + (1 < size ? 0 : 2));
			} else if (isRm.test(instruction.op0))
				if (instruction.op0.size == 2 || instruction.op0.size == mode.addrSize)
					encode = assembleRm(instruction, 0x50, 0xFE, 6);
				else
					encode = invalid;
			else if (instruction.op0 instanceof OpRegSegment) {
				var sreg = (OpRegSegment) instruction.op0;
				switch (sreg.sreg) {
				case 0: // PUSH ES
					encode = isLongMode ? invalid : assemble(0x06);
					break;
				case 1: // PUSH CS
					encode = isLongMode ? invalid : assemble(0x0E);
					break;
				case 2: // PUSH SS
					encode = isLongMode ? invalid : assemble(0x16);
					break;
				case 3: // PUSH DS
					encode = isLongMode ? invalid : assemble(0x1E);
					break;
				case 4: // PUSH FS
					encode = new InsnCode(sreg.size, bs(0x0F, 0xA0));
					break;
				case 5: // PUSH GS
					encode = new InsnCode(sreg.size, bs(0x0F, 0xA8));
					break;
				default:
					encode = invalid;
				}
			} else
				encode = invalid;
			break;
		case PUSHA:
			encode = assemble(0x60);
			break;
		case PUSHF:
			encode = assemble(0x9C);
			break;
		case RDMSR:
			encode = new InsnCode(bs(0x0F, 0x32));
			break;
		case RDTSC:
			encode = new InsnCode(bs(0x0F, 0x31));
			break;
		case RDTSCP:
			encode = new InsnCode(bs(0x0F, 0x01, 0xF9));
			break;
		case REMARK:
			encode = new InsnCode(bs());
			break;
		case REP:
			encode = assemble(0xF3);
			break;
		case REPE:
			encode = assemble(0xF3);
			break;
		case REPNE:
			encode = assemble(0xF2);
			break;
		case RET:
			if (instruction.op0 instanceof OpNone)
				encode = assemble(0xC3);
			else if ((opImm = instruction.op0.cast(OpImm.class)) != null && instruction.op0.size == 2)
				encode = new InsnCode(instruction.op0.size, opImm).setByte(0xC2);
			else
				encode = invalid;
			break;
		case SAL:
			encode = assembleShift(instruction, 0xC0, 4);
			break;
		case SAR:
			encode = assembleShift(instruction, 0xC0, 7);
			break;
		case SBB:
			encode = assembleRmRegImm(instruction, 0x18, 0x80, 3);
			break;
		case SETA:
			encode = assemble(instruction.op0, 0x97, 0).pre(0x0F);
			break;
		case SETAE:
			encode = assemble(instruction.op0, 0x93, 0).pre(0x0F);
			break;
		case SETB:
			encode = assemble(instruction.op0, 0x92, 0).pre(0x0F);
			break;
		case SETBE:
			encode = assemble(instruction.op0, 0x96, 0).pre(0x0F);
			break;
		case SETE:
			encode = assemble(instruction.op0, 0x94, 0).pre(0x0F);
			break;
		case SETG:
			encode = assemble(instruction.op0, 0x9F, 0).pre(0x0F);
			break;
		case SETGE:
			encode = assemble(instruction.op0, 0x9D, 0).pre(0x0F);
			break;
		case SETL:
			encode = assemble(instruction.op0, 0x9C, 0).pre(0x0F);
			break;
		case SETLE:
			encode = assemble(instruction.op0, 0x9E, 0).pre(0x0F);
			break;
		case SETNE:
			encode = assemble(instruction.op0, 0x95, 0).pre(0x0F);
			break;
		case SHL:
			encode = assembleShift(instruction, 0xC0, 4);
			break;
		case SHR:
			encode = assembleShift(instruction, 0xC0, 5);
			break;
		case STI:
			encode = assemble(0xFB);
			break;
		case STOSB:
			encode = new InsnCode(1, bs(0xAA));
			break;
		case STOSD:
			encode = new InsnCode(4, bs(0xAB));
			break;
		case STOSQ:
			encode = new InsnCode(8, bs(0xAB));
			break;
		case STOSW:
			encode = new InsnCode(2, bs(0xAB));
			break;
		case SUB:
			encode = assembleRmRegImm(instruction, 0x28, 0x80, 5);
			break;
		case SUBPS:
			encode = assembleRegRm(instruction.op0, instruction.op1, 0x5C).pre(0x0F);
			break;
		case SYSCALL:
			encode = new InsnCode(bs(0x0F, 0x05));
			break;
		case SYSENTER:
			encode = new InsnCode(bs(0x0F, 0x34));
			break;
		case SYSEXIT:
			encode = new InsnCode(bs(0x0F, 0x35));
			break;
		case TEST:
			if ((opImm = instruction.op1.cast(OpImm.class)) != null)
				encode = instruction.op0.size == instruction.op1.size
						? assembleRmImm(instruction.op0, opImm, 0xA8, 0xF6, 0)
						: invalid;
			else
				encode = assembleByteFlag(instruction.op0, 0x84, instruction.op1);
			break;
		case VADDPS:
			encode = assembleRegRm(instruction.op0, instruction.op2, 0x58).vex(Vexp.VP__, instruction.op1, Vexm.VM0F__);
			break;
		case VMOVAPS:
			encode = assembleRmReg(instruction, 0x29, 0x28, isXmmYmm).vex(Vexp.VP__, 0, Vexm.VM0F__);
			break;
		case VMOVD:
			encode = assembleRmReg(instruction, 0x7E, 0x6E, isXmm).size(4).vex(Vexp.VP66, 0, Vexm.VM0F__);
			break;
		case VMOVQ:
			encode = assembleRmReg(instruction, 0x7E, 0x6E, isXmm).size(8).vex(Vexp.VP66, 0, Vexm.VM0F__);
			break;
		case VMULPS:
			encode = assembleRegRm(instruction.op0, instruction.op2, 0x59).vex(Vexp.VP__, instruction.op1, Vexm.VM0F__);
			break;
		case VSUBPS:
			encode = assembleRegRm(instruction.op0, instruction.op2, 0x5C).vex(Vexp.VP__, instruction.op1, Vexm.VM0F__);
			break;
		case XCHG:
			if ((opReg = instruction.op1.cast(OpReg.class)) != null)
				if (isAcc.test(instruction.op0) && instruction.op0.size == instruction.op1.size)
					encode = assemble(0x90 + opReg.reg);
				else
					encode = assembleByteFlag(instruction.op0, 0x86, instruction.op1);
			else
				encode = invalid;
			break;
		case WRMSR:
			encode = new InsnCode(bs(0x0F, 0x30));
			break;
		case XOR:
			encode = assembleRmRegImm(instruction, 0x30, 0x80, 6);
			break;
		default:
			encode = invalid;
		}

		return encode.encode_(offset, instruction);
	}

	private InsnCode assembleInOut(Operand port, Operand acc, int b) {
		if (isAcc.test(acc)) {
			var portImm = port.cast(OpImm.class);
			var portReg = port.cast(OpReg.class);
			var isDX = port.size == 2 && portReg != null && portReg.reg == 2;
			var insnCode = new InsnCode(acc.size, bs(b + (acc.size == 1 ? 0 : 1) + (portImm != null ? 0 : 8)));

			if (portImm != null) {
				insnCode.immSize = 1;
				insnCode.imm = portImm.imm;
				return insnCode;
			} else if (isDX)
				return insnCode;
			else
				return invalid;
		} else
			return invalid;
	}

	private InsnCode assembleJump(Instruction instruction, long offset, int bj1, byte[] bj24) {
		var opImm = instruction.op0.cast(OpImm.class);
		if (opImm != null)
			return assembleJumpImm(opImm, offset, bj1, bj24);
		else
			return invalid;
	}

	private InsnCode assembleJumpImm(OpImm op0, long offset, int bj1, byte[] bj24) {
		var size = min(op0.size, min(mode.addrSize, 4));
		byte[] bs0;

		if (size == 1)
			bs0 = bs(bj1);
		else if (size == 2 || size == 4)
			bs0 = bj24;
		else
			return invalid;

		var rel = op0.isBound() ? op0.imm - (offset + bs0.length + size) : 0l;
		InsnCode insnCode;

		if (size == 1 && Byte.MIN_VALUE <= rel && rel <= Byte.MAX_VALUE //
				|| size == 2 && Short.MIN_VALUE <= rel && rel <= Short.MAX_VALUE //
				|| size == 4 && Integer.MIN_VALUE <= rel && rel <= Integer.MAX_VALUE) {
			insnCode = new InsnCode(size, bs0);
			insnCode.immSize = size;
			insnCode.imm = rel;
			return insnCode;
		} else
			return invalid;
	}

	private InsnCode assembleRegRmExtended(Instruction instruction, int b) {
		var reg = instruction.op0.cast(OpReg.class);
		if (reg != null && isRm.test(instruction.op1))
			return assemble(instruction.op1, b + (instruction.op1.size <= 1 ? 0 : 1), reg.reg, reg.size);
		else
			return invalid;
	}

	private InsnCode assembleRm(Instruction instruction, int bReg, int bModrm, int num) {
		if (bReg != -1 && instruction.op0 instanceof OpReg && 1 < instruction.op0.size)
			return assembleReg(instruction, bReg);
		else if (isRm.test(instruction.op0))
			return assembleByteFlag(instruction.op0, bModrm, num);
		else
			return invalid;
	}

	private InsnCode assembleReg(Instruction instruction, int bReg) {
		var op0 = instruction.op0.cast(OpReg.class);
		return new InsnCode(op0.size, bs(bReg + op0.reg));
	}

	private InsnCode assembleRmRegImm(Instruction instruction, int bModrm, int bImm, int num) {
		var size0 = instruction.op0.size;
		var size1 = instruction.op1.size;
		var opImm = instruction.op1.cast(OpImm.class);
		if ((size1 == 1 || size0 == size1) && opImm != null)
			return assembleRmImm(instruction.op0, opImm, bModrm + 4, bImm, num);
		else
			return size0 == size1 ? assembleRmReg(instruction, bModrm) : invalid;
	}

	private InsnCode assembleRmReg(Instruction instruction, int b) {
		return assembleRmReg(instruction, b, b + 2, isReg);
	}

	private InsnCode assembleRmReg(Instruction instruction, int bRmReg, int bRegRm, Predicate<Operand> pred) {
		if (isRm.test(instruction.op0) && pred.test(instruction.op1))
			return assembleRmReg(instruction.op0, (OpReg) instruction.op1, bRmReg);
		else if (pred.test(instruction.op0) && isRm.test(instruction.op1))
			return assembleRmReg(instruction.op1, (OpReg) instruction.op0, bRegRm);
		else
			return invalid;
	}

	private InsnCode assembleRmReg(Operand rm, OpReg reg, int b) {
		return 0 <= b ? assembleByteFlag(rm, b, reg) : invalid;
	}

	private InsnCode assembleRegRm(Operand reg, Operand rm, int b) {
		return reg.size == rm.size ? assembleRegRm_(reg, rm, b) : invalid;
	}

	private InsnCode assembleRegRm_(Operand reg, Operand rm, int b) {
		return isReg.test(reg) && isRm.test(rm) ? assemble(rm, b, ((OpReg) reg).reg, reg.size) : invalid;
	}

	private InsnCode assembleRmImm(Operand op0, OpImm op1, int bAccImm, int bRmImm, int num) {
		var size0 = op0.size;
		var size1 = op1.isBound() && Byte.MIN_VALUE <= op1.imm && op1.imm <= Byte.MAX_VALUE ? 1 : op1.size;
		InsnCode insnCode;

		if (Integer.MIN_VALUE <= op1.imm && op1.imm <= Integer.MAX_VALUE) {
			insnCode = new InsnCode(size0, op1.imm, min(size1, 4));

			if (isAcc.test(op0) && size0 == size1)
				insnCode.bs = bs(bAccImm + (size0 <= 1 ? 0 : 1));
			else if (isRm.test(op0)) {
				var b0 = ((1 < size0 && size1 <= 1) ? 2 : 0) + (size0 <= 1 ? 0 : 1);
				insnCode.bs = bs(bRmImm + b0);
				insnCode.modrm = modrm(op0, num);
			} else
				insnCode = invalid;
		} else
			insnCode = invalid;

		return insnCode;
	}

	private InsnCode assembleShift(Instruction instruction, int b, int num) {
		if (isRm.test(instruction.op0)) {
			var shift = instruction.op1;
			var shiftImm = shift.cast(OpImm.class);
			boolean isShiftImm;
			int b1;

			if (shiftImm != null) {
				isShiftImm = shiftImm.imm != 1;
				b1 = b + (isShiftImm ? 0 : 16);
			} else if (shift.size == 1 && shift instanceof OpReg && ((OpReg) shift).reg == 1) { // CL
				isShiftImm = false;
				b1 = b + 16 + 2;
			} else
				return invalid;

			var insnCode = assembleByteFlag(instruction.op0, b1, num);

			if (isShiftImm) {
				insnCode.immSize = 1;
				insnCode.imm = shiftImm.imm;
			}

			return insnCode;
		} else
			return invalid;
	}

	private InsnCode assembleByteFlag(Operand operand, int b, Operand reg) {
		return operand.size == reg.size ? assembleByteFlag(operand, b, ((OpReg) reg).reg) : invalid;
	}

	private InsnCode assembleByteFlag(Operand operand, int b, int num) {
		return assemble(operand, b + (operand.size <= 1 ? 0 : 1), num);
	}

	private InsnCode assemble(int b) {
		return new InsnCode(mode.opSize, bs(b));
	}

	private InsnCode assemble(Operand operand, int b, int num) {
		return assemble(operand, b, num, operand.size);
	}

	private InsnCode assemble(Operand operand, int b, int num, int size) {
		var insnCode = new InsnCode(size, bs(b));
		insnCode.modrm = modrm(operand, num);
		return insnCode;
	}

	private Bytes encode(long offset, InsnCode insnCode,boolean isEnforceRex, byte[] vexs) {
		if (insnCode.isValid()) {
			var modrm = insnCode.modrm;
			var bb = new BytesBuilder();
			if (vexs != null)
				bb.append(vexs);
			else {
				if (mode.opSize == 2 && Set.of(4, 8).contains(insnCode.opSize))
					bb.append((byte) 0x66);
				if (mode.opSize != 2 && insnCode.opSize == 2)
					bb.append((byte) 0x66);
				if (isLongMode) {
					var rex04 = modrm != null ? rexModrm(insnCode.opSize, insnCode) : rex(insnCode.opSize, 0, 0, 0);
					appendIf(bb, isEnforceRex || rex04 != 0 ? 0x40 + rex04 : -1);
				}
			}
			bb.append(insnCode.bs);
			if (modrm != null) {
				bb.append(b(modrm.rm, modrm.num, modrm.mod));
				appendIf(bb, sib(modrm));

				long disp;

				if (isLongMode && modrm.mod == 0 && (modrm.rm & 7) == 5) // RIP-relative addressing
					disp = modrm.disp - offset - bb.size() - modrm.dispSize - insnCode.immSize;
				else
					disp = modrm.disp;

				appendImm(bb, modrm.dispSize, disp);
			}
			appendImm(bb, insnCode.immSize, insnCode.imm);
			return bb.toBytes();
		} else
			return fail("bad instruction");
	}

	private void appendIf(BytesBuilder bb, int b) {
		if (b != -1)
			bb.append((byte) b);
	}

	private void appendImm(BytesBuilder bb, int size, long v) {
		for (var i = 0; i < size; i++) {
			bb.append((byte) (v & 0xFF));
			v >>= 8;
		}
	}

	private Predicate<Operand> isAcc = op -> op instanceof OpReg && ((OpReg) op).reg == 0;
	private Predicate<Operand> isNonRexReg = op -> op instanceof OpReg && ((OpReg) op).reg < 8;
	private Predicate<Operand> isReg = op -> op instanceof OpReg;
	private Predicate<Operand> isRm = op -> op instanceof OpMem || op instanceof OpReg;
	private Predicate<Operand> isXmm = op -> op instanceof OpRegXmm;
	private Predicate<Operand> isXmmYmm = op -> op instanceof OpRegXmm || op instanceof OpRegYmm;

	private Modrm modrm(Operand operand, int num) {
		int mod, rm, s, i, b, dispSize;
		long disp;

		if (operand instanceof OpReg) { // EAX
			var op = (OpReg) operand;
			mod = 3;
			rm = op.reg;
			s = i = b = -1;
			dispSize = 0;
			disp = 0;
		} else if (operand instanceof OpMem) {
			var op = (OpMem) operand;
			var br = op.baseReg;
			var ir = op.indexReg;

			if (mode.addrSize == 2) {
				if (op.disp.size == 2 && br < 0 && ir < 0) {
					mod = 0;
					rm = 6;
				} else if (op.disp.size == 0 && ir < 0 && 0 < br) {
					mod = op.disp.size;
					rm = br == amd64.bxReg ? 7 : fail();
				} else if (op.disp.size == 0 || op.disp.size == 1 || op.disp.size == 2) {
					mod = op.disp.size;
					var rm0 = br < 0 ? 4 : br == amd64.bxReg ? 0 : br == amd64.bpReg ? 2 : Fail.<Integer> fail();
					var rm1 = ir == amd64.siReg ? 0 : ir == amd64.diReg ? 1 : Fail.<Integer> fail();
					rm = rm0 + rm1;
				} else
					return fail("bad operand");
				s = i = b = -1;
				dispSize = op.disp.size;
			} else {
				int indexReg;
				int ds0;

				if (op.disp.isBound())
					if (op.disp.imm == 0)
						ds0 = 0;
					else if (Byte.MIN_VALUE <= op.disp.imm && op.disp.imm <= Byte.MAX_VALUE)
						ds0 = 1;
					else
						ds0 = op.disp.size;
				else
					ds0 = op.disp.size;

				if ((ir & 7) != 4)
					indexReg = ir;
				else
					indexReg = fail("bad operand");

				if (br < 0 && indexReg < 0) { // [0x1234]
					mod = 0;
					rm = 5;
					s = i = b = -1;
					dispSize = 4;
				} else if (0 <= br && indexReg < 0)
					if ((br & 7) != amd64.spReg) {
						// [EAX], [EAX + 0x1234]
						var ds1 = (br & 7) == amd64.bpReg && ds0 == 0 ? 1 : ds0;
						mod = dispMod(ds1);
						rm = br;
						s = i = b = -1;
						dispSize = ds1;
					} else {
						// [ESP + 0], [ESP + 0x1234]
						var ds1 = br == amd64.spReg && ds0 == 0 ? 1 : ds0;
						mod = dispMod(ds1);
						rm = 4;
						s = 0;
						i = 4;
						b = br & 7;
						dispSize = ds1;
					}
				else if (br < 0 && 0 <= indexReg) { // [4 * EBX + 0x1234]
					mod = 0;
					rm = 4;
					s = scale(op);
					i = indexReg;
					b = 5;
					dispSize = 4;
				} else if (0 <= br && 0 <= indexReg)
					if ((br & 7) != amd64.bpReg) {
						// [4 * EBX + EAX + 0x1234]
						mod = dispMod(ds0);
						rm = 4;
						s = scale(op);
						i = indexReg;
						b = br;
						dispSize = ds0;
					} else
						return fail("bad operand");
				else
					return fail("bad operand");
			}
			
			disp = op.disp.imm;
		} else
			throw new RuntimeException("bad operand");

		var modrm = new Modrm();
		modrm.mod = mod;
		modrm.num = num;
		modrm.rm = rm;
		modrm.s = s;
		modrm.i = i;
		modrm.b = b;
		modrm.dispSize = dispSize;
		modrm.disp = disp;
		return modrm;
	}

	private int dispMod(int dispSize) {
		return dispSize == 0 ? 0 : dispSize == 1 ? 1 : dispSize == 4 ? 2 : fail("bad displacement");
	}

	private int sib(Modrm modrm) {
		return 0 <= modrm.s ? b(modrm.b, modrm.i, modrm.s) : -1;
	}

	private int scale(OpMem op) {
		var l = Integer.numberOfTrailingZeros(op.scale);
		return 0 <= l && l < 4 ? l : fail("bad scale");
	}

	private int rexModrm(int opSize, InsnCode insnCode) {
		var modrm = insnCode.modrm;
		return rex(opSize, modrm.num, modrm.i, 0 <= modrm.b ? modrm.b : modrm.rm);
	}

	private int rex(int opSize, int r, int x, int b) {
		return ((opSize != 8 ? 0 : 1) << 3) //
				+ (bit4(r) << 2) //
				+ (bit4(x) << 1) //
				+ (bit4(b) << 0);
	}

	// https://en.wikipedia.org/wiki/VEX_prefix
	private byte[] vex(int m, int p, int size, Modrm modrm, int w, int v) {
		var x = bit4(modrm.i);
		var b = bit4(modrm.b);
		var w_ = bit4(w);
		if (m == 1 && x == 0 && b == 0 && w == 0) {
			var b1 = ((bit4(modrm.num) ^ 1) << 7) //
					+ (~v << 3) //
					+ ((size != 16 ? 1 : 0) << 2) //
					+ (p << 0);
			return bs(0xC5, b1);
		} else {
			var b1 = ((bit4(modrm.num) ^ 1) << 7) //
					+ ((x ^ 1) << 6) //
					+ ((b ^ 1) << 5) //
					+ (~m << 0);
			var b2 = (w_ << 7) //
					+ (~v << 3)//
					+ ((size != 16 ? 1 : 0) << 2) //
					+ (p << 0);
			return bs(0xC4, b1, b2);
		}
	}

	private int bit4(int r) {
		return 0 <= r ? r >> 3 & 1 : 0;
	}

	private byte[] bs(int... is) {
		var length = is.length;
		var bs = new byte[length];
		for (var i = 0; i < length; i++)
			bs[i] = (byte) is[i];
		return bs;
	}

	private byte b(int b03, int b36, int b68) {
		return (byte) ((b03 & 7) + ((b36 & 7) << 3) + ((b68 & 3) << 6));
	}

}
