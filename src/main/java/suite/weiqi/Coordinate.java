package suite.weiqi;

import java.util.ArrayList;
import java.util.List;

import suite.adt.IntIntPair;
import suite.util.Object_;

public class Coordinate implements Comparable<Coordinate> {

	public final List<Coordinate> leftOrUp = new ArrayList<>();
	public final List<Coordinate> neighbors = new ArrayList<>();

	private int index;

	private static Coordinate[][] coords;
	private static List<Coordinate> all = new ArrayList<>();

	static {
		initialize();
	}

	/**
	 * Performed when initializing, or after a board resize.
	 */
	public static void initialize() {
		coords = new Coordinate[Weiqi.size][Weiqi.size];
		all.clear();

		for (int x = 0; x < Weiqi.size; x++)
			for (int y = 0; y < Weiqi.size; y++)
				all.add(coords[x][y] = new Coordinate(x, y));

		for (int x = 0; x < Weiqi.size; x++)
			for (int y = 0; y < Weiqi.size; y++) {
				Coordinate c0 = coords[x][y];

				if (0 < x) {
					Coordinate c1 = coords[x - 1][y];
					c0.leftOrUp.add(c1);
					c0.neighbors.add(c1);
					c1.neighbors.add(c0);
				}

				if (0 < y) {
					Coordinate c2 = coords[x][y - 1];
					c0.leftOrUp.add(c2);
					c0.neighbors.add(c2);
					c2.neighbors.add(c0);
				}
			}
	}

	public static Iterable<Coordinate> all() {
		return all;
	}

	public static Coordinate c(int x, int y) {
		return coords[x][y];
	}

	private Coordinate(int x, int y) {
		index = (x << Weiqi.shift) + y;
	}

	public int getX() {
		return getLocation().t0;
	}

	public int getY() {
		return getLocation().t1;
	}

	@Override
	public int compareTo(Coordinate coord) {
		return index - coord.index;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Coordinate.class) {
			Coordinate c = (Coordinate) object;
			return index == c.index;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return index;
	}

	@Override
	public String toString() {
		IntIntPair location = getLocation();
		return String.format("%d,%d", location.t0, location.t1);
	}

	public int index() {
		return index;
	}

	private IntIntPair getLocation() {
		int x = index >> Weiqi.shift;
		int y = index & (1 << Weiqi.shift) - 1;
		return IntIntPair.of(x, y);
	}

}
