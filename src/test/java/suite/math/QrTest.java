package suite.math;

import org.junit.Test;

public class QrTest {

	private static Matrix mtx = new Matrix();

	@Test
	public void testGramSchmidt() {
		float[][] m0 = new float[][] { //
				{ 12f, -51f, 4f, }, //
				{ 6f, 167f, -68f, }, //
				{ -4f, 24f, -41f, }, //
		};

		float[][][] qr = Qr.decompose(m0);
		float[][] q = qr[0];
		float[][] r = qr[1];
		float[][] m1 = mtx.mul(q, r);

		float[][] expectedq = new float[][] { //
				{ 6f / 7f, -69f / 175f, -58f / 175f, }, //
				{ 3f / 7f, 158f / 175f, 6f / 175f, }, //
				{ -2f / 7f, 6f / 35f, -33f / 35f, }, //
		};

		float[][] expectedr = new float[][] { //
				{ 14f, 21f, -14f, }, //
				{ 0f, 175f, -70f, }, //
				{ 0f, 0f, 35f, }, //
		};

		mtx.verifyEquals(m0, m1);
		mtx.verifyEquals(q, expectedq);
		mtx.verifyEquals(r, expectedr);
	}

}
