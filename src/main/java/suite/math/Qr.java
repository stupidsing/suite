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

			for (int i = 0; i < length; i++) {
				float a[] = m0[i];
				float u1[] = a;

				for (int j = 0; j < i; j++)
					u1 = Matrix.sub(u1, Matrix.scale(m2[j], Matrix.dot(m2[j], a)));

				float u1u1 = 1f / Matrix.dot(u1, u1);
				m1[i] = u1;
				m2[i] = Matrix.scale(u1, Math.sqrt(u1u1));
			}

			float r[][] = new float[length][length];

			for (int i = 0; i < length; i++)
				for (int j = 0; j <= i; j++)
					r[i][j] = Matrix.dot(m2[j], m0[i]);

			return new float[][][] { m2, r, };
		} else
			throw new RuntimeException("Wrong matrix sizes");
	}

}
