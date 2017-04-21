package suite.assembler;

import java.util.List;

import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpNone;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.OpRegControl;
import suite.assembler.Amd64.OpRegSegment;
import suite.assembler.Amd64.Operand;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;

// TODO validate number of operands
// TODO validate size of operands
public class Amd64Assembler {

	private class InsnCode {
		private int size;
		private byte[] bs;
		private ModRm modRm;
		private int immSize;
		private long imm;

		private InsnCode(int size, OpImm imm) {
			this.size = size;
			this.immSize = imm.size;
			this.imm = imm.imm;
		}

		private InsnCode(int size, byte[] bs) {
			this.size = size;
			this.bs = bs;
		}
	}

	private class ModRm {
		private int size, mod, num, rm, s, i, b, dispSize;
		private long disp;
	}

	public Bytes assemble(long offset, List<Instruction> instructions) {
		BytesBuilder bb = new BytesBuilder();
		for (Instruction instruction : instructions) {
			Bytes bytes = assemble(offset, instruction);
			bb.append(bytes);
			offset += bytes.size();
		}
		return bb.toBytes();
	}

	public Bytes assemble(long offset, Instruction instruction) {
		InsnCode insnCode;
		switch (instruction.insn) {
		case AAA:
			insnCode = assemble(instruction, 0x37);
			break;
		case ADC:
			insnCode = assembleRmRegImm(instruction, 0x10, 0x80, 2);
			break;
		case ADD:
			insnCode = assembleRmRegImm(instruction, 0x00, 0x80, 0);
			break;
		case AND:
			insnCode = assembleRmRegImm(instruction, 0x20, 0x80, 4);
			break;
		case AOP:
			insnCode = assemble(instruction, 0x67);
			break;
		case CALL:
			if (instruction.op0 instanceof OpImm && instruction.op0.size == 4)
				insnCode = assembleJumpImm((OpImm) instruction.op0, offset, -1, bs(0xE8));
			else if (isRm(instruction.op0))
				insnCode = assemble(instruction.op0, bs(0xFF), 2);
			else
				throw new RuntimeException("Bad instruction");
			break;
		case CLD:
			insnCode = assemble(instruction, 0xFC);
			break;
		case CLI:
			insnCode = assemble(instruction, 0xFA);
			break;
		case CMP:
			insnCode = assembleRmRegImm(instruction, 0x38, 0x80, 7);
			break;
		case CMPXCHG:
			insnCode = assembleRegRm(instruction.op1, instruction.op0, bs(0xB0));
			break;
		case CPUID:
			insnCode = new InsnCode(4, bs(0x0F, 0xA2));
			break;
		case DEC:
			insnCode = assembleRm(instruction, 0x48, 0xFE, 1);
			break;
		case DIV:
			insnCode = assembleByteFlag(instruction.op0, 0xF6, 6);
			break;
		case HLT:
			insnCode = assemble(instruction, 0xF4);
			break;
		case IDIV:
			insnCode = assembleByteFlag(instruction.op0, 0xF6, 7);
			break;
		case IMM:
			if (instruction.op0 instanceof OpImm) {
				insnCode = new InsnCode(0, (OpImm) instruction.op0);
				insnCode.bs = new byte[] {};
			} else
				throw new RuntimeException("Bad instruction");
			break;
		case IMUL:
			if (instruction.op1 instanceof OpNone)
				insnCode = assembleByteFlag(instruction.op0, 0xF6, 5);
			else if (instruction.op0.size == instruction.op1.size)
				if (instruction.op2 instanceof OpNone)
					insnCode = assembleRegRm(instruction.op0, instruction.op1, bs(0x0F, 0xAF));
				else if (instruction.op2 instanceof OpImm) {
					OpImm imm = (OpImm) instruction.op2;
					if (imm.size <= 1)
						insnCode = assembleRegRm(instruction.op0, instruction.op1, bs(0x6B));
					else if (imm.size == instruction.op0.size)
						insnCode = assembleRegRm(instruction.op0, instruction.op1, bs(0x69));
					else
						throw new RuntimeException("Bad instruction");
					insnCode.immSize = imm.size;
					insnCode.imm = imm.imm;
				} else
					throw new RuntimeException("Bad instruction");
			else
				throw new RuntimeException("Bad instruction");
			break;
		case IN:
			insnCode = assembleInOut(instruction.op1, instruction.op0, 0xE4);
			break;
		case INC:
			insnCode = assembleRm(instruction, 0x40, 0xFE, 0);
			break;
		case INT:
			if (instruction.op0 instanceof OpImm) {
				long iv = ((OpImm) instruction.op0).imm;
				if (iv != 3) {
					insnCode = assemble(instruction, 0xCD);
					insnCode.immSize = 1;
					insnCode.imm = iv;
				} else
					insnCode = assemble(instruction, 0xCC);
			} else
				throw new RuntimeException("Bad instruction");
			break;
		case INTO:
			insnCode = assemble(instruction, 0xCE);
			break;
		case INVLPG:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x01), 7);
			break;
		case IRET:
			insnCode = assemble(instruction, 0xCF);
			break;
		case JA:
			insnCode = assembleJump(instruction, offset, 0x77, bs(0x0F, 0x87));
			break;
		case JAE:
			insnCode = assembleJump(instruction, offset, 0x73, bs(0x0F, 0x83));
			break;
		case JB:
			insnCode = assembleJump(instruction, offset, 0x72, bs(0x0F, 0x82));
			break;
		case JBE:
			insnCode = assembleJump(instruction, offset, 0x76, bs(0x0F, 0x86));
			break;
		case JE:
			insnCode = assembleJump(instruction, offset, 0x74, bs(0x0F, 0x84));
			break;
		case JG:
			insnCode = assembleJump(instruction, offset, 0x7F, bs(0x0F, 0x8F));
			break;
		case JGE:
			insnCode = assembleJump(instruction, offset, 0x7D, bs(0x0F, 0x8D));
			break;
		case JL:
			insnCode = assembleJump(instruction, offset, 0x7C, bs(0x0F, 0x8C));
			break;
		case JLE:
			insnCode = assembleJump(instruction, offset, 0x7E, bs(0x0F, 0x8E));
			break;
		case JMP:
			if (isRm(instruction.op0) && instruction.op0.size == 4)
				insnCode = assemble(instruction.op0, bs(0xFF), 4);
			else
				insnCode = assembleJump(instruction, offset, 0xEB, bs(0xE9));
			break;
		case JNE:
			insnCode = assembleJump(instruction, offset, 0x75, bs(0x0F, 0x85));
			break;
		case JNO:
			insnCode = assembleJump(instruction, offset, 0x71, bs(0x0F, 0x81));
			break;
		case JNP:
			insnCode = assembleJump(instruction, offset, 0x7B, bs(0x0F, 0x8B));
			break;
		case JNS:
			insnCode = assembleJump(instruction, offset, 0x79, bs(0x0F, 0x89));
			break;
		case JNZ:
			insnCode = assembleJump(instruction, offset, 0x75, bs(0x0F, 0x85));
			break;
		case JO:
			insnCode = assembleJump(instruction, offset, 0x70, bs(0x0F, 0x80));
			break;
		case JP:
			insnCode = assembleJump(instruction, offset, 0x7A, bs(0x0F, 0x8A));
			break;
		case JS:
			insnCode = assembleJump(instruction, offset, 0x78, bs(0x0F, 0x88));
			break;
		case JZ:
			insnCode = assembleJump(instruction, offset, 0x74, bs(0x0F, 0x84));
			break;
		case LEA:
			insnCode = assembleRegRm(instruction.op0, instruction.op1, bs(0x8D));
			break;
		case LOCK:
			insnCode = assemble(instruction, 0xF0);
			break;
		case LOOP:
			insnCode = assembleJump(instruction, offset, 0xE2, null);
			break;
		case LOOPE:
			insnCode = assembleJump(instruction, offset, 0xE1, null);
			break;
		case LOOPNE:
			insnCode = assembleJump(instruction, offset, 0xE0, null);
			break;
		case LOOPNZ:
			insnCode = assembleJump(instruction, offset, 0xE0, null);
			break;
		case LOOPZ:
			insnCode = assembleJump(instruction, offset, 0xE1, null);
			break;
		case LGDT:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x01), 2);
			break;
		case LIDT:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x01), 3);
			break;
		case LTR:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x00), 3);
			break;
		case MOV:
			if (instruction.op0.size == instruction.op1.size)
				if (instruction.op1 instanceof OpImm) {
					OpImm op1 = (OpImm) instruction.op1;

					if (instruction.op0 instanceof OpReg) {
						insnCode = new InsnCode(op1.size, op1);
						OpReg op0 = (OpReg) instruction.op0;
						insnCode.bs = bs(0xB0 + (op0.size <= 1 ? 0 : 8) + op0.reg);
					} else if (isRm(instruction.op0))
						insnCode = assembleByteFlag(op1, 0xC6, 0);
					else
						throw new RuntimeException("Bad instruction");

					insnCode.immSize = op1.size;
					insnCode.imm = op1.imm;
				} else if (instruction.op0 instanceof OpRegSegment) {
					OpRegSegment regSegment = (OpRegSegment) instruction.op0;
					insnCode = assemble(instruction.op1, bs(0x8E), regSegment.sreg);
				} else if (instruction.op1 instanceof OpRegSegment) {
					OpRegSegment regSegment = (OpRegSegment) instruction.op1;
					insnCode = assemble(instruction.op0, bs(0x8C), regSegment.sreg);
				} else if (instruction.op0.size == 4 //
						&& instruction.op0 instanceof OpReg //
						&& instruction.op1 instanceof OpRegControl) {
					OpReg reg = (OpReg) instruction.op0;
					OpRegControl regControl = (OpRegControl) instruction.op1;
					insnCode = new InsnCode(4, new byte[] { (byte) 0x0F, (byte) 0x20, b(reg.reg, regControl.creg, 3), });
				} else if (instruction.op0.size == 4 //
						&& instruction.op0 instanceof OpRegControl //
						&& instruction.op1 instanceof OpReg) {
					OpRegControl regControl = (OpRegControl) instruction.op0;
					OpReg reg = (OpReg) instruction.op1;
					insnCode = new InsnCode(4, new byte[] { (byte) 0x0F, (byte) 0x22, b(reg.reg, regControl.creg, 3), });
				} else if ((insnCode = assembleRmReg(instruction, 0x88)) != null)
					;
				else
					throw new RuntimeException("Bad instruction");
			else
				throw new RuntimeException("Bad instruction");
			break;
		case MOVSB:
			insnCode = new InsnCode(1, bs(0xA4));
			break;
		case MOVSD:
			insnCode = new InsnCode(4, bs(0xA5));
			break;
		case MOVSW:
			insnCode = new InsnCode(2, bs(0xA5));
			break;
		case MOVSX:
			insnCode = assembleRegRmExtended(instruction, 0x0F, 0xBE);
			break;
		case MOVZX:
			insnCode = assembleRegRmExtended(instruction, 0x0F, 0xB6);
			break;
		case MUL:
			insnCode = assembleByteFlag(instruction.op0, 0xF6, 4);
			break;
		case NOP:
			insnCode = assemble(instruction, 0x90);
			break;
		case OR:
			insnCode = assembleRmRegImm(instruction, 0x08, 0x80, 1);
			break;
		case OUT:
			insnCode = assembleInOut(instruction.op0, instruction.op1, 0xE6);
			break;
		case POP:
			if (1 < instruction.op0.size)
				if (isRm(instruction.op0))
					insnCode = assembleRm(instruction, 0x58, 0x8E, 0);
				else if (instruction.op0 instanceof OpRegSegment) {
					OpRegSegment sreg = (OpRegSegment) instruction.op0;
					switch (sreg.sreg) {
					case 0: // POP ES
						insnCode = assemble(instruction, 0x07);
						break;
					// case 1: // POP CS, no such thing
					case 2: // POP SS
						insnCode = assemble(instruction, 0x17);
						break;
					case 3: // POP DS
						insnCode = assemble(instruction, 0x1F);
						break;
					case 4: // POP FS
						insnCode = new InsnCode(sreg.size, bs(0x0F, 0xA1));
						break;
					case 5: // POP GS
						insnCode = new InsnCode(sreg.size, bs(0x0F, 0xA9));
						break;
					default:
						throw new RuntimeException("Bad instruction");
					}
				} else
					throw new RuntimeException("Bad instruction");
			else
				throw new RuntimeException("Bad instruction");
			break;
		case POPA:
			insnCode = assemble(instruction, 0x61);
			break;
		case POPF:
			insnCode = assemble(instruction, 0x9D);
			break;
		case PUSH:
			if (instruction.op0 instanceof OpImm) {
				insnCode = new InsnCode(instruction.op0.size, (OpImm) instruction.op0);
				insnCode.bs = bs(0x6A + (1 < instruction.op0.size ? 0 : 2));
			} else if (1 < instruction.op0.size)
				if (isRm(instruction.op0))
					insnCode = assembleRm(instruction, 0x50, 0xFE, 6);
				else if (instruction.op0 instanceof OpRegSegment) {
					OpRegSegment sreg = (OpRegSegment) instruction.op0;
					switch (sreg.sreg) {
					case 0: // PUSH ES
						insnCode = assemble(instruction, 0x06);
						break;
					case 1: // PUSH CS
						insnCode = assemble(instruction, 0x0E);
						break;
					case 2: // PUSH SS
						insnCode = assemble(instruction, 0x16);
						break;
					case 3: // PUSH DS
						insnCode = assemble(instruction, 0x1E);
						break;
					case 4: // PUSH FS
						insnCode = new InsnCode(sreg.size, bs(0x0F, 0xA0));
						break;
					case 5: // PUSH GS
						insnCode = new InsnCode(sreg.size, bs(0x0F, 0xA8));
						break;
					default:
						throw new RuntimeException("Bad instruction");
					}
				} else
					throw new RuntimeException("Bad instruction");
			else
				throw new RuntimeException("Bad instruction");
			break;
		case PUSHA:
			insnCode = assemble(instruction, 0x60);
			break;
		case PUSHF:
			insnCode = assemble(instruction, 0x9C);
			break;
		case RDMSR:
			insnCode = new InsnCode(4, bs(0x0F, 0x32));
			break;
		case REP:
			insnCode = assemble(instruction, 0xF3);
			break;
		case REPE:
			insnCode = assemble(instruction, 0xF3);
			break;
		case REPNE:
			insnCode = assemble(instruction, 0xF2);
			break;
		case RET:
			if (instruction.op0 instanceof OpNone)
				insnCode = assemble(instruction, 0xC3);
			else if (instruction.op0 instanceof OpImm && instruction.op0.size == 2) {
				insnCode = new InsnCode(instruction.op0.size, (OpImm) instruction.op0);
				insnCode.bs = bs(0xC2);
			} else
				throw new RuntimeException("Bad instruction");
			break;
		case SAL:
			insnCode = assembleShift(instruction, 0xC0, 4);
			break;
		case SAR:
			insnCode = assembleShift(instruction, 0xC0, 7);
			break;
		case SBB:
			insnCode = assembleRmRegImm(instruction, 0x18, 0x80, 3);
			break;
		case SETA:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x97), 0);
			break;
		case SETAE:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x93), 0);
			break;
		case SETB:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x92), 0);
			break;
		case SETBE:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x96), 0);
			break;
		case SETE:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x94), 0);
			break;
		case SETG:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x9F), 0);
			break;
		case SETGE:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x9D), 0);
			break;
		case SETL:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x9C), 0);
			break;
		case SETLE:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x9E), 0);
			break;
		case SETNE:
			insnCode = assemble(instruction.op0, bs(0x0F, 0x95), 0);
			break;
		case SHL:
			insnCode = assembleShift(instruction, 0xC0, 4);
			break;
		case SHR:
			insnCode = assembleShift(instruction, 0xC0, 5);
			break;
		case STI:
			insnCode = assemble(instruction, 0xFB);
			break;
		case STOSB:
			insnCode = new InsnCode(1, bs(0xAA));
			break;
		case STOSD:
			insnCode = new InsnCode(4, bs(0xAB));
			break;
		case STOSW:
			insnCode = new InsnCode(2, bs(0xAB));
			break;
		case SUB:
			insnCode = assembleRmRegImm(instruction, 0x28, 0x80, 5);
			break;
		case SYSENTER:
			insnCode = new InsnCode(4, bs(0x0F, 0x34));
			break;
		case SYSEXIT:
			insnCode = new InsnCode(4, bs(0x0F, 0x35));
			break;
		case TEST:
			if (instruction.op0.size == instruction.op1.size)
				if (instruction.op1 instanceof OpImm)
					insnCode = assembleRmImm(instruction.op0, (OpImm) instruction.op1, 0xA8, 0xF6, 0);
				else
					insnCode = assembleRegRm(instruction.op1, instruction.op0, bs(0x84));
			else
				throw new RuntimeException("Bad instruction");
			break;
		case XCHG:
			if (instruction.op0.size == instruction.op1.size)
				if (isAcc(instruction.op0) && instruction.op1 instanceof OpReg)
					insnCode = assemble(instruction, 0x90 + ((OpReg) instruction.op1).reg);
				else
					insnCode = assembleRegRm(instruction.op1, instruction.op0, bs(0x86));
			else
				throw new RuntimeException("Bad instruction");
			break;
		case WRMSR:
			insnCode = new InsnCode(4, bs(0x0F, 0x30));
			break;
		case XOR:
			insnCode = assembleRmRegImm(instruction, 0x30, 0x80, 6);
			break;
		default:
			insnCode = null;
		}

		if (insnCode != null)
			return encode(insnCode);
		else
			throw new RuntimeException("Bad instruction");
	}

	private InsnCode assembleInOut(Operand port, Operand acc, int b) {
		if (isAcc(acc)) {
			OpImm portImm;
			if (port instanceof OpImm)
				portImm = (OpImm) port;
			else if (port.size == 2 && port instanceof OpReg && ((OpReg) port).reg == 2) // DX
				portImm = null;
			else
				throw new RuntimeException("Bad instruction");

			InsnCode insnCode = new InsnCode(acc.size, bs(b + (acc.size == 1 ? 0 : 1) + (portImm != null ? 0 : 8)));
			if (portImm != null) {
				insnCode.immSize = 1;
				insnCode.imm = portImm.imm;
			}
			return insnCode;
		} else
			throw new RuntimeException("Bad instruction");
	}

	private InsnCode assembleJump(Instruction instruction, long offset, int b_near, byte b_far[]) {
		if (instruction.op0 instanceof OpImm)
			return assembleJumpImm((OpImm) instruction.op0, offset, b_near, b_far);
		else
			throw new RuntimeException("Bad instruction");
	}

	private InsnCode assembleJumpImm(OpImm op0, long offset, int b1, byte[] bs4) {
		InsnCode insnCode;
		int size = op0.size;
		byte[] bs0;

		switch (size) {
		case 1:
			bs0 = bs(b1);
			break;
		case 4:
			bs0 = bs4;
			break;
		default:
			throw new RuntimeException("Bad instruction");
		}

		long rel = op0.imm - (offset + bs0.length + size);

		if (1 < size || -128 <= rel && rel < 128) {
			insnCode = new InsnCode(size, bs0);
			insnCode.immSize = size;
			insnCode.imm = rel;
			return insnCode;
		} else
			throw new RuntimeException("Jump too far");
	}

	private InsnCode assembleRegRmExtended(Instruction instruction, int b0, int b1) {
		if (instruction.op0 instanceof OpReg && isRm(instruction.op1)) {
			OpReg reg = (OpReg) instruction.op0;
			return assemble(instruction.op1, bs(b0, b1 + (instruction.op1.size <= 1 ? 0 : 1)), reg.reg);
		} else
			throw new RuntimeException("Bad instruction");
	}

	private InsnCode assembleRegRm(Operand reg, Operand rm, byte[] bs) {
		if (reg instanceof OpReg && isRm(rm))
			return assemble(rm, bs, ((OpReg) reg).reg);
		else
			throw new RuntimeException("Bad instruction");
	}

	private InsnCode assembleRm(Instruction instruction, int b_reg, int b_modrm, int num) {
		InsnCode insnCode;
		if (instruction.op0 instanceof OpReg && 1 < instruction.op0.size) {
			OpReg op0 = (OpReg) instruction.op0;
			insnCode = new InsnCode(instruction.op0.size, bs(b_reg + op0.reg));
		} else if (isRm(instruction.op0))
			insnCode = assembleByteFlag(instruction.op0, b_modrm, num);
		else
			throw new RuntimeException("Bad instruction");
		return insnCode;
	}

	private InsnCode assembleRmRegImm(Instruction instruction, int b_modrm, int b_imm, int num) {
		return assembleRmRegImm(instruction, b_modrm, b_imm + 4, b_imm, num);
	}

	private InsnCode assembleRmRegImm(Instruction instruction, int b_modrm, int b_accImm, int b_rmImm, int num) {
		InsnCode insnCode, insnCodeRmReg;
		if ((insnCodeRmReg = assembleRmReg(instruction, b_modrm)) != null)
			insnCode = insnCodeRmReg;
		else if (instruction.op1 instanceof OpImm)
			insnCode = assembleRmImm(instruction.op0, (OpImm) instruction.op1, b_accImm, b_rmImm, num);
		else
			throw new RuntimeException("Bad instruction");
		return insnCode;
	}

	private InsnCode assembleRmReg(Instruction instruction, int b) {
		Operand rm;
		OpReg reg;
		int b1;
		if (isRm(instruction.op0) && instruction.op1 instanceof OpReg) {
			rm = instruction.op0;
			reg = (OpReg) instruction.op1;
			b1 = b;
		} else if (instruction.op0 instanceof OpReg && isRm(instruction.op1)) {
			rm = instruction.op1;
			reg = (OpReg) instruction.op0;
			b1 = b + 2;
		} else {
			rm = reg = null;
			b1 = -1;
		}
		return 0 <= b1 ? assembleByteFlag(rm, b1, reg.reg) : null;
	}

	private InsnCode assembleRmImm(Operand op0, OpImm op1, int b_accImm, int b_rmImm, int num) {
		InsnCode insnCode;
		insnCode = new InsnCode(op0.size, op1);

		if (isAcc(op0))
			insnCode.bs = bs(b_accImm + (op0.size <= 1 ? 0 : 1));
		else if (isRm(op0)) {
			int b0 = ((1 < op0.size && op1.size <= 1) ? 2 : 0) + (op0.size <= 1 ? 0 : 1);
			insnCode.bs = bs(b_rmImm + b0);
			insnCode.modRm = modRm(op0, num);
		} else
			throw new RuntimeException("Bad instruction");
		return insnCode;
	}

	private InsnCode assembleShift(Instruction instruction, int b, int num) {
		if (isRm(instruction.op0)) {
			Operand shift = instruction.op1;
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
				throw new RuntimeException("Bad instruction");

			InsnCode insnCode = assembleByteFlag(instruction.op0, b1, num);

			if (shiftImm != null && !isShiftImm) {
				insnCode.immSize = 1;
				insnCode.imm = shiftImm.imm;
			}

			return insnCode;
		} else
			throw new RuntimeException("Bad instruction");
	}

	private InsnCode assembleByteFlag(Operand operand, int b, int num) {
		int b1 = b + (operand.size <= 1 ? 0 : 1);
		return assemble(operand, bs(b1), num);
	}

	private InsnCode assemble(Instruction instruction, int b) {
		return new InsnCode(4, bs(b));
	}

	private InsnCode assemble(Operand operand, byte[] bs, int num) {
		InsnCode insnCode = new InsnCode(operand.size, bs);
		insnCode.modRm = modRm(operand, num);
		return insnCode;
	}

	private Bytes encode(InsnCode insnCode) {
		ModRm modRm = insnCode.modRm;
		int rex = modRm != null ? rex(modRm) : rex(insnCode.size, 0, 0, 0);

		BytesBuilder bb = new BytesBuilder();
		if (insnCode.size == 2)
			bb.append((byte) 0x66);
		appendIf(bb, rex);
		bb.append(insnCode.bs);
		if (modRm != null) {
			bb.append(b(modRm.rm, modRm.num, modRm.mod));
			appendIf(bb, sib(modRm));
			appendImm(bb, modRm.dispSize, modRm.disp);
		}
		appendImm(bb, insnCode.immSize, insnCode.imm);
		return bb.toBytes();
	}

	private void appendIf(BytesBuilder bb, int b) {
		if (0 <= b)
			bb.append((byte) b);
	}

	private void appendImm(BytesBuilder bb, int size, long v) {
		for (int i = 0; i < size; i++) {
			bb.append((byte) (v & 0xFF));
			v >>= 8;
		}
	}

	private boolean isAcc(Operand operand) {
		return operand instanceof OpReg && ((OpReg) operand).reg == 0;
	}

	private boolean isRm(Operand operand) {
		return operand instanceof OpMem || operand instanceof OpReg;
	}

	private ModRm modRm(Operand operand, int num) {
		int mod, rm, s, i, b, dispSize;
		long disp;

		if (operand instanceof OpReg) { // EAX
			OpReg op = (OpReg) operand;
			mod = 3;
			rm = op.reg;
			s = i = b = -1;
			dispSize = 0;
			disp = 0;
		} else if (operand instanceof OpMem) {
			OpMem op = (OpMem) operand;
			if (op.baseReg < 0 && op.indexReg < 0) { // [0x1234]
				mod = 0;
				rm = 5;
				s = i = b = -1;
				dispSize = 4;
			} else if (0 <= op.baseReg && op.indexReg < 0)
				if ((op.baseReg & 7) != 4) {
					// [EAX], [EAX + 0x1234]
					int ds = op.dispSize == 0 && (op.baseReg & 7) == 5 ? 1 : op.dispSize;
					mod = dispMod(ds);
					rm = op.baseReg;
					s = i = b = -1;
					dispSize = ds;
				} else
					throw new RuntimeException("Bad operand");
			else if (op.baseReg < 0 && 0 <= op.indexReg)
				if ((op.indexReg & 7) != 4) { // [4 * EBX + 0x1234]
					mod = 0;
					rm = 4;
					s = scale(op);
					i = op.indexReg;
					b = 5;
					dispSize = 4;
				} else
					throw new RuntimeException("Bad operand");
			else if (0 <= op.baseReg && 0 <= op.indexReg)
				if ((op.baseReg & 7) != 5 && (op.indexReg & 7) != 4) {
					// [4 * EBX + EAX + 0x1234]
					mod = dispMod(op.dispSize);
					rm = 4;
					s = scale(op);
					i = op.indexReg;
					b = op.baseReg;
					dispSize = op.dispSize;
				} else
					throw new RuntimeException("Bad operand");
			else
				throw new RuntimeException("Bad operand");

			disp = op.disp;
		} else
			throw new RuntimeException("Bad operand");

		ModRm modRm = new ModRm();
		modRm.size = operand.size;
		modRm.mod = mod;
		modRm.num = num;
		modRm.rm = rm;
		modRm.s = s;
		modRm.i = i;
		modRm.b = b;
		modRm.dispSize = dispSize;
		modRm.disp = disp;
		return modRm;
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
			throw new RuntimeException("Bad displacement");
		}
	}

	private int sib(ModRm modRm) {
		return 0 <= modRm.s ? b(modRm.b, modRm.i, modRm.s) : -1;
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
			throw new RuntimeException("Bad scale");
		}
	}

	private int rex(ModRm modRm) {
		return rex(modRm.size, modRm.num, modRm.i, modRm.b);
	}

	private int rex(int size, int r, int x, int b) {
		int b04 = ((size != 8 ? 0 : 1) << 3) //
				+ (((r >> 3) & 1) << 2) //
				+ (((x >> 3) & 1) << 1) //
				+ (((b >> 3) & 1) << 0);
		return b04 != 0 ? 0x40 + b04 : -1;
	}

	private byte[] bs(int b0, int b1) {
		return new byte[] { (byte) b0, (byte) b1, };
	}

	private byte[] bs(int b) {
		return new byte[] { (byte) b, };
	}

	private byte b(int b03, int b36, int b68) {
		return (byte) ((b03 & 7) + ((b36 & 7) << 3) + ((b68 & 3) << 6));
	}

}
