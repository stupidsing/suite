package suite.algo;

import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import static suite.util.Streamlet_.forInt;

import java.util.List;

import primal.MoreVerbs.Read;
import primal.primitive.FltMoreVerbs.ReadFlt;
import primal.primitive.fp.AsDbl;
import suite.streamlet.As;
import suite.util.To;

/**
 * https://towardsdatascience.com/how-to-code-gaussian-mixture-models-from-scratch-in-python-9e7975df5252
 * 
 * @author ywsing
 */
public class GaussianMixtureModel {

	public final List<GaussComponent> components;

	private double invpi = 1d / PI;

	public class GaussComponent {
		public double mean;
		public double var;
		public double scale;

		public GaussComponent(double mean, double var, double scale) {
			this.mean = mean;
			this.var = var;
			this.scale = scale;
		}
	}

	public GaussianMixtureModel(int n, float[][] obs) {
		var comps = forInt(n).map(i -> new GaussComponent(0d, 0d, 0d / n)).toList();

		for (var iter = 0; iter < 256; iter++) {
			var comps_ = comps;

			// expectation
			var bks = Read.from(obs).map(xs -> {
				var x = xs[0];

				var fs = To.vector(n, k -> {
					var mvs = comps_.get(k);
					var d = x - mvs.mean;
					var ivar2 = .5d / mvs.var;
					var f = sqrt(ivar2 * invpi) * exp(-d * d * ivar2);
					return f * mvs.scale;
				});

				var bk = ReadFlt.from(fs).sum();
				return To.vector(fs, f -> f / bk);
			}).toArray(float[].class);

			// maximization
			comps = forInt(n).map(k -> {
				var bksum = Read.from(bks).toDouble(AsDbl.sum(bk -> bk[k]));
				var ibk = 1d / bksum;
				var mean_ = comps_.get(k).mean;

				var mean1 = forInt(obs.length).toDouble(As.sum(i -> bks[i][k] * obs[i][0])) * ibk;

				var var1 = forInt(obs.length).toDouble(As.sum(i -> {
					var d = obs[i][0] - mean_;
					return bks[i][k] * d * d;
				})) * ibk;

				var scale1 = bksum / obs.length;

				return new GaussComponent(mean1, var1, scale1);
			}).toList();
		}

		components = comps;
	}

}
