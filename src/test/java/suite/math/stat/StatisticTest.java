package suite.math.stat;

import java.util.Random;

import org.junit.Test;

import suite.inspect.Dump;
import suite.math.MathUtil;
import suite.math.linalg.Vector_;
import suite.math.stat.Statistic.LinearRegression;
import suite.primitive.Floats_;
import suite.primitive.adt.pair.FltObjPair;
import suite.streamlet.Read;
import suite.util.To;

public class StatisticTest {

	private Statistic stat = new Statistic();
	private Random random = new Random();
	private Vector_ vec = new Vector_();

	@Test
	public void testCovariance() {
		float[] fs = { -.5f, 1f, };
		MathUtil.verifyEquals(.5625f, (float) stat.covariance(fs, fs), 0f);
	}

	@Test
	public void testLinearRegression() {
		int m = 7, n = 9;
		float[] expect = Floats_.toArray(m, j -> random.nextFloat());
		float[][] xs = To.matrix(n, m, (i, j) -> random.nextFloat());

		LinearRegression lr = stat.linearRegression(Read //
				.from(xs) //
				.map(x -> FltObjPair.of((float) (vec.dot(expect, x) + random.nextGaussian() * .01f), x)));

		Dump.out(lr);

		float[] actual = lr.coefficients();
		vec.verifyEquals(expect, actual, .1f);

		float[] xtest = Floats_.toArray(m, j -> random.nextFloat());
		MathUtil.verifyEquals(vec.dot(expect, xtest), lr.predict(xtest), .1f);
		MathUtil.verifyEquals(1f, (float) lr.r2, .1f);
	}

}
