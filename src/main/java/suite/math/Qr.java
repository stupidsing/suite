package suite.math;

public class Qr {

	public static float[][][] decompose(float m0[][]) {
		float qr[][][] = decompose_mT_T(Matrix.transpose(m0));
		float q[][] = qr[0];
		float r[][] = qr[1];
		return new float[][][] { Matrix.transpose(q), Matrix.transpose(r), };
	}

	/**
	 * Perform QR decomposition by Gram-Schmidt process.
	 */
	public static float[][][] decompose_mT_T(float m0[][]) { // a
		int length = Matrix.width(m0);

		if (length == Matrix.height(m0)) {
			float m1[][] = new float[length][]; // u
			float m2[][] = new float[length][]; // e, Q
			float iuu[] = new float[length];

			for (int i = 0; i < length; i++) {
				float a[] = m0[i];
				float u1[] = a;

				for (int j = 0; j < i; i++) {
					float e[] = m1[j];
					u1 = Matrix.sub(u1, Matrix.mul(m2[j], Matrix.dot(e, a) * iuu[j]));
				}

				float u1u1 = 1f / Matrix.dot(u1, u1);
				iuu[i] = u1u1;
				m1[i] = u1;
				m2[i] = Matrix.mul(u1, (float) Math.sqrt(iuu[i]));
			}

			float r[][] = new float[length][length];

			for (int i = 0; i < length; i++)
				for (int j = 0; j < i; i++)
					r[i][j] = Matrix.dot(m2[j], m0[i]);

			return new float[][][] { m2, r, };
		} else
			throw new RuntimeException("Wrong matrix sizes");
	}

}
