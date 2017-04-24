package suite.math;

import java.util.Random;

import org.junit.Test;

import suite.adt.Pair;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class CholeskyTest {

	private Matrix mtx = new Matrix();
	private Cholesky cholesky = new Cholesky();

	private float[][] m0 = { //
			{ 4f, 12f, -16f, }, //
			{ 12f, 37f, -43f, }, //
			{ -16f, -43f, 98f, }, //
	};

	@Test
	public void testInverseMul() {
		Random random = new Random();
		float[] fs = To.floatArray(3, i -> random.nextFloat());
		Fun<float[], float[]> invm0 = cholesky.inverseMul(mtx.of(m0));
		float[] actual0 = mtx.mul(m0, invm0.apply(fs));
		float[] actual1 = invm0.apply(mtx.mul(m0, fs));
		mtx.verifyEquals(fs, actual0, .01f);
		mtx.verifyEquals(fs, actual1, .01f);
	}

	@Test
	public void testLdlt() {
		float[][] expectL = { //
				{ 1f, 0f, 0f, }, //
				{ 3f, 1f, 0f, }, //
				{ -4f, 5f, 1f, }, //
		};

		float[] expectD = { 4f, 1f, 9f, };

		Pair<float[][], float[]> ldlt = cholesky.ldlt(mtx.of(m0));
		float[][] actualL = ldlt.t0;
		float[] actualD = ldlt.t1;
		mtx.verifyEquals(actualL, expectL);
		mtx.verifyEquals(actualD, expectD);

		float[][] matrixD = To.floatArray(actualD.length, actualD.length, (i, j) -> i == j ? actualD[i] : 0f);
		float[][] m1 = mtx.mul(mtx.mul(actualL, matrixD), mtx.transpose(actualL));
		mtx.verifyEquals(m0, m1);
	}

	@Test
	public void testLlt() {
		float[][] expect = { //
				{ 2f, 0f, 0f, }, //
				{ 6f, 1f, 0f, }, //
				{ -8f, 5f, 3f, }, //
		};

		float[][] actual = cholesky.decompose(mtx.of(m0));
		mtx.verifyEquals(actual, expect);

		float[][] conjugate = mtx.transpose(actual);
		float[][] m1 = mtx.mul(actual, conjugate);
		mtx.verifyEquals(m0, m1);
	}

}
