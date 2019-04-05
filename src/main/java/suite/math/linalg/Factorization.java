package suite.math.linalg;

import java.util.Random;

import suite.adt.pair.Pair;
import suite.util.To;

public class Factorization {

	private Matrix mtx = new Matrix();
	private Random random = new Random();

	public Pair<float[][], float[][]> factor(float[][] m, int w) { // ALS?
		var height = mtx.height(m);
		var width = mtx.width(m);
		var u = To.matrix(height, w, (i, j) -> random.nextFloat());
		var v = To.matrix(w, width, (i, j) -> random.nextFloat());

		for (var iter = 0; iter < 99; iter++) {
			// TODO check if error is small enough

			var u0 = u;
			var v0 = v;

			var error = mtx.sub(m, mtx.mul(u, v));
			var alpha = .1f;

			u = To.matrix(height, w, (i, q) -> {
				var sum = 0d;
				for (var j = 0; j < width; j++)
					sum += error[i][j] * v0[q][j];
				return (float) (u0[i][q] + alpha * sum);
			});

			v = To.matrix(w, width, (q, j) -> {
				var sum = 0d;
				for (var i = 0; i < height; i++)
					sum += error[i][j] * u0[i][q];
				return (float) (v0[q][j] + alpha * sum);
			});
		}

		return Pair.of(u, v);
	}

}
