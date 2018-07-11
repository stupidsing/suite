package suite.assembler; import static suite.util.Friends.fail;

import java.util.List;

import suite.assembler.Amd64.Instruction;
import suite.assembler.Amd64.OpImm;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpNone;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.streamlet.Read;

public class Amd64Dump {

	private Amd64 amd64 = Amd64.me;

	public String dump(List<Instruction> instructions) {
		return Read.from(instructions).map(this::dump).toString();
	}

	public String dump(Instruction instruction) {
		var op0 = instruction.op0;
		var op1 = instruction.op1;
		var op2 = instruction.op2;
		return instruction.insn //
				+ (!(op0 instanceof OpNone) ? " " + dump(op0) : "") //
				+ (!(op1 instanceof OpNone) ? "," + dump(op1) : "") //
				+ (!(op2 instanceof OpNone) ? "," + dump(op2) : "");
	}

	private String dump(Operand op0) {
		var pointerSize = 4;
		OpReg[] regs;
		String name;

		if (pointerSize == 4)
			regs = amd64.reg32;
		else if (pointerSize == 8)
			regs = amd64.reg64;
		else
			return fail();

		if (op0 instanceof OpImm) {
			var opImm = (OpImm) op0;
			return dump(opImm.imm, opImm.size);
		} else if (op0 instanceof OpMem) {
			var opMem = (OpMem) op0;
			var baseReg = opMem.baseReg;
			var indexReg = opMem.indexReg;
			var s = "" //
					+ (0 <= baseReg ? " + " + dump(regs[baseReg]) : "") //
					+ (0 <= indexReg ? " + " + dump(regs[indexReg]) + " * " + (1 << opMem.scale) : "") //
					+ (0 < opMem.dispSize ? dumpDisp(opMem.disp, pointerSize) : "");
			return "[" + s.substring(3) + "]";
		} else if ((name = (amd64.registerByName.inverse().get(op0).name)) != null)
			return name;
		else
			return op0.toString();
	}

	private String dumpDisp(long disp, int pointerSize) {
		if (0 < disp)
			return " + " + dump(disp, pointerSize);
		else if (disp == 0)
			return "";
		else
			return " - " + dump(-disp, pointerSize);
	}

	private String dump(long imm, int size) {
		var s = "";
		for (var i = 0; i < size * 2; i++) {
			s = "0123456789ABCDEF".charAt((int) (imm & 15)) + s;
			imm >>= 4;
		}
		return s;
	}

}
