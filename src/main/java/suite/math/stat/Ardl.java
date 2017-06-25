package suite.math.stat;

import suite.math.linalg.Matrix;
import suite.math.stat.Statistic.LinearRegression;
import suite.streamlet.IntStreamlet;
import suite.util.Copy;
import suite.util.To;

/**
 * Auto-regressive distributed lag.
 *
 * @author ywsing
 */
public class Ardl {

	private int maxLag;
	private boolean isIncludeCurrent;

	private Matrix mtx = new Matrix();
	private Statistic stat = new Statistic();

	public Ardl(int maxLag, boolean isIncludeCurrent) {
		this.maxLag = maxLag;
		this.isIncludeCurrent = isIncludeCurrent;
	}

	public LinearRegression[] ardl(float[][] fsList) {
		int n = fsList.length;
		int length = fsList[0].length;

		LinearRegression[] lrs = IntStreamlet //
				.range(n) //
				.map(it -> {
					float[] fs = fsList[it];

					if (length == fs.length) {
						float[][] x = To.array(float[].class, length - maxLag, t -> getExplanatoryVariables(fsList, it, t));
						return stat.linearRegression(x, fs);
					} else
						throw new RuntimeException("wrong input sizes");
				}) //
				.toArray(LinearRegression.class);

		return lrs;
	}

	public float[] predict(LinearRegression[] lrs, float[][] fsList, int index) {
		return To.arrayOfFloats(lrs.length, it -> lrs[it].predict(getExplanatoryVariables(fsList, it, index)));
	}

	private float[] getExplanatoryVariables(float[][] fsList, int it, int t) {
		return mtx.concat(IntStreamlet //
				.range(fsList.length) //
				.map(is -> {
					float[] fsi = fsList[is];
					float[] xs = new float[maxLag + (isIncludeCurrent ? 1 : 0)];
					Copy.floats(fsi, t, xs, 0, maxLag);
					if (isIncludeCurrent)
						xs[maxLag] = is != it ? fsi[t + maxLag] : 1f;
					return xs;
				}) //
				.toArray(float[].class));
	}

}
