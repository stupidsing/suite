package suite.lp.doer;

import org.junit.jupiter.api.Test;
import suite.Suite;
import suite.lp.compile.impl.CompileBinderImpl;
import suite.lp.doer.BinderFactory.BindEnv;
import suite.lp.sewing.impl.SewingBinderImpl;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
		for (var bf : new BinderFactory[] { new CompileBinderImpl(), new SewingBinderImpl(), }) {
			var node = new Generalizer().generalize(Suite.parse(pattern));
			var p = bf.binder(node);
			var be = new BindEnv(bf.mapper().env());

			assertTrue(p.test(be, Suite.parse(match)));
		}
	}

}
