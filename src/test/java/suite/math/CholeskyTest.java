package suite.math;

import org.junit.Test;

public class CholeskyTest {

	private static Matrix mtx = new Matrix();

	@Test
	public void test() {
		float[][] m0 = { //
				{ 4f, 12f, -16f, }, //
				{ 12f, 37f, -43f, }, //
				{ -16f, -43f, 98f, }, //
		};

		float[][] expect = { //
				{ 2f, 0f, 0f, }, //
				{ 6f, 1f, 0f, }, //
				{ -8f, 5f, 3f, }, //
		};

		float[][] actual = new Cholesky().decompose(mtx.of(m0));
		mtx.verifyEquals(actual, expect);

		float[][] conjugate = mtx.transpose(actual);
		float[][] m1 = mtx.mul(actual, conjugate);
		mtx.verifyEquals(m0, m1);
	}

}
