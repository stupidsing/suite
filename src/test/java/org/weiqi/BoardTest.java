package org.weiqi;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.util.Util;
import org.weiqi.Weiqi.Occupation;

public class BoardTest {

	@Test
	public void testNeighbour() {
		Coordinate c = new Coordinate(10, 10);
		Set<Coordinate> neighbours = Util.createHashSet();

		for (Coordinate c1 : c.neighbours())
			neighbours.add(c1);

		assertEquals(new HashSet<Coordinate>( //
				Arrays.asList( //
						new Coordinate(9, 10) //
						, new Coordinate(11, 10) //
						, new Coordinate(10, 9) //
						, new Coordinate(10, 11) //
				)), neighbours);
	}

	@Test
	public void testEat() {
		Board board = new Board();
		board.move(new Coordinate(0, 1), Occupation.BLACK);
		board.move(new Coordinate(0, 0), Occupation.WHITE);
		board.move(new Coordinate(1, 0), Occupation.BLACK);
		assertEquals(board.get(new Coordinate(0, 0)), Occupation.EMPTY);
	}

	@Test
	public void testGroupAnalysis() {
		Board board = blackBoard();
		board.set(new Coordinate(3, 3), Occupation.EMPTY);
		board.set(new Coordinate(3, 4), Occupation.EMPTY);
		board.set(new Coordinate(7, 3), Occupation.EMPTY);
		board.set(new Coordinate(18, 0), Occupation.EMPTY);
		board.set(new Coordinate(17, 1), Occupation.EMPTY);
		board.set(new Coordinate(17, 0), Occupation.WHITE);
		board.set(new Coordinate(18, 1), Occupation.WHITE);

		GroupAnalysis ga = new GroupAnalysis(board);
		Integer groupId = ga.getGroupId(new Coordinate(15, 15));
		assertEquals(4, ga.getNumberOfBreathes(groupId));
	}

	private Board blackBoard() {
		Board board = new Board();
		for (Coordinate c : Coordinate.all())
			board.set(c, Occupation.BLACK);
		return board;
	}

}
