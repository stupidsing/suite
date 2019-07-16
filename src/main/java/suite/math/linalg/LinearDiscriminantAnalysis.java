package suite.math.linalg;

import suite.streamlet.FunUtil.Fun;
import suite.util.To;

// https://sebastianraschka.com/Articles/2014_python_lda.html
public class LinearDiscriminantAnalysis {

	private CholeskyDecomposition cd = new CholeskyDecomposition();
	private Eigen eigen = new Eigen();
	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	public Fun<float[], float[]> lda(int k, float[][][] data) {
		var nCategories = data.length;
		var nParameters = data[0][0].length;
		var overallMean = new float[nParameters];
		var nSamples = 0;

		for (var c = 0; c < nCategories; c++) {
			nSamples += data[c].length;
			for (var r : data[c])
				vec.addOn(overallMean, r);
		}

		vec.scaleOn(overallMean, 1d / nSamples);

		var withinClassMeans = new float[nCategories][];
		var sw = new float[nParameters][nParameters];

		for (var c = 0; c < nCategories; c++) {
			var withinClassMean = new float[nParameters];

			for (var r : data[c])
				vec.addOn(withinClassMean, r);

			vec.scaleOn(withinClassMean, 1d / data[c].length);

			for (var r : data[c])
				mtx.addOn(sw, mtx.mul(vec.sub(r, withinClassMean)));

			withinClassMeans[c] = withinClassMean;
		}

		var sb = new float[nParameters][nParameters];

		for (var c = 0; c < nCategories; c++) {
			var m = mtx.scaleOn(mtx.mul(vec.sub(withinClassMeans[c], overallMean)), data[c].length);
			mtx.addOn(sb, m);
		}

		var mul = cd.inverseMul(sw);
		var mt = mtx.transpose(To.array(nParameters, float[].class, n -> mul.apply(mtx.transpose(sb)[n])));
		var eigens = eigen.power1(mt);
		var w = new float[k][];

		for (var i = 0; i < k; i++)
			w[i] = vec.normalizeOn(eigens.get(i).t1);

		return x -> mtx.mul(w, x);
	}

}
