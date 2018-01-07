package suite.lp.doer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.Suite;
import suite.lp.compile.impl.CompileClonerImpl;
import suite.lp.compile.impl.CompileExpressionImpl;
import suite.lp.doer.EvaluatorFactory.Evaluate_;
import suite.lp.sewing.Env;
import suite.lp.sewing.impl.SewingClonerImpl;
import suite.lp.sewing.impl.SewingExpressionImpl;

public class EvaluatorFactoryTest {

	@Test
	public void test() {
		test("1 + 2 * 3", 7);
	}

	private void test(String pattern, int result) {
		for (EvaluatorFactory ef : new EvaluatorFactory[] { //
				new CompileExpressionImpl(new CompileClonerImpl()), //
				new SewingExpressionImpl(new SewingClonerImpl()), }) {
			Evaluate_ e = ef.evaluator(Suite.parse(pattern));

			assertEquals(result, e.evaluate(Env.empty(0)));
		}
	}

}
