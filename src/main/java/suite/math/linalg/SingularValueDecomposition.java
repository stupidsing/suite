package suite.math.linalg;

import static suite.util.Friends.sqrt;

import java.util.Random;

import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.Fixie3;
import suite.primitive.Floats_;
import suite.primitive.Int_Dbl;
import suite.primitive.Ints_;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.To;

public class SingularValueDecomposition {

	private int k = 9;

	private Eigen eigen = new Eigen();
	private Matrix mtx = new Matrix();
	private Random random = new Random();
	private Vector vec = new Vector();

	// Machine Learning - An Algorithm Perspective
	// 6.2 Principal Components Analysis
	public float[] pca(float[][] m0) {
		var m1 = mtx.of(m0);
		var height = mtx.height(m1);
		var width_ = mtx.width(m1);

		for (var j = 0; j < width_; j++) {
			var j_ = j;
			var mean = Ints_.range(height).toDouble(Int_Dbl.sum(i -> m1[i][j_])) / height;
			for (var i = 0; i < height; i++)
				m1[i][j_] -= mean;
		}

		var cov = mtx.scale(mtx.mul_mTn(m1, m1), 1d / height);
		var evs = eigen.power(cov);
		return eigen.values(cov, evs);
	}

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
		var x = Floats_.toArray(n, i -> random.nextFloat());
		var at = mtx.transpose(a);

		for (var i = 0; i < 16; i++)
			x = mtx.mul(at, mtx.mul(a, x));

		var v = vec.normalize(x);
		var av = mtx.mul(a, v);
		var s = vec.abs(av);
		var u = vec.scale(av, 1d / s);
		return Fixie.of(s, u, v);
	}

	// http://www.anstuocmath.ro/mathematics/anale2015vol2/Bentbib_A.H.__Kanber_A..pdf
	// "3 SVD Power Method"
	private Fixie3<Double, float[], float[]> svd1(float[][] a) {
		var v = Floats_.toArray(mtx.width(a), i -> random.nextFloat());
		var at = mtx.transpose(a);

		for (var i = 0; i < 256; i++) {
			var u = vec.normalize(mtx.mul(a, v));
			var z = mtx.mul(at, u);
			var beta = vec.abs(z);
			var invBeta = 1d / beta;
			v = vec.scale(z, invBeta);
			var error = vec.abs(vec.sub(mtx.mul(a, v), vec.scale(u, invBeta)));
			if (error < .01d)
				return Fixie.of(beta, u, v);
		}

		return Fail.t();
	}

	// http://www.dcs.gla.ac.uk/~vincia/textbook.pdf
	// Francesca Camastra, Alessandro Vinciarelli, "Machine Learning for Audio,
	// Image and Video Analysis"
	// 5.7.3 Whitening Transformation
	public float[][] whiten(float[][] omega) {
		var covs = mtx.covariance(omega);
		var evs = eigen.power(covs);
		var evals = eigen.values(omega, covs);
		var m = To.matrix(mtx.height(evs), mtx.width(evs), (i, j) -> evs[i][j] / sqrt(evals[j]));
		return mtx.mul(m, omega);
	}

}
