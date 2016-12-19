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

	private class ModRm {
		private int mod, num, rm, s, i, b, dispSize;
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
		Bytes bytes;
		switch (instruction.insn) {
		case AAA:
			bytes = Bytes.of(new byte[] { 0x37, });
			break;
		case ADD:
			bytes = assembleModRm(0x00, 0x80, 0, instruction);
			break;
		case JMP:
			if (isRm(instruction.op0))
				bytes = assembleModRmReg(0xFF, modRm(instruction.op0, 4), 4);
			else if (instruction.op0 instanceof OpImm) {
				OpImm op0 = (OpImm) instruction.op0;
				int b;

				switch (op0.size) {
				case 1:
					b = 0xEB;
					break;
				case 4:
					b = 0xE9;
					break;
				default:
					throw new RuntimeException("Bad instruction");
				}

				Bytes b0 = Bytes.of(new byte[] { (byte) b, });
				long rel = op0.imm - (offset + b0.size() + op0.size);

				BytesBuilder bb = new BytesBuilder();
				bb.append(b0);
				appendImm(bb, rel, op0.size);
				bytes = bb.toBytes();
			} else
				throw new RuntimeException("Bad instruction");
			break;
		default:
			bytes = null;
		}

		if (bytes != null)
			return bytes;
		else
			throw new RuntimeException("Bad instruction");
	}

	private Bytes assembleModRm(int b_modrm, int b_imm, int num, Instruction instruction) {
		Bytes bytes;
		if (instruction.op2 instanceof OpNone)
			if (isRm(instruction.op0) && instruction.op1 instanceof OpReg)
				bytes = assembleModRmReg(b_modrm, modRm(instruction.op0, num), instruction.op1.size);
			else if (instruction.op0 instanceof OpReg && isRm(instruction.op1))
				bytes = assembleModRmReg(b_modrm + 2, modRm(instruction.op1, num), instruction.op0.size);
			else if (instruction.op1 instanceof OpImm) {
				OpImm op1 = (OpImm) instruction.op1;
				BytesBuilder bb = new BytesBuilder();
				if (isAcc(instruction.op0))
					bb.append((byte) (b_imm + 4 + (1 < instruction.op0.size ? 1 : 0)));
				else if (isRm(instruction.op0)) {
					ModRm modRm = modRm(instruction.op0, num);
					int b0 = op1.size == 1 ? 3 : (1 < instruction.op0.size ? 1 : 0);
					bb.append((byte) (b_imm + b0));
					bb.append(modNumRm(modRm));
				} else
					throw new RuntimeException("Bad instruction");
				appendImm(bb, op1);
				bytes = bb.toBytes();
			} else
				throw new RuntimeException("Bad instruction");
		else
			throw new RuntimeException("Bad instruction");
		return bytes;
	}

	private Bytes assembleModRmReg(int b0, ModRm modRm, int size) {
		BytesBuilder bb = new BytesBuilder();
		int rex = rex(modRm);
		int sib = sib(modRm);

		if (0 <= rex(modRm))
			bb.append((byte) rex);
		bb.append((byte) (b0 + (1 < size ? 1 : 0)));
		bb.append(modNumRm(modRm));
		if (0 <= sib)
			bb.append((byte) sib);
		appendImm(bb, modRm.disp, modRm.dispSize);
		return bb.toBytes();
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

	private byte rex(ModRm modRm) {
		int isOpSize64 = 0;
		int b04 = (isOpSize64 << 3) //
				+ (((modRm.num >> 3) & 1) << 2) //
				+ (((modRm.i >> 3) & 1) << 1) //
				+ (((modRm.b >> 3) & 1) << 0);
		return b04 != 0 ? (byte) (0x40 + b04) : -1;
	}

	private byte modNumRm(ModRm modRm) {
		return b(modRm.rm, modRm.num, modRm.mod);
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

	private byte b(int b03, int b36, int b68) {
		return (byte) ((b03 & 7) + ((b36 & 7) << 3) + ((b68 & 3) << 6));
	}

	private void appendImm(BytesBuilder bb, OpImm op) {
		appendImm(bb, op.imm, op.size);
	}

	private void appendImm(BytesBuilder bb, long v, int size) {
		for (int i = 0; i < size; i++) {
			bb.append((byte) (v & 0xFF));
			v >>= 8;
		}
	}

}
