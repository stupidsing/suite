package suite.math.linalg;

import java.util.Random;

import suite.adt.pair.Pair;
import suite.util.To;

public class Factorization {

	private Matrix mtx = new Matrix();
	private Random random = new Random();

	public Pair<float[][], float[][]> factorize(float[][] m, int w) {
		var height = mtx.height(m);
		var width = mtx.width(m);
		float[][] u = To.matrix(height, w, (i, j) -> random.nextFloat());
		float[][] v = To.matrix(w, width, (i, j) -> random.nextFloat());

		for (int iter = 0; iter < 20; iter++) {
			// TODO check if error is small enough

			float[][] u0 = u;
			float[][] v0 = v;

			float[][] error = mtx.sub(m, mtx.mul(u, v));
			var alpha = .1f;

			float[][] u1 = To.matrix(height, w, (i, q) -> {
				var sum = 0d;
				for (int j = 0; j < width; j++)
					sum += error[i][j] * v0[q][j];
				return (float) (u0[i][q] + alpha * sum);
			});

			float[][] v1 = To.matrix(w, width, (q, j) -> {
				var sum = 0d;
				for (int i = 0; i < height; i++)
					sum += error[i][j] * u0[i][q];
				return (float) (v0[q][j] + alpha * sum);
			});

			u = u1;
			v = v1;
		}

		return Pair.of(u, v);
	}

}
