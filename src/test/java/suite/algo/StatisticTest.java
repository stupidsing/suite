package suite.algo;

import java.util.Random;

import org.junit.Test;

import suite.math.Matrix;
import suite.util.To;

public class StatisticTest {

	private Matrix mtx = new Matrix();

	@Test
	public void testLinearRegression() {
		int m = 7, n = 9;
		Random random = new Random();
		float[] expect = To.floatArray(m, j -> random.nextFloat());
		float[][] xs = To.floatArray(n, m, (i, j) -> random.nextFloat());
		float[] ys = To.floatArray(n, i -> (float) (mtx.dot(expect, xs[i]) + random.nextGaussian() * .01f));
		float[] actual = new Statistic().linearRegression(xs, ys);
		mtx.verifyEquals(expect, actual, .1f);
	}

}
