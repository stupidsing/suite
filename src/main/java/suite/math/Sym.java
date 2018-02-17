package suite.math;

import suite.BindArrayUtil.Pattern;
import suite.Suite;

public class Sym {

	public static Sym me = new Sym();

	private Sym() {
	}

	public Pattern patAdd = Suite.pattern(".0 + .1");
	public Pattern patNeg = Suite.pattern("neg .0");
	public Pattern patMul = Suite.pattern(".0 * .1");
	public Pattern patInv = Suite.pattern("inv .0");
	public Pattern patPow = Suite.pattern(".0^.1");
	public Pattern patExp = Suite.pattern("exp .0");
	public Pattern patLn = Suite.pattern("ln .0");
	public Pattern patSin = Suite.pattern("sin .0");
	public Pattern patCos = Suite.pattern("cos .0");

}
