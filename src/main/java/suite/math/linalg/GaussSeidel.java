package suite.math.linalg;

public class GaussSeidel {

	private Matrix mtx = new Matrix();

	// solve x in A*x = b
	public float[] solve(float[][] a, float[] b) {
		var size = mtx.sqSize(a);
		var phi = new float[size];

		// Gauss-Seidel, or Jacobi
		var phi0 = Boolean.TRUE ? phi : new float[size];

		for (var iteration = 0; iteration < 16; iteration++) {
			for (var i = 0; i < size; i++) {
				var ai = a[i];
				var o = 0d;
				for (var j = 0; j < size; j++)
					if (i != j)
						o += ai[j] * phi0[j];
				phi[i] = (float) ((b[i] - o) / a[i][i]);
			}

			var t = phi;
			phi = phi0;
			phi0 = t;
		}

		return phi;
	}

}
