package suite.assembler;

import suite.util.RunUtil;

public class Amd64Cfg {

	public static boolean isLongMode = RunUtil.isLinux64();
	public static Amd64Mode mode = isLongMode ? Amd64Mode.LONG64 : Amd64Mode.PROT32;
	public static int pointerSize = mode.addrSize;
	public static int pushSize = mode.pushSize;
	public static int nRegisters = mode.nRegisters;

}
