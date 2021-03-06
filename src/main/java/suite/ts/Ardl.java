package suite.ts;

import static primal.statics.Fail.fail;
import static suite.util.Streamlet_.forInt;

import primal.Verbs.New;
import primal.primitive.FltVerbs.ConcatFlt;
import primal.primitive.FltVerbs.CopyFlt;
import primal.primitive.adt.pair.FltObjPair;
import suite.math.numeric.Statistic;
import suite.math.numeric.Statistic.LinearRegression;
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
		var n = fsList.length;
		var length = fsList[0].length;

		return New.array(n, LinearRegression.class, it -> {
			var fs = fsList[it];

			return length == fs.length
					? stat.linearRegression(forInt(length - maxLag).map(t -> FltObjPair.of(fs[t], getExplanatoryVariables(fsList, it, t))))
					: fail("wrong input sizes");
		});
	}

	public float[] predict(LinearRegression[] lrs, float[][] fsList, int index) {
		return To.vector(lrs.length, it -> lrs[it].predict(getExplanatoryVariables(fsList, it, index)));
	}

	private float[] getExplanatoryVariables(float[][] fsList, int it, int t) {
		return ConcatFlt.arrays(New.array(fsList.length, float[].class, is -> {
			var fsi = fsList[is];
			var xs = new float[maxLag + (isIncludeCurrent ? 1 : 0)];
			CopyFlt.array(fsi, t, xs, 0, maxLag);
			if (isIncludeCurrent)
				xs[maxLag] = is != it ? fsi[t + maxLag] : 1f;
			return xs;
		}));
	}

}
