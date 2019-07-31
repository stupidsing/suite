package suite.math.linalg;

import static java.lang.Math.min;
import static primal.statics.Fail.fail;
import static suite.util.Streamlet_.forInt;

import java.util.Arrays;

import suite.math.Math_;
import suite.math.R3;
import suite.primitive.Dbl_Dbl;
import suite.primitive.Int_Dbl;
import suite.util.To;

public class Matrix {

	public float[][] add(float[][] m, float[][] n) {
		return addOn(copy(m), n);
	}

	public float[][] addOn(float[][] m, float[][] n) {
		var height = h(m);
		var width = w(m);
		if (height == h(n) && width == w(n))
			for (var i = 0; i < height; i++)
				for (var j = 0; j < width; j++)
					m[i][j] += n[i][j];
		else
			fail("wrong input sizes");
		return m;
	}

	public float[][] convolute(float[][] m, float[][] k) {
		var kh = h(k);
		var kw = w(k);
		var h1 = h(m) - kh + 1;
		var w1 = w(m) - kw + 1;
		var o = of(h1, w1);
		for (var i = 0; i < h1; i++)
			for (var j = 0; j < w1; j++)
				for (var di = 0; di < kh; di++)
					for (var dj = 0; dj < kw; dj++)
						o[i][j] += m[i + di][j + dj] * k[di][dj];
		return o;
	}

	// https://en.wikipedia.org/wiki/Covariance_matrix
	// vs :: nParameters x nSamples
	public float[][] covariance(float[][] vs) {
		var h = h(vs);
		var w = w(vs);
		var means = To.vector(vs, v -> forInt(w).toDouble(Int_Dbl.sum(j -> v[j])) / w);
		return To.matrix(h, h, (i0, i1) -> forInt(w).toDouble(Int_Dbl.sum(j -> vs[i0][j] * vs[i1][j])) / w - means[i0] * means[i1]);
	}

	public float[][] copyOf(float[][] m) {
		return copy(m);
	}

	public double det(float[][] m) {
		var size = sqSize_(m);
		var cols = forInt(size).toArray();

		class Det {
			private double sum;

			private void det(int i0, double d) {
				if (i0 < size) {
					var col0 = cols[i0];
					var i1 = i0 + 1;
					for (var it = i0; it < size; it++) {
						var colt = cols[it];
						cols[it] = col0;
						det(i1, (it == i0 ? d : -d) * m[i0][colt]);
						cols[it] = colt;
					}
				} else
					sum += d;
			}
		}

		var det = new Det();
		det.det(0, 1d);
		return det.sum;
	}

	public boolean equals(float[][] m, float[][] n) {
		var h = h(m);
		var w = w(m);
		var b = h == h(n) && w == w(n);
		for (var i = 0; i < h && b; i++)
			for (var j = 0; j < w && b; j++)
				b &= m[i][j] == n[i][j];
		return b;
	}

	public int hashCode(float[][] m) {
		var h = 7;
		for (var row : m)
			h = h * 31 + Arrays.hashCode(row);
		return h;
	}

	public int height(float[][] m) {
		return h(m);
	}

	public float[][] identity(int size) {
		var m = of(size, size);
		for (var r = 0; r < size; r++)
			m[r][r] = 1f;
		return m;
	}

	/**
	 * Calculates matric inverse by Gaussian-Jordan elimination.
	 */
	public float[][] inverse(float[][] m0) {
		var m = copyOf(m0); // do not alter input matrix
		var size = h(m);

		if (size != w(m))
			fail("wrong input sizes");

		var n = identity(size);

		for (var r = 0; r < size; r++) {
			var c = r;

			for (; c < size; c++)
				if (m[c][r] != 0f)
					break;

			if (c == size)
				fail("no inverse exists");

			if (r != c) {
				swapRows(m, r, c);
				swapRows(n, r, c);
			}

			var factor = 1f / m[r][r];
			mulRow(m, r, factor);
			mulRow(n, r, factor);

			for (var r1 = 0; r1 < size; r1++)
				if (r != r1) {
					var factor1 = -m[r1][r];
					addMultipliedRow(m, r, factor1, r1);
					addMultipliedRow(n, r, factor1, r1);
				}
		}

		return n;
	}

	public float[][] map(float[][] m, Dbl_Dbl f) {
		return mapOn(copyOf(m), f);
	}

	public float[][] mapOn(float[][] m, Dbl_Dbl f) {
		var h = h(m);
		var w = w(m);
		for (var i = 0; i < h; i++)
			for (var j = 0; j < w; j++)
				m[i][j] = (float) f.apply(m[i][j]);
		return m;
	}

	// v is a column vector; return vvT
	public float[][] mul(float[] v) {
		return mul(v, v);
	}

	public float[][] mul(float[] u, float[] v) {
		var m = new float[u.length][v.length];
		for (var i = 0; i < u.length; i++)
			for (var j = 0; j < v.length; j++)
				m[i][j] = u[i] * v[j];
		return m;
	}

	public float[][] mul(float[][] m, float[][] n, float[][] o, float[][] p) {
		return mul(m, mul(n, mul(o, p)));
	}

	public float[][] mul(float[][] m, float[][] n, float[][] o) {
		return mul(m, mul(n, o));
	}

	public float[] mul(float[] m, float[][] n) {
		var ix = m.length;
		var jx = w(n);
		var o = new float[jx];
		int i1, j1;
		if (ix == h(n))
			for (var i0 = 0; i0 < ix; i0 = i1) {
				i1 = min(i0 + 64, ix);
				for (var j0 = 0; j0 < jx; j0 = j1) {
					j1 = min(j0 + 64, jx);
					for (var i = i0; i < i1; i++)
						for (var j = j0; j < j1; j++)
							o[j] += m[i] * n[i][j];
				}
			}
		else
			fail("wrong input sizes");
		return o;
	}

	// nT is a column vector; returns a column vector
	public float[] mul(float[][] m, float[] nT) {
		var ix = h(m);
		var jx = w(m);
		var o = new float[ix];
		int i1, j1;
		if (ix == 0 || jx == nT.length)
			for (var i0 = 0; i0 < ix; i0 = i1) {
				i1 = min(i0 + 64, ix);
				for (var j0 = 0; j0 < jx; j0 = j1) {
					j1 = min(j0 + 64, jx);
					for (var i = i0; i < i1; i++)
						for (var j = j0; j < j1; j++)
							o[i] += m[i][j] * nT[j];
				}
			}
		else
			fail("wrong input sizes");
		return o;
	}

	public float[][] mul(float[][] m, float[][] n) {
		var ks = w(m);
		var height = h(m);
		var width = w(n);
		var o = of(height, width);
		int i1, j1, k1;
		if (height == 0 || ks == h(n))
			for (var i0 = 0; i0 < height; i0 = i1) {
				i1 = min(i0 + 64, height);
				for (var j0 = 0; j0 < width; j0 = j1) {
					j1 = min(j0 + 64, width);
					for (var k0 = 0; k0 < ks; k0 = k1) {
						k1 = min(k0 + 64, ks);
						for (var i = i0; i < i1; i++)
							for (var j = j0; j < j1; j++)
								for (var k = k0; k < k1; k++)
									o[i][j] += m[i][k] * n[k][j];
					}
				}
			}
		else
			fail("wrong input sizes");

		return o;
	}

	public R3 mul(float[][] m, R3 v) {
		if (sqSize_(m) == 3) {
			var x1 = m[0][0] * v.x + m[0][1] * v.y + m[0][2] * v.z;
			var y1 = m[1][0] * v.x + m[1][1] * v.y + m[1][2] * v.z;
			var z1 = m[2][0] * v.x + m[2][1] * v.y + m[2][2] * v.z;
			return new R3(x1, y1, z1);
		} else
			return fail("wrong input sizes");
	}

	// calculate m * nT
	public float[][] mul_mnT(float[][] m, float[][] n) {
		var ks = w(m);
		var height = h(m);
		var width = h(n);
		var o = of(height, width);
		int i1, j1, k1;
		if (ks == w(n))
			for (var i0 = 0; i0 < height; i0 = i1) {
				i1 = min(i0 + 64, height);
				for (var j0 = 0; j0 < width; j0 = j1) {
					j1 = min(j0 + 64, width);
					for (var k0 = 0; k0 < ks; k0 = k1) {
						k1 = min(k0 + 64, ks);
						for (var i = i0; i < i1; i++)
							for (var j = j0; j < j1; j++)
								for (var k = k0; k < k1; k++)
									o[i][j] += m[i][k] * n[j][k];
					}
				}
			}
		else
			fail("wrong input sizes");
		return o;
	}

	// calculate mT * n
	public float[][] mul_mTn(float[][] m, float[][] n) {
		var ks = h(m);
		var height = w(m);
		var width = w(n);
		var o = of(height, width);
		int i1, j1, k1;
		if (ks == h(n))
			for (var i0 = 0; i0 < height; i0 = i1) {
				i1 = min(i0 + 64, height);
				for (var j0 = 0; j0 < width; j0 = j1) {
					j1 = min(j0 + 64, width);
					for (var k0 = 0; k0 < ks; k0 = k1) {
						k1 = min(k0 + 64, ks);
						for (var i = i0; i < i1; i++)
							for (var j = j0; j < j1; j++)
								for (var k = k0; k < k1; k++)
									o[i][j] += m[k][i] * n[k][j];
					}
				}
			}
		else
			fail("wrong input sizes");
		return o;
	}

	public float[][] neg(float[][] m) {
		return negOn(copy(m));
	}

	public float[][] negOn(float[][] m) {
		var height = h(m);
		var width = w(m);
		for (var i = 0; i < height; i++)
			for (var j = 0; j < width; j++)
				m[i][j] = -m[i][j];
		return m;
	}

	public float[][] of(int height, int width) {
		return new float[height][width];
	}

	public float[][] rotate(float angle) {
		var sin = (float) Math.sin(angle);
		var cos = (float) Math.cos(angle);
		return new float[][] { { cos, -sin, }, { sin, cos, }, };
	}

	public float[][] rotateX(float angle) {
		var sin = (float) Math.sin(angle);
		var cos = (float) Math.cos(angle);
		return new float[][] { { 0f, 0f, 0f, }, { 0f, cos, -sin, }, { 0f, sin, cos, }, };
	}

	public float[][] rotateY(float angle) {
		var sin = (float) Math.sin(angle);
		var cos = (float) Math.cos(angle);
		return new float[][] { { cos, 0f, -sin, }, { 0f, 0f, 0f, }, { sin, 0f, cos, }, };
	}

	public float[][] rotateZ(float angle) {
		var sin = (float) Math.sin(angle);
		var cos = (float) Math.cos(angle);
		return new float[][] { { cos, -sin, 0f, }, { sin, cos, 0f, }, { 0f, 0f, 0f, }, };
	}

	public float[][] scale(float[][] m, double f) {
		return scaleOn(copy(m), f);
	}

	public float[][] scaleOn(float[][] m, double d) {
		var height = h(m);
		var width = w(m);
		for (var i = 0; i < height; i++)
			for (var j = 0; j < width; j++)
				m[i][j] *= d;
		return m;
	}

	public int sqSize(float[][] m) {
		return sqSize_(m);
	}

	public float[][] sub(float[][] m, float[][] n) {
		return subOn(copy(m), n);
	}

	public float[][] subOn(float[][] m, float[][] n) {
		var height = h(m);
		var width = w(m);
		if (height == h(n) && width == w(n))
			for (var i = 0; i < height; i++)
				for (var j = 0; j < width; j++)
					m[i][j] -= n[i][j];
		else
			fail("wrong input sizes");
		return m;
	}

	public float[][] sum(float[][][] ms) {
		if (0 < ms.length) {
			var m0 = ms[0];
			var sum = new float[h(m0)][w(m0)];
			for (var error : ms)
				addOn(sum, error);
			return sum;
		} else
			return new float[][] {};
	}

	public void verifyEquals(float[][] m0, float[][] m1) {
		verifyEquals(m0, m1, Math_.epsilon);
	}

	public void verifyEquals(float[][] m0, float[][] m1, float epsilon) {
		var height = h(m0);
		var width = w(m0);
		if (height == h(m1) && width == w(m1))
			for (var i = 0; i < height; i++)
				for (var j = 0; j < width; j++)
					Math_.verifyEquals(m0[i][j], m1[i][j], epsilon);
		else
			fail("wrong input sizes");
	}

	private void swapRows(float[][] m, int row0, int row1) {
		var temp = m[row0];
		m[row0] = m[row1];
		m[row1] = temp;
	}

	public String toString(float[] m) {
		return To.string(sb -> dump(sb, m));
	}

	public String toString(float[][] m) {
		return To.string(sb -> {
			for (var row : m) {
				dump(sb, row);
				sb.append("\n");
			}
		});
	}

	public float[][] transpose(float[][] m) {
		var height = h(m);
		var width = w(m);
		var o = of(width, height);
		int i1, j1;
		for (var i0 = 0; i0 < height; i0 = i1) {
			i1 = min(i0 + 64, height);
			for (var j0 = 0; j0 < width; j0 = j1) {
				j1 = min(j0 + 64, width);
				for (var i = i0; i < i1; i++)
					for (var j = j0; j < j1; j++)
						o[j][i] = m[i][j];
			}
		}
		return o;
	}

	public int width(float[][] m) {
		return w(m);
	}

	private void mulRow(float[][] m, int row, float factor) {
		for (var col = 0; col < w(m); col++)
			m[row][col] *= factor;
	}

	private void addMultipliedRow(float[][] m, int sourceRow, float factor, int targetRow) {
		for (var col = 0; col < w(m); col++)
			m[targetRow][col] = m[targetRow][col] + factor * m[sourceRow][col];
	}

	private float[][] copy(float[][] m0) {
		var height = h(m0);
		var width = w(m0);
		var m1 = of(height, width);
		for (var i = 0; i < height; i++)
			for (var j = 0; j < width; j++)
				m1[i][j] = m0[i][j];
		return m1;
	}

	private void dump(StringBuilder sb, float[] m) {
		sb.append("[ ");
		for (var f : m)
			sb.append(To.string(f) + " ");
		sb.append("]");
	}

	private int sqSize_(float[][] m) {
		var height = h(m);
		if (height == w(m))
			return height;
		else
			return fail("wrong input sizes");
	}

	private int h(float[][] m) {
		return m.length;
	}

	private int w(float[][] m) {
		return 0 < m.length ? m[0].length : 0;
	}

}
