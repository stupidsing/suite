package suite.funp;

import primal.statics.Fail;
import suite.assembler.Amd64;
import suite.assembler.Amd64.OpReg;
import suite.assembler.Amd64Cfg;
import suite.assembler.Amd64Mode;
import suite.funp.Funp_.Funp;
import suite.funp.P2.FunpFramePointer;

public class FunpCfg {

	private Amd64Mode mode;
	public int booleanSize;
	public int integerSize;
	public int pointerSize = Amd64Cfg.pointerSize;
	public int pushSize = Amd64Cfg.pushSize;

	public Funp framePointer;

	public OpReg[] integerRegs;
	public OpReg[] pointerRegs;
	public OpReg[] pushRegs;
	public OpReg _bp;
	public OpReg _sp;

	public FunpCfg(Amd64 amd64, boolean isLongMode) {
		mode = isLongMode ? Amd64Mode.LONG64 : Amd64Mode.PROT32;
		booleanSize = 1;
		integerSize = mode.opSize;
		pointerSize = mode.addrSize;
		pushSize = mode.pushSize;

		framePointer = new FunpFramePointer();

		integerRegs = amd64.regs(integerSize);
		pointerRegs = amd64.regs(pointerSize);
		pushRegs = amd64.regs(pushSize);
		_bp = pointerRegs[amd64.bpReg];
		_sp = pointerRegs[amd64.spReg];
	}

	public FunpCfg(FunpCfg fc) {
		for (var f : FunpCfg.class.getFields())
			try {
				f.set(this, f.get(fc));
			} catch (Exception ex) {
				Fail.fail(ex);
			}
	}

}
