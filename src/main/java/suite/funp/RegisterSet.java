package suite.funp;

import java.util.Map;

import suite.assembler.Amd64;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.primitive.streamlet.IntStreamlet;
import suite.streamlet.Read;

public class RegisterSet {

	private static int nRegisters = 8;
	private static OpReg[] registers;

	private int flag;

	static {
		Amd64 amd64 = Amd64.me;

		Map<Integer, OpReg> map = Read //
				.from2(amd64.regsByName) //
				.values() //
				.filter(opReg -> opReg.size == Funp_.integerSize) //
				.map2(opReg -> opReg.reg, opReg -> opReg) //
				.toMap();

		registers = IntStreamlet //
				.range(nRegisters) //
				.map(map::get) //
				.toArray(OpReg.class);
	}

	public RegisterSet() {
		this(0);
	}

	public RegisterSet(int flag) {
		this.flag = flag;
	}

	public OpReg get() {
		for (int i = 0; i < nRegisters; i++)
			if (!isSet(i))
				return registers[i];
		throw new RuntimeException();
	}

	public boolean isSet(OpReg op) {
		return isSet(op.reg);
	}

	public OpReg[] list() {
		return IntStreamlet.range(nRegisters) //
				.filter(this::isSet) //
				.map(r -> registers[r]) //
				.toArray(OpReg.class);
	}

	public RegisterSet mask(Operand... operands) {
		int flag_ = flag;
		for (Operand operand : operands)
			if (operand instanceof OpReg)
				flag_ = flag_ | 1 << ((OpReg) operand).reg;
			else if (operand instanceof OpMem) {
				OpMem operand1 = (OpMem) operand;
				if (0 <= operand1.baseReg)
					flag_ |= 1 << operand1.baseReg;
				if (0 <= operand1.indexReg)
					flag_ |= 1 << operand1.indexReg;
			}
		return new RegisterSet(flag_);
	}

	private boolean isSet(int reg) {
		return (flag & 1 << reg) != 0;
	}

}
