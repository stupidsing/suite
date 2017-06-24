package suite.math.stat;

import suite.math.linalg.Matrix;
import suite.math.stat.Statistic.LinearRegression;
import suite.util.Copy;
import suite.util.To;

/**
 * Auto-regressive distributed lag.
 *
 * @author ywsing
 */
public class Ardl {

	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();

	public LinearRegression[] ardl(float[][] fsList, int lambda) {
		int n = fsList.length;
		int length = fsList[0].length;
		LinearRegression[] lrs = new LinearRegression[n];

		for (int it = 0; it < n; it++) // dependent time series
			if (length == fsList[it].length) {
				int it_ = it;

				float[][] x = To.array(float[].class, length - lambda, t -> {
					int tx = t + lambda;
					float[][] xl = new float[n + 1][lambda];
					float[] last = xl[n];
					for (int is = 0; is < n; is++) { // explanatory time series
						float[] fsi = fsList[is];
						Copy.floats(fsi, t, xl[is], 0, lambda);
						last[is] = fsi[tx];
					}
					last[it_] = 1f;
					return mtx.concat(xl);
				});

				lrs[it] = stat.linearRegression(x, fsList[0]);
			} else
				throw new RuntimeException("wrong input sizes");

		return lrs;
	}

}
