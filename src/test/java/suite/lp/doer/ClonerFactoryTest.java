package suite.lp.doer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.lp.compile.impl.CompileClonerImpl;
import suite.lp.sewing.impl.SewingClonerImpl;

public class ClonerFactoryTest {

	@Test
	public void test0() {
		test("mem ((.e, _), .e)", "mem ((a, ), a)");
	}

	@Test
	public void test1() {
		test(".e .e", "a a");
	}

	private void test(String pattern, String match) {
		for (var cf : new ClonerFactory[] { new CompileClonerImpl(), new SewingClonerImpl(), }) {
			var node = new Generalizer().generalize(Suite.parse(pattern));
			var p = cf.cloner(node);

			assertTrue(Binder.bind(p.apply(cf.mapper().env()), Suite.parse(match)));
		}
	}

}
