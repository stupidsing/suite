package suite.funp;

import static primal.statics.Fail.fail;
import static suite.util.Streamlet_.forInt;

import primal.MoreVerbs.Read;
import primal.primitive.IntPrim.IntPred;
import suite.assembler.Amd64;
import suite.assembler.Amd64.OpMem;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64.Operand;

public class RegisterSet {

	private static Amd64 amd64 = Amd64.me;
	private static int nRegisters = Funp_.nRegisters;
	private static OpReg[] registers;

	public final int flag;

	static {
		var map = Read //
				.from2(amd64.regByName) //
				.values() //
				.filter(opReg -> opReg.size == Funp_.pushSize) //
				.map2(opReg -> opReg.reg, opReg -> opReg) //
				.toMap();

		registers = forInt(nRegisters).map(map::get).toArray(OpReg.class);
	}

	public RegisterSet() {
		this(0);
	}

	private RegisterSet(int flag) {
		this.flag = flag;
	}

	public OpReg get(Operand op) {
		var prefer = op.cast(OpReg.class);
		return prefer != null && !isSet(prefer.reg) ? prefer : get_(op.size, false);
	}

	public OpReg get(int size) {
		return get_(size, false);
	}

	public OpReg get(int size, boolean isAllowAllByteRegisters) {
		return get_(size, isAllowAllByteRegisters);
	}

	public boolean isAnyMasked(Operand... operands) {
		return (flag & flag(operands)) != 0;
	}

	public OpReg[] list(IntPred pred) {
		return forInt(nRegisters) //
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
		var flag_ = 0;
		for (var operand : operands)
			if (operand instanceof OpReg)
				flag_ |= flag(((OpReg) operand).reg);
			else if (operand instanceof OpMem) {
				var operand1 = (OpMem) operand;
				if (0 <= operand1.baseReg)
					flag_ |= flag(operand1.baseReg);
				if (0 <= operand1.indexReg)
					flag_ |= flag(operand1.indexReg);
			}
		return flag_;
	}

	private OpReg get_(int size, boolean isAllowAllByteRegisters) {
		var reg = get_();
		if (size == 1 && (isAllowAllByteRegisters || reg < 4)) // AL, BL, CL or DL
			return amd64.reg8[reg];
		else if (size == 2)
			return amd64.reg16[reg];
		else if (size == 4)
			return amd64.reg32[reg];
		else if (size == 8)
			return amd64.reg64[reg];
		else
			return fail("cannot allocate register with size " + size);
	}

	public boolean isSet(int reg) {
		return isSet_(reg);
	}

	private int get_() {
		for (var i = 0; i < nRegisters; i++)
			if (!isSet_(i))
				return i;
		return fail();
	}

	private boolean isSet_(int reg) {
		return (flag & flag(reg)) != 0;
	}

	private int flag(int reg) {
		return 1 << reg;
	}

}
