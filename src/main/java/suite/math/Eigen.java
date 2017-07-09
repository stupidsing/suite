package suite.math;

import java.util.Random;

import suite.adt.pair.Pair;
import suite.math.linalg.Matrix;
import suite.streamlet.Read;
import suite.util.To;

public class Eigen {

	private Matrix mtx = new Matrix();
	private Random random = new Random();

	public float[][] power(float[][] m0) {
		float[][] m = mtx.of(m0);
		for (int i = 0; i < 20; i++) {
			m = Read.from(m).map(mtx::normalize).toArray(float[].class);
			m = mtx.mul(m, m);
			m = mtx.mul(m, m0);
		}
		return m;
	}

	// https://en.wikipedia.org/wiki/Lanczos_algorithm
	// returns V and T, where m ~= V T V*
	public Pair<float[][], float[][]> lanczos(float[][] m) {
		int n = mtx.height(m);
		int nIterations = 20; // n
		float[] alphas = new float[nIterations];
		float[] betas = new float[nIterations];
		float[][] vs = new float[nIterations][];
		float[][] ws = new float[nIterations][];

		if (n == mtx.width(m)) {
			float[] vj1 = null;

			for (int j = 1; j < nIterations; j++) {
				float beta = 0f;
				float[] prevw;
				float[] vj;

				if (0 < j && (beta = mtx.dot(prevw = ws[j - 1])) != 0d)
					vj = mtx.scale(prevw, 1d / (betas[j] = beta));
				else
					vj = mtx.normalize(To.arrayOfFloats(n, i -> random.nextFloat()));

				float[] wp = mtx.mul(m, vs[j] = vj);
				float[] sub0 = mtx.scale(vj, alphas[0] = mtx.dot(wp, vj));
				float[] sub1 = 0 < j ? mtx.add(sub0, mtx.scale(vj1, beta)) : sub0;

				vj1 = vj;
				ws[j] = mtx.sub(wp, sub1);
			}

			float[][] t = new float[nIterations][nIterations];

			for (int i = 0; i < nIterations; i++)
				t[i][i] = alphas[i];
			for (int i = 1; i < nIterations; i++)
				t[i - 1][i] = t[i][i - 1] = betas[i];

			return Pair.of(mtx.transpose(vs), t);
		} else
			throw new RuntimeException("wrong input sizes");
	}

	public float[] values(float[][] m, float[][] vs) {
		return To.arrayOfFloats(vs.length, i -> {
			float[] v = vs[i];
			return (float) (mtx.abs(mtx.mul(m, v)) / mtx.abs(v));
		});
	}

}
