package suite.math.linalg;

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
		float[][] m1 = mtx.of(m0);
		var height = mtx.height(m1);
		var width_ = mtx.width(m1);

		for (int j = 0; j < width_; j++) {
			var j_ = j;
			double mean = Ints_.range(0, height).toDouble(Int_Dbl.sum(i -> m1[i][j_])) / height;
			for (int i = 0; i < height; i++)
				m1[i][j_] -= mean;
		}

		float[][] cov = mtx.scale(mtx.mul_mTn(m1, m1), 1d / height);
		float[][] evs = eigen.power(cov);
		return eigen.values(cov, evs);
	}

	// http://www.cs.yale.edu/homes/el327/datamining2013aFiles/07_singular_value_decomposition.pdf
	// "Computing the SVD: The power method"
	public Fixie3<float[], float[][], float[][]> svd(float[][] a) {
		Fun<float[][], Fixie3<Double, float[], float[]>> f = Boolean.TRUE ? this::svd0 : this::svd1;
		float[] ss = new float[k];
		float[][] us = new float[k][];
		float[][] vs = new float[k][];

		for (int i = 0; i < k; i++) {
			Fixie3<Double, float[], float[]> fixie = f.apply(a);
			float[][] a0 = a;
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
		float[] x = Floats_.toArray(n, i -> random.nextFloat());
		float[][] at = mtx.transpose(a);

		for (int i = 0; i < 16; i++)
			x = mtx.mul(at, mtx.mul(a, x));

		float[] v = vec.normalize(x);
		float[] av = mtx.mul(a, v);
		var s = vec.abs(av);
		float[] u = vec.scale(av, 1d / s);
		return Fixie.of(s, u, v);
	}

	// http://www.anstuocmath.ro/mathematics/anale2015vol2/Bentbib_A.H.__Kanber_A..pdf
	// "3 SVD Power Method"
	private Fixie3<Double, float[], float[]> svd1(float[][] a) {
		float[] v = Floats_.toArray(mtx.width(a), i -> random.nextFloat());
		float[][] at = mtx.transpose(a);

		for (int i = 0; i < 256; i++) {
			float[] u = vec.normalize(mtx.mul(a, v));
			float[] z = mtx.mul(at, u);
			var beta = vec.abs(z);
			var invBeta = 1d / beta;
			v = vec.scale(z, invBeta);
			double error = vec.abs(vec.sub(mtx.mul(a, v), vec.scale(u, invBeta)));
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
		float[][] covs = mtx.covariance(omega);
		float[][] evs = eigen.power(covs);
		float[] evals = eigen.values(omega, covs);
		float[][] m = To.matrix(mtx.height(evs), mtx.width(evs), (i, j) -> evs[i][j] / Math.sqrt(evals[j]));
		return mtx.mul(m, omega);
	}

}
