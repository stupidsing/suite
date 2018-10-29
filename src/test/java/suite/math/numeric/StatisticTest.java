package suite.math.numeric;

import java.util.Random;

import org.junit.Test;

import suite.inspect.Dump;
import suite.math.Math_;
import suite.math.linalg.Vector;
import suite.primitive.adt.pair.FltObjPair;
import suite.streamlet.Read;
import suite.util.To;

public class StatisticTest {

	private Statistic stat = new Statistic();
	private Random random = new Random();
	private Vector vec = new Vector();

	@Test
	public void testCovariance() {
		float[] fs = { -.5f, 1f, };
		Math_.verifyEquals(.5625f, (float) stat.covariance(fs, fs), 0f);
	}

	@Test
	public void testLinearRegression() {
		int m = 7, n = 9;
		var expect = To.vector(m, j -> random.nextFloat());
		var xs = To.matrix(n, m, (i, j) -> random.nextFloat());

		var lr = stat.linearRegression(Read //
				.from(xs) //
				.map(x -> FltObjPair.of((float) (vec.dot(expect, x) + random.nextGaussian() * .01f), x)));

		Dump.details(lr);

		var actual = lr.coefficients();
		vec.verifyEquals(expect, actual, .1f);

		var xtest = To.vector(m, j -> random.nextFloat());
		Math_.verifyEquals(vec.dot(expect, xtest), lr.predict(xtest), .1f);
		Math_.verifyEquals(1f, (float) lr.r2, .1f);
	}

}
