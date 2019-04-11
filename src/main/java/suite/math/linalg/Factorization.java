package suite.math.linalg;

import java.util.Random;

import suite.adt.pair.Pair;
import suite.util.To;

public class Factorization {

	private Matrix mtx = new Matrix();
	private Random random = new Random();

	public Pair<float[][], float[][]> factor(float[][] m, int w) {
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

	// https://towardsdatascience.com/prototyping-a-recommender-system-step-by-step-part-2-alternating-least-square-als-matrix-4a76c58714a1
	// SGD Algorithm for MF
	public Pair<float[][], float[][]> sgd(float[][] v, int k) {
		var eps = .25d;
		var lambda = .01d;
		var inv = 1d / Math.sqrt(k);
		var height = mtx.height(v);
		var width = mtx.width(v);
		var w = To.matrix(height, k, (i, j) -> random.nextFloat() * inv);
		var h = To.matrix(k, width, (i, j) -> random.nextFloat() * inv);

		for (var iter = 0; iter < 999999; iter++) {
			var i = random.nextInt(height);
			var j = random.nextInt(width);
			var dot = 0d;

			for (var s = 0; s < k; s++)
				dot += w[i][s] * h[s][j];

			var error = dot - v[i][j];

			for (var s = 0; s < k; s++)
				w[i][s] -= eps * (error * h[s][j] + lambda * w[i][s]);

			for (var s = 0; s < k; s++)
				h[s][j] -= eps * (error * w[i][s] + lambda * h[s][j]);
		}

		return Pair.of(w, h);
	}

}
