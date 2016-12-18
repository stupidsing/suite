package suite.assembler;

import java.util.List;

import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;

public class AssemblerAmd64 {

	public enum Insn {
		AAA, ADD,
	};

	public abstract class Operand {
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

	public class OpRegSegment extends Operand {
		public int sreg;
	}

	public class Instruction {
		public Insn insn;
		public Operand op0, op1, op2;
	}

	public Bytes assemble(List<Instruction> instructions) {
		BytesBuilder bb = new BytesBuilder();
		for (Instruction instruction : instructions)
			bb.append(assemble(instruction));
		return bb.toBytes();
	}

	public Bytes assemble(Instruction instruction) {
		Bytes bytes;
		switch (instruction.insn) {
		case AAA:
			bytes = Bytes.of(new byte[] { 0x37, });
			break;
		case ADD:
			bytes = assembleModRm(0x00, 0x80, 0, instruction);
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
				bytes = assembleModRmReg(b_modrm, new ModRm(instruction.op0, num), (OpReg) instruction.op1);
			else if (instruction.op0 instanceof OpReg && isRm(instruction.op1))
				bytes = assembleModRmReg(b_modrm + 2, new ModRm(instruction.op1, num), (OpReg) instruction.op0);
			else if (instruction.op1 instanceof OpImm) {
				OpImm op1 = (OpImm) instruction.op1;
				BytesBuilder bb = new BytesBuilder();
				if (isAcc(instruction.op0))
					bb.append((byte) (b_imm + 4 + (1 < instruction.op0.size ? 1 : 0)));
				else if (isRm(instruction.op0)) {
					ModRm modRm = new ModRm(instruction.op0, num);
					int b0 = op1.size == 1 ? 3 : (1 < instruction.op0.size ? 1 : 0);
					bb.append((byte) (b_imm + b0));
					bb.append(modRm.modNumRm());
				} else
					throw new RuntimeException("Bad instruction");
				appendImm(bb, op1.imm, op1.size);
				bytes = bb.toBytes();
			} else
				throw new RuntimeException("Bad instruction");
		else
			throw new RuntimeException("Bad instruction");
		return bytes;
	}

	private void appendImm(BytesBuilder bb, long v, int size) {
		for (int i = 0; i < size; i++) {
			bb.append((byte) (v & 0xFF));
			v >>= 8;
		}
	}

	private Bytes assembleModRmReg(int b0, ModRm modRm, OpReg op1) {
		BytesBuilder bb = new BytesBuilder();
		int rex = modRm.rex();
		int sib = modRm.sib();

		if (0 <= modRm.rex())
			bb.append((byte) rex);
		bb.append((byte) (b0 + (1 < op1.size ? 1 : 0)));
		bb.append(modRm.modNumRm());
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

	private class ModRm {
		private int mod, num, rm, s, i, b, dispSize;
		private long disp;

		private ModRm(Operand operand, int n) {
			num = n;
			if (operand instanceof OpReg) { // EAX
				OpReg op = (OpReg) operand;
				mod = 3;
				rm = op.reg;
				s = i = b = -1;
				dispSize = 0;
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
					if ((op.indexReg & 7) != 4) { // [EBX * 4 + 0x1234]
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
						// [EBX * 4 + EAX + 0x1234]
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
			}
		}

		private byte rex() {
			int isOpSize64 = 0;
			int b04 = (isOpSize64 << 3) //
					+ (((num >> 3) & 1) << 2) //
					+ (((i >> 3) & 1) << 1) //
					+ (((b >> 3) & 1) << 0);
			return b04 != 0 ? (byte) (0x40 + b04) : -1;
		}

		private byte modNumRm() {
			return b(rm, num, mod);
		}

		private int dispMod(int dispSize) {
			return dispSize == 0 ? 0 : dispSize == 1 ? 1 : 2;
		}

		private int sib() {
			return 0 <= s ? b(b, i, s) : -1;
		}

		private int scale(OpMem op) {
			return op.scale == 1 ? 0 : op.scale == 2 ? 1 : op.scale == 4 ? 2 : 3;
		}

		private byte b(int b03, int b36, int b68) {
			return (byte) ((b03 & 7) + ((b36 & 7) << 3) + ((b68 & 3) << 6));
		}
	}

}
