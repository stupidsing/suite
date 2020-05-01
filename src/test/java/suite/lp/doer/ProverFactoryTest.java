package suite.lp.doer;

import org.junit.jupiter.api.Test;
import suite.Suite;
import suite.lp.Configuration.ProverCfg;
import suite.lp.compile.impl.CompileProverImpl;
import suite.lp.sewing.impl.SewingProverImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProverFactoryTest {

	@Test
	public void test() {
		test("yes, fail", false);
		test("yes, yes", true);
		test("fail; yes", true);
		test("yes; yes", true);
	}

	private void test(String query, boolean result) {
		for (var pf : new ProverFactory[] { //
				new CompileProverImpl(), //
				new SewingProverImpl(Suite.newRuleSet(List.of("auto.sl"))), }) {
			var p = pf.prover(Suite.parse(query));

			assertEquals(result, p.test(new ProverCfg()));
		}
	}

}
