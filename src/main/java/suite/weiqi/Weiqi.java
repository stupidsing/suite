package suite.weiqi;

import java.util.Arrays;

import suite.util.Util;

public class Weiqi {

	public static int size;
	public static int area;

	public static int shift; // SIZE <= 2^SHIFT

	static {
		initialize();
	}

	public static void initialize() {
		adjustSize(19);
	}

	public static void adjustSize(int s) {
		size = s;
		area = s * s;

		shift = 0;
		while (1 << ++shift < size)
			;

		Coordinate.initialize();
	}

	public enum Occupation {
		EMPTY, BLACK, WHITE;

		public Occupation opponent() {
			switch (this) {
			case BLACK:
				return WHITE;
			case WHITE:
				return BLACK;
			default:
				return this;
			}
		}

		public String display() {
			switch (this) {
			case BLACK:
				return "X";
			case WHITE:
				return "O";
			default:
				return ".";
			}
		}
	}

	public static Occupation players[] = { Occupation.BLACK, Occupation.WHITE };

	/**
	 * A generic board type.
	 */
	public static class Array<T> {
		@SuppressWarnings("unchecked")
		private T positions[] = (T[]) new Object[size << shift];

		public Array() {
		}

		public Array(Array<T> array) {
			System.arraycopy(array.positions, 0, positions, 0, size << shift);
		}

		public static <T1> Array<T1> create() {
			return new Array<>();
		}

		public void dump() {
			for (int x = 0; x < Weiqi.size; x++) {
				for (int y = 0; y < Weiqi.size; y++) {
					Coordinate c = Coordinate.c(x, y);
					System.out.print(get(c) + " ");
				}

				System.out.println();
			}
		}

		@Override
		public boolean equals(Object object) {
			if (Util.clazz(object) == Array.class) {
				Array<?> array = (Array<?>) object;
				return Arrays.equals(positions, array.positions);
			} else
				return false;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(positions);
		}

		public void set(Coordinate c, T t) {
			positions[c.index()] = t;
		}

		public T get(Coordinate c) {
			return positions[c.index()];
		}
	}

}
