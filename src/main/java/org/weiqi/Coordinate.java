package org.weiqi;

import java.util.ArrayList;
import java.util.List;

public class Coordinate implements Comparable<Coordinate> {

	private final int x, y;
	private final int index;
	private final List<Coordinate> leftOrUp = new ArrayList<>();
	private final List<Coordinate> neighbors = new ArrayList<>();

	private static Coordinate coords[][];
	private static final List<Coordinate> all = new ArrayList<>();

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

				if (x > 0) {
					Coordinate c1 = coords[x - 1][y];
					c0.leftOrUp.add(c1);
					c0.neighbors.add(c1);
					c1.neighbors.add(c0);
				}

				if (y > 0) {
					Coordinate c2 = coords[x][y - 1];
					c0.leftOrUp.add(c2);
					c0.neighbors.add(c2);
					c2.neighbors.add(c0);
				}
			}
	}

	private Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
		index = (x << Weiqi.shift) + y;
	}

	public static Coordinate c(int x, int y) {
		return coords[x][y];
	}

	public int index() {
		return index;
	}

	public Iterable<Coordinate> leftOrUp() {
		return leftOrUp;
	}

	public Iterable<Coordinate> neighbors() {
		return neighbors;
	}

	public static Iterable<Coordinate> all() {
		return all;
	}

	@Override
	public int hashCode() {
		return index;
	}

	@Override
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
		return (dx << Weiqi.shift) + dy;
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
