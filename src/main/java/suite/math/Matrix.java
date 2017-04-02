package suite.math;

import java.util.Arrays;

import suite.util.Util;

public class Matrix {

	public final float v[][];

	public Matrix(Matrix m) {
		this(m.height(), m.width());
		for (int i = 0; i < height(); i++)
			for (int j = 0; j < width(); j++)
				v[i][j] = m.v[i][j];
	}

	public Matrix(int height, int width) {
		this(new float[height][width]);
	}

	public Matrix(float v[][]) {
		this.v = v;
	}

	public static Matrix rotate(float angle) {
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		return new Matrix(new float[][] { { cos, -sin, }, { sin, cos, }, });
	}

	public static Matrix rotateX(float angle) {
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		return new Matrix(new float[][] { { 0f, 0f, 0f, }, { 0f, cos, -sin, }, { 0f, sin, cos, }, });
	}

	public static Matrix rotateY(float angle) {
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		return new Matrix(new float[][] { { cos, 0f, -sin, }, { 0f, 0f, 0f, }, { sin, 0f, cos, }, });
	}

	public static Matrix rotateZ(float angle) {
		float sin = (float) Math.sin(angle);
		float cos = (float) Math.cos(angle);
		return new Matrix(new float[][] { { cos, -sin, 0f, }, { sin, cos, 0f, }, { 0f, 0f, 0f, }, });
	}

	public static Matrix add(Matrix m, Matrix n) {
		int height = m.height(), width = m.width();

		if (height == n.height() && width == n.width()) {
			Matrix o = new Matrix(height, width);

			for (int i = 0; i < height; i++)
				for (int j = 0; j < width; j++)
					o.v[i][j] = m.v[i][j] + n.v[i][j];

			return o;
		} else
			throw new RuntimeException("Wrong matrix sizes");
	}

	public static Matrix neg(Matrix m) {
		int height = m.height(), width = m.width();
		Matrix o = new Matrix(height, width);

		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				o.v[i][j] = -m.v[i][j];

		return o;
	}

	public static Matrix transpose(Matrix m) {
		int height = m.height(), width = m.width();
		Matrix o = new Matrix(width, height);

		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				o.v[j][i] = m.v[i][j];

		return o;
	}

	public static Matrix mul(Matrix m, float f) {
		int height = m.height(), width = m.width();
		Matrix m1 = new Matrix(height, width);

		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				m1.v[i][j] = m.v[i][j] * f;

		return m1;
	}

	public static Vector mul(Matrix m, Vector v) {
		if (m.height() == 3 && m.width() == 3) {
			float x1 = m.v[0][0] * v.x + m.v[0][1] * v.y + m.v[0][2] * v.z;
			float y1 = m.v[1][0] * v.x + m.v[1][1] * v.y + m.v[1][2] * v.z;
			float z1 = m.v[2][0] * v.x + m.v[2][1] * v.y + m.v[2][2] * v.z;
			return new Vector(x1, y1, z1);
		} else
			throw new RuntimeException("Wrong matrix size");
	}

	// calculate mT * n
	public static Matrix transposeMul(Matrix m, Matrix n) {
		int ks = m.height();

		if (ks == n.height()) {
			int height = m.width();
			int width = n.width();
			Matrix o = new Matrix(height, width);
			int i1, j1, k1;

			for (int i0 = 0; i0 < height; i0 = i1) {
				i1 = Math.min(i0 + 64, height);
				for (int j0 = 0; j0 < width; j0 = j1) {
					j1 = Math.min(j0 + 64, width);
					for (int k0 = 0; k0 < ks; k0 = k1) {
						k1 = Math.min(k0 + 64, ks);
						for (int i = i0; i < j1; i++)
							for (int j = j0; j < j1; j++)
								for (int k = k0; k < k1; k++)
									o.v[i][j] += m.v[k][i] * n.v[k][j];
					}
				}
			}

			return o;
		} else
			throw new RuntimeException("Wrong matrix sizes");
	}

	public static Matrix mul(Matrix m, Matrix n) {
		int ks = m.width();

		if (ks == n.height()) {
			int height = m.height();
			int width = n.width();
			Matrix o = new Matrix(height, width);
			int i1, j1, k1;

			for (int i0 = 0; i0 < height; i0 = i1) {
				i1 = Math.min(i0 + 64, height);
				for (int j0 = 0; j0 < width; j0 = j1) {
					j1 = Math.min(j0 + 64, width);
					for (int k0 = 0; k0 < ks; k0 = k1) {
						k1 = Math.min(k0 + 64, ks);
						for (int i = i0; i < j1; i++)
							for (int j = j0; j < j1; j++)
								for (int k = k0; k < k1; k++)
									o.v[i][j] += m.v[i][k] * n.v[k][j];
					}
				}
			}

			return o;
		} else
			throw new RuntimeException("Wrong matrix sizes");
	}

	public static Matrix convolute(Matrix m, Matrix k) {
		int kh = k.height(), kw = k.width();
		int h1 = m.height() - kh + 1;
		int w1 = m.width() - kw + 1;
		Matrix o = new Matrix(h1, w1);

		for (int i = 0; i < h1; i++)
			for (int j = 0; j < w1; j++) {
				float sum = 0;
				for (int di = 0; di < kh; di++)
					for (int dj = 0; dj < kw; dj++)
						sum += m.v[i + di][j + dj] * k.v[di][dj];
				o.v[i][j] = sum;
			}

		return o;
	}

	/**
	 * Calculates matric inverse by Gaussian-Jordan elimination.
	 */
	public static Matrix inverse(Matrix m0) {
		Matrix m = new Matrix(m0); // do not alter input matrix
		int size = m.height();

		if (size != m.width())
			throw new RuntimeException("Wrong matrix size");

		Matrix n = identity(size);

		for (int r = 0; r < size; r++) {
			int c = r;

			for (; c < size; c++)
				if (m.v[c][r] != 0f)
					break;

			if (c == size)
				throw new RuntimeException("No inverse exists");

			if (r != c) {
				swapRows(m, r, c);
				swapRows(n, r, c);
			}

			float factor = 1f / m.v[r][r];
			mulRow(m, r, factor);
			mulRow(n, r, factor);

			for (int r1 = 0; r1 < size; r1++)
				if (r != r1) {
					float factor1 = -m.v[r1][r];
					addMultipliedRow(m, r, factor1, r1);
					addMultipliedRow(n, r, factor1, r1);
				}
		}

		return n;
	}

	private static void swapRows(Matrix m, int row0, int row1) {
		float temp[] = m.v[row0];
		m.v[row0] = m.v[row1];
		m.v[row1] = temp;
	}

	private static void mulRow(Matrix m, int row, float factor) {
		for (int col = 0; col < m.width(); col++)
			m.v[row][col] *= factor;
	}

	private static void addMultipliedRow(Matrix m, int sourceRow, float factor, int targetRow) {
		for (int col = 0; col < m.width(); col++)
			m.v[targetRow][col] = m.v[targetRow][col] + factor * m.v[sourceRow][col];
	}

	public static Matrix identity(int size) {
		Matrix n = new Matrix(size, size);

		for (int r = 0; r < size; r++)
			n.v[r][r] = 1f;
		return n;
	}

	public int height() {
		return v.length;
	}

	public int width() {
		return v[0].length;
	}

	@Override
	public boolean equals(Object object) {
		return Util.clazz(object) == Matrix.class && Arrays.deepEquals(v, ((Matrix) object).v);
	}

	@Override
	public int hashCode() {
		return Arrays.deepHashCode(v);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (float row[] : v) {
			sb.append("[");
			for (float f : row)
				sb.append(f + " ");
			sb.append("]\n");
		}

		return sb.toString();
	}

}
