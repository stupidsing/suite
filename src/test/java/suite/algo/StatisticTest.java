package suite.algo;

import java.util.Random;

import org.junit.Test;

import suite.algo.Statistic.LinearRegression;
import suite.math.Matrix;
import suite.util.To;
import suite.util.Util;

public class StatisticTest {

	private Matrix mtx = new Matrix();
	private Statistic statistic = new Statistic();

	@Test
	public void testLinearRegression() {
		int m = 7, n = 9;
		Random random = new Random();
		float[] expect = To.floatArray(m, j -> random.nextFloat());
		float[][] xs = To.floatArray(n, m, (i, j) -> random.nextFloat());
		float[] ys = To.floatArray(n, i -> (float) (mtx.dot(expect, xs[i]) + random.nextGaussian() * .01f));
		LinearRegression lr = statistic.linearRegression(xs, ys);
		Util.dump(lr);
		float[] actual = lr.betas;
		mtx.verifyEquals(expect, actual, .1f);
	}

}
