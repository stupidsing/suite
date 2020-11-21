package suite.assembler;

import static primal.statics.Fail.fail;

import suite.util.RunUtil;

public class Amd64Cfg {

	public boolean isLongMode;
	public Amd64Mode mode;
	public int pointerSize;
	public int pushSize;
	public int nRegisters;

	public static Amd64Cfg me = new Amd64Cfg();

	public Amd64Cfg() {
		isLongMode = RunUtil.isLinux64();
		mode = isLongMode ? Amd64Mode.LONG64 : Amd64Mode.PROT32;
		pointerSize = mode.addrSize;
		pushSize = mode.pushSize;
		nRegisters = mode.nRegisters;
	}

	public Amd64Cfg(Amd64Cfg fc) {
		for (var f : getClass().getFields())
			try {
				f.set(this, f.get(fc));
			} catch (Exception ex) {
				fail(ex);
			}
	}

}
