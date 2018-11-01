import org.junit.Test;

import suite.Suite;
import suite.math.sym.Symbolic;

public class MoliuTest {

	@Test
	public void test() {
		Symbolic sym = new Symbolic();
		// System.out.println(sym.simplify(Suite.parse("neg (neg 1)"),
		// Suite.parse("x")));
		System.out.println(sym.simplify(Suite.parse("neg (neg 1 * exp neg x)"), Suite.parse("x")));
		// System.out.println(
		// sym.simplify(Suite.parse("neg (neg 1 * exp neg x) * inv (1 + exp neg x) * inv
		// (1 + exp neg x)"), Suite.parse("x")));
	}

}
