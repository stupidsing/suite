package suite.lp.doer;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import suite.lp.Suite;
import suite.lp.kb.RuleSet;

public class RbTreeTest {

	@Test
	public void test() throws IOException {
		RuleSet rs = Suite.createRuleSet(Arrays.asList("auto.sl", "rbt.sl"));

		assertTrue(Suite.proveLogic(rs, "" //
				+ "rbt-add-list (6, 7, 8, 9, 10, 1, 2, 3, 4, 5,) ()/.t \n" //
				+ ", rbt-get .t 8" //
				+ ", rbt-member .t 4"));
	}

}
