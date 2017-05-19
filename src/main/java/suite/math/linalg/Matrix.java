package suite.math.linalg;

import java.util.Arrays;

import suite.math.MathUtil;
import suite.math.Vector;
import suite.util.Copy;
import suite.util.To;

public class Matrix {

	public float[] add(float[] m, float[] n) {
		return addOn(copy(m), n);
	}

	public float[][] add(float[][] m, float[][] n) {
		return addOn(copy(m), n);
	}

	public float[] addOn(float[] m, float[] n) {
		int length = m.length;
		if (length == n.length)
			for (int i = 0; i < length; i++)
				m[i] += n[i];
		else
			throw new RuntimeException("wrong input sizes");
		return m;
	}

	public float[][] addOn(float[][] m, float[][] n) {
		int height = h(m);
		int width = w(m);
		if (height == h(n) && width == w(n))
			for (int i = 0; i < height; i++)
				for (int j = 0; j < width; j++)
					m[i][j] += n[i][j];
		else
			throw new RuntimeException("wrong input sizes");
		return m;
	}

	public float[] addScaleOn(float[] m, float[] n, float f) {
		int length = m.length;
		if (length == n.length)
			for (int i = 0; i < length; i++)
				m[i] += n[i] * f;
		else
			throw new RuntimeException("wrong input sizes");
		return m;
	}

	public float[] concat(float[]... array) {
		int length = 0;
		for (float[] fs : array)
			length += fs.length;
		float[] fs1 = new float[length];
		int i = 0;
		for (float[] fs : array) {
			Copy.floats(fs, 0, fs1, i, fs.length);
			i += fs.length;
		}
		return fs1;
	}

	public float[][] convolute(float[][] m, float[][] k) {
		int kh = h(k), kw = w(k);
		int h1 = h(m) - kh + 1;
		int w1 = w(m) - kw + 1;
		float[][] o = of(h1, w1);
		for (int i = 0; i < h1; i++)
			for (int j = 0; j < w1; j++)
				for (int di = 0; di < kh; di++)
					for (int dj = 0; dj < kw; dj++)
						o[i][j] += m[i + di][j + dj] * k[di][dj];
		return o;
	}

	public float dot(float[] m, float[] n) {
		int length = m.length;
		float sum = 0;
		if (length == n.length)
			for (int i = 0; i < length; i++)
				sum += m[i] * n[i];
		else
			throw new RuntimeException("wrong input sizes");
		return sum;
	}

	public boolean equals(float[][] m, float[][] n) {
		int h = h(m);
		int w = w(m);
		if (h == h(n) && w == w(n)) {
			for (int i = 0; i < h; i++)
				for (int j = 0; j < w; j++)
					if (m[i][j] != n[i][j])
						return false;
			return true;
		} else
			return false;
	}

	public int hashCode(float[][] m) {
		int hashCode = 0;
		for (float[] row : m)
			hashCode = hashCode * 31 + Arrays.hashCode(row);
		return hashCode;
	}

	public int height(float[][] m) {
		return h(m);
	}

	public float[][] identity(int size) {
		float[][] m = of(size, size);
		for (int r = 0; r < size; r++)
			m[r][r] = 1f;
		return m;
	}

	/**
	 * Calculates matric inverse by Gaussian-Jordan elimination.
	 */
	public float[][] inverse(float[][] m0) {
		float[][] m = of(m0); // do not alter input matrix
		int size = h(m);

		if (size != w(m))
			throw new RuntimeException("wrong input sizes");

		float[][] n = identity(size);

		for (int r = 0; r < size; r++) {
			int c = r;

			for (; c < size; c++)
				if (m[c][r] != 0f)
					break;

			if (c == size)
				throw new RuntimeException("no inverse exists");

			if (r != c) {
				swapRows(m, r, c);
				swapRows(n, r, c);
			}

			float factor = 1f / m[r][r];
			mulRow(m, r, factor);
			mulRow(n, r, factor);

			for (int r1 = 0; r1 < size; r1++)
				if (r != r1) {
					float factor1 = -m[r1][r];
					addMultipliedRow(m, r, factor1, r1);
					addMultipliedRow(n, r, factor1, r1);
				}
		}

		return n;
	}

	public float[] mul(float[] m, float[][] n) {
		int ix = m.length;
		int jx = w(n);
		float[] o = new float[jx];
		int i1, j1;
		if (ix == h(n))
			for (int i0 = 0; i0 < ix; i0 = i1) {
				i1 = Math.min(i0 + 64, ix);
				for (int j0 = 0; j0 < jx; j0 = j1) {
					j1 = Math.min(j0 + 64, jx);
					for (int i = i0; i < i1; i++)
						for (int j = j0; j < j1; j++)
							o[j] += m[i] * n[i][j];
				}
			}
		else
			throw new RuntimeException("wrong input sizes");
		return o;
	}

	// nT is a column vector; returns a column vector
	public float[] mul(float[][] m, float[] nT) {
		int ix = h(m);
		int jx = w(m);
		float[] o = new float[ix];
		int i1, j1;
		if (jx == nT.length)
			for (int i0 = 0; i0 < ix; i0 = i1) {
				i1 = Math.min(i0 + 64, ix);
				for (int j0 = 0; j0 < jx; j0 = j1) {
					j1 = Math.min(j0 + 64, jx);
					for (int i = i0; i < i1; i++)
						for (int j = j0; j < j1; j++)
							o[i] += m[i][j] * nT[j];
				}
			}
		else
			throw new RuntimeException("wrong input sizes");
		return o;
	}

	public float[][] mul(float[][] m, float[][] n) {
		int ks = w(m);
		int height = h(m);
		int width = w(n);
		float[][] o = of(height, width);
		int i1, j1, k1;
		if (ks == h(n))
			for (int i0 = 0; i0 < height; i0 = i1) {
				i1 = Math.min(i0 + 64, height);
				for (int j0 = 0; j0 < width; j0 = j1) {
					j1 = Math.min(j0 + 64, width);
					for (int k0 = 0; k0 < ks; k0 = k1) {
						k1 = Math.min(k0 + 64, ks);
						for (int i = i0; i < i1; i++)
							for (int j = j0; j < j1; j++)
								for (int k = k0; k < k1; k++)
									o[i][j] += m[i][k] * n[k][j];
					}
				}
			}
		else
			throw new RuntimeException("wrong input sizes");

		return o;
	}

	public Vector mul(float[][] m, Vector v) {
		if (h(m) == 3 && w(m) == 3) {
			float x1 = m[0][0] * v.x + m[0][1] * v.y + m[0][2] * v.z;
			float y1 = m[1][0] * v.x + m[1][1] * v.y + m[1][2] * v.z;
			float z1 = m[2][0] * v.x + m[2][1] * v.y + m[2][2] * v.z;
			return new Vector(x1, y1, z1);
		} else
			throw new RuntimeException("wrong input sizes");
	}

	// calculate m * nT
	public float[][] mul_mnT(float[][] m, float[][] n) {
		int ks = w(m);
		int height = h(m);
		int width = h(n);
		float[][] o = of(height, width);
		int i1, j1, k1;
		if (ks == w(n))
			for (int i0 = 0; i0 < height; i0 = i1) {
				i1 = Math.min(i0 + 64, height);
				for (int j0 = 0; j0 < width; j0 = j1) {
					j1 = Math.min(j0 + 64, width);
					for (int k0 = 0; k0 < ks; k0 = k1) {
						k1 = Math.min(k0 + 64, ks);
						for (int i = i0; i < i1; i++)
							for (int j = j0; j < j1; j++)
								for (int k = k0; k < k1; k++)
									o[i][j] += m[i][k] * n[j][k];
					}
				}
			}
		else
			throw new RuntimeException("wrong input sizes");
		return o;
	}

	// calculate mT * n
	public float[][] mul_mTn(float[][] m, float[][] n) {
		int ks = h(m);
		int height = w(m);
		int width = w(n);
		float[][] o = of(height, width);
		int i1, j1, k1;

		if (ks == h(n))
			for (int i0 = 0; i0 < height; i0 = i1) {
				i1 = Math.min(i0 + 64, height);
				for (int j0 = 0; j0 < width; j0 = j1) {
					j1 = Math.min(j0 + 64, width);
					for (int k0 = 0; k0 < ks; k0 = k1) {
						k1 = Math.min(k0 + 64, ks);
						for (int i = i0; i < i1; i++)
							for (int j = j0; j < j1; j++)
								for (int k = k0; k < k1; k++)
									o[i][j] += m[k][i] * n[k][j];
					}
				}
			}
		else
			throw new RuntimeException("wrong input sizes");

		return o;
	}

	public float[][] neg(float[][] m) {
		return negOn(copy(m));
	}

	public float[][] negOn(float[][] m) {
		int height = h(m);
		int width = w(m);
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				m[i][j] = -m[i][j];
		return m;
	}

	public float[] of(float[] m) {
		return copy(m);
	}

	public float[][] of(float[][] m) {
		return copy(m);
	}

	public float[][] of(int height, int width) {
		return new float[height][width];
	}

	public float[][] rotate(float angle) {
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		return new float[][] { { cos, -sin, }, { sin, cos, }, };
	}

	public float[][] rotateX(float angle) {
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		return new float[][] { { 0f, 0f, 0f, }, { 0f, cos, -sin, }, { 0f, sin, cos, }, };
	}

	public float[][] rotateY(float angle) {
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		return new float[][] { { cos, 0f, -sin, }, { 0f, 0f, 0f, }, { sin, 0f, cos, }, };
	}

	public float[][] rotateZ(float angle) {
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		return new float[][] { { cos, -sin, 0f, }, { sin, cos, 0f, }, { 0f, 0f, 0f, }, };
	}

	public float[] scale(float[] m, double d) {
		return scaleOn(copy(m), d);
	}

	public float[] scale(float[] m, float f) {
		return scaleOn(copy(m), f);
	}

	public float[][] scale(float[][] m, float f) {
		return scaleOn(copy(m), f);
	}

	public float[] scaleOn(float[] m, double d) {
		int length = m.length;
		for (int i = 0; i < length; i++)
			m[i] = (float) (m[i] * d);
		return m;
	}

	public float[] scaleOn(float[] m, float f) {
		int length = m.length;
		for (int i = 0; i < length; i++)
			m[i] *= f;
		return m;
	}

	public float[][] scaleOn(float[][] m, float f) {
		int height = h(m);
		int width = w(m);
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				m[i][j] *= f;
		return m;
	}

	public float[] sub(float[] m, float[] n) {
		return subOn(copy(m), n);
	}

	public float[] subOn(float[] m, float[] n) {
		int length = m.length;
		if (length == n.length)
			for (int i = 0; i < length; i++)
				m[i] -= n[i];
		else
			throw new RuntimeException("wrong input sizes");
		return m;
	}

	public void verifyEquals(float[] m0, float[] m1) {
		verifyEquals(m0, m1, MathUtil.epsilon);
	}

	public void verifyEquals(float[][] m0, float[][] m1) {
		verifyEquals(m0, m1, MathUtil.epsilon);
	}

	public void verifyEquals(float[] m0, float[] m1, float epsilon) {
		int length = m0.length;
		if (length == m1.length)
			for (int i = 0; i < length; i++)
				MathUtil.verifyEquals(m0[i], m1[i], epsilon);
		else
			throw new RuntimeException("wrong input sizes");
	}

	public void verifyEquals(float[][] m0, float[][] m1, float epsilon) {
		int height = h(m0);
		int width = w(m0);
		if (height == h(m1) && width == w(m1))
			for (int i = 0; i < height; i++)
				for (int j = 0; j < width; j++)
					MathUtil.verifyEquals(m0[i][j], m1[i][j], epsilon);
		else
			throw new RuntimeException("wrong input sizes");
	}

	private void swapRows(float[][] m, int row0, int row1) {
		float[] temp = m[row0];
		m[row0] = m[row1];
		m[row1] = temp;
	}

	public String toString(float[] m) {
		StringBuilder sb = new StringBuilder();
		dump(sb, m);
		return sb.toString();
	}

	public String toString(float[][] m) {
		StringBuilder sb = new StringBuilder();
		dump(sb, m);
		return sb.toString();
	}

	public float[][] transpose(float[][] m) {
		int height = h(m);
		int width = w(m);
		float[][] o = of(width, height);
		int i1, j1;
		for (int i0 = 0; i0 < height; i0 = i1) {
			i1 = Math.min(i0 + 64, height);
			for (int j0 = 0; j0 < width; j0 = j1) {
				j1 = Math.min(j0 + 64, width);
				for (int i = i0; i < i1; i++)
					for (int j = j0; j < j1; j++)
						o[j][i] = m[i][j];
			}
		}
		return o;
	}

	public int width(float[][] m) {
		return w(m);
	}

	private void mulRow(float[][] m, int row, float factor) {
		for (int col = 0; col < w(m); col++)
			m[row][col] *= factor;
	}

	private void addMultipliedRow(float[][] m, int sourceRow, float factor, int targetRow) {
		for (int col = 0; col < w(m); col++)
			m[targetRow][col] = m[targetRow][col] + factor * m[sourceRow][col];
	}

	private float[] copy(float[] m) {
		return Arrays.copyOf(m, m.length);
	}

	private float[][] copy(float[][] m0) {
		int height = h(m0);
		int width = w(m0);
		float[][] m1 = of(height, width);
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				m1[i][j] = m0[i][j];
		return m1;
	}

	private void dump(StringBuilder sb, float[][] m) {
		for (float[] row : m) {
			dump(sb, row);
			sb.append("\n");
		}
	}

	private void dump(StringBuilder sb, float[] m) {
		sb.append("[ ");
		for (float f : m)
			sb.append(To.string(f) + " ");
		sb.append("]");
	}

	private int h(float[][] m) {
		return m.length;
	}

	private int w(float[][] m) {
		return 0 < m.length ? m[0].length : 0;
	}

}
