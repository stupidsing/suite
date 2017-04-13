package suite.math;

import java.util.Arrays;

public class Matrix {

	public static float[] add(float[] m, float[] n) {
		return addOn(clone(m), n);
	}

	public static float[][] add(float[][] m, float[][] n) {
		return addOn(clone(m), n);
	}

	public static float[] addOn(float[] m, float[] n) {
		int length = m.length;
		if (length == n.length)
			for (int i = 0; i < length; i++)
				m[i] += n[i];
		else
			throw new RuntimeException("Wrong matrix sizes");
		return m;
	}

	public static float[][] addOn(float[][] m, float[][] n) {
		int height = h(m);
		int width = w(m);
		if (height == h(n) && width == w(n))
			for (int i = 0; i < height; i++)
				for (int j = 0; j < width; j++)
					m[i][j] += n[i][j];
		else
			throw new RuntimeException("Wrong matrix sizes");
		return m;
	}

	public static float[] addScaleOn(float[] m, float[] n, float f) {
		int length = m.length;
		if (length == n.length)
			for (int i = 0; i < length; i++)
				m[i] += n[i] * f;
		else
			throw new RuntimeException("Wrong matrix sizes");
		return m;
	}

	public static float[][] convolute(float[][] m, float[][] k) {
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

	public static float dot(float[] m, float[] n) {
		int length = m.length;
		float sum = 0;
		if (length == n.length)
			for (int i = 0; i < length; i++)
				sum += m[i] * n[i];
		else
			throw new RuntimeException("Wrong matrix sizes");
		return sum;
	}

	public static boolean equals(float[][] m, float[][] n) {
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

	public static int hashCode(float[][] m) {
		int hashCode = 0;
		for (float[] row : m)
			hashCode = hashCode * 31 + Arrays.hashCode(row);
		return hashCode;
	}

	public static int height(float[][] m) {
		return h(m);
	}

	public static float[][] identity(int size) {
		float[][] m = of(size, size);
		for (int r = 0; r < size; r++)
			m[r][r] = 1f;
		return m;
	}

	/**
	 * Calculates matric inverse by Gaussian-Jordan elimination.
	 */
	public static float[][] inverse(float[][] m0) {
		float[][] m = of(m0); // do not alter input matrix
		int size = h(m);

		if (size != w(m))
			throw new RuntimeException("Wrong matrix size");

		float[][] n = identity(size);

		for (int r = 0; r < size; r++) {
			int c = r;

			for (; c < size; c++)
				if (m[c][r] != 0f)
					break;

			if (c == size)
				throw new RuntimeException("No inverse exists");

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

	public static float[] mul(float[] m, float[][] n) {
		int ks = m.length;
		int width = w(n);
		float[] o = new float[width];
		int j1, k1;
		if (ks == h(n))
			for (int j0 = 0; j0 < width; j0 = j1) {
				j1 = Math.min(j0 + 64, width);
				for (int k0 = 0; k0 < ks; k0 = k1) {
					k1 = Math.min(k0 + 64, ks);
					for (int j = j0; j < j1; j++)
						for (int k = k0; k < k1; k++)
							o[j] += m[k] * n[k][j];
				}
			}
		else
			throw new RuntimeException("Wrong matrix sizes");
		return o;
	}

	// nT is column vector
	public static float[] mul(float[][] m, float[] nT) {
		int ks = w(m);
		int height = h(m);
		float[] o = new float[height];
		int i1, k1;
		if (ks == nT.length)
			for (int i0 = 0; i0 < height; i0 = i1) {
				i1 = Math.min(i0 + 64, height);
				for (int k0 = 0; k0 < ks; k0 = k1) {
					k1 = Math.min(k0 + 64, ks);
					for (int i = i0; i < i1; i++)
						for (int k = k0; k < k1; k++)
							o[i] += m[i][k] * nT[k];
				}
			}
		else
			throw new RuntimeException("Wrong matrix sizes");
		return o;
	}

	public static float[][] mul(float[][] m, float[][] n) {
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
			throw new RuntimeException("Wrong matrix sizes");

		return o;
	}

	public static Vector mul(float[][] m, Vector v) {
		if (h(m) == 3 && w(m) == 3) {
			float x1 = m[0][0] * v.x + m[0][1] * v.y + m[0][2] * v.z;
			float y1 = m[1][0] * v.x + m[1][1] * v.y + m[1][2] * v.z;
			float z1 = m[2][0] * v.x + m[2][1] * v.y + m[2][2] * v.z;
			return new Vector(x1, y1, z1);
		} else
			throw new RuntimeException("Wrong matrix size");
	}

	// calculate m * nT
	public static float[][] mul_mnT(float[][] m, float[][] n) {
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
			throw new RuntimeException("Wrong matrix sizes");
		return o;
	}

	// calculate mT * n
	public static float[][] mul_mTn(float[][] m, float[][] n) {
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
			throw new RuntimeException("Wrong matrix sizes");

		return o;
	}

	public static float[][] neg(float[][] m) {
		return negOn(clone(m));
	}

	public static float[][] negOn(float[][] m) {
		int height = h(m);
		int width = w(m);
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				m[i][j] = -m[i][j];
		return m;
	}

	public static float[] of(float[] m) {
		return clone(m);
	}

	public static float[][] of(float[][] m) {
		return clone(m);
	}

	public static float[][] of(int height, int width) {
		return new float[height][width];
	}

	public static float[][] rotate(float angle) {
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		return new float[][] { { cos, -sin, }, { sin, cos, }, };
	}

	public static float[][] rotateX(float angle) {
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		return new float[][] { { 0f, 0f, 0f, }, { 0f, cos, -sin, }, { 0f, sin, cos, }, };
	}

	public static float[][] rotateY(float angle) {
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		return new float[][] { { cos, 0f, -sin, }, { 0f, 0f, 0f, }, { sin, 0f, cos, }, };
	}

	public static float[][] rotateZ(float angle) {
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		return new float[][] { { cos, -sin, 0f, }, { sin, cos, 0f, }, { 0f, 0f, 0f, }, };
	}

	public static float[] scale(float[] m, double d) {
		return scaleOn(clone(m), d);
	}

	public static float[] scale(float[] m, float f) {
		return scaleOn(clone(m), f);
	}

	public static float[][] scale(float[][] m, float f) {
		return scaleOn(clone(m), f);
	}

	public static float[] scaleOn(float[] m, double d) {
		int length = m.length;
		for (int i = 0; i < length; i++)
			m[i] = (float) (m[i] * d);
		return m;
	}

	public static float[] scaleOn(float[] m, float f) {
		int length = m.length;
		for (int i = 0; i < length; i++)
			m[i] *= f;
		return m;
	}

	public static float[][] scaleOn(float[][] m, float f) {
		int height = h(m);
		int width = w(m);
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				m[i][j] *= f;
		return m;
	}

	public static float[] sub(float[] m, float[] n) {
		return subOn(clone(m), n);
	}

	public static float[] subOn(float[] m, float[] n) {
		int length = m.length;
		if (length == n.length)
			for (int i = 0; i < length; i++)
				m[i] -= n[i];
		else
			throw new RuntimeException("Wrong matrix sizes");
		return m;
	}

	public static void verifyEquals(float[][] m0, float[][] m1) {
		int height = h(m0);
		int width = w(m0);
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				MathUtil.verifyEquals(m0[i][j], m1[i][j]);
	}

	private static void swapRows(float[][] m, int row0, int row1) {
		float[] temp = m[row0];
		m[row0] = m[row1];
		m[row1] = temp;
	}

	public static String toString(float[][] m) {
		StringBuilder sb = new StringBuilder();

		for (float[] row : m) {
			sb.append("[");
			for (float f : row)
				sb.append(f + " ");
			sb.append("]\n");
		}

		return sb.toString();
	}

	public static float[][] transpose(float[][] m) {
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

	public static int width(float[][] m) {
		return w(m);
	}

	private static void mulRow(float[][] m, int row, float factor) {
		for (int col = 0; col < w(m); col++)
			m[row][col] *= factor;
	}

	private static void addMultipliedRow(float[][] m, int sourceRow, float factor, int targetRow) {
		for (int col = 0; col < w(m); col++)
			m[targetRow][col] = m[targetRow][col] + factor * m[sourceRow][col];
	}

	private static float[] clone(float[] m) {
		return Arrays.copyOf(m, m.length);
	}

	private static float[][] clone(float[][] m0) {
		int height = h(m0);
		int width = w(m0);
		float[][] m1 = of(height, width);
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				m1[i][j] = m0[i][j];
		return m1;
	}

	private static int h(float[][] m) {
		return m.length;
	}

	private static int w(float[][] m) {
		return 0 < m.length ? m[0].length : 0;
	}

}
