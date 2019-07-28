package suite.math.linalg;

import static java.lang.Math.sqrt;
import static suite.util.Friends.fail;

import java.util.Random;

import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.Fixie3;
import suite.streamlet.FunUtil.Fun;
import suite.util.To;

public class SingularValueDecomposition {

	private int k = 9;

	private Eigen eigen = new Eigen();
	private Matrix mtx = new Matrix();
	private Random random = new Random();
	private Vector vec = new Vector();

	// http://www.cs.yale.edu/homes/el327/datamining2013aFiles/07_singular_value_decomposition.pdf
	// "Computing the SVD: The power method"
	public Fixie3<float[], float[][], float[][]> svd(float[][] a) {
		Fun<float[][], Fixie3<Double, float[], float[]>> f = Boolean.TRUE ? this::svd0 : this::svd1;
		var ss = new float[k];
		var us = new float[k][];
		var vs = new float[k][];

		for (var i = 0; i < k; i++) {
			var fixie = f.apply(a);
			var a0 = a;
			var i_ = i;

			a = fixie.map((s, u, v) -> {
				ss[i_] = s.floatValue();
				us[i_] = u;
				vs[i_] = v;

				return VirtualMatrix //
						.of(u) //
						.mul(VirtualMatrix.of(v).transpose()) //
						.scale(-s) //
						.add(VirtualMatrix.of(a0)) //
						.matrix();

				// return mtx.add(a_, mtx.scale(mtx.mul_mnT(new float[][] { u,
				// }, new float[][]
				// { v, }), -s));
			});
		}

		return Fixie.of(ss, us, mtx.transpose(vs));
	}

	private Fixie3<Double, float[], float[]> svd0(float[][] a) {
		var n = mtx.width(a);
		var x = To.vector(n, i -> random.nextFloat());
		var at = mtx.transpose(a);

		for (var i = 0; i < 16; i++)
			x = mtx.mul(at, mtx.mul(a, x));

		var v = vec.normalizeOn(x);
		var av = mtx.mul(a, v);
		var s = vec.abs(av);
		var u = vec.scale(av, 1d / s);
		return Fixie.of(s, u, v);
	}

	// http://www.anstuocmath.ro/mathematics/anale2015vol2/Bentbib_A.H.__Kanber_A..pdf
	// "3 SVD Power Method"
	private Fixie3<Double, float[], float[]> svd1(float[][] a) {
		var v = To.vector(mtx.width(a), i -> random.nextFloat());
		var at = mtx.transpose(a);

		for (var i = 0; i < 256; i++) {
			var u = vec.normalizeOn(mtx.mul(a, v));
			var z = mtx.mul(at, u);
			var beta = vec.abs(z);
			var invBeta = 1d / beta;
			v = vec.scale(z, invBeta);
			var error = vec.abs(vec.sub(mtx.mul(a, v), vec.scale(u, invBeta)));
			if (error < .01d)
				return Fixie.of(beta, u, v);
		}

		return fail();
	}

	// http://www.dcs.gla.ac.uk/~vincia/textbook.pdf
	// Francesca Camastra, Alessandro Vinciarelli, "Machine Learning for Audio,
	// Image and Video Analysis"
	// 5.7.3 Whitening Transformation
	public float[][] whiten(float[][] omega) {
		var covs = mtx.covariance(omega);
		var evs = eigen.power0(covs);
		var m = To.matrix(covs.length, covs.length, (i, j) -> evs.get(i).v[j] / sqrt(evs.get(j).k));
		return mtx.mul(m, omega);
	}

}
