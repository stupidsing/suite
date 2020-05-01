package suite.math.numeric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogisticRegressionTest {

	private LogisticRegression lr = new LogisticRegression();

	@Test
	public void test() {
		var xs = new float[][] { //
				{ 2f, 1f, }, //
				{ 4f, 1.5f, }, //
				{ 3f, 1f, }, //
				{ 3.5f, .5f, }, //
				{ 2f, .5f, }, //
				{ 5.5f, 1f, }, //
				{ 1f, 1f, }, };

		var ys = new float[] { 0f, 1f, 0f, 1f, 0f, 1f, 0f, };

		var f = lr.train(xs, ys);

		for (var i = 0; i < xs.length; i++) {
			var actual = ys[i] < .5d;
			var expected = f.apply(xs[i]) < .5d;
			assertTrue(expected == actual);
		}
	}

}
