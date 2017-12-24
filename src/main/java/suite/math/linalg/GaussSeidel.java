package suite.math.linalg;

public class GaussSeidel {

	private Matrix_ mtx = new Matrix_();

	// solve x in A*x = b
	public float[] solve(float[][] a, float[] b) {
		int size = mtx.sqSize(a);
		float[] phi = new float[size];
		float[] phi0 = Boolean.TRUE ? phi : new float[size]; // Gauss-Seidel, or Jacobi

		for (int iteration = 0; iteration < 16; iteration++) {
			for (int i = 0; i < size; i++) {
				float[] ai = a[i];
				double o = 0d;
				for (int j = 0; j < size; j++)
					if (i != j)
						o += ai[j] * phi0[j];
				phi[i] = (float) ((b[i] - o) / a[i][i]);
			}

			float[] t = phi;
			phi = phi0;
			phi0 = t;
		}

		return phi;
	}

}
