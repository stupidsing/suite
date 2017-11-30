package suite.math.stat;

import suite.math.stat.Statistic.LinearRegression;
import suite.primitive.Floats_;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.FltObjPair;
import suite.util.To;

/**
 * Auto-regressive distributed lag.
 *
 * @author ywsing
 */
public class Ardl {

	private int maxLag;
	private boolean isIncludeCurrent;

	private Statistic stat = new Statistic();

	public Ardl(int maxLag, boolean isIncludeCurrent) {
		this.maxLag = maxLag;
		this.isIncludeCurrent = isIncludeCurrent;
	}

	public LinearRegression[] ardl(float[][] fsList) {
		int n = fsList.length;
		int length = fsList[0].length;

		return To.array(n, LinearRegression.class, it -> {
			float[] fs = fsList[it];

			if (length == fs.length) {
				return stat.linearRegression(Ints_ //
						.range(length - maxLag) //
						.map(t -> FltObjPair.of(fs[t], getExplanatoryVariables(fsList, it, t))) //
						.toList());
			} else
				throw new RuntimeException("wrong input sizes");
		});
	}

	public float[] predict(LinearRegression[] lrs, float[][] fsList, int index) {
		return Floats_.toArray(lrs.length, it -> lrs[it].predict(getExplanatoryVariables(fsList, it, index)));
	}

	private float[] getExplanatoryVariables(float[][] fsList, int it, int t) {
		return Floats_.concat(To.array(fsList.length, float[].class, is -> {
			float[] fsi = fsList[is];
			float[] xs = new float[maxLag + (isIncludeCurrent ? 1 : 0)];
			Floats_.copy(fsi, t, xs, 0, maxLag);
			if (isIncludeCurrent)
				xs[maxLag] = is != it ? fsi[t + maxLag] : 1f;
			return xs;
		}));
	}

}
