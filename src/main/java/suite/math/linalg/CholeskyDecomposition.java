package suite.math.linalg;

import static suite.util.Friends.sqrt;

import suite.adt.pair.Pair;
import suite.streamlet.FunUtil.Iterate;
import suite.util.To;

public class CholeskyDecomposition {

	private Matrix mtx = new Matrix();

	/**
	 * @param m a Hermitian (i.e. symmetric), positive-definite matrix.
	 * @return a function that calculates x -> m^-1 * x.
	 */
	public Iterate<float[]> inverseMul(float[][] m) {
		Pair<float[][], float[]> ldlt = ldlt(m);
		var l = ldlt.t0;
		var d = ldlt.t1;
		var reciprocalsD = To.vector(d, f -> 1f / f);

		var height = mtx.height(m);
		var width = mtx.width(m);

		return fs0 -> {
			var fs1 = new float[height]; // will be inverse(L) * fs0

			for (var i = 0; i < height; i++) {
				var sum = fs0[i];
				for (var j = 0; j < i; j++)
					sum -= l[i][j] * fs1[j];
				fs1[i] = sum;
			}

			// will be inverse(D) * fs1
			var fs2 = To.vector(fs1.length, i -> fs1[i] * reciprocalsD[i]);
			var fs3 = new float[width]; // will be inverse(L*) * fs2

			for (var i = width - 1; 0 <= i; i--) {
				var sum = fs2[i];
				for (var j = height - 1; i < j; j--)
					sum -= l[j][i] * fs3[j];
				fs3[i] = sum;
			}

			return fs3;
		};
	}

	/**
	 * Performs Cholesky decomposition. Input m must be symmetric and positive
	 * definite. Also its contents will be destroyed as the decomposition is
	 * performed in-place.
	 * 
	 * @param m a Hermitian (i.e. symmetric), positive-definite matrix.
	 * @return lower-triangular matrix L that satisfy m = L * L*
	 */
	public float[][] decompose(float[][] m) {
		var size = mtx.sqSize(m);
		var l = mtx.identity(size);

		for (var c = 0; c < size; c++) {
			var mii = m[c][c];
			var mii_sqrt = sqrt(mii);
			var imii = 1f / mii;
			var imii_sqrt = 1f / mii_sqrt;

			for (var i = c; i < size; i++) {
				var sum = l[i][c] * mii_sqrt;
				for (var j = c + 1; j < size; j++)
					sum += imii_sqrt * l[i][j] * m[j][c];
				l[i][c] = (float) sum;
			}

			for (var i = c + 1; i < size; i++)
				for (var j = c + 1; j < size; j++)
					m[i][j] -= imii * m[i][c] * m[j][c];
		}

		return l;
	}

	/**
	 * Performs Cholesky decomposition. Input m must be symmetric and positive
	 * definite. Also its contents will be destroyed as the decomposition is
	 * performed in-place.
	 * 
	 * @param m a Hermitian (i.e. symmetric), positive-definite matrix.
	 * @return A pair of lower-triangular matrix L and diagonal vector D that
	 *         satisfies m = L * D * L*.
	 */
	public Pair<float[][], float[]> ldlt(float[][] m) {
		var size = mtx.height(m);
		var l = mtx.identity(size);
		var d = new float[size];

		for (var c = 0; c < size; c++) {
			var imii = 1f / (d[c] = m[c][c]);
			var c1 = c + 1;

			for (var i = c1; i < size; i++)
				for (var j = c1; j < size; j++) {
					var imii_mjc = imii * m[j][c];
					l[i][c] += imii_mjc * l[i][j];
					m[i][j] -= imii_mjc * m[i][c];
				}
		}

		return Pair.of(l, d);
	}

}
