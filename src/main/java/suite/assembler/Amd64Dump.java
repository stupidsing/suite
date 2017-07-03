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
import suite.streamlet.As;
import suite.streamlet.Read;

public class Amd64Dump {

	private Amd64 amd64 = new Amd64();

	public String dump(List<Instruction> instructions) {
		return Read.from(instructions).map(instruction -> dump(instruction) + "\n").collect(As.joined());
	}

	public String dump(Instruction instruction) {
		Operand op0 = instruction.op0;
		Operand op1 = instruction.op1;
		Operand op2 = instruction.op2;
		return instruction.insn //
				+ (!(op0 instanceof OpNone) ? " " + dump(op0) : "") //
				+ (!(op1 instanceof OpNone) ? "," + dump(op1) : "") //
				+ (!(op2 instanceof OpNone) ? "," + dump(op2) : "");
	}

	private String dump(Operand op0) {
		if (op0 instanceof OpImm) {
			OpImm opImm = (OpImm) op0;
			return dump(opImm.imm, opImm.size);
		} else if (op0 instanceof OpMem) {
			OpMem opMem = (OpMem) op0;
			int baseReg = opMem.baseReg;
			int indexReg = opMem.indexReg;
			String s = "" //
					+ (0 <= baseReg ? " + " + amd64.reg32[baseReg] : "") //
					+ (0 <= indexReg ? " + " + amd64.reg32[indexReg] + " * " + (1 << opMem.scale) : "") //
					+ (0 <= opMem.dispSize ? " + " + dump(opMem.disp, opMem.dispSize) : "");
			return "[" + s.substring(3) + "]";
		} else if (op0 instanceof OpReg)
			return amd64.regsByName.inverse().get(op0).name;
		else if (op0 instanceof OpRegControl)
			return amd64.cregsByName.inverse().get(op0).name;
		else if (op0 instanceof OpRegSegment)
			return amd64.sregsByName.inverse().get(op0).name;
		else
			return op0.toString();
	}

	private String dump(long imm, int size) {
		String s = "";
		for (int i = 0; i < size; i++) {
			s += "0123456789ABCDEF".charAt((int) (imm & 15));
			imm >>= 4;
		}
		return s;
	}

}
