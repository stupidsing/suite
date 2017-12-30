package suite.lp.sewing;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.lp.Trail;
import suite.lp.compile.impl.CompileClonerImpl;
import suite.lp.doer.Binder;
import suite.lp.doer.ClonerFactory;
import suite.lp.doer.ClonerFactory.Clone_;
import suite.lp.doer.Generalizer;
import suite.lp.sewing.impl.SewingClonerImpl;
import suite.node.Node;

public class SewingClonerTest {

	@Test
	public void test0() {
		test("mem ((.e, _), .e)", "mem ((a, ), a)");
	}

	@Test
	public void test1() {
		test(".e .e", "a a");
	}

	private void test(String pattern, String match) {
		for (ClonerFactory cf : new ClonerFactory[] { new CompileClonerImpl(), new SewingClonerImpl(), }) {
			Node node = new Generalizer().generalize(Suite.parse(pattern));
			Clone_ p = cf.compile(node);

			assertTrue(Binder.bind(p.apply(cf.env()), Suite.parse(match), new Trail()));
		}
	}

}
