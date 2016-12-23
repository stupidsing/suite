package suite.assembler;

import java.util.List;

import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpNone;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;

public class Amd64Assembler {

	private class InsnCode {
		private int size;
		private byte bs[];
		private ModRm modRm;
		private int immSize;
		private long imm;

		private InsnCode(int size) {
			this.size = size;
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
			insnCode = new InsnCode(1);
			insnCode.bs = new byte[] { 0x37, };
			break;
		case ADD:
			insnCode = assembleRmReg(instruction, 0x00, 0x80, 0);
			break;
		case DEC:
			insnCode = assembleRm(instruction, 0x48, 0xFE, 1);
			break;
		case IN:
			insnCode = assembleInOut(instruction.op1, instruction.op0, 0xE4);
			break;
		case JMP:
			insnCode = assembleJump(instruction, offset, 0xEB, new byte[] { (byte) 0xE9, });
			break;
		case MOV:
			if (instruction.op0.size == instruction.op1.size)
				if (instruction.op0 instanceof OpReg && instruction.op1 instanceof OpImm) {
					OpReg op0 = (OpReg) instruction.op0;
					OpImm op1 = (OpImm) instruction.op1;

					insnCode = new InsnCode(op0.size);
					insnCode.bs = new byte[] { (byte) (0xB0 + (op0.size <= 1 ? 0 : 8) + op0.reg), };
					insnCode.immSize = op1.size;
					insnCode.imm = op1.imm;
				} else
					throw new RuntimeException("Bad instruction");
			else
				throw new RuntimeException("Bad instruction");
		case OUT:
			insnCode = assembleInOut(instruction.op0, instruction.op1, 0xE6);
			break;
		default:
			insnCode = null;
		}

		if (insnCode != null)
			return assemble(insnCode);
		else
			throw new RuntimeException("Bad instruction");
	}

	private InsnCode assembleInOut(Operand port, Operand acc, int b) {
		if (isAcc(acc)) {
			OpImm portImm = port instanceof OpImm ? (OpImm) port : null;
			InsnCode insnCode = new InsnCode(acc.size);
			insnCode.bs = new byte[] { (byte) (b + (acc.size == 1 ? 0 : 1) + (portImm != null ? 0 : 8)), };
			if (portImm != null) {
				insnCode.immSize = 1;
				insnCode.imm = portImm.imm;
			}
			return insnCode;
		} else
			throw new RuntimeException("Bad instruction");
	}

	private InsnCode assembleJump(Instruction instruction, long offset, int b_near, byte b_far[]) {
		InsnCode insnCode;
		if (isRm(instruction.op0))
			insnCode = assembleModRmReg(4, 0xFF, modRm(instruction.op0, 4));
		else if (instruction.op0 instanceof OpImm) {
			OpImm op0 = (OpImm) instruction.op0;
			int size = op0.size;
			byte bs0[];

			switch (size) {
			case 1:
				bs0 = new byte[] { (byte) b_near, };
				break;
			case 4:
				bs0 = b_far;
				break;
			default:
				throw new RuntimeException("Bad instruction");
			}

			long rel = op0.imm - (offset + bs0.length + size);

			insnCode = new InsnCode(size);
			insnCode.bs = bs0;
			insnCode.immSize = size;
			insnCode.imm = rel;
		} else
			throw new RuntimeException("Bad instruction");
		return insnCode;
	}

	private InsnCode assembleRm(Instruction instruction, int b_reg, int b_modrm, int num) {
		InsnCode insnCode;
		if (instruction.op0 instanceof OpReg && 1 < instruction.op0.size) {
			OpReg op0 = (OpReg) instruction.op0;
			insnCode = new InsnCode(instruction.op0.size);
			insnCode.bs = new byte[] { (byte) (b_reg + op0.reg), };
		} else if (isRm(instruction.op0))
			insnCode = assembleModRmReg(instruction.op0.size, b_modrm, modRm(instruction.op0, num));
		else
			throw new RuntimeException("Bad instruction");
		return insnCode;
	}

	private InsnCode assembleRmReg(Instruction instruction, int b_modrm, int b_imm, int num) {
		InsnCode insnCode;
		if (instruction.op2 instanceof OpNone)
			if (isRm(instruction.op0) && instruction.op1 instanceof OpReg)
				insnCode = assembleModRmReg(instruction.op1.size, b_modrm, modRm(instruction.op0, num));
			else if (instruction.op0 instanceof OpReg && isRm(instruction.op1))
				insnCode = assembleModRmReg(instruction.op0.size, b_modrm + 2, modRm(instruction.op1, num));
			else if (instruction.op1 instanceof OpImm) {
				OpImm op1 = (OpImm) instruction.op1;

				insnCode = new InsnCode(instruction.op0.size);
				insnCode.immSize = op1.size;
				insnCode.imm = op1.imm;

				if (isAcc(instruction.op0))
					insnCode.bs = new byte[] { (byte) (b_imm + 4 + (instruction.op0.size <= 1 ? 0 : 1)), };
				else if (isRm(instruction.op0)) {
					int b0 = op1.size == 1 ? 3 : (instruction.op0.size <= 1 ? 0 : 1);
					insnCode.bs = new byte[] { (byte) (b_imm + b0), };
					insnCode.modRm = modRm(instruction.op0, num);
				} else
					throw new RuntimeException("Bad instruction");
			} else
				throw new RuntimeException("Bad instruction");
		else
			throw new RuntimeException("Bad instruction");
		return insnCode;
	}

	private boolean isAcc(Operand operand) {
		return operand instanceof OpReg && ((OpReg) operand).reg == 0;
	}

	private boolean isRm(Operand operand) {
		return operand instanceof OpMem || operand instanceof OpReg;
	}

	private InsnCode assembleModRmReg(int size, int b0, ModRm modRm) {
		InsnCode insnCode = new InsnCode(size);
		insnCode.bs = new byte[] { (byte) (b0 + (size <= 1 ? 0 : 1)), };
		insnCode.modRm = modRm;
		return insnCode;
	}

	private Bytes assemble(InsnCode insnCode) {
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

	private byte b(int b03, int b36, int b68) {
		return (byte) ((b03 & 7) + ((b36 & 7) << 3) + ((b68 & 3) << 6));
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

}
