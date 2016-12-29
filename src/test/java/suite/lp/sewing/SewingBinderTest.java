package suite.lp.sewing;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.Suite;
import suite.lp.Trail;
import suite.lp.doer.Generalizer;
import suite.lp.sewing.SewingBinder.BindEnv;
import suite.lp.sewing.SewingBinder.BindPredicate;
import suite.lp.sewing.VariableMapper.Env;
import suite.lp.sewing.impl.SewingBinderImpl;
import suite.node.Node;

public class SewingBinderTest {

	@Test
	public void test0() {
		test("mem ((.e, _), .e)", "mem ((a, ), a)");
	}

	@Test
	public void test1() {
		test(".e .e", "a a");
	}

	private void test(String pattern, String match) {
		Node node = new Generalizer().generalize(Suite.parse(match));
		SewingBinder sb = new SewingBinderImpl();
		BindPredicate p = sb.compileBind(node);
		Env env = sb.env();
		Trail trail = new Trail();

		BindEnv be = new BindEnv() {
			public Env getEnv() {
				return env;
			}

			public Trail getTrail() {
				return trail;
			}
		};

		assertTrue(p.test(be, Suite.parse(match)));
	}

}
