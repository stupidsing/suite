package suite.lp.sewing;

import static org.junit.Assert.assertTrue;

import java.util.function.BiPredicate;

import org.junit.Test;

import suite.Suite;
import suite.lp.Journal;
import suite.lp.doer.Generalizer;
import suite.lp.sewing.SewingBinder.BindEnv;
import suite.lp.sewing.impl.SewingBinderImpl;
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
		Node node = new Generalizer().generalize(Suite.parse(pattern));

		SewingBinder sb = new SewingBinderImpl();
		BiPredicate<BindEnv, Node> p = sb.compileBind(node);

		BindEnv be = new BindEnv(new Journal(), sb.env());
		assertTrue(p.test(be, Suite.parse(match)));
	}

}
