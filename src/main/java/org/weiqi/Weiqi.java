package org.weiqi;

public class Weiqi {

	public final static int SIZE = 19;

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
	};

	public static Occupation players[] = { Occupation.BLACK, Occupation.WHITE };

	/**
	 * A generic board type.
	 */
	public static class Array<T> {
		@SuppressWarnings("unchecked")
		// JDK bug
		private T positions[] = (T[]) new Object[Weiqi.SIZE * Weiqi.SIZE];

		public Array() {
		}

		public Array(Array<T> array) {
			System.arraycopy( //
					array.positions, 0, positions, 0, Weiqi.SIZE * Weiqi.SIZE);
		}

		public static <T1> Array<T1> create() {
			return new Array<T1>();
		}

		public void set(Coordinate c, T t) {
			positions[c.getArrayPosition()] = t;
		}

		public T get(Coordinate c) {
			return positions[c.getArrayPosition()];
		}
	}

}
