package suite.math.stat;

import java.util.List;
import java.util.Random;

import org.junit.Test;

import suite.inspect.Dump;
import suite.math.MathUtil;
import suite.math.linalg.Matrix_;
import suite.math.linalg.Vector_;
import suite.math.stat.Statistic.LinearRegression;
import suite.primitive.Floats_;
import suite.primitive.adt.pair.FltObjPair;
import suite.streamlet.Read;
import suite.util.To;

public class StatisticTest {

	private Statistic stat = new Statistic();
	private Vector_ vec = new Vector_();

	@Test
	public void testCovariance() {
		float[] fs = { -.5f, 1f, };
		MathUtil.verifyEquals(.5625f, (float) stat.covariance(fs, fs), 0f);
	}

	@Test
	public void testLinearRegression0() {
		Matrix_ mtx = new Matrix_();

		float[][] x = new float[][] { //
				new float[] { 1f, 2.25f, 0f, }, //
				new float[] { 0f, 1f, 2f, }, };

		float[][] xt = mtx.transpose(x);
		float[][] xtx = mtx.mul(xt, x);
		Dump.out(xtx);
		Dump.out(mtx.inverse(xtx));

		LinearRegression lr = stat.linearRegression(List.of( //
				FltObjPair.of(1f, new float[] { .5f, 1f, 0f, }), //
				FltObjPair.of(1f, new float[] { 0f, .5f, 1f, })));

		System.out.println(lr.toString());
	}

	@Test
	public void testLinearRegression1() {
		int m = 7, n = 9;
		Random random = new Random();
		float[] expect = Floats_.toArray(m, j -> random.nextFloat());
		float[][] xs = To.matrix(n, m, (i, j) -> random.nextFloat());
		LinearRegression lr = stat.linearRegression(Read //
				.from(xs) //
				.map(x -> FltObjPair.of( //
						(float) (vec.dot(expect, x) + random.nextGaussian() * .01f), //
						Floats_.toArray(m, j -> random.nextFloat()))) //
				.toList());
		Dump.out(lr);
		float[] actual = lr.coefficients;
		vec.verifyEquals(expect, actual, .1f);

		float[] xtest = Floats_.toArray(m, j -> random.nextFloat());
		MathUtil.verifyEquals(vec.dot(expect, xtest), lr.predict(xtest), .1f);
		MathUtil.verifyEquals(1f, (float) lr.r2, .1f);
	}

}
