package suite.math.linalg;

import java.util.Random;

import org.junit.Test;

import suite.util.To;

public class CholeskyDecompositionTest {

	private Matrix mtx = new Matrix();
	private CholeskyDecomposition cholesky = new CholeskyDecomposition();
	private Vector vec = new Vector();

	private float[][] m0 = { //
			{ 4f, 12f, -16f, }, //
			{ 12f, 37f, -43f, }, //
			{ -16f, -43f, 98f, }, //
	};

	@Test
	public void testInverseMul() {
		var random = new Random();
		var fs = To.vector(3, i -> random.nextFloat());
		var invm0 = cholesky.inverseMul(mtx.copyOf(m0));
		var actual0 = mtx.mul(m0, invm0.apply(fs));
		var actual1 = invm0.apply(mtx.mul(m0, fs));
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

		var ldlt = cholesky.ldlt(mtx.copyOf(m0));
		var actuall = ldlt.t0;
		var actuald = ldlt.t1;
		mtx.verifyEquals(actuall, expectl);
		vec.verifyEquals(actuald, expectd);

		var matrixd = To.matrix(actuald.length, actuald.length, (i, j) -> i == j ? actuald[i] : 0f);
		var m1 = mtx.mul(actuall, matrixd, mtx.transpose(actuall));
		mtx.verifyEquals(m0, m1);
	}

	@Test
	public void testLlt() {
		float[][] expect = { //
				{ 2f, 0f, 0f, }, //
				{ 6f, 1f, 0f, }, //
				{ -8f, 5f, 3f, }, //
		};

		var actual = cholesky.decompose(mtx.copyOf(m0));
		mtx.verifyEquals(actual, expect);

		var conjugate = mtx.transpose(actual);
		var m1 = mtx.mul(actual, conjugate);
		mtx.verifyEquals(m0, m1);
	}

}
