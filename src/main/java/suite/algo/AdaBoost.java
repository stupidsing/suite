package suite.algo;

import static suite.util.Friends.forInt;

import java.util.List;

import suite.math.linalg.Vector;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Floats;
import suite.primitive.adt.pair.DblIntPair;
import suite.streamlet.Read;
import suite.util.To;

/**
 * https://en.wikipedia.org/wiki/AdaBoost#Example_algorithm_(Discrete_AdaBoost)
 * 
 * @author ywsing
 */
public class AdaBoost {

	private Vector vec = new Vector();

	public class XY {
		public final boolean[] xs; // weak classifiers
		public final boolean y;

		public XY(boolean[] xs, boolean y) {
			this.xs = xs;
			this.y = y;
		}
	}

	public Obj_Dbl<boolean[]> classify(List<XY> xys) {
		var n = xys.size();
		var inv_n = 1d / n;
		var ws = To.vector(n, i -> inv_n);

		var p_alphas = forInt(1024).map(t -> {
			var min = DblIntPair.of(Double.MIN_VALUE, -1);

			forInt(xys.get(0).xs.length).sink(p -> {
				var error = Read //
						.from(xys) //
						.map(xy -> xy.y == xy.xs[p] ? 0d : ws[p]) //
						.toDouble(Obj_Dbl.sum(e -> e));

				if (error < min.t0)
					min.update(error, p);
			});

			var error = min.t0;
			var p = min.t1;
			var alpha = Math.log((1d - error) / error) * .5d;

			for (var i = 0; i < n; i++) {
				var xy = xys.get(i);
				ws[i] *= Math.exp(-(xy.y ? 1d : -1d) * alpha * d(xy.xs[p]));
			}

			vec.scaleOn(ws, 1d / Floats.of(ws).streamlet().sum()); // renormalize

			return DblIntPair.of(alpha, p);
		}).collect();

		return xs -> p_alphas.toDouble(Obj_Dbl.sum(pair -> pair.map((alpha, p) -> alpha * d(xs[p]))));
	}

	private double d(boolean b) {
		return b ? 1d : -1d;
	}

}
