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

	/**
	 * A generic board type.
	 */
	public static class Array<T> {
		@SuppressWarnings("unchecked")
		// JDK bug
		private T position[][] = (T[][]) new Object[Weiqi.SIZE][Weiqi.SIZE];

		public void set(Coordinate c, T t) {
			position[c.x][c.y] = t;
		}

		public T get(Coordinate c) {
			return position[c.x][c.y];
		}
	}

}
