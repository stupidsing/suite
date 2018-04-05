package suite.lp.doer;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.lp.Configuration.ProverConfig;
import suite.lp.compile.impl.CompileProverImpl;
import suite.lp.doer.ProverFactory.Prove_;
import suite.lp.sewing.impl.SewingProverImpl;

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
			Prove_ p = pf.prover(Suite.parse(query));

			assertEquals(result, p.test(new ProverConfig()));
		}
	}

}
