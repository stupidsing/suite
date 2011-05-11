package org.weiqi;

import java.util.Iterator;

public class Coordinate implements Comparable<Coordinate> {

	private int x, y;

	private final static Coordinate coords[][] = new Coordinate[Weiqi.SIZE][Weiqi.SIZE];
	static {
		for (int x = 0; x < Weiqi.SIZE; x++)
			for (int y = 0; y < Weiqi.SIZE; y++)
				coords[x][y] = new Coordinate(x, y);
	}

	private Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public static Coordinate c(int x, int y) {
		return coords[x][y];
	}

	public int getArrayPosition() {
		return (x << Weiqi.SHIFT) + y;
	}

	public static Coordinate fromArrayPosition(int position) {
		int x = position >> Weiqi.SHIFT;
		int y = position & ((1 << Weiqi.SHIFT) - 1);
		return coords[x][y];
	}

	public Coordinate[] leftOrUp() {
		Coordinate left = x > 0 ? Coordinate.c(x - 1, y) : null;
		Coordinate up = y > 0 ? Coordinate.c(x, y - 1) : null;

		if (left == null || up == null)
			if (left != null)
				return new Coordinate[] { left };
			else if (up != null)
				return new Coordinate[] { up };
			else
				return new Coordinate[] {};
		else
			return new Coordinate[] { left, up };
	}

	public Iterable<Coordinate> neighbours() {
		return new Iterable<Coordinate>() {
			public Iterator<Coordinate> iterator() {
				return new Iterator<Coordinate>() {
					public int n = 0;

					public boolean hasNext() {
						return n < (y == 0 ? 3 : 4);
					}

					public Coordinate next() {
						switch (n++) {
						case 0:
							if (x < Weiqi.SIZE - 1)
								return Coordinate.c(x + 1, y);
						case 1:
							if (x > 0)
								return Coordinate.c(x - 1, y);
						case 2:
							if (y < Weiqi.SIZE - 1)
								return Coordinate.c(x, y + 1);
						case 3:
							if (y > 0)
								return Coordinate.c(x, y - 1);
						default:
							throw new RuntimeException("Run out of neighbours");
						}
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	public static Iterable<Coordinate> all() {
		return new Iterable<Coordinate>() {
			public Iterator<Coordinate> iterator() {
				return new Iterator<Coordinate>() {
					public Coordinate c = new Coordinate(0, 0);

					public boolean hasNext() {
						return c.x < Weiqi.SIZE && c.y < Weiqi.SIZE;
					}

					public Coordinate next() {
						Coordinate ret = Coordinate.c(c.x, c.y);
						c.y++;
						if (c.y == Weiqi.SIZE) {
							c.x++;
							c.y = 0;
						}
						return ret;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	@Override
	public int hashCode() {
		return getArrayPosition();
	}

	public boolean equals(Object object) {
		if (object instanceof Coordinate) {
			Coordinate c = (Coordinate) object;
			return x == c.x && y == c.y;
		} else
			return false;
	}

	@Override
	public int compareTo(Coordinate coord) {
		int dx = x - coord.x;
		int dy = y - coord.y;
		return (dx << Weiqi.SHIFT) + dy;
	}

	@Override
	public String toString() {
		return String.format("%d,%d", x, y);
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

}
