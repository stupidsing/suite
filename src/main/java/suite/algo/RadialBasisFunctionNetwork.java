package suite.algo;

import static java.lang.Math.exp;
import static suite.util.Streamlet_.forInt;

import java.util.List;

import primal.MoreVerbs.Read;
import primal.fp.Funs.Fun;
import suite.math.linalg.CholeskyDecomposition;
import suite.math.linalg.Matrix;
import suite.math.linalg.Vector;
import suite.streamlet.As;
import suite.util.To;

/**
 * Radial basis function network with K-means clustering.
 *
 * @author ywsing
 */
public class RadialBasisFunctionNetwork {

	private CholeskyDecomposition cd = new CholeskyDecomposition();
	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	private int nHiddens = 19;
	private float[][] centers;
	private float[] invVariances;

	public Fun<float[], float[]> train(float[][] ins, float[][] outs) {
		var length = ins[0].length;
		var kmc = new KmeansCluster(length).kMeansCluster(List.of(ins), nHiddens, nHiddens);

		var sizes = new int[nHiddens];
		var sums = new float[nHiddens][length];
		var variances = new float[nHiddens];

		for (var i = 0; i < ins.length; i++) {
			var cl = kmc[i];
			sizes[cl]++;
			vec.addOn(sums[cl], ins[i]);
		}

		centers = To.array(nHiddens, float[].class, cl -> vec.scale(sums[cl], 1d / sizes[cl]));

		for (var i = 0; i < ins.length; i++) {
			var cl = kmc[i];
			variances[cl] += vec.dotDiff(ins[i], centers[cl]);
		}

		invVariances = To.vector(variances.length, i -> 1f / variances[i]);
		var rbfs = Read.from(ins).map(this::evaluateRbfs).toArray(float[].class);
		var rbfs_t = mtx.transpose(rbfs);
		var cdf = cd.inverseMul(mtx.mul(rbfs_t, rbfs));
		var psi = Read.from(rbfs).map(cdf).toArray(float[].class);
		return in -> mtx.mul(evaluateRbfs(in), psi);
	}

	private float[] evaluateRbfs(float[] in) {
		return forInt(nHiddens)
				.collect(As.floats(cl -> (float) exp(-.5d * vec.dotDiff(in, centers[cl]) * invVariances[cl])))
				.toArray();
	}

}
