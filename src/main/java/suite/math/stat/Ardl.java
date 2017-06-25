package suite.math.stat;

import suite.math.linalg.Matrix;
import suite.math.stat.Statistic.LinearRegression;
import suite.streamlet.Read;
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

	public LinearRegression[] ardl(float[][] fsList, int maxLag) {
		int n = fsList.length;
		int length = fsList[0].length;

		LinearRegression[] lrs = Read.range(n) //
				.map(it -> {
					float[] fs = fsList[it];

					if (length == fs.length) {
						float[][] x = To.array(float[].class, length - maxLag, t -> {
							float[][] xl = new float[n][maxLag + 1];
							for (int is = 0; is < n; is++) {
								float[] fsi = fsList[is];
								float[] xs = xl[is];
								Copy.floats(fsi, t, xs, 0, maxLag);
								xs[maxLag] = is != it ? fsi[t + maxLag] : 1f;
							}
							return mtx.concat(xl);
						});

						return stat.linearRegression(x, fs);
					} else
						throw new RuntimeException("wrong input sizes");
				}) //
				.toArray(LinearRegression.class);

		return lrs;
	}

}
