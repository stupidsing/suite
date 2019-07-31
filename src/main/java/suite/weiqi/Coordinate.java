package suite.weiqi;

import java.util.ArrayList;
import java.util.List;

import primal.Ob;
import suite.primitive.Coord;

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

		for (var x = 0; x < Weiqi.size; x++)
			for (var y = 0; y < Weiqi.size; y++)
				all.add(coords[x][y] = new Coordinate(x, y));

		for (var x = 0; x < Weiqi.size; x++)
			for (var y = 0; y < Weiqi.size; y++) {
				var c0 = coords[x][y];

				if (0 < x) {
					var c1 = coords[x - 1][y];
					c0.leftOrUp.add(c1);
					c0.neighbors.add(c1);
					c1.neighbors.add(c0);
				}

				if (0 < y) {
					var c2 = coords[x][y - 1];
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
		return getLocation().x;
	}

	public int getY() {
		return getLocation().y;
	}

	@Override
	public int compareTo(Coordinate coord) {
		return index - coord.index;
	}

	@Override
	public boolean equals(Object object) {
		if (Ob.clazz(object) == Coordinate.class) {
			var c = (Coordinate) object;
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
		var location = getLocation();
		return String.format("%d,%d", location.x, location.y);
	}

	public int index() {
		return index;
	}

	private Coord getLocation() {
		var x = index >> Weiqi.shift;
		var y = index & (1 << Weiqi.shift) - 1;
		return Coord.of(x, y);
	}

}
