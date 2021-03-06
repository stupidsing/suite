package suite.assembler;

import static primal.statics.Fail.fail;

public class Amd64Cfg {

	public boolean isLongMode;
	public Amd64Mode mode;
	public int pointerSize;
	public int pushSize;
	public int nRegisters;

	public Amd64Cfg(boolean isLongMode) {
		mode = isLongMode ? Amd64Mode.LONG64 : Amd64Mode.PROT32;
		pointerSize = mode.addrSize;
		pushSize = mode.pushSize;
		nRegisters = mode.nRegisters;
	}

	public Amd64Cfg(Amd64Cfg fc) {
		for (var f : Amd64Cfg.class.getFields())
			try {
				f.set(this, f.get(fc));
			} catch (Exception ex) {
				fail(ex);
			}
	}

}
