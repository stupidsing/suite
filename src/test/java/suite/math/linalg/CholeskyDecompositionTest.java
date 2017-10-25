package suite.math.linalg;

import java.util.Random;

import org.junit.Test;

import suite.adt.pair.Pair;
import suite.primitive.Floats_;
import suite.util.FunUtil.Iterate;
import suite.util.To;

public class CholeskyDecompositionTest {

	private Matrix_ mtx = new Matrix_();
	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Vector_ vec = new Vector_();

	private float[][] m0 = { //
			{ 4f, 12f, -16f, }, //
			{ 12f, 37f, -43f, }, //
			{ -16f, -43f, 98f, }, //
	};

	@Test
	public void testInverseMul() {
		Random random = new Random();
		float[] fs = Floats_.toArray(3, i -> random.nextFloat());
		Iterate<float[]> invm0 = cholesky.inverseMul(mtx.of(m0));
		float[] actual0 = mtx.mul(m0, invm0.apply(fs));
		float[] actual1 = invm0.apply(mtx.mul(m0, fs));
		vec.verifyEquals(fs, actual0, .01f);
		vec.verifyEquals(fs, actual1, .01f);
	}

	@Test
	public void testLdlt() {
		float[][] expectl = { //
				{ 1f, 0f, 0f, }, //
				{ 3f, 1f, 0f, }, //
				{ -4f, 5f, 1f, }, //
		};

		float[] expectd = { 4f, 1f, 9f, };

		Pair<float[][], float[]> ldlt = cholesky.ldlt(mtx.of(m0));
		float[][] actuall = ldlt.t0;
		float[] actuald = ldlt.t1;
		mtx.verifyEquals(actuall, expectl);
		vec.verifyEquals(actuald, expectd);

		float[][] matrixd = To.arrayOfFloats(actuald.length, actuald.length, (i, j) -> i == j ? actuald[i] : 0f);
		float[][] m1 = mtx.mul(mtx.mul(actuall, matrixd), mtx.transpose(actuall));
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
