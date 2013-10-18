package suite.math;

public class Matrix {

	private final float v[][];

	public Matrix(float v[][]) {
		this.v = v;
	}

	public static Matrix add(Matrix m, Matrix n) {
		int height = m.height();
		int width = m.width();

		if (height == n.height() && width == n.width()) {
			float o[][] = new float[height][width];

			for (int i = 0; i < height; i++)
				for (int j = 0; j < width; j++)
					o[i][j] = m.v[i][j] + n.v[i][j];

			return new Matrix(o);
		} else
			throw new RuntimeException("Incompatible matrix size");
	}

	public static Matrix neg(Matrix m) {
		int height = m.height();
		int width = m.width();
		float o[][] = new float[height][width];

		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				o[i][j] = -m.v[i][j];

		return new Matrix(o);
	}

	public static Matrix transpose(Matrix m) {
		int height = m.height();
		int width = m.width();
		float o[][] = new float[width][height];

		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				o[j][i] = m.v[i][j];

		return new Matrix(o);
	}

	public static Matrix mul(Matrix m, Matrix n) {
		int ks = m.width();

		if (ks == n.height()) {
			float o[][] = new float[m.height()][n.width()];

			for (int i = 0; i < m.height(); i++)
				for (int j = 0; j < n.width(); j++)
					for (int k = 0; k < ks; k++)
						o[i][j] += m.v[i][k] * n.v[k][j];

			return new Matrix(o);
		} else
			throw new RuntimeException("Incompatible matrix size");
	}

	public int height() {
		return v.length;
	}

	public int width() {
		return v[0].length;
	}

}
