package suite.math;

public class Qr {

	private static Matrix mtx = new Matrix();

	public static float[][][] decompose(float[][] m0) {
		float[][][] qr = decompose_mT_T(mtx.transpose(m0));
		float[][] q = qr[0];
		float[][] r = qr[1];
		return new float[][][] { mtx.transpose(q), mtx.transpose(r), };
	}

	/**
	 * Perform QR decomposition by Gram-Schmidt process.
	 */
	public static float[][][] decompose_mT_T(float[][] m) { // a
		int length = mtx.width(m);

		if (length == mtx.height(m)) {
			float[][] q = new float[length][]; // e

			for (int i = 0; i < length; i++) {
				float[] a = m[i];
				float[] u1 = mtx.of(a);

				for (int j = 0; j < i; j++) {
					float[] u = q[j];
					mtx.addScaleOn(u1, u, -mtx.dot(u, a));
				}

				q[i] = mtx.scaleOn(u1, Math.sqrt(1f / mtx.dot(u1, u1)));
			}

			float[][] r = new float[length][length];

			for (int i = 0; i < length; i++)
				for (int j = 0; j <= i; j++)
					r[i][j] = mtx.dot(q[j], m[i]);

			return new float[][][] { q, r, };
		} else
			throw new RuntimeException("Wrong matrix sizes");
	}

}
