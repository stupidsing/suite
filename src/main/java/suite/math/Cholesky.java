package suite.math;

import suite.adt.Pair;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class Cholesky {

	private Matrix mtx = new Matrix();

	/**
	 * @param m
	 *            a Hermitian (i.e. symmetric), positive-definite matrix.
	 * @return a function that calculates x -> m^-1 * x.
	 */
	public Fun<float[], float[]> inverseMul(float[][] m) {
		Pair<float[][], float[]> ldlt = ldlt(m);
		float[][] l = ldlt.t0;
		float[] d = ldlt.t1;
		float[] reciprocalsD = To.arrayOfFloats(d, f -> 1f / f);
		return fs0 -> {
			int height = mtx.height(m);
			int width = mtx.width(m);
			float[] fs1 = new float[height]; // will be inverse(L) * fs0

			for (int i = 0; i < height; i++) {
				float sum = fs0[i];
				for (int j = 0; j < i; j++)
					sum -= l[i][j] * fs1[j];
				fs1[i] = sum;
			}

			// will be inverse(D) * fs1
			float[] fs2 = To.arrayOfFloats(fs1.length, i -> fs1[i] * reciprocalsD[i]);
			float[] fs3 = new float[width]; // will be inverse(L*) * fs2

			for (int i = width - 1; 0 <= i; i--) {
				float sum = fs2[i];
				for (int j = height - 1; i < j; j--)
					sum -= l[j][i] * fs3[j];
				fs3[i] = sum;
			}

			return fs3;
		};
	}

	/**
	 * Performs Cholesky decomposition. Input m must be symmetric and positive
	 * definite. Also its contents will be destroyed as the decomposition is
	 * performed in-place.
	 * 
	 * @param m
	 *            a Hermitian (i.e. symmetric), positive-definite matrix.
	 * @return lower-triangular matrix L that satisfy m = L * L*
	 */
	public float[][] decompose(float[][] m) {
		int length = mtx.height(m);
		float[][] l = mtx.identity(length);
		if (length == mtx.width(m))
			for (int c = 0; c < length; c++) {
				float mii = m[c][c];
				double mii_sqrt = Math.sqrt(mii);
				float imii = 1f / mii;
				double imii_sqrt = 1f / mii_sqrt;

				for (int i = c; i < length; i++) {
					double sum = l[i][c] * mii_sqrt;
					for (int j = c + 1; j < length; j++)
						sum += imii_sqrt * l[i][j] * m[j][c];
					l[i][c] = (float) sum;
				}

				for (int i = c + 1; i < length; i++)
					for (int j = c + 1; j < length; j++)
						m[i][j] -= imii * m[i][c] * m[j][c];
			}
		else
			throw new RuntimeException("Wrong input sizes");
		return l;
	}

	/**
	 * Performs Cholesky decomposition. Input m must be symmetric and positive
	 * definite. Also its contents will be destroyed as the decomposition is
	 * performed in-place.
	 * 
	 * @param m
	 *            a Hermitian (i.e. symmetric), positive-definite matrix.
	 * @return A pair of lower-triangular matrix L and diagonal vector D that
	 *         satisfies m = L * D * L*.
	 */
	public Pair<float[][], float[]> ldlt(float[][] m) {
		int length = mtx.height(m);
		float[][] l = mtx.identity(length);
		float[] d = new float[length];
		if (length == mtx.width(m))
			for (int c = 0; c < length; c++) {
				float imii = 1f / (d[c] = m[c][c]);

				for (int i = c + 1; i < length; i++)
					for (int j = c + 1; j < length; j++) {
						float imii_mjc = imii * m[j][c];
						l[i][c] += imii_mjc * l[i][j];
						m[i][j] -= imii_mjc * m[i][c];
					}
			}
		else
			throw new RuntimeException("Wrong input sizes");
		return Pair.of(l, d);
	}

}
