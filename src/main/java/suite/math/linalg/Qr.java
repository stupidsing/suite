package suite.math.linalg;

import static suite.util.Friends.sqrt;

import suite.adt.pair.Pair;

public class Qr {

	private Matrix mtx = new Matrix();
	private Vector vec = new Vector();

	public Pair<float[][], float[][]> decompose(float[][] m0) {
		var qr = decompose_mT_T(mtx.transpose(m0));
		return qr.map((q, r) -> Pair.of(mtx.transpose(q), mtx.transpose(r)));
	}

	/**
	 * Perform QR decomposition by Gram-Schmidt process.
	 */
	public Pair<float[][], float[][]> decompose_mT_T(float[][] m) { // a
		var size = mtx.sqSize(m);
		var q = new float[size][]; // e

		for (var i = 0; i < size; i++) {
			var a = m[i];
			var u1 = vec.copyOf(a);

			for (var j = 0; j < i; j++) {
				var u = q[j];
				vec.addScaleOn(u1, u, -vec.dot(u, a));
			}

			q[i] = vec.scaleOn(u1, sqrt(1f / vec.dot(u1, u1)));
		}

		var r = new float[size][size];

		for (var i = 0; i < size; i++)
			for (var j = 0; j <= i; j++)
				r[i][j] = (float) vec.dot(q[j], m[i]);

		return Pair.of(q, r);
	}

	public Pair<float[][], float[][]> decomposeByGivensRotation(float[][] m) {
		var r = mtx.copyOf(m);
		var size = mtx.sqSize(r);
		var q = mtx.identity(size);

		for (var k = 0; k < size; k++)
			for (var i = size - 1; k < i; i--) {
				mtx.verifyEquals(m, mtx.mul(q, r));

				var i0 = i - 1;
				var i1 = i - 0;
				var f0 = r[i0][k];
				var f1 = r[i1][k];

				if (f1 != 0f) {
					var radius = sqrt(f0 * f0 + f1 * f1);
					var ir = 1d / radius;
					double cos = f0 * ir, sin = f1 * ir;

					for (var j = 0; j < size; j++) {
						var m0 = r[i0][j];
						var m1 = r[i1][j];
						r[i0][j] = (float) (m0 * cos + m1 * sin);
						r[i1][j] = (float) (m1 * cos - m0 * sin);

						var q0 = q[j][i0];
						var q1 = q[j][i1];
						q[j][i0] = (float) (q0 * cos + q1 * sin);
						q[j][i1] = (float) (q1 * cos - q0 * sin);
					}
				}
			}

		return Pair.of(q, r);
	}

}
