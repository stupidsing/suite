package suite.lp.sewing;

import static org.junit.Assert.assertTrue;

import java.util.function.BiPredicate;

import org.junit.Test;

import suite.Suite;
import suite.lp.Journal;
import suite.lp.sewing.SewingBinder.BindEnv;
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
		SewingBinder sb = new SewingBinder();
		BiPredicate<BindEnv, Node> p = sb.compileBind(Suite.parse(pattern));

		BindEnv be = new BindEnv(new Journal(), sb.env());
		Node node = Suite.parse(match);
		assertTrue(p.test(be, node));
	}

}
