package suite.lp.doer;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.lp.compile.impl.CompileBinderImpl;
import suite.lp.doer.BinderFactory.BindEnv;
import suite.lp.doer.BinderFactory.Bind_;
import suite.lp.sewing.impl.SewingBinderImpl;
import suite.node.Node;

public class BinderFactoryTest {

	@Test
	public void test0() {
		test("mem ((.e, _), .e)", "mem ((a, ), a)");
	}

	@Test
	public void test1() {
		test(".e .e", "a a");
	}

	private void test(String pattern, String match) {
		for (BinderFactory bf : new BinderFactory[] { new CompileBinderImpl(), new SewingBinderImpl(), }) {
			Node node = new Generalizer().generalize(Suite.parse(pattern));
			Bind_ p = bf.binder(node);
			BindEnv be = new BindEnv(bf.env());

			assertTrue(p.test(be, Suite.parse(match)));
		}
	}

}
