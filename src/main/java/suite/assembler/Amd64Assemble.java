package suite.assembler;

import static java.util.Map.entry;
import static suite.util.Friends.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import suite.adt.pair.Fixie_.FixieFun3;
import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
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
public class Amd64Assemble {

	private InsnCode invalid = new InsnCode(-1, new byte[0]);
	private boolean isAmd64;

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
		public int immSize;
		public long imm;

		private InsnCode(int size, OpImm imm) {
			this.size = size;
			this.immSize = imm.size;
			this.imm = imm.imm;
		}

		private InsnCode(int size, byte[] bs) {
			this.size = size;
			this.bs = bs;
		}

		private InsnCode imm(OpImm imm) {
			return imm(imm.imm, imm.size);
		}

		private InsnCode imm(long imm1, int size1) {
			return set(size1, bs, imm1, size1);
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
		private int size, mod, num, rm, s, i, b, dispSize;
		private long disp;
	}

	public Amd64Assemble(boolean isAmd64) {
		this.isAmd64 = isAmd64;
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
		switch (instruction.insn) {
		case AAA:
			encode = assemble(instruction, 0x37);
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
		case AND:
			encode = assembleRmRegImm(instruction, 0x20, 0x80, 4);
			break;
		case AOP:
			encode = assemble(instruction, 0x67);
			break;
		case CALL:
			if (instruction.op0 instanceof OpImm && instruction.op0.size == 4)
				encode = assembleJumpImm((OpImm) instruction.op0, offset, -1, bs(0xE8));
			else if (isRm.test(instruction.op0))
				encode = assemble(instruction.op0, 0xFF, 2);
			else
				encode = invalid;
			break;
		case CLD:
			encode = assemble(instruction, 0xFC);
			break;
		case CLI:
			encode = assemble(instruction, 0xFA);
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
		case CMPSW:
			encode = new InsnCode(2, bs(0xA7));
			break;
		case CMPXCHG:
			encode = assembleRegRm(instruction.op1, instruction.op0, 0xB0);
			break;
		case CPUID:
			encode = new InsnCode(4, bs(0x0F, 0xA2));
			break;
		case D:
			var opImm = ((OpImm) instruction.op0);
			var bb = new BytesBuilder();
			appendImm(bb, opImm.size, opImm.imm);
			encode = new InsnCode(4, bb.toBytes().toArray());
			break;
		case DEC:
			encode = assembleRm(instruction, isAmd64 ? -1 : 0x48, 0xFE, 1);
			break;
		case DIV:
			encode = assembleByteFlag(instruction.op0, 0xF6, 6);
			break;
		case DS:
			var bs = new byte[(int) ((OpImm) instruction.op0).imm];
			var b = instruction.op1 instanceof OpImm ? ((OpImm) instruction.op1).imm : 0x90;
			Arrays.fill(bs, (byte) b);
			encode = new InsnCode(4, Bytes.of(bs).toArray());
			break;
		case HLT:
			encode = assemble(instruction, 0xF4);
			break;
		case IDIV:
			encode = assembleByteFlag(instruction.op0, 0xF6, 7);
			break;
		case IMM:
			if (instruction.op0 instanceof OpImm) {
				var insnCode_ = new InsnCode(0, (OpImm) instruction.op0);
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
			encode = assembleRm(instruction, isAmd64 ? -1 : 0x40, 0xFE, 0);
			break;
		case INT:
			if (instruction.op0 instanceof OpImm) {
				var iv = ((OpImm) instruction.op0).imm;
				if (iv != 3)
					encode = assemble(instruction, 0xCD).imm(iv, 1);
				else
					encode = assemble(instruction, 0xCC);
			} else
				encode = invalid;
			break;
		case INTO:
			encode = assemble(instruction, 0xCE);
			break;
		case INVLPG:
			encode = assemble(instruction.op0, 0x01, 7).pre(0x0F);
			break;
		case IRET:
			encode = assemble(instruction, 0xCF);
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
			if (isRm.test(instruction.op0) && instruction.op0.size == 4)
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
				((OpImm) instruction.op0).imm += offset;
			encode = new InsnCode(4, new byte[0]);
			break;
		case LEA:
			encode = assembleRegRm_(instruction.op0, instruction.op1, 0x8D);
			break;
		case LOG:
			encode = new InsnCode(4, new byte[0]);
			break;
		case LOCK:
			encode = assemble(instruction, 0xF0);
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
			encode = assemble(instruction.op0, 0x01, 2).pre(0x0F);
			break;
		case LIDT:
			encode = assemble(instruction.op0, 0x01, 3).pre(0x0F);
			break;
		case LTR:
			encode = assemble(instruction.op0, 0x00, 3).pre(0x0F);
			break;
		case MOV:
			if (instruction.op0.size == instruction.op1.size)
				if (instruction.op1 instanceof OpImm) {
					var op1 = (OpImm) instruction.op1;
					if (instruction.op0 instanceof OpReg && isNonRexReg.test(instruction.op0))
						encode = assembleReg(instruction, 0xB0 + (op1.size <= 1 ? 0 : 8)).imm(op1);
					else if (isRm.test(instruction.op0))
						encode = assembleByteFlag(instruction.op0, 0xC6, 0).imm(op1.imm, Math.min(op1.size, 4));
					else
						encode = invalid;
				} else if (instruction.op0 instanceof OpRegSegment) {
					var regSegment = (OpRegSegment) instruction.op0;
					encode = assemble(instruction.op1, 0x8E, regSegment.sreg);
				} else if (instruction.op1 instanceof OpRegSegment) {
					var regSegment = (OpRegSegment) instruction.op1;
					encode = assemble(instruction.op0, 0x8C, regSegment.sreg);
				} else if (instruction.op0.size == 4 //
						&& instruction.op0 instanceof OpReg //
						&& instruction.op1 instanceof OpRegControl) {
					var reg = (OpReg) instruction.op0;
					var regControl = (OpRegControl) instruction.op1;
					encode = new InsnCode(4, new byte[] { (byte) 0x0F, (byte) 0x20, b(reg.reg, regControl.creg, 3), });
				} else if (instruction.op0.size == 4 //
						&& instruction.op0 instanceof OpRegControl //
						&& instruction.op1 instanceof OpReg) {
					var regControl = (OpRegControl) instruction.op0;
					var reg = (OpReg) instruction.op1;
					encode = new InsnCode(4, new byte[] { (byte) 0x0F, (byte) 0x22, b(reg.reg, regControl.creg, 3), });
				} else if ((encode = assembleRmReg(instruction, 0x88)).isValid())
					;
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
			encode = assemble(instruction, 0x90);
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
			if (1 < instruction.op0.size)
				if (isRm.test(instruction.op0))
					encode = assembleRm(instruction, 0x58, 0x8E, 0);
				else if (instruction.op0 instanceof OpRegSegment) {
					var sreg = (OpRegSegment) instruction.op0;
					switch (sreg.sreg) {
					case 0: // POP ES
						encode = assemble(instruction, 0x07);
						break;
					// case 1: // POP CS, no such thing
					case 2: // POP SS
						encode = assemble(instruction, 0x17);
						break;
					case 3: // POP DS
						encode = assemble(instruction, 0x1F);
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
			else
				encode = invalid;
			break;
		case POPA:
			encode = assemble(instruction, 0x61);
			break;
		case POPF:
			encode = assemble(instruction, 0x9D);
			break;
		case PUSH:
			if (instruction.op0 instanceof OpImm) {
				var size = instruction.op0.size;
				encode = new InsnCode(size, (OpImm) instruction.op0).setByte(0x68 + (1 < size ? 0 : 2));
			} else if (1 < instruction.op0.size)
				if (isRm.test(instruction.op0))
					encode = assembleRm(instruction, 0x50, 0xFE, 6);
				else if (instruction.op0 instanceof OpRegSegment) {
					var sreg = (OpRegSegment) instruction.op0;
					switch (sreg.sreg) {
					case 0: // PUSH ES
						encode = assemble(instruction, 0x06);
						break;
					case 1: // PUSH CS
						encode = assemble(instruction, 0x0E);
						break;
					case 2: // PUSH SS
						encode = assemble(instruction, 0x16);
						break;
					case 3: // PUSH DS
						encode = assemble(instruction, 0x1E);
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
			else
				encode = invalid;
			break;
		case PUSHA:
			encode = assemble(instruction, 0x60);
			break;
		case PUSHF:
			encode = assemble(instruction, 0x9C);
			break;
		case RDMSR:
			encode = new InsnCode(4, bs(0x0F, 0x32));
			break;
		case REP:
			encode = assemble(instruction, 0xF3);
			break;
		case REPE:
			encode = assemble(instruction, 0xF3);
			break;
		case REPNE:
			encode = assemble(instruction, 0xF2);
			break;
		case RET:
			if (instruction.op0 instanceof OpNone)
				encode = assemble(instruction, 0xC3);
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
			encode = assemble(instruction, 0xFB);
			break;
		case STOSB:
			encode = new InsnCode(1, bs(0xAA));
			break;
		case STOSD:
			encode = new InsnCode(4, bs(0xAB));
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
			encode = new InsnCode(4, bs(0x0F, 0x05));
			break;
		case SYSENTER:
			encode = new InsnCode(4, bs(0x0F, 0x34));
			break;
		case SYSEXIT:
			encode = new InsnCode(4, bs(0x0F, 0x35));
			break;
		case TEST:
			if (instruction.op0.size == instruction.op1.size)
				if (instruction.op1 instanceof OpImm)
					encode = assembleRmImm(instruction.op0, (OpImm) instruction.op1, 0xA8, 0xF6, 0);
				else
					encode = assembleByteFlag(instruction.op0, 0x84, instruction.op1);
			else
				encode = invalid;
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
			if (instruction.op0.size == instruction.op1.size && instruction.op1 instanceof OpReg)
				if (isAcc.test(instruction.op0))
					encode = assemble(instruction, 0x90 + ((OpReg) instruction.op1).reg);
				else
					encode = assembleByteFlag(instruction.op0, 0x86, instruction.op1);
			else
				encode = invalid;
			break;
		case WRMSR:
			encode = new InsnCode(4, bs(0x0F, 0x30));
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

	private InsnCode assembleJump(Instruction instruction, long offset, int bNear, byte[] bFar) {
		if (instruction.op0 instanceof OpImm)
			return assembleJumpImm((OpImm) instruction.op0, offset, bNear, bFar);
		else
			return invalid;
	}

	private InsnCode assembleJumpImm(OpImm op0, long offset, int b1, byte[] bs4) {
		var size = op0.size;
		byte[] bs0;

		switch (size) {
		case 1:
			bs0 = bs(b1);
			break;
		case 4:
			bs0 = bs4;
			break;
		default:
			return invalid;
		}

		var rel = op0.imm - (offset + bs0.length + size);
		InsnCode insnCode;

		if (1 < size || -128 <= rel && rel < 128) {
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
			return assemble(instruction.op1, b + (instruction.op1.size <= 1 ? 0 : 1), reg.reg);
		} else
			return invalid;
	}

	private InsnCode assembleRm(Instruction instruction, int bReg, int bModrm, int num) {
		if (bReg != -1 && instruction.op0 instanceof OpReg && 1 < instruction.op0.size && isNonRexReg.test(instruction.op0))
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
		return assembleRmRegImm(instruction, bModrm, bModrm + 4, bImm, num);
	}

	private InsnCode assembleRmRegImm(Instruction instruction, int bModrm, int bAccImm, int bRmImm, int num) {
		InsnCode insnCodeRmReg;
		if (instruction.op0.size == instruction.op1.size)
			if ((insnCodeRmReg = assembleRmReg(instruction, bModrm)).isValid())
				return insnCodeRmReg;
			else if (instruction.op1 instanceof OpImm)
				return assembleRmImm(instruction.op0, (OpImm) instruction.op1, bAccImm, bRmImm, num);
			else
				return invalid;
		else
			return invalid;
	}

	private InsnCode assembleRmReg(Instruction instruction, int b) {
		return assembleRmReg(instruction, b, b + 2, isReg);
	}

	private InsnCode assembleRmReg(Instruction instruction, int bRmReg, int bRegRm, Predicate<Operand> pred) {
		FixieFun3<Operand, Integer, OpReg, InsnCode> fun = (rm, b1, reg) -> 0 <= b1 ? assembleByteFlag(rm, b1, reg) : invalid;
		if (isRm.test(instruction.op0) && pred.test(instruction.op1))
			return fun.apply(instruction.op0, bRmReg, (OpReg) instruction.op1);
		else if (pred.test(instruction.op0) && isRm.test(instruction.op1))
			return fun.apply(instruction.op1, bRegRm, (OpReg) instruction.op0);
		else
			return invalid;
	}

	private InsnCode assembleRegRm(Operand reg, Operand rm, int b) {
		return reg.size == rm.size ? assembleRegRm_(reg, rm, b) : invalid;
	}

	private InsnCode assembleRegRm_(Operand reg, Operand rm, int b) {
		return isReg.test(reg) && isRm.test(rm) ? assemble(rm, b, ((OpReg) reg).reg) : invalid;
	}

	private InsnCode assembleRmImm(Operand op0, OpImm op1, int bAccImm, int bRmImm, int num) {
		var insnCode = new InsnCode(op0.size, op1);

		if (isAcc.test(op0))
			insnCode.bs = bs(bAccImm + (op0.size <= 1 ? 0 : 1));
		else if (isRm.test(op0)) {
			var b0 = ((1 < op0.size && op1.size <= 1) ? 2 : 0) + (op0.size <= 1 ? 0 : 1);
			insnCode.bs = bs(bRmImm + b0);
			insnCode.modrm = modrm(op0, num);
		} else
			insnCode = invalid;
		return insnCode;
	}

	private InsnCode assembleShift(Instruction instruction, int b, int num) {
		if (isRm.test(instruction.op0)) {
			var shift = instruction.op1;
			boolean isShiftImm;
			OpImm shiftImm;
			int b1;
			if (shift instanceof OpImm) {
				shiftImm = (OpImm) shift;
				isShiftImm = 1 <= shiftImm.imm;
				b1 = b + (isShiftImm ? 0 : 16);
			} else if (shift.size == 1 && shift instanceof OpReg && ((OpReg) shift).reg == 1) { // CL
				shiftImm = null;
				isShiftImm = false;
				b1 = b + 16 + 2;
			} else
				return invalid;

			InsnCode insnCode = assembleByteFlag(instruction.op0, b1, num);

			if (shiftImm != null && !isShiftImm) {
				insnCode.immSize = 1;
				insnCode.imm = shiftImm.imm;
			}

			return insnCode;
		} else
			return invalid;
	}

	private InsnCode assembleByteFlag(Operand operand, int b, Operand reg) {
		return assembleByteFlag(operand, b, ((OpReg) reg).reg);
	}

	private InsnCode assembleByteFlag(Operand operand, int b, int num) {
		return assemble(operand, b + (operand.size <= 1 ? 0 : 1), num);
	}

	private InsnCode assemble(Instruction instruction, int b) {
		return new InsnCode(4, bs(b));
	}

	private InsnCode assemble(Operand operand, int b, int num) {
		var insnCode = new InsnCode(operand.size, bs(b));
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
				if (insnCode.size == 2)
					bb.append((byte) 0x66);
				appendIf(bb, modrm != null ? rex(modrm) : rex(insnCode.size, 0, 0, 0));
			}
			bb.append(insnCode.bs);
			if (modrm != null) {
				bb.append(b(modrm.rm, modrm.num, modrm.mod));
				appendIf(bb, sib(modrm));

				long disp;

				if (isAmd64 && modrm.mod == 0 && (modrm.rm & 7) == 5) // RIP-relative addressing
					disp = modrm.disp - offset;
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
		if (0 <= b)
			bb.append((byte) b);
	}

	private void appendImm(BytesBuilder bb, int size, long v) {
		for (var i = 0; i < size; i++) {
			bb.append((byte) (v & 0xFF));
			v >>= 8;
		}
	}

	Predicate<Operand> isAcc = operand -> operand instanceof OpReg && ((OpReg) operand).reg == 0;
	Predicate<Operand> isReg = operand -> operand instanceof OpReg;
	Predicate<Operand> isNonRexReg = operand -> operand instanceof OpReg && ((OpReg) operand).reg < 8;
	Predicate<Operand> isRm = operand -> operand instanceof OpMem || operand instanceof OpReg;
	Predicate<Operand> isXmm = op -> op instanceof OpRegXmm;
	Predicate<Operand> isXmmYmm = op -> op instanceof OpRegXmm || op instanceof OpRegYmm;

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
			var ds0 = op.disp.size;

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
		modrm.size = operand.size;
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
		switch (dispSize) {
		case 0:
			return 0;
		case 1:
			return 1;
		case 4:
			return 2;
		default:
			return fail("bad displacement");
		}
	}

	private int sib(Modrm modrm) {
		return 0 <= modrm.s ? b(modrm.b, modrm.i, modrm.s) : -1;
	}

	private int scale(OpMem op) {
		switch (op.scale) {
		case 1:
			return 0;
		case 2:
			return 1;
		case 4:
			return 2;
		case 8:
			return 3;
		default:
			return fail("bad scale");
		}
	}

	private int rex(Modrm modrm) {
		return rex(modrm.size, modrm.num, modrm.i, 0 <= modrm.b ? modrm.b : modrm.rm);
	}

	private int rex(int size, int r, int x, int b) {
		var b04 = ((size != 8 ? 0 : 1) << 3) //
				+ (bit4(r) << 2) //
				+ (bit4(x) << 1) //
				+ (bit4(b) << 0);
		return b04 != 0 ? 0x40 + b04 : -1;
	}

	// https://en.wikipedia.org/wiki/VEX_prefix
	private byte[] vex(int m, int p, int size, Modrm modrm, int w, int v) {
		var x = bit4(modrm.i);
		var b = bit4(modrm.b);
		var w_ = bit4(w);
		if (m == 1 && x == 0 && b == 0 && w == 0) {
			var b1 = ((bit4(modrm.num) ^ 1) << 7) //
					+ (~v << 3)//
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
