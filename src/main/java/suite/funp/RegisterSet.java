package suite.funp;

import java.util.Map;

import suite.assembler.Amd64;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;
import suite.primitive.IntPrimitives.IntPredicate;
import suite.primitive.Ints_;
import suite.streamlet.Read;

public class RegisterSet {

	private static int nRegisters = 8;
	private static OpReg[] registers;

	public final int flag;

	static {
		Amd64 amd64 = Amd64.me;

		Map<Integer, OpReg> map = Read //
				.from2(amd64.regsByName) //
				.values() //
				.filter(opReg -> opReg.size == Funp_.integerSize) //
				.map2(opReg -> opReg.reg, opReg -> opReg) //
				.toMap();

		registers = Ints_.range(nRegisters).map(map::get).toArray(OpReg.class);
	}

	public RegisterSet() {
		this(0);
	}

	public RegisterSet(int flag) {
		this.flag = flag;
	}

	public boolean contains(Operand... operands) {
		return (flag & flag(operands)) != 0;
	}

	public OpReg get(OpReg prefer) {
		return prefer != null && !isSet(prefer.reg) ? prefer : get_();
	}

	public OpReg get() {
		return get_();
	}

	public OpReg[] list(IntPredicate pred) {
		return Ints_ //
				.range(nRegisters) //
				.filter(this::isSet) //
				.filter(pred) //
				.map(r -> registers[r]) //
				.toArray(OpReg.class);
	}

	public RegisterSet mask(Operand... operands) {
		return new RegisterSet(flag | flag(operands));
	}

	public RegisterSet unmask(int i) {
		return new RegisterSet(flag & ~flag(i));
	}

	private int flag(Operand... operands) {
		int flag_ = 0;
		for (Operand operand : operands)
			if (operand instanceof OpReg)
				flag_ |= flag(((OpReg) operand).reg);
			else if (operand instanceof OpMem) {
				OpMem operand1 = (OpMem) operand;
				if (0 <= operand1.baseReg)
					flag_ |= flag(operand1.baseReg);
				if (0 <= operand1.indexReg)
					flag_ |= flag(operand1.indexReg);
			}
		return flag_;
	}

	private OpReg get_() {
		for (int i = 0; i < nRegisters; i++)
			if (!isSet(i))
				return registers[i];
		throw new RuntimeException();
	}

	private boolean isSet(int reg) {
		return (flag & flag(reg)) != 0;
	}

	private int flag(int reg) {
		return 1 << reg;
	}

}
