package org.weiqi;

import java.util.ArrayList;
import java.util.List;

public class Coordinate implements Comparable<Coordinate> {

	private final int x, y;
	private final List<Coordinate> neighbours = new ArrayList<>();

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
					c0.neighbours.add(c1);
					c1.neighbours.add(c0);
				}

				if (y > 0) {
					Coordinate c2 = coords[x][y - 1];
					c0.neighbours.add(c2);
					c2.neighbours.add(c0);
				}
			}
	}

	private Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public static Coordinate c(int x, int y) {
		return coords[x][y];
	}

	public int getArrayPosition() {
		return (x << Weiqi.shift) + y;
	}

	public static Coordinate fromArrayPosition(int position) {
		int x = position >> Weiqi.shift;
		int y = position & ((1 << Weiqi.shift) - 1);
		return c(x, y);
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
		return neighbours;
	}

	public static Iterable<Coordinate> all() {
		return all;
	}

	@Override
	public int hashCode() {
		return getArrayPosition();
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
