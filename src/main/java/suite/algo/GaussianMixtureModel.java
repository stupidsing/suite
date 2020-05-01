package suite.algo;

import primal.MoreVerbs.Read;
import primal.primitive.FltMoreVerbs.ReadFlt;
import primal.primitive.fp.AsDbl;
import suite.math.linalg.GaussSeidel;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.util.To;

import java.util.List;
import java.util.Random;

import static java.lang.Math.*;
import static suite.util.Streamlet_.forInt;

/**
 * https://towardsdatascience.com/how-to-code-gaussian-mixture-models-from-scratch-in-python-9e7975df5252
 *
 * https://www.geeksforgeeks.org/gaussian-mixture-model/
 *
 * @author ywsing
 */
public class GaussianMixtureModel {

	private GaussSeidel gs = new GaussSeidel();
	private Matrix mtx = new Matrix();
	private Random random = new Random();
	private Vector vec = new Vector();

	private double hinvpi = .5d / PI;

	public final List<GaussComponent> components;

	public class GaussComponent {
		public double scale;
		public float[] mean;
		public float[][] covar;

		public GaussComponent(double scale, float[] mean, float[][] covar) {
			this.scale = scale;
			this.mean = mean;
			this.covar = covar;
		}
	}

	public GaussianMixtureModel(int n, float[][] obs) {
		var nObs = obs.length;
		var dim = obs[0].length;

		var comps = forInt(n).map(i -> new GaussComponent(
				1d / n,
				To.vector(dim, j -> random.nextGaussian()),
				mtx.identity(dim))).toList();

		for (var iter = 0; iter < 32; iter++) {
			var comps_ = comps;

			var factors = To.vector(n, i -> {
				var mvs = comps_.get(i);
				return mvs.scale * sqrt(hinvpi / mtx.det(mvs.covar));
			});

			// expectation
			var bks = Read.from(obs).map(x -> {
				var fs = To.vector(n, k -> {
					var mvs = comps_.get(k);
					var d = vec.sub(x, mvs.mean);
					var f = factors[k] * exp(-.5d * vec.dot(d, gs.solve(mvs.covar, d)));
					return max(1e-32f, min(1e32f, f));
				});

				return vec.scaleOn(fs, 1d / ReadFlt.from(fs).sum());
			}).toArray(float[].class);

			// maximization
			comps = forInt(n).map(k -> {
				var bksum = Read.from(bks).toDouble(AsDbl.sum(bk -> bk[k]));
				var ibksum = 1d / bksum;
				var mean_ = comps_.get(k).mean;

				var mean1 = vec.scaleOn(forInt(nObs).fold(new float[dim], (o, sum) -> {
					return vec.addOn(sum, vec.scale(obs[o], bks[o][k]));
				}), ibksum);

				var covar1 = mtx.scaleOn(forInt(nObs).fold(new float[dim][dim], (o, sum) -> {
					var d = vec.sub(obs[o], mean_);
					return mtx.addOn(sum, mtx.scaleOn(mtx.mul(d), bks[o][k]));
				}), ibksum);

				var scale1 = bksum / nObs;

				return new GaussComponent(scale1, mean1, covar1);
			}).toList();
		}

		components = comps;
	}

}
