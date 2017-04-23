package suite.math;

public class Cholesky {

	private Matrix mtx = new Matrix();

	/**
	 * Performs Cholesky decomposition. Input m must be symmetric and positive
	 * definite. Also its contents will be destroyed as the decomposition is
	 * performed in-place.
	 */
	public float[][] decompose(float[][] m) {
		int length = mtx.height(m);
		float[][] inverse = mtx.identity(length);
		if (length == mtx.width(m))
			for (int c = 0; c < length; c++) {
				float mii = m[c][c];
				double mii_sqrt = Math.sqrt(mii);
				float imii = 1f / mii;
				double imii_sqrt = 1f / mii_sqrt;

				for (int i = 0; i < length; i++) {
					double sum = inverse[i][c] * mii_sqrt;
					for (int j = c + 1; j < length; j++)
						sum += inverse[i][j] * imii_sqrt * m[j][c];
					inverse[i][c] = (float) sum;
				}

				for (int i = c + 1; i < length; i++)
					for (int j = c + 1; j < length; j++)
						m[i][j] -= imii * m[i][c] * m[j][c];
			}
		else
			throw new RuntimeException("Wrong input sizes");
		return inverse;
	}

}
