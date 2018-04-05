package suite.lp.doer;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.lp.Trail;
import suite.lp.compile.impl.CompileClonerImpl;
import suite.lp.doer.ClonerFactory.Clone_;
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
			Clone_ p = cf.cloner(node);

			assertTrue(Binder.bind(p.apply(cf.mapper().env()), Suite.parse(match), new Trail()));
		}
	}

}
