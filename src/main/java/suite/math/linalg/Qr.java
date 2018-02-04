package suite.math.linalg;

import suite.adt.pair.Pair;

public class Qr {

	private Matrix_ mtx = new Matrix_();
	private Vector_ vec = new Vector_();

	public Pair<float[][], float[][]> decompose(float[][] m0) {
		Pair<float[][], float[][]> qr = decompose_mT_T(mtx.transpose(m0));
		return qr.map((q, r) -> Pair.of(mtx.transpose(q), mtx.transpose(r)));
	}

	/**
	 * Perform QR decomposition by Gram-Schmidt process.
	 */
	public Pair<float[][], float[][]> decompose_mT_T(float[][] m) { // a
		int size = mtx.sqSize(m);
		float[][] q = new float[size][]; // e

		for (int i = 0; i < size; i++) {
			float[] a = m[i];
			float[] u1 = vec.of(a);

			for (int j = 0; j < i; j++) {
				float[] u = q[j];
				vec.addScaleOn(u1, u, -vec.dot(u, a));
			}

			q[i] = vec.scaleOn(u1, Math.sqrt(1f / vec.dot(u1, u1)));
		}

		float[][] r = new float[size][size];

		for (int i = 0; i < size; i++)
			for (int j = 0; j <= i; j++)
				r[i][j] = vec.dot(q[j], m[i]);

		return Pair.of(q, r);
	}

	public Pair<float[][], float[][]> decomposeByGivensRotation(float[][] m) {
		float[][] r = mtx.of(m);
		int size = mtx.sqSize(r);
		float[][] q = mtx.identity(size);

		for (int k = 0; k < size; k++)
			for (int i = size - 1; k < i; i--) {
				mtx.verifyEquals(m, mtx.mul(q, r));

				int i0 = i - 1;
				int i1 = i - 0;
				float f0 = r[i0][k];
				float f1 = r[i1][k];

				if (f1 != 0f) {
					double radius = Math.sqrt(f0 * f0 + f1 * f1);
					double ir = 1d / radius;
					double cos = f0 * ir, sin = f1 * ir;

					for (int j = 0; j < size; j++) {
						double m0 = r[i0][j];
						double m1 = r[i1][j];
						r[i0][j] = (float) (m0 * cos + m1 * sin);
						r[i1][j] = (float) (m1 * cos - m0 * sin);

						double q0 = q[j][i0];
						double q1 = q[j][i1];
						q[j][i0] = (float) (q0 * cos + q1 * sin);
						q[j][i1] = (float) (q1 * cos - q0 * sin);
					}
				}
			}

		return Pair.of(q, r);
	}

}
