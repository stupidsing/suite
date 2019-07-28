package suite.assembler;

import static java.lang.Math.min;
import static java.util.Map.entry;
import static suite.util.Fail.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

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
import suite.os.Log_;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.primitive.Bytes_;

// TODO validate number of operands
// TODO validate size of operands
// do not use SPL, BPL, SIL, DIL in 32-bit mode
// do not use AH, BH, CH, DH in 64-bit long mode
public class Amd64Assemble {

	private InsnCode invalid = new InsnCode(-1, new byte[0]);
	private int archSize; // 2, 4 or 8 for 286, i686 and amd64
	private int defaultSize;
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

		public Bytes encode_(long offset);
	}

	private class VexCode implements Encode {
		public int m, w, p, v;
		public InsnCode code;

		public boolean isValid() {
			return code.isValid();
		}

		public Bytes encode_(long offset) {
			return encode(offset, code, vex(m, p, code.size, code.modrm, w, v));
		}
	}

	private class InsnCode implements Encode {
		public int size;
		public byte[] bs;
		public Modrm modrm;
		public long imm;
		public int immSize;

		private InsnCode(int size, OpImm imm) {
			this(size, imm.imm, imm.size);
		}

		private InsnCode(int size, long imm, int immSize) {
			this.size = size;
			this.imm = imm;
			this.immSize = immSize;
		}

		private InsnCode(byte[] bs) {
			this(defaultSize, bs);
		}

		private InsnCode(int size, byte[] bs) {
			this.size = size;
			this.bs = bs;
		}

		private InsnCode imm(OpImm imm) {
			return imm(imm.imm, imm.size);
		}

		private InsnCode imm(long imm1, int size1) {
			return set(size, bs, imm1, size1);
		}

		private InsnCode pre(int pre) {
			return pre(bs(pre));
		}

		private InsnCode pre(byte[] pre) {
			var length0 = pre.length;
			var length1 = bs.length;
			var bs1 = Arrays.copyOf(pre, length0 + length1);
			Bytes_.copy(bs, 0, bs1, length0, length1);
			return set(size, bs1, imm, immSize);
		}

		private InsnCode setByte(int b) {
			return set(size, bs(b), imm, immSize);
		}

		private InsnCode size(int size1) {
			return set(size1, bs, imm, immSize);
		}

		private InsnCode set(int size1, byte[] bs1, long imm, int immSize) {
			var insnCode = new InsnCode(size1, bs1);
			insnCode.modrm = modrm;
			insnCode.immSize = immSize;
			insnCode.imm = imm;
			return insnCode;
		}

		private Encode vex(Vexp vexp, Operand op, Vexm vexm) {
			var opReg = (OpReg) op;
			if (opReg.size == size)
				return vex(vexp, opReg.reg, vexm, size == 8 ? 1 : 0);
			else
				return invalid;
		}

		private VexCode vex(Vexp vexp, int v, Vexm vexm) {
			return vex(vexp, v, vexm, size == 8 ? 1 : 0);
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
			return 0 < size;
		}

		public Bytes encode_(long offset) {
			return encode(offset, size == 1 || size == 2 || size == 4 || size == 8 ? this : invalid, null);
		}

	}

	private class Modrm {
		private int mod, num, rm, s, i, b, dispSize;
		private long disp;
	}

	public Amd64Assemble(int archSize) {
		this.archSize = archSize;
		this.defaultSize = min(archSize, 4);
		this.isLongMode = archSize == 8;
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
			var align = ((OpImm) instruction.op0).imm;
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
			if (instruction.op0 instanceof OpImm && 4 <= instruction.op0.size)
				encode = assembleJumpImm((OpImm) instruction.op0, offset, -1, bs(0xE8));
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
			bs = new byte[(int) ((OpImm) instruction.op0).imm];
			var b = instruction.op1 instanceof OpImm ? ((OpImm) instruction.op1).imm : 0x90;
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
			if (instruction.op0 instanceof OpImm) {
				var insnCode_ = new InsnCode(archSize, (OpImm) instruction.op0);
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
			else if (instruction.op2 instanceof OpImm) {
				var imm = (OpImm) instruction.op2;
				if (imm.size <= 1)
					encode = assembleRegRm(instruction.op0, instruction.op1, 0x6B).imm(imm);
				else if (imm.size == instruction.op0.size)
					encode = assembleRegRm(instruction.op0, instruction.op1, 0x69).imm(imm);
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
			if (instruction.op0 instanceof OpImm) {
				var iv = ((OpImm) instruction.op0).imm;
				encode = iv != 3 ? assemble(0xCD).imm(iv, 1) : assemble(0xCC);
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
			if (isRm.test(instruction.op0) && instruction.op0.size == archSize)
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
			encode = assemble(instruction.op0, 0x01, 2, defaultSize).pre(0x0F);
			break;
		case LIDT:
			encode = assemble(instruction.op0, 0x01, 3, defaultSize).pre(0x0F);
			break;
		case LTR:
			encode = assemble(instruction.op0, 0x00, 3, defaultSize).pre(0x0F);
			break;
		case MOV:
			if ((opImm = instruction.op1.cast(OpImm.class)) != null //
					&& isRm.test(instruction.op0) //
					// && opImm.isBound() //
					&& Integer.MIN_VALUE <= opImm.imm && opImm.imm <= Integer.MAX_VALUE //
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
				if (instruction.op1 instanceof OpImm) {
					var op1 = (OpImm) instruction.op1;
					if (instruction.op0 instanceof OpReg && isNonRexReg.test(instruction.op0))
						encode = assembleReg(instruction, 0xB0 + (op1.size <= 1 ? 0 : 8)).imm(op1);
					else
						encode = invalid;
				} else if (instruction.op0 instanceof OpRegSegment) {
					var opRegSegment = (OpRegSegment) instruction.op0;
					encode = assemble(instruction.op1, 0x8E, opRegSegment.sreg);
				} else if (instruction.op1 instanceof OpRegSegment) {
					var opRegSegment = (OpRegSegment) instruction.op1;
					encode = assemble(instruction.op0, 0x8C, opRegSegment.sreg);
				} else if (instruction.op0.size == 4 //
						&& instruction.op0 instanceof OpReg //
						&& instruction.op1 instanceof OpRegControl) {
					var opReg = (OpReg) instruction.op0;
					var opRegCt = (OpRegControl) instruction.op1;
					encode = new InsnCode(4, new byte[] { (byte) 0x0F, (byte) 0x20, b(opReg.reg, opRegCt.creg, 3), });
				} else if (instruction.op0.size == 4 //
						&& instruction.op0 instanceof OpRegControl //
						&& instruction.op1 instanceof OpReg) {
					var opRegCt = (OpRegControl) instruction.op0;
					var opReg = (OpReg) instruction.op1;
					encode = new InsnCode(4, new byte[] { (byte) 0x0F, (byte) 0x22, b(opReg.reg, opRegCt.creg, 3), });
				} else
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
			encode = assembleRegRmExtended(instruction, 0xBE).pre(0x0F);
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
				if (instruction.op0.size == 2 || instruction.op0.size == archSize)
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
			if (instruction.op0 instanceof OpImm) {
				var size = instruction.op0.size;
				encode = new InsnCode(size, (OpImm) instruction.op0).setByte(0x68 + (1 < size ? 0 : 2));
			} else if (isRm.test(instruction.op0))
				if (instruction.op0.size == 2 || instruction.op0.size == archSize)
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
			else if (instruction.op0 instanceof OpImm && instruction.op0.size == 2)
				encode = new InsnCode(instruction.op0.size, (OpImm) instruction.op0).setByte(0xC2);
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
			if (instruction.op1 instanceof OpImm)
				encode = instruction.op0.size == instruction.op1.size
						? assembleRmImm(instruction.op0, (OpImm) instruction.op1, 0xA8, 0xF6, 0)
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
			if (instruction.op1 instanceof OpReg)
				if (isAcc.test(instruction.op0) && instruction.op0.size == instruction.op1.size)
					encode = assemble(0x90 + ((OpReg) instruction.op1).reg);
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

		return encode.encode_(offset);
	}

	private InsnCode assembleInOut(Operand port, Operand acc, int b) {
		if (isAcc.test(acc)) {
			OpImm portImm;
			if (port instanceof OpImm)
				portImm = (OpImm) port;
			else if (port.size == 2 && port instanceof OpReg && ((OpReg) port).reg == 2) // DX
				portImm = null;
			else
				return invalid;

			var insnCode = new InsnCode(acc.size, bs(b + (acc.size == 1 ? 0 : 1) + (portImm != null ? 0 : 8)));
			if (portImm != null) {
				insnCode.immSize = 1;
				insnCode.imm = portImm.imm;
			}
			return insnCode;
		} else
			return invalid;
	}

	private InsnCode assembleJump(Instruction instruction, long offset, int bj1, byte[] bj24) {
		if (instruction.op0 instanceof OpImm)
			return assembleJumpImm((OpImm) instruction.op0, offset, bj1, bj24);
		else
			return invalid;
	}

	private InsnCode assembleJumpImm(OpImm op0, long offset, int bj1, byte[] bj24) {
		var size = min(op0.size, defaultSize);
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
		if (instruction.op0 instanceof OpReg && isRm.test(instruction.op1)) {
			var reg = (OpReg) instruction.op0;
			return assemble(instruction.op1, b + (instruction.op1.size <= 1 ? 0 : 1), reg.reg, reg.size);
		} else
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
		var op0 = (OpReg) instruction.op0;
		return new InsnCode(op0.size, bs(bReg + op0.reg));
	}

	private InsnCode assembleRmRegImm(Instruction instruction, int bModrm, int bImm, int num) {
		int size0 = instruction.op0.size;
		int size1 = instruction.op1.size;
		if ((size1 == 1 || size0 == size1) && instruction.op1 instanceof OpImm)
			return assembleRmImm(instruction.op0, (OpImm) instruction.op1, bModrm + 4, bImm, num);
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
		return new InsnCode(archSize, bs(b));
	}

	private InsnCode assemble(Operand operand, int b, int num) {
		return assemble(operand, b, num, operand.size);
	}

	private InsnCode assemble(Operand operand, int b, int num, int size) {
		var insnCode = new InsnCode(size, bs(b));
		insnCode.modrm = modrm(operand, num);
		return insnCode;
	}

	private Bytes encode(long offset, InsnCode insnCode, byte[] vexs) {
		if (insnCode.isValid()) {
			var modrm = insnCode.modrm;
			var bb = new BytesBuilder();
			if (vexs != null)
				bb.append(vexs);
			else {
				if (archSize == 2 && Set.of(4, 8).contains(insnCode.size))
					bb.append((byte) 0x66);
				if (archSize != 2 && insnCode.size == 2)
					bb.append((byte) 0x66);
				appendIf(bb, modrm != null ? rexModrm(insnCode.size, insnCode) : rex(insnCode.size, 0, 0, 0));
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
			var baseReg = op.baseReg;
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

			if ((op.indexReg & 7) != 4)
				indexReg = op.indexReg;
			else
				indexReg = fail("bad operand");

			if (baseReg < 0 && indexReg < 0) { // [0x1234]
				mod = 0;
				rm = 5;
				s = i = b = -1;
				dispSize = 4;
			} else if (0 <= baseReg && indexReg < 0)
				if ((baseReg & 7) != 4) {
					// [EAX], [EAX + 0x1234]
					var ds1 = (baseReg & 7) == 5 && ds0 == 0 ? 1 : ds0;
					mod = dispMod(ds1);
					rm = baseReg;
					s = i = b = -1;
					dispSize = ds1;
				} else {
					// [ESP + 0], [ESP + 0x1234]
					var ds1 = baseReg == 4 && ds0 == 0 ? 1 : ds0;
					mod = dispMod(ds1);
					rm = 4;
					s = 0;
					i = 4;
					b = baseReg & 7;
					dispSize = ds1;
				}
			else if (baseReg < 0 && 0 <= indexReg) { // [4 * EBX + 0x1234]
				mod = 0;
				rm = 4;
				s = scale(op);
				i = indexReg;
				b = 5;
				dispSize = 4;
			} else if (0 <= baseReg && 0 <= indexReg)
				if ((baseReg & 7) != 5) {
					// [4 * EBX + EAX + 0x1234]
					mod = dispMod(ds0);
					rm = 4;
					s = scale(op);
					i = indexReg;
					b = baseReg;
					dispSize = ds0;
				} else
					throw new RuntimeException("bad operand");
			else
				throw new RuntimeException("bad operand");

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

	private int rexModrm(int size, InsnCode insnCode) {
		var modrm = insnCode.modrm;
		return rex(size, modrm.num, modrm.i, 0 <= modrm.b ? modrm.b : modrm.rm);
	}

	private int rex(int size, int r, int x, int b) {
		var b04 = ((size != 8 ? 0 : 1) << 3) //
				+ (bit4(r) << 2) //
				+ (bit4(x) << 1) //
				+ (bit4(b) << 0);
		if (Boolean.TRUE) {
			return b04 != 0 ? 0x40 + b04 : -1;
		} else {
			// why it was like this?
			return isLongMode && size == 1 || b04 != 0 ? 0x40 + b04 : -1;
		}
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
