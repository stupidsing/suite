package suite.math;

import java.util.Arrays;

public class Matrix {

	private final float v[][];

	public Matrix(float v[][]) {
		this.v = v;
	}

	public static Matrix add(Matrix m, Matrix n) {
		int height = m.height();
		int width = m.width();

		if (height == n.height() && width == n.width()) {
			Matrix o = new Matrix(new float[height][width]);

			for (int i = 0; i < height; i++)
				for (int j = 0; j < width; j++)
					o.v[i][j] = m.v[i][j] + n.v[i][j];

			return o;
		} else
			throw new RuntimeException("Wrong matrix sizes");
	}

	public static Matrix neg(Matrix m) {
		int height = m.height();
		int width = m.width();
		Matrix o = new Matrix(new float[height][width]);

		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				o.v[i][j] = -m.v[i][j];

		return o;
	}

	public static Matrix transpose(Matrix m) {
		int height = m.height();
		int width = m.width();
		Matrix o = new Matrix(new float[width][height]);

		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				o.v[j][i] = m.v[i][j];

		return o;
	}

	public static Vector mul(Matrix m, Vector v) {
		if (m.height() == 3 && m.width() == 3) {
			float x1 = m.v[0][0] * v.getX() + m.v[0][1] * v.getY() + m.v[0][2] * v.getZ();
			float y1 = m.v[1][0] * v.getX() + m.v[1][1] * v.getY() + m.v[1][2] * v.getZ();
			float z1 = m.v[2][0] * v.getX() + m.v[2][1] * v.getY() + m.v[2][2] * v.getZ();
			return new Vector(x1, y1, z1);
		} else
			throw new RuntimeException("Wrong matrix size");
	}

	public static Matrix mul(Matrix m, Matrix n) {
		int ks = m.width();

		if (ks == n.height()) {
			Matrix o = new Matrix(new float[m.height()][n.width()]);

			for (int i = 0; i < m.height(); i++)
				for (int j = 0; j < n.width(); j++)
					for (int k = 0; k < ks; k++)
						o.v[i][j] += m.v[i][k] * n.v[k][j];

			return o;
		} else
			throw new RuntimeException("Wrong matrix sizes");
	}

	/**
	 * Calculates matric inverse by Gaussian-Jordan elimination.
	 */
	public static Matrix inverse(Matrix m) {
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
		Matrix n = new Matrix(new float[size][size]);

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

	public float[][] get() {
		return v;
	}

	@Override
	public int hashCode() {
		return Arrays.deepHashCode(v);
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof Matrix && Arrays.deepEquals(v, ((Matrix) object).v);
	}

}
