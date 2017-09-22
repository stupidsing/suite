package suite.math.linalg;

import java.util.Random;

import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.Fixie3;
import suite.primitive.Floats_;

// http://www.cs.yale.edu/homes/el327/datamining2013aFiles/07_singular_value_decomposition.pdf
public class SingularValueDecomposition {

	private int k = 9;

	private Matrix mtx = new Matrix();
	private Random random = new Random();

	public Fixie3<float[], float[][], float[][]> svd(float[][] a) {
		float[] ss = new float[k];
		float[][] us = new float[k][];
		float[][] vs = new float[k][];

		for (int i = 0; i < k; i++) {
			float[][] a_ = a;
			int i_ = i;
			Fixie3<Double, float[], float[]> fixie = svd0(a);
			a = fixie.map((s, u, v) -> {
				ss[i_] = s.floatValue();
				us[i_] = u;
				vs[i_] = v;
				return mtx.add(a_, mtx.scale(mtx.mul_mnT(new float[][] { u, }, new float[][] { v, }), -s));
			});
		}

		return Fixie.of(ss, us, mtx.transpose(vs));
	}

	public Fixie3<Double, float[], float[]> svd0(float[][] a) {
		int n = mtx.width(a);
		float[] x = Floats_.toArray(n, i -> random.nextFloat());
		float[][] at = mtx.transpose(a);

		for (int i = 0; i < 16; i++)
			x = mtx.mul(at, mtx.mul(a, x));

		float[] v = mtx.normalize(x);
		float[] av = mtx.mul(a, v);
		double s = mtx.abs(av);
		float[] u = mtx.scale(av, 1d / s);
		return Fixie.of(s, u, v);
	}

}
