package suite.math;

public class Qr {

	public static float[][][] decompose(float[][] m0) {
		float[][][] qr = decompose_mT_T(Matrix.transpose(m0));
		float[][] q = qr[0];
		float[][] r = qr[1];
		return new float[][][] { Matrix.transpose(q), Matrix.transpose(r), };
	}

	/**
	 * Perform QR decomposition by Gram-Schmidt process.
	 */
	public static float[][][] decompose_mT_T(float[][] m) { // a
		int length = Matrix.width(m);

		if (length == Matrix.height(m)) {
			float[][] q = new float[length][]; // e

			for (int i = 0; i < length; i++) {
				float[] a = m[i];
				float[] u1 = Matrix.of(a);

				for (int j = 0; j < i; j++) {
					float[] u = q[j];
					Matrix.addScaleOn(u1, u, -Matrix.dot(u, a));
				}

				q[i] = Matrix.scaleOn(u1, Math.sqrt(1f / Matrix.dot(u1, u1)));
			}

			float[][] r = new float[length][length];

			for (int i = 0; i < length; i++)
				for (int j = 0; j <= i; j++)
					r[i][j] = Matrix.dot(q[j], m[i]);

			return new float[][][] { q, r, };
		} else
			throw new RuntimeException("Wrong matrix sizes");
	}

}
