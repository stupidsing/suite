package suite.lp.doer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import suite.Suite;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.SewingProverBuilder2;

public class RbTreeTest {

	@Test
	public void test() throws IOException {
		var rs = Suite.newRuleSet(List.of("auto.sl", "rbt.sl"));
		var gs = "" //
				+ "rbt-insert-list (6, 7, 8, 9, 10, 1, 2, 3, 4, 5,) ()/.t \n" //
				+ ", rbt-get .t 8 \n" //
				+ ", rbt-member .t 4";

		for (var builder : List.of(new InterpretedProverBuilder(), new SewingProverBuilder2()))
			assertTrue(Suite.proveLogic(builder, rs, gs));
	}

}
