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
			throw new RuntimeException("Wrong input sizes");
	}

	public static float[][][] decomposeByGivensRotation(float[][] m) {
		float[][] r = mtx.of(m);
		int height = mtx.height(r);
		int width = mtx.width(r);
		float[][] q = mtx.identity(height);

		if (height == width) {
			for (int k = 0; k < width; k++)
				for (int i = height - 1; k < i; i--) {
					mtx.verifyEquals(m, mtx.mul(q, r));

					int i0 = i - 1;
					int i1 = i - 0;
					float f0 = r[i0][k];
					float f1 = r[i1][k];

					if (f1 != 0f) {
						double radius = Math.sqrt(f0 * f0 + f1 * f1);
						double ir = 1d / radius;
						double cos = f0 * ir, sin = f1 * ir;

						for (int j = 0; j < height; j++) {
							float m0 = r[i0][j];
							float m1 = r[i1][j];
							r[i0][j] = (float) (m0 * cos + m1 * sin);
							r[i1][j] = (float) (m1 * cos - m0 * sin);

							float q0 = q[j][i0];
							float q1 = q[j][i1];
							q[j][i0] = (float) (q0 * cos + q1 * sin);
							q[j][i1] = (float) (q1 * cos - q0 * sin);
						}
					}
				}

			return new float[][][] { q, r, };
		} else
			throw new RuntimeException("Wrong input sizes");
	}

}
